import java.util.*;
import java.util.concurrent.*;

public class CallManager {
    private Map<String, CallSession> activeCalls;
    private Set<String> registeredUsers;

    public CallManager() {
        this.activeCalls = new ConcurrentHashMap<>();
        // 使用兼容的方式创建并发Set
        this.registeredUsers = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    }

    public boolean registerUser(String username, String ipAddress) {
        String userKey = username + "@" + ipAddress;
        // 检查用户是否已经注册
        if (registeredUsers.contains(userKey)) {
            return false;
        }
        return registeredUsers.add(userKey);
    }

    public boolean unregisterUser(String username, String ipAddress) {
        String userKey = username + "@" + ipAddress;
        return registeredUsers.remove(userKey);
    }

    public CallSession createCallSession(String caller, String callee, String callerIP, String calleeIP) {
        String sessionId = generateSessionId(caller, callee);
        CallSession session = new CallSession(sessionId, caller, callee, callerIP, calleeIP);
        activeCalls.put(sessionId, session);
        System.out.println("创建通话会话: " + sessionId);
        return session;
    }

    public CallSession getCallSession(String sessionId) {
        return activeCalls.get(sessionId);
    }

    public void endCallSession(String sessionId) {
        CallSession session = activeCalls.remove(sessionId);
        if (session != null) {
            session.endCall();
            System.out.println("结束通话会话: " + sessionId);
        }
    }

    public boolean isUserAvailable(String username, String ipAddress) {
        String userKey = username + "@" + ipAddress;
        return registeredUsers.contains(userKey);
    }

    public List<String> getAvailableUsers() {
        return new ArrayList<>(registeredUsers);
    }

    public Map<String, CallSession> getActiveCalls() {
        return new HashMap<>(activeCalls);
    }

    private String generateSessionId(String caller, String callee) {
        return caller + "-" + callee + "-" + System.currentTimeMillis();
    }

    public void cleanupExpiredSessions(long timeoutMs) {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, CallSession>> iterator = activeCalls.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, CallSession> entry = iterator.next();
            CallSession session = entry.getValue();

            if (session.getStatus() == CallStatus.ENDED &&
                    (currentTime - session.getEndTime()) > timeoutMs) {
                iterator.remove();
                System.out.println("清理过期会话: " + session.getSessionId());
            }
        }
    }

    public static class CallSession {
        private String sessionId;
        private String caller;
        private String callee;
        private String callerIP;
        private String calleeIP;
        private CallStatus status;
        private long startTime;
        private long endTime;

        public CallSession(String sessionId, String caller, String callee, String callerIP, String calleeIP) {
            this.sessionId = sessionId;
            this.caller = caller;
            this.callee = callee;
            this.callerIP = callerIP;
            this.calleeIP = calleeIP;
            this.status = CallStatus.RINGING;
            this.startTime = System.currentTimeMillis();
        }

        public void acceptCall() {
            this.status = CallStatus.ACTIVE;
            System.out.println("通话已接受: " + sessionId);
        }

        public void endCall() {
            this.status = CallStatus.ENDED;
            this.endTime = System.currentTimeMillis();
            System.out.println("通话已结束: " + sessionId + ", 持续时间: " + getCallDuration() + "ms");
        }

        public void rejectCall() {
            this.status = CallStatus.REJECTED;
            this.endTime = System.currentTimeMillis();
            System.out.println("通话被拒绝: " + sessionId);
        }

        public boolean isActive() {
            return status == CallStatus.ACTIVE;
        }

        public long getCallDuration() {
            if (status == CallStatus.RINGING) {
                return 0;
            }
            if (endTime == 0) {
                return System.currentTimeMillis() - startTime;
            }
            return endTime - startTime;
        }

        // Getters
        public String getSessionId() { return sessionId; }
        public String getCaller() { return caller; }
        public String getCallee() { return callee; }
        public String getCallerIP() { return callerIP; }
        public String getCalleeIP() { return calleeIP; }
        public CallStatus getStatus() { return status; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }

        @Override
        public String toString() {
            return String.format("CallSession{sessionId='%s', caller='%s', callee='%s', status=%s, duration=%dms}",
                    sessionId, caller, callee, status, getCallDuration());
        }
    }

    public enum CallStatus {
        RINGING, ACTIVE, ENDED, REJECTED
    }
}