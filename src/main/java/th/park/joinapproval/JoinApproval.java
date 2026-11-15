package th.park.joinapproval;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * When a non-whitelisted player tries to join, ask the approver(s) in chat.
 * Approver replies "Y" to whitelist the oldest pending player, "N" to reject.
 */
public final class JoinApproval extends JavaPlugin implements Listener {

    private static final long REQUEST_TTL_MS = 10 * 60 * 1000L;

    private record Request(UUID uuid, String name, long createdAt) {}

    // Insertion-ordered so Y/N always answers the oldest request.
    private final Map<UUID, Request> pending = new LinkedHashMap<>();
    private List<String> approvers;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        approvers = getConfig().getStringList("approvers").stream().map(String::toLowerCase).toList();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Approvers: " + approvers);
    }

    private boolean isApprover(Player p) {
        return approvers.contains(p.getName().toLowerCase());
    }

    private synchronized void expireOld() {
        long now = System.currentTimeMillis();
        pending.values().removeIf(r -> now - r.createdAt() > REQUEST_TTL_MS);
    }

    private synchronized Request oldest() {
        expireOld();
        Iterator<Request> it = pending.values().iterator();
        return it.hasNext() ? it.next() : null;
    }

    private void notifyApprovers(Request r) {
        String msg = ChatColor.YELLOW + "[ขอเข้าเซิร์ฟ] " + ChatColor.WHITE + r.name()
                + ChatColor.YELLOW + " ขอเข้าเซิร์ฟเวอร์ ให้ผ่านมั้ย? พิมพ์ "
                + ChatColor.GREEN + "Y" + ChatColor.YELLOW + " หรือ " + ChatColor.RED + "N";
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isApprover(p)) p.sendMessage(msg);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        if (e.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        if (!Bukkit.hasWhitelist()) return;
        OfflinePlayer op = Bukkit.getOfflinePlayer(e.getUniqueId());
        if (op.isWhitelisted() || op.isOp()) return;

        Request r = new Request(e.getUniqueId(), e.getName(), System.currentTimeMillis());
        boolean isNew;
        synchronized (this) {
            expireOld();
            isNew = !pending.containsKey(r.uuid());
            if (isNew) pending.put(r.uuid(), r);
        }
        e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                "ส่งคำขอเข้าเซิร์ฟเวอร์แล้ว\nรอแอดมินอนุมัติ แล้วเข้าใหม่อีกครั้ง");
        if (isNew) {
            getLogger().info("Join request from " + r.name() + " (" + r.uuid() + ")");
            Bukkit.getScheduler().runTask(this, () -> notifyApprovers(r));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!isApprover(p)) return;
        Request r = oldest();
        if (r == null) return;
        int count;
        synchronized (this) { count = pending.size(); }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            p.sendMessage(ChatColor.YELLOW + "มีคำขอเข้าเซิร์ฟค้างอยู่ " + count + " คน");
            notifyApprovers(r);
        }, 40L);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        if (!isApprover(p)) return;
        String text = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        boolean yes = text.equalsIgnoreCase("y");
        boolean no = text.equalsIgnoreCase("n");
        if (!yes && !no) return;

        Request r;
        synchronized (this) {
            r = oldest();
            if (r != null) pending.remove(r.uuid());
        }
        if (r == null) return; // no request pending: let the message through as normal chat
        e.setCancelled(true);

        Bukkit.getScheduler().runTask(this, () -> {
            if (yes) {
                Bukkit.getOfflinePlayer(r.uuid()).setWhitelisted(true);
                Bukkit.reloadWhitelist();
                getLogger().info(p.getName() + " approved " + r.name() + " (" + r.uuid() + ")");
                p.sendMessage(ChatColor.GREEN + "อนุมัติ " + r.name() + " แล้ว ให้เขาเข้าใหม่ได้เลย");
            } else {
                getLogger().info(p.getName() + " rejected " + r.name() + " (" + r.uuid() + ")");
                p.sendMessage(ChatColor.RED + "ปฏิเสธ " + r.name() + " แล้ว");
            }
            Request next = oldest();
            if (next != null) notifyApprovers(next);
        });
    }
}
