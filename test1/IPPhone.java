import java.io.*;
import java.net.*;
import java.util.Scanner;

public class IPPhone {
    private static final int DIAL_PORT = 6060;
    private static final int AUDIO_PORT = 6061;
    private static final int MESSAGE_PORT = 6062;

    private String remoteIP;
    private boolean isInCall = false;
    private String username;
    private Scanner scanner;
    private AudioManager audioManager;

    public static void main(String[] args) {
        IPPhone phone = new IPPhone();
        phone.start();
    }

    public IPPhone() {
        this.username = "User_" + (System.currentTimeMillis() % 1000);
        this.scanner = new Scanner(System.in);
        this.audioManager = new AudioManager(AUDIO_PORT);
    }

    public void start() {
        System.out.println("=== Java IP Phone ===");
        System.out.println("用户名: " + username);
        System.out.println("本地IP: " + getLocalIP());

        // 启动音频播放（一直监听音频数据）
        audioManager.startPlaying();

        startServer();
        showMenu();
    }

    private void startServer() {
        // 启动拨号服务器
        new Thread(new DialingServer()).start();
        // 启动消息服务器
        new Thread(new MessageServer()).start();

        System.out.println("IP Phone服务器已启动...");
        System.out.println("拨号端口: " + DIAL_PORT);
        System.out.println("音频端口: " + AUDIO_PORT);
        System.out.println("消息端口: " + MESSAGE_PORT);
        System.out.println("等待来电或拨号...");
    }

    private void showMenu() {
        while (true) {
            System.out.println("\n=== IP Phone 菜单 ===");
            System.out.println("1. 拨号");
            System.out.println("2. 查看状态");
            System.out.println("3. 测试音频");
            System.out.println("4. 退出");
            System.out.print("请选择: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    System.out.print("输入目标IP地址: ");
                    String ip = scanner.nextLine();
                    dial(ip);
                    break;
                case "2":
                    showStatus();
                    break;
                case "3":
                    testAudio();
                    break;
                case "4":
                    shutdown();
                    return;
                default:
                    System.out.println("无效选择!");
            }
        }
    }

    private void dial(String ip) {
        if (isInCall) {
            System.out.println("当前正在通话中，请先结束当前通话");
            return;
        }

        this.remoteIP = ip;

        try (Socket socket = new Socket(ip, DIAL_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // 发送拨号请求
            out.println("DIAL:" + username);
            System.out.println("正在呼叫 " + ip + "...");

            // 等待响应
            String response = in.readLine();
            if (response != null && response.startsWith("ACCEPT")) {
                System.out.println("通话已连接!");
                startCall();
            } else if (response != null && response.startsWith("REJECT")) {
                System.out.println("对方拒绝接听");
            } else {
                System.out.println("呼叫失败: " + response);
            }

        } catch (ConnectException e) {
            System.err.println("无法连接到目标IP: " + ip);
        } catch (IOException e) {
            System.err.println("拨号失败: " + e.getMessage());
        }
    }

    private void startCall() {
        isInCall = true;

        // 开始录音并发送音频
        audioManager.startRecording(remoteIP, AUDIO_PORT);

        System.out.println("=== 通话中 ===");
        System.out.println("输入 'hangup' 结束通话");
        System.out.println("输入其他内容发送文本消息");
        System.out.println("=========================");

        // 通话控制循环
        while (isInCall) {
            System.out.print(">> ");
            String input = scanner.nextLine();

            if ("hangup".equalsIgnoreCase(input)) {
                endCall();
            } else if (!input.trim().isEmpty()) {
                sendMessage(input);
            }
        }
    }

    private void endCall() {
        if (!isInCall) {
            return;
        }

        isInCall = false;

        // 停止音频传输
        audioManager.stopRecording();

        // 发送结束通话消息
        try (Socket socket = new Socket(remoteIP, MESSAGE_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println("CALL_END");
        } catch (IOException e) {
            // 忽略结束通话时的错误
        }

        System.out.println("通话已结束");
    }

    private void sendMessage(String message) {
        try (Socket socket = new Socket(remoteIP, MESSAGE_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println("MSG:" + message);
            System.out.println("你: " + message);

        } catch (IOException e) {
            System.err.println("发送消息失败: " + e.getMessage());
        }
    }

    private void testAudio() {
        System.out.println("开始音频测试...");
        System.out.println("请对着麦克风说话，你应该能听到自己的声音（回音测试）");
        System.out.println("测试将持续5秒...");

        // 临时录制并播放自己的声音
        audioManager.startRecording("127.0.0.1", AUDIO_PORT);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        audioManager.stopRecording();
        System.out.println("音频测试结束");
    }

    private void showStatus() {
        System.out.println("\n=== 系统状态 ===");
        System.out.println("用户名: " + username);
        System.out.println("本地IP: " + getLocalIP());
        System.out.println("通话状态: " + (isInCall ? "通话中 (" + remoteIP + ")" : "空闲"));
        System.out.println("音频录制: " + (audioManager.isRecording() ? "进行中" : "停止"));
        System.out.println("音频播放: " + (audioManager.isPlaying() ? "进行中" : "停止"));
        System.out.println("服务器端口: " + DIAL_PORT + "(拨号), " + AUDIO_PORT + "(音频), " + MESSAGE_PORT + "(消息)");
    }

    private void shutdown() {
        System.out.println("正在关闭IP Phone...");
        isInCall = false;
        audioManager.close();
        if (scanner != null) {
            scanner.close();
        }
        System.out.println("IP Phone 已关闭，再见!");
    }

    private String getLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    // 拨号服务器 - 处理来电
    class DialingServer implements Runnable {
        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(DIAL_PORT)) {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new DialingHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                System.err.println("拨号服务器错误: " + e.getMessage());
            }
        }
    }

    // 拨号处理器 - 处理来电请求
    class DialingHandler implements Runnable {
        private Socket socket;

        public DialingHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                String request = in.readLine();
                if (request != null && request.startsWith("DIAL:")) {
                    String caller = request.substring(5);
                    String callerIP = socket.getInetAddress().getHostAddress();

                    System.out.println("\n=== 来电 ===");
                    System.out.println("来自: " + caller + " (" + callerIP + ")");
                    System.out.print("是否接听? (y/n): ");

                    String answer = scanner.nextLine();

                    if ("y".equalsIgnoreCase(answer)) {
                        // 设置远程IP
                        remoteIP = callerIP;
                        out.println("ACCEPT");
                        System.out.println("通话已连接!");
                        startCall();
                    } else {
                        out.println("REJECT");
                        System.out.println("已拒绝通话");
                    }
                }

            } catch (IOException e) {
                System.err.println("拨号处理错误: " + e.getMessage());
            }
        }
    }

    // 消息服务器 - 处理文本消息
    class MessageServer implements Runnable {
        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(MESSAGE_PORT)) {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new MessageHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                System.err.println("消息服务器错误: " + e.getMessage());
            }
        }
    }

    // 消息处理器 - 处理收到的消息
    class MessageHandler implements Runnable {
        private Socket socket;

        public MessageHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                String message;
                while ((message = in.readLine()) != null) {
                    if ("CALL_END".equals(message)) {
                        System.out.println("\n对方已挂断通话");
                        isInCall = false;
                        audioManager.stopRecording();
                        break;
                    } else if (message.startsWith("MSG:")) {
                        String text = message.substring(4);
                        System.out.println("\n对方: " + text);
                        if (isInCall) {
                            System.out.print(">> ");
                        }
                    }
                }

            } catch (IOException e) {
                if (isInCall) {
                    System.err.println("消息处理错误: " + e.getMessage());
                }
            }
        }
    }
}