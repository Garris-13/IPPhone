package controller;

import model.CallModel;
import model.NetworkModel;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 服务器控制器
 * - 8081 专用于 TCP 通话信令（DIAL_REQUEST, CALL_END）
 * - 8182 专用于音频消息 AUDIO_MESSAGE
 */
public class ServerController {

    private ServerSocket mainServerSocket;      // 8081 用于通话信令
    private ServerSocket audioServerSocket;     // 8182 用于音频消息
    private boolean running = false;

    private CallController callController;
    private final NetworkModel networkModel;
    private final CallModel callModel;

    private static final String AUDIO_DIR = "received_audio_messages";

    public ServerController(NetworkModel networkModel, CallModel callModel, AudioController audioController) {
        this.networkModel = networkModel;
        this.callModel = callModel;

        File dir = new File(AUDIO_DIR);
        if (!dir.exists()) dir.mkdirs();
    }

    public void setCallController(CallController callController) {
        this.callController = callController;
        System.out.println("ServerController: CallController 已设置");
    }

    /**
     * 启动两个服务器:
     *  - 8081：通话请求
     *  - 8182：音频消息
     */
    public boolean startServer(int port) {
        if (running) return true;

        try {
            // ==============================
            // ★ 1. 启动主服务器 (8081)
            // ==============================
            mainServerSocket = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"));
            System.out.println("主服务器（通话）监听端口: " + port);

            new Thread(this::acceptLoopCall, "CallServerThread").start();

            // ==============================
            // ★ 2. 启动音频消息服务器 (port + 101)
            // ==============================
            int audioPort = port + 101;  // 8081 → 8182
            audioServerSocket = new ServerSocket(audioPort, 50, InetAddress.getByName("0.0.0.0"));
            System.out.println("音频消息服务器监听端口: " + audioPort);

            new Thread(() -> acceptLoopAudio(audioPort), "AudioMsgServerThread").start();

            running = true;
            return true;

        } catch (IOException e) {
            System.err.println("服务器启动失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * ======================================================
     * 8081：通话服务器（处理通话信令）
     * ======================================================
     */
    private void acceptLoopCall() {
        while (running) {
            try {
                Socket client = mainServerSocket.accept();
                new Thread(() -> handleCallSignal(client)).start();
            } catch (Exception ignored) {}
        }
    }

    private void handleCallSignal(Socket socket) {
        try {
            socket.setSoTimeout(30000); // 设置30秒超时
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            String command = in.readLine();

            if (command == null) {
                socket.close();
                return;
            }

            // ======================
            // 处理通话信令
            // ======================
            command = command.trim();
            System.out.println("收到信令: " + command + " 来自: " + socket.getInetAddress().getHostAddress());

            if ("DIAL_REQUEST".equals(command)) {
                handleDialRequest(socket, in);
            } else if ("CALL_END".equals(command)) {
                // 处理通话结束信令
                System.out.println("收到通话结束信令，关闭连接");
                try {
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                    out.println("CALL_END_ACK");
                    out.flush();
                } catch (Exception ignored) {}
                socket.close();
            } else {
                // 未知命令
                System.out.println("收到未知信令: " + command);
                socket.close();
            }

        } catch (SocketTimeoutException e) {
            System.out.println("连接超时: " + socket.getInetAddress().getHostAddress());
            try { socket.close(); } catch (Exception ignored) {}
        } catch (Exception e) {
            System.out.println("处理信令异常: " + e.getMessage());
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * 来电处理（弹出接听框）
     */
    private void handleDialRequest(Socket socket, BufferedReader in) {

        PrintWriter out;
        try {
            socket.setSoTimeout(30000); // 设置超时
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        } catch (Exception e) {
            try { socket.close(); } catch (Exception ignored) {}
            return;
        }

        String remoteIP = socket.getInetAddress().getHostAddress();
        callModel.setRemoteIP(remoteIP);//增加
        SwingUtilities.invokeLater(() -> {
            int choice = JOptionPane.showConfirmDialog(
                    null,
                    "来自 " + remoteIP + " 的来电，是否接听？",
                    "来电提示",
                    JOptionPane.YES_NO_OPTION
            );

            if (choice == JOptionPane.YES_OPTION) {
                try {
                    out.println("DIAL_ACCEPT");
                    out.flush();

                    // 设置socket为不超时模式，用于长连接
                    socket.setSoTimeout(0);

                } catch (Exception e) {
                    System.out.println("发送接听响应失败：" + e.getMessage());
                    try { socket.close(); } catch (Exception ignored) {}
                    return;
                }

                // 把 socket 交给 CallController
                callController.handleIncomingCallAccepted(socket, remoteIP);

            } else {
                try {
                    out.println("DIAL_REJECT");
                    out.flush();
                } catch (Exception e) {
                    System.out.println("发送拒绝响应失败：" + e.getMessage());
                }
                try { socket.close(); } catch (Exception ignored) {}
            }
        });
    }


    /**
     * ======================================================
     * 8182：音频消息服务器（专门收 AUDIO_MESSAGE）
     * ======================================================
     */
    private void acceptLoopAudio(int audioPort) {
        while (running) {
            try {
                Socket client = audioServerSocket.accept();
                new Thread(() -> handleAudioMessage(client)).start();
            } catch (Exception ignored) {}
        }
    }

    /**
     * 纯音频消息处理，不涉及拨号逻辑
     */
    private void handleAudioMessage(Socket socket) {
        String remoteIP = socket.getInetAddress().getHostAddress();
        System.out.println("收到音频消息连接来自: " + remoteIP);

        try {
            InputStream is = socket.getInputStream();
            //BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            DataInputStream dis = new DataInputStream(is);
            // ★★★ 1. 接收文件名与大小
            String fileName = dis.readLine();
            String sizeStr = dis.readLine();

            if (fileName == null || sizeStr == null) {
                socket.close();
                return;
            }

            long size = Long.parseLong(sizeStr);
            System.out.println("收到大小" + sizeStr);

            // ★★★ 2. 保存到文件
            File dir = new File(AUDIO_DIR);
            if (!dir.exists()) dir.mkdirs();

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File output = new File(dir, timestamp + "_" + fileName);

            FileOutputStream fos = new FileOutputStream(output);
            //InputStream is = socket.getInputStream();
            //DataInputStream dis = new DataInputStream(is);
            byte[] buffer = new byte[4096];
            long total = 0;
            int len;

            // while (total < size && (len = dis.read(buffer,0,(int)Math.min(buffer.length,size - total))) != -1) {
            while (total < size && (len = is.read(buffer)) != -1){
                //System.out.println(len);
                fos.write(buffer, 0, len);
                total += len;
            }

            System.out.println("实际大小 " + total);
            fos.close();
            socket.close();

            // ★★★ 3. 弹窗通知
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                        null,
                        "收到音频消息: " + output.getName() + "\n来自: " + remoteIP,
                        "新音频消息",
                        JOptionPane.INFORMATION_MESSAGE
                );
            });

            System.out.println("音频消息接收完成: " + output.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("接收音频消息失败: " + e.getMessage());
        }
    }

    /**
     * 停止服务器
     */
    public void stopServer() {
        running = false;
        try { if (mainServerSocket != null) mainServerSocket.close(); } catch (Exception ignored) {}
        try { if (audioServerSocket != null) audioServerSocket.close(); } catch (Exception ignored) {}
    }

    public boolean isServerRunning() {
        return running;
    }
}