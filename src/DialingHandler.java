import java.io.*;
import java.net.*;

public class DialingHandler implements Runnable {
    private Socket socket;
    private CallManager callManager;
    private BufferedReader in;
    private PrintWriter out;

    public DialingHandler(Socket socket, CallManager callManager) {
        this.socket = socket;
        this.callManager = callManager;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String request = in.readLine();
            if (request != null) {
                processDialRequest(request);
            }

        } catch (IOException e) {
            System.err.println("拨号处理错误: " + e.getMessage());
        } finally {
            close();
        }
    }

    private void processDialRequest(String request) {
        System.out.println("处理拨号请求: " + request + " 来自 " + socket.getInetAddress());

        String[] parts = request.split(":", 2);
        String requestType = parts[0];
        String payload = parts.length > 1 ? parts[1] : "";

        switch (requestType) {
            case "DIAL":
                handleDial(payload);
                break;
            case "ACCEPT":
                handleAccept(payload);
                break;
            case "REJECT":
                handleReject(payload);
                break;
            case "CANCEL":
                handleCancel(payload);
                break;
            default:
                sendResponse("ERROR:Unknown dial request type");
        }
    }

    private void handleDial(String calleeInfo) {
        String[] infoParts = calleeInfo.split("@");
        if (infoParts.length == 2) {
            String callee = infoParts[0];
            String calleeIP = infoParts[1];
            String callerIP = socket.getInetAddress().getHostAddress();

            // 创建通话会话
            CallManager.CallSession session = callManager.createCallSession(
                    "unknown", callee, callerIP, calleeIP
            );

            sendResponse("DIALING:" + session.getSessionId());

        } else {
            sendResponse("ERROR:Invalid dial format");
        }
    }

    private void handleAccept(String sessionId) {
        CallManager.CallSession session = callManager.getCallSession(sessionId);
        if (session != null) {
            session.acceptCall();
            sendResponse("CALL_ACTIVE:" + sessionId);
            System.out.println("通话 " + sessionId + " 已激活");
        } else {
            sendResponse("ERROR:Session not found");
        }
    }

    private void handleReject(String sessionId) {
        callManager.endCallSession(sessionId);
        sendResponse("CALL_REJECTED:" + sessionId);
        System.out.println("通话 " + sessionId + " 被拒绝");
    }

    private void handleCancel(String sessionId) {
        callManager.endCallSession(sessionId);
        sendResponse("CALL_CANCELLED:" + sessionId);
        System.out.println("通话 " + sessionId + " 已取消");
    }

    private void sendResponse(String response) {
        if (out != null) {
            out.println(response);
        }
    }

    private void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("关闭拨号连接时出错: " + e.getMessage());
        }
    }
}