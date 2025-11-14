import javax.sound.sampled.*;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IPPhone {
    private static final int DIAL_PORT = 6060;
    private static final int AUDIO_PORT = 6061;
    private static final int MESSAGE_PORT = 6062;

    private String remoteIP;
    private int remoteDialPort = DIAL_PORT;
    private int remoteAudioPort = AUDIO_PORT;
    private int remoteMessagePort = MESSAGE_PORT;

    private boolean isInCall = false;
    private AudioFormat audioFormat;
    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private String username;

    private ScheduledExecutorService scheduler;
    private Scanner scanner;

    public static void main(String[] args) {
        IPPhone phone = new IPPhone();
        phone.start();
    }

    public IPPhone() {
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.username = "User_" + System.currentTimeMillis() % 1000;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("=== Java IP Phone ===");
        System.out.println("用户名: " + username);

        if (initializeAudio()) {
            startServer();
            showMenu();
        } else {
            System.err.println("音频设备初始化失败，程序退出");
        }
    }

    private boolean initializeAudio() {
        try {
            audioFormat = new AudioFormat(16000, 16, 1, true, true);

            DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
            if (!AudioSystem.isLineSupported(micInfo)) {
                System.err.println("不支持麦克风音频格式");
                return false;
            }
            microphone = (TargetDataLine) AudioSystem.getLine(micInfo);

            DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
            if (!AudioSystem.isLineSupported(speakerInfo)) {
                System.err.println("不支持扬声器音频格式");
                return false;
            }
            speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);

            System.out.println("音频设备初始化成功");
            return true;

        } catch (LineUnavailableException e) {
            System.err.println("音频设备初始化失败: " + e.getMessage());
            return false;
        }
    }

    private void startServer() {
        // 启动拨号服务器
        new Thread(new DialingServer()).start();
        // 启动音频服务器
        new Thread(new AudioServer()).start();
        // 启动消息服务器
        new Thread(new MessageServer()).start();

        System.out.println("IP Phone服务器已启动...");
        System.out.println("拨号端口: " + DIAL_PORT);
        System.out.println("音频端口: " + AUDIO_PORT);
        System.out.println("消息端口: " + MESSAGE_PORT);
    }

    private void showMenu() {
        while (true) {
            System.out.println("\n=== IP Phone 菜单 ===");
            System.out.println("1. 拨号");
            System.out.println("2. 查看状态");
            System.out.println("3. 退出");
            System.out.print("请选择: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    System.out.print("输入目标IP地址: ");
                    remoteIP = scanner.nextLine();
                    dial(remoteIP);
                    break;
                case "2":
                    showStatus();
                    break;
                case "3":
                    shutdown();
                    return;
                default:
                    System.out.println("无效选择!");
            }
        }
    }

    private void dial(String ip) {
        try (Socket socket = new Socket(ip, remoteDialPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // 发送拨号请求
            out.println("DIAL:" + username + "@" + getLocalIP());
            String response = in.readLine();

            if (response != null && response.startsWith("DIALING:")) {
                System.out.println("拨号中...");
                // 等待用户确认接听
                acceptCall();
            } else {
                System.out.println("拨号失败: " + response);
            }

        } catch (IOException e) {
            System.err.println("拨号失败: " + e.getMessage());
        }
    }

    private void acceptCall() {
        System.out.print("是否接听通话? (y/n): ");
        String answer = scanner.nextLine();

        if ("y".equalsIgnoreCase(answer)) {
            try (Socket socket = new Socket(remoteIP, remoteDialPort);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                out.println("ACCEPT:" + username + "-" + System.currentTimeMillis());
                System.out.println("通话已连接!");
                startCall();

            } catch (IOException e) {
                System.err.println("接听通话失败: " + e.getMessage());
            }
        } else {
            rejectCall();
        }
    }

    private void rejectCall() {
        try (Socket socket = new Socket(remoteIP, remoteDialPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println("REJECT:" + username + "-" + System.currentTimeMillis());
            System.out.println("已拒绝通话");

        } catch (IOException e) {
            System.err.println("拒绝通话失败: " + e.getMessage());
        }
    }

    private void startCall() {
        isInCall = true;

        // 启动音频发送线程
        new Thread(new AudioSender()).start();
        // 启动音频接收线程
        new Thread(new AudioReceiver()).start();

        System.out.println("通话中... 输入 'hangup' 结束通话");

        // 通话控制循环
        while (isInCall) {
            String input = scanner.nextLine();
            if ("hangup".equalsIgnoreCase(input)) {
                endCall();
            } else {
                sendMessage(input);
            }
        }
    }

    private void endCall() {
        isInCall = false;
        try {
            // 发送结束通话消息
            Socket socket = new Socket(remoteIP, remoteMessagePort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("CALL_END");
            socket.close();
        } catch (IOException e) {
            System.err.println("结束通话时出错: " + e.getMessage());
        }

        // 关闭音频设备
        if (microphone != null && microphone.isOpen()) {
            microphone.close();
        }
        if (speakers != null && speakers.isOpen()) {
            speakers.close();
        }

        System.out.println("通话已结束");
    }

    private void sendMessage(String message) {
        try (Socket socket = new Socket(remoteIP, remoteMessagePort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println("MESSAGE:" + message);
            System.out.println("你: " + message);

        } catch (IOException e) {
            System.err.println("发送消息失败: " + e.getMessage());
        }
    }

    private void showStatus() {
        System.out.println("\n=== 系统状态 ===");
        System.out.println("用户名: " + username);
        System.out.println("本地IP: " + getLocalIP());
        System.out.println("通话状态: " + (isInCall ? "通话中" : "空闲"));
        System.out.println("音频设备: " + (microphone != null ? "就绪" : "未就绪"));
    }

    private void shutdown() {
        isInCall = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (microphone != null) {
            microphone.close();
        }
        if (speakers != null) {
            speakers.close();
        }

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

    // 拨号服务器
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

    // 拨号处理器
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
                    String callerInfo = request.substring(5);
                    System.out.println("\n来电来自: " + callerInfo);
                    System.out.print("是否接听? (y/n): ");

                    String answer = scanner.nextLine();

                    if ("y".equalsIgnoreCase(answer)) {
                        out.println("DIALING:" + System.currentTimeMillis());
                        remoteIP = socket.getInetAddress().getHostAddress();
                        startCall();
                    } else {
                        out.println("DIAL_REJECT:User declined");
                    }
                }

            } catch (IOException e) {
                System.err.println("拨号处理错误: " + e.getMessage());
            }
        }
    }

    // 音频服务器
    class AudioServer implements Runnable {
        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket(AUDIO_PORT)) {
                byte[] buffer = new byte[1024];

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    if (isInCall && remoteIP.equals(packet.getAddress().getHostAddress())) {
                        try {
                            if (speakers != null && !speakers.isOpen()) {
                                speakers.open(audioFormat);
                                speakers.start();
                            }
                            if (speakers != null && speakers.isOpen()) {
                                speakers.write(packet.getData(), 0, packet.getLength());
                            }
                        } catch (LineUnavailableException e) {
                            System.err.println("扬声器错误: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("音频服务器错误: " + e.getMessage());
            }
        }
    }

    // 音频发送器
    class AudioSender implements Runnable {
        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket()) {
                try {
                    microphone.open(audioFormat);
                    microphone.start();
                } catch (LineUnavailableException e) {
                    System.err.println("无法打开麦克风: " + e.getMessage());
                    return;
                }

                byte[] buffer = new byte[1024];

                while (isInCall) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        try {
                            DatagramPacket packet = new DatagramPacket(
                                    buffer, bytesRead,
                                    InetAddress.getByName(remoteIP), remoteAudioPort
                            );
                            socket.send(packet);
                        } catch (IOException e) {
                            System.err.println("音频发送错误: " + e.getMessage());
                        }
                    }

                    // 短暂休眠
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                microphone.close();
            } catch (IOException e) {
                System.err.println("音频发送器错误: " + e.getMessage());
            }
        }
    }

    // 音频接收器
    class AudioReceiver implements Runnable {
        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket(AUDIO_PORT + 1)) {
                byte[] buffer = new byte[1024];

                try {
                    speakers.open(audioFormat);
                    speakers.start();
                } catch (LineUnavailableException e) {
                    System.err.println("无法打开扬声器: " + e.getMessage());
                    return;
                }

                while (isInCall) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    try {
                        socket.receive(packet);

                        if (remoteIP.equals(packet.getAddress().getHostAddress())) {
                            speakers.write(packet.getData(), 0, packet.getLength());
                        }
                    } catch (IOException e) {
                        if (isInCall) {
                            System.err.println("音频接收错误: " + e.getMessage());
                        }
                    }
                }

                speakers.close();
            } catch (IOException e) {
                System.err.println("音频接收器错误: " + e.getMessage());
            }
        }
    }

    // 消息服务器
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

    // 消息处理器
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
                    if (message.startsWith("MESSAGE:")) {
                        System.out.println("\n对方: " + message.substring(8));
                        System.out.print(">> ");
                    } else if ("CALL_END".equals(message)) {
                        System.out.println("\n对方已挂断通话");
                        isInCall = false;
                        break;
                    }
                }

            } catch (IOException e) {
                System.err.println("消息处理错误: " + e.getMessage());
            }
        }
    }
}