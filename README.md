# JoinApproval (Paper plugin)

Lets an approver let new players in from in-game chat instead of the console.

1. A player who is **not on the whitelist** tries to join. They are turned away
   with "ส่งคำขอเข้าเซิร์ฟเวอร์แล้ว รอแอดมินอนุมัติ แล้วเข้าใหม่อีกครั้ง".
2. Every online approver sees: `[ขอเข้าเซิร์ฟ] <name> ขอเข้าเซิร์ฟเวอร์ ให้ผ่านมั้ย? พิมพ์ Y หรือ N`.
3. The approver types `Y` (added to the whitelist; they rejoin) or `N` (rejected).
   The Y/N message is not broadcast.

- Requests are answered oldest first and expire after 10 minutes.
- If no approver is online, the approver is asked when they next join.
- Works for Bedrock players through Floodgate (whitelisted by their Floodgate UUID).
- With no request pending, `Y`/`N` are normal chat.
- Requires `white-list=true`.

## Config (`plugins/JoinApproval/config.yml`)
```yaml
approvers:
  - Verpong321
  - .verpong321   # Bedrock name via Floodgate
```

## Build
```
./build.sh /path/to/paper-server-dir   # -> build/JoinApproval.jar
```
Built for Paper 26.3 (Java 25+).
