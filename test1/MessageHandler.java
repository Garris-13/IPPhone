import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class MessageHandler implements Runnable {
    private Socket socket;
    private CallManager callManager;
    private BufferedReader in;
    private PrintWriter out;
    private Consumer<String> messageCallback;
    private boolean running;

    public MessageHandler(Socket socket, CallManager callManager) {
        this.socket = socket;
        this.callManager = callManager;
        this.running = true;
    }

    public MessageHandler(Socket socket, CallManager callManager, Consumer<String> messageCallback) {
        this(socket, callManager);
        this.messageCallback = messageCallback;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String message;
            while (running && (message = in.readLine()) != null) {
                processMessage(message);
            }

        } catch (IOException e) {
            System.err.println("消息处理错误: " + e.getMessage());
        } finally {
            close();
        }
    }

    private void processMessage(String message) {
        System.out.println("收到消息: " + message);

        if (messageCallback != null) {
            messageCallback.accept(message);
        }

        String[] parts = message.split(":", 3);
        if (parts.length < 2) {
            sendResponse("ERROR:Invalid message format");
            return;
        }

        String messageType = parts[0];
        String payload = parts.length > 1 ? parts[1] : "";

        switch (messageType) {
            case "DIAL_REQUEST":
                handleDialRequest(payload);
                break;
            case "DIAL_RESPONSE":
                handleDialResponse(payload);
                break;
            case "MESSAGE":
                handleTextMessage(payload);
                break;
            case "CALL_END":
                handleCallEnd();
                break;
            case "REGISTER":
                handleRegister(payload);
                break;
            case "STATUS":
                handleStatusRequest();
                break;
            default:
                sendResponse("ERROR:Unknown message type");
        }
    }

    private void handleDialRequest(String callee) {
        System.out.println("收到来自 " + socket.getInetAddress() + " 的拨号请求");

        // 这里可以添加逻辑来检查被叫方是否可用
        boolean isAvailable = callManager.isUserAvailable(callee, socket.getInetAddress().getHostAddress());

        if (isAvailable) {
            sendResponse("DIAL_RINGING:" + callee);
        } else {
            sendResponse("DIAL_REJECT:User not available");
        }
    }

    private void handleDialResponse(String response) {
        String[] responseParts = response.split(":", 2);
        String status = responseParts[0];
        String details = responseParts.length > 1 ? responseParts[1] : "";

        if ("ACCEPT".equals(status)) {
            System.out.println("通话被接受: " + details);
        } else if ("REJECT".equals(status)) {
            System.out.println("通话被拒绝: " + details);
        } else if ("RINGING".equals(status)) {
            System.out.println("对方电话响铃中: " + details);
        }
    }

    private void handleTextMessage(String text) {
        System.out.println("收到文本消息: " + text);
        // 可以在这里添加消息存储或转发逻辑
        sendResponse("MESSAGE_RECEIVED:" + text);
    }

    private void handleCallEnd() {
        System.out.println("通话结束请求");
        running = false;
        sendResponse("CALL_END_ACK");
    }

    private void handleRegister(String userInfo) {
        String[] userParts = userInfo.split("@");
        if (userParts.length == 2) {
            String username = userParts[0];
            String ipAddress = userParts[1];
            boolean success = callManager.registerUser(username, ipAddress);

            if (success) {
                sendResponse("REGISTER_SUCCESS:" + username);
            } else {
                sendResponse("REGISTER_FAILED:User already registered");
            }
        } else {
            sendResponse("REGISTER_FAILED:Invalid user info format");
        }
    }

    private void handleStatusRequest() {
        int activeCalls = callManager.getAvailableUsers().size();
        sendResponse("STATUS:Active users - " + activeCalls);
    }

    public void sendMessage(String messageType, String payload) {
        if (out != null) {
            out.println(messageType + ":" + payload);
        }
    }

    public void sendResponse(String response) {
        if (out != null) {
            out.println(response);
        }
    }

    public void close() {
        running = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("关闭连接时出错: " + e.getMessage());
        }
    }

    public void setMessageCallback(Consumer<String> callback) {
        this.messageCallback = callback;
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }
}