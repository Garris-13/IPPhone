package controller;

import model.*;
import view.*;
import util.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * 完整版 CallController - 增加聊天功能
 */
public class CallController implements ChatController.ChatRequestCallback {

    private final CallModel callModel;
    private final NetworkModel networkModel;
    private final MainView mainView;
    private final DialingView dialingView;
    private final CallView callView;
    private final AudioController audioController;
    private final ServerController serverController;

    // 聊天相关
    private ChatController chatController;
    private ChatView chatView;

    private JPanel mainPanel;
    private CardLayout cardLayout;

    // 音频检测定时器
    private Timer audioDetectionTimer;

    // 通话结束同步相关
    private volatile boolean isHangupNotified = false;
    private Thread tcpListenerThread;
    private volatile boolean callEnded = false;
    private volatile boolean isLocalHangup = false;

    public CallController(CallModel callModel,
                          NetworkModel networkModel,
                          MainView mainView,
                          DialingView dialingView,
                          CallView callView,
                          JPanel mainPanel,
                          CardLayout cardLayout) {

        this.callModel = callModel;
        this.networkModel = networkModel;
        this.mainView = mainView;
        this.dialingView = dialingView;
        this.callView = callView;
        this.mainPanel = mainPanel;
        this.cardLayout = cardLayout;

        // 初始化音频控制器
        this.audioController = new AudioController(new AudioModel(), networkModel);

        // 初始化服务器并注册自己
        this.serverController = new ServerController(networkModel, callModel, audioController);
        this.serverController.setCallController(this);

        // 设置音频检测监听器
        setupAudioDetection();

        bindUI();

        // 启动通话服务器
        boolean ok = this.serverController.startServer(callModel.getTcpPort());
        if (!ok) {
            mainView.showError("通话服务器启动失败，请检查端口是否被占用");
        }

        // 启动聊天服务器
        startChatServer();
    }

    /**
     * 启动聊天服务器
     */
    private void startChatServer() {
        String localIP = networkModel.getLocalIP();

        // 如果已有聊天控制器，先重置它
        if (chatController != null) {
            System.out.println("重置现有聊天控制器");
            chatController.reset();
        } else {
            // 创建新的聊天控制器
            chatController = new ChatController(localIP, "", networkModel);
            chatController.setCallback(this);
        }

        // 启动聊天服务器
        if (chatController.startChatServer()) {
            System.out.println("聊天服务器启动成功，监听端口: 8283");
            SwingUtilities.invokeLater(() -> {
                mainView.appendMessage("系统", "聊天服务器已启动");
            });
        } else {
            System.err.println("聊天服务器启动失败");
            SwingUtilities.invokeLater(() -> {
                mainView.appendMessage("系统", "聊天服务器启动失败，聊天功能可能无法使用");
            });
        }
    }

    /**
     * 设置音频检测监听器
     */
    private void setupAudioDetection() {
        // 设置音频检测监听器
        audioController.setAudioDetectionListener(new AudioController.AudioDetectionListener() {
            @Override
            public void onAudioDetected(String ip, long timestamp) {
                // 更新通话界面的音频状态显示
                callView.updateAudioStatus(ip, timestamp);
            }
        });

        // 创建音频检测状态更新定时器（每秒检查一次）
        audioDetectionTimer = new Timer(1000, e -> updateAudioDetectionStatus());
    }

    /**
     * 绑定所有 UI 事件
     */
    private void bindUI() {

        // 连接按钮 - 处理拨号和音频消息发送
        mainView.addConnectButtonListener(e -> {
            String ip = mainView.getRemoteIP();

            if (ip == null || ip.trim().isEmpty()) {
                mainView.showError("请输入对方的 IP 地址");
                return;
            }

            callModel.setRemoteIP(ip.trim());

            String selectedMode = mainView.getSelectedMode();

            if ("拨号通话".equals(selectedMode)) {
                // 拨号通话模式
                startDialing();
            } else if ("音频消息".equals(selectedMode)) {
                // 音频消息模式
                sendAudioMessage();
            }
        });

        // 新增：在线聊天按钮监听器
        mainView.addChatButtonListener(e -> {
            String ip = mainView.getRemoteIP();

            if (ip == null || ip.trim().isEmpty()) {
                mainView.showError("请输入对方的 IP 地址");
                return;
            }

            callModel.setRemoteIP(ip.trim());
            initiateChat();
        });

        // 录音按钮
        mainView.addRecordButtonListener(e -> {
            if (!audioController.isRecording()) {
                if (audioController.startRecording()) {
                    mainView.setRecordingState(true, false);
                } else {
                    mainView.showError("无法开始录音");
                }
            } else {
                File f = audioController.stopRecording();
                mainView.setRecordingState(false, f != null);

                if (f != null) {
                    mainView.showInfo("录音已保存: " + f.getName());
                }
            }
        });

        // 服务器开关
        mainView.addServerToggleListener(e -> {
            if (serverController.isServerRunning()) {
                serverController.stopServer();
                mainView.setServerState(false);
                mainView.showInfo("服务器已停止");
            } else {
                boolean ok = serverController.startServer(callModel.getTcpPort());
                if (ok) {
                    mainView.setServerState(true);
                    mainView.showInfo("服务器已启动");
                } else {
                    mainView.showError("服务器启动失败");
                }
            }
        });

        // 拨号界面取消按钮
        dialingView.addCancelButtonListener(e -> {
            try {
                Socket s = networkModel.getTcpSocket();
                if (s != null && !s.isClosed()) s.close();
            } catch (Exception ignored) {}

            // 停止音频检测定时器
            if (audioDetectionTimer != null) {
                audioDetectionTimer.stop();
            }

            // 停止TCP监听线程
            if (tcpListenerThread != null && tcpListenerThread.isAlive()) {
                tcpListenerThread.interrupt();
            }

            cardLayout.show(mainPanel, "MAIN");
        });

        // 通话界面：静音
        callView.addMuteButtonListener(e -> {
            boolean newState = !audioController.isMuted();
            audioController.setMuted(newState);
            callView.updateMuteButton(newState);

            // 在消息区域记录静音状态变化
            if (newState) {
                callView.appendMessage("系统", "已开启静音");
            } else {
                callView.appendMessage("系统", "已关闭静音");
            }
        });

        // 通话界面：结束通话
        callView.addEndCallButtonListener(e -> {
            endCall();
        });

        // 添加诊断按钮监听器
        mainView.addDiagnoseButtonListener(e -> {
            diagnoseChat();
        });
    }

    /**
     * 发起聊天请求
     */
    private void initiateChat() {
        String remoteIP = callModel.getRemoteIP();
        String localIP = networkModel.getLocalIP();

        if (remoteIP == null || remoteIP.isEmpty()) {
            mainView.showError("请输入对方IP地址");
            return;
        }

        // 检查是否已经在聊天
        if (chatController != null && chatController.isChatting()) {
            mainView.showInfo("已经在聊天中");
            return;
        }

        // 确保聊天服务器在运行
        if (chatController == null || !chatController.isServerRunning()) {
            startChatServer();
        }

        // 显示发送聊天请求提示
        mainView.appendMessage("系统", "正在发送聊天请求到 " + remoteIP + " ...");

        // 在新线程中发送聊天请求
        new Thread(() -> {
            boolean success = chatController.sendChatRequest(remoteIP);

            SwingUtilities.invokeLater(() -> {
                if (success) {
                    // 对方接受了聊天请求
                    mainView.appendMessage("系统", "对方接受了聊天请求");
                    openChatWindow(remoteIP);
                } else {
                    // 对方拒绝了聊天请求或连接失败
                    mainView.appendMessage("系统", "聊天请求失败或对方拒绝");
                    JOptionPane.showMessageDialog(
                            mainView,
                            "聊天请求失败或对方拒绝\n\n可能原因：\n1. 对方未启动聊天功能\n2. 网络连接问题\n3. 对方拒绝了请求",
                            "聊天请求",
                            JOptionPane.WARNING_MESSAGE
                    );
                }
            });
        }, "Chat-Request-Thread").start();
    }

    /**
     * 打开聊天窗口
     */
    private void openChatWindow(String remoteIP) {
        String localIP = networkModel.getLocalIP();

        // 创建聊天视图
        chatView = new ChatView(localIP, remoteIP);

        // 为聊天控制器设置远程IP
        chatController.setRemoteIP(remoteIP);

        // 互相设置引用
        chatController.setChatView(chatView);
        chatView.setChatController(chatController);

        // 设置聊天视图事件监听器
        chatView.addSendButtonListener(e -> {
            String message = chatView.getInputMessage();
            if (!message.isEmpty()) {
                chatController.sendChatMessage(message);
                chatView.clearInput();
            }
        });

        chatView.addCloseButtonListener(e -> {
            chatController.closeChat();
        });

        // 显示聊天窗口
        chatView.setVisible(true);

        // 在聊天界面添加欢迎消息
        chatView.appendMessage("系统", "聊天已开始，可以发送消息了");
    }

    /**
     * 实现ChatRequestCallback接口方法
     */

    @Override
    public void onChatRequest(String remoteIP, ChatController controller) {
        // 这个方法由ChatController调用，当有聊天请求到达时
        // 注意：这个回调已经在JOptionPane对话框中处理了，所以这里可以留空
        System.out.println("收到聊天请求来自: " + remoteIP);
    }

    @Override
    public void onChatAccepted(String remoteIP) {
        // 对方接受了聊天请求（当本机是被动接受方时）
        SwingUtilities.invokeLater(() -> {
            mainView.appendMessage("系统", "对方(" + remoteIP + ")接受了聊天请求");
            openChatWindow(remoteIP);
        });
    }

    @Override
    public void onChatRejected(String remoteIP) {
        // 对方拒绝了聊天请求
        SwingUtilities.invokeLater(() -> {
            mainView.appendMessage("系统", "对方(" + remoteIP + ")拒绝了聊天请求");
            JOptionPane.showMessageDialog(
                    mainView,
                    "对方拒绝了聊天请求",
                    "聊天请求",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });
    }
    @Override
    public void onChatClosed(String remoteIP) {
        // 聊天已关闭
        SwingUtilities.invokeLater(() -> {
            System.out.println("聊天已关闭，来自: " + remoteIP);
            mainView.appendMessage("系统", "与 " + remoteIP + " 的聊天已结束");

            // 如果聊天窗口还存在，确保它被关闭
            if (chatView != null && chatView.isVisible()) {
                chatView.dispose();
                chatView = null;
            }

            // 重置聊天控制器，清理资源
            if (chatController != null) {
                chatController.reset();
            }
        });
    }

    /**
     * 拨号流程（主动发起）
     */
    private void startDialing() {
        // 拨号前检查麦克风
        if (!audioController.isMicrophoneAvailable()) {
            SwingUtilities.invokeLater(() -> {
                mainView.showError("麦克风未连接，无法进行通话\n\n请检查：\n1. 麦克风是否正确连接\n2. 是否授予录音权限\n3. 麦克风是否被其他程序占用");
            });
            return;
        }

        // 更新拨号界面的麦克风状态
        dialingView.setMicrophoneStatus(true, "就绪");

        cardLayout.show(mainPanel, "DIALING");
        dialingView.setStatus("正在拨号 " + callModel.getRemoteIP() + " ...");

        new Thread(() -> {
            Socket socket = null;

            try {
                // 更新连接状态
                SwingUtilities.invokeLater(() ->
                        dialingView.setStatus("正在连接 " + callModel.getRemoteIP() + " ..."));

                socket = new Socket();
                socket.connect(new InetSocketAddress(
                        callModel.getRemoteIP(),
                        callModel.getTcpPort()
                ), 8000);

                socket.setSoTimeout(20000);

                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), "UTF-8"));

                // 更新拨号状态
                SwingUtilities.invokeLater(() ->
                        dialingView.setStatus("已连接，等待对方接听..."));

                out.println("DIAL_REQUEST");
                out.flush();

                String resp = in.readLine();
                System.out.println("收到对方响应: " + resp);

                if (resp == null) {
                    SwingUtilities.invokeLater(() -> {
                        mainView.showInfo("对方无响应或已关闭连接");
                        cardLayout.show(mainPanel, "MAIN");
                    });
                    return;
                }

                if (resp.trim().equals("DIAL_ACCEPT")) {
                    networkModel.setTcpSocket(socket);

                    // 设置TCP socket超时
                    socket.setSoTimeout(5000);

                    SwingUtilities.invokeLater(() -> {
                        callView.setStatus("已接通：" + callModel.getRemoteIP());
                        callView.appendMessage("系统", "通话已接通");
                        cardLayout.show(mainPanel, "CALL");
                    });

                    // 建立UDP连接并启动音频
                    setupAudioConnection();

                } else if (resp.trim().equals("DIAL_REJECT")) {
                    SwingUtilities.invokeLater(() -> {
                        mainView.showInfo("对方拒绝了通话");
                        cardLayout.show(mainPanel, "MAIN");
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        mainView.showInfo("收到未知响应: " + resp);
                        cardLayout.show(mainPanel, "MAIN");
                    });
                }

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    mainView.showError("拨号失败: " + e.getMessage());
                    cardLayout.show(mainPanel, "MAIN");
                });

            }
        }, "DialingThread").start();
    }

    /**
     * 建立音频连接
     */
    private void setupAudioConnection() {
        try {
            // 重置状态
            callEnded = false;
            isHangupNotified = false;
            isLocalHangup = false;

            // 重置音频检测状态
            audioController.resetAudioDetection();
            callView.resetAudioStatus();
            callView.resetCallEndStatus();
            callView.clearMessages();

            // 建立UDP套接字
            DatagramSocket udp = new DatagramSocket();
            networkModel.setUdpSocket(udp);
            callModel.setCalling(true);

            // 启动TCP监听线程
            startTcpListenerThread();

            // 启动音频流
            audioController.startAudioStreaming(callModel.getRemoteIP(), callModel.getUdpPort());

            // 启动音频检测定时器
            if (audioDetectionTimer != null) {
                audioDetectionTimer.start();
            }

            // 在通话界面显示连接状态
            SwingUtilities.invokeLater(() -> {
                if (audioController.isMicrophoneAvailable()) {
                    callView.appendMessage("系统", "音频连接已建立，可以开始通话");
                    callView.appendMessage("系统", "音频检测已启动");
                } else {
                    callView.appendMessage("系统", "音频连接已建立（麦克风异常）");
                }
            });

        } catch (Exception e) {
            System.err.println("建立音频连接失败: " + e.getMessage());
            SwingUtilities.invokeLater(() -> {
                callView.appendMessage("系统", "音频连接失败: " + e.getMessage());
                mainView.showError("音频连接失败: " + e.getMessage());
            });
        }
    }

    /**
     * 启动TCP监听线程，用于接收通话结束信令
     */
    private void startTcpListenerThread() {
        if (tcpListenerThread != null && tcpListenerThread.isAlive()) {
            tcpListenerThread.interrupt();
        }

        tcpListenerThread = new Thread(() -> {
            try {
                Socket tcpSocket = networkModel.getTcpSocket();
                if (tcpSocket == null || tcpSocket.isClosed()) {
                    return;
                }

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(tcpSocket.getInputStream(), "UTF-8"));

                while (callModel.isCalling() && !callEnded && !Thread.currentThread().isInterrupted()) {
                    try {
                        String signal = in.readLine();
                        if (signal == null) {
                            // TCP连接已关闭
                            System.out.println("TCP连接已关闭");
                            handleRemoteHangup();
                            break;
                        }

                        System.out.println("收到TCP信号: " + signal);

                        if ("CALL_END".equals(signal.trim())) {
                            // 对方挂断了通话
                            handleRemoteHangup();
                            break;
                        }
                    } catch (SocketTimeoutException e) {
                        // 超时继续循环
                        continue;
                    } catch (IOException e) {
                        // TCP连接异常
                        System.err.println("TCP连接异常: " + e.getMessage());
                        if (!isLocalHangup) {
                            handleRemoteHangup();
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("TCP监听线程异常: " + e.getMessage());
                if (!isLocalHangup) {
                    handleRemoteHangup();
                }
            }
        }, "TCP-Listener-Thread");

        tcpListenerThread.start();
    }

    /**
     * 处理对方挂断的情况
     */
    private void handleRemoteHangup() {
        if (callEnded) {
            return; // 已经处理过了
        }

        callEnded = true;
        callModel.setCalling(false);

        SwingUtilities.invokeLater(() -> {
            callView.appendMessage("系统", "对方已挂断");
            callView.showCallEndNotification("对方已挂断通话");
            showHangupNotification();
        });

        // 延迟后自动结束通话
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 等待2秒，让用户看到通知
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            SwingUtilities.invokeLater(() -> {
                cleanupAfterCallEnd();
            });
        }).start();
    }

    /**
     * 发送通话结束信令
     */
    private void sendCallEndSignal() {
        if (isHangupNotified) {
            return; // 已经发送过或接收过结束信令
        }

        try {
            Socket tcpSocket = networkModel.getTcpSocket();
            if (tcpSocket != null && !tcpSocket.isClosed()) {
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(tcpSocket.getOutputStream(), "UTF-8"), true);
                out.println("CALL_END");
                out.flush();
                System.out.println("已发送CALL_END信号");
                isHangupNotified = true;
            }
        } catch (Exception e) {
            System.err.println("发送CALL_END信号失败: " + e.getMessage());
        }
    }

    /**
     * 显示挂断通知
     */
    private void showHangupNotification() {
        // 在通话界面显示明显的通知
        JOptionPane.showMessageDialog(
                callView,
                "对方已挂断通话",
                "通话结束",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    /**
     * 通话结束后的清理工作
     */
    private void cleanupAfterCallEnd() {
        // 停止音频检测定时器
        if (audioDetectionTimer != null) {
            audioDetectionTimer.stop();
        }

        // 重置音频检测状态
        audioController.resetAudioDetection();

        try {
            DatagramSocket udp = networkModel.getUdpSocket();
            if (udp != null && !udp.isClosed()) {
                udp.close();
            }
        } catch (Exception ignored) {}

        try {
            Socket s = networkModel.getTcpSocket();
            if (s != null && !s.isClosed()) {
                s.close();
            }
        } catch (Exception ignored) {}

        networkModel.setTcpSocket(null);
        networkModel.setUdpSocket(null);

        audioController.stopAudio();

        SwingUtilities.invokeLater(() -> {
            callView.resetAudioStatus();
            callView.appendMessage("系统", "通话结束");
            mainView.showInfo("通话已结束");
            cardLayout.show(mainPanel, "MAIN");
        });
    }

    /**
     * 更新音频检测状态
     */
    private void updateAudioDetectionStatus() {
        if (!callModel.isCalling() || callEnded) {
            if (audioDetectionTimer != null) {
                audioDetectionTimer.stop();
            }
            return;
        }

        // 这里可以添加持续的音频状态监测逻辑
        // 例如：检查当前说话者、音量等级等
    }

    /**
     * 被动接听方进入此流程
     */
    public void handleIncomingCallAccepted(Socket acceptedSocket, String remoteIP) {
        // 接听前检查麦克风
        if (!audioController.isMicrophoneAvailable()) {
            SwingUtilities.invokeLater(() -> {
                int choice = JOptionPane.showConfirmDialog(
                        null,
                        "麦克风未连接！对方将无法听到你的声音。\n是否继续接听？",
                        "麦克风警告",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (choice != JOptionPane.YES_OPTION) {
                    // 拒绝接听
                    try {
                        PrintWriter out = new PrintWriter(
                                new OutputStreamWriter(acceptedSocket.getOutputStream(), "UTF-8"), true);
                        out.println("DIAL_REJECT");
                        out.flush();
                        acceptedSocket.close();
                    } catch (Exception e) {
                        System.err.println("拒绝接听失败: " + e.getMessage());
                    }
                    return;
                }
            });
        }

        try {
            networkModel.setTcpSocket(acceptedSocket);

            // 设置TCP socket超时
            acceptedSocket.setSoTimeout(5000);

            SwingUtilities.invokeLater(() -> {
                String status = audioController.isMicrophoneAvailable() ?
                        "与 " + remoteIP + " 通话中..." :
                        "与 " + remoteIP + " 通话中（麦克风异常）";
                callView.setStatus(status);
                callView.appendMessage("系统", "接听来电，通话开始");
                cardLayout.show(mainPanel, "CALL");
            });

            // 建立音频连接
            setupAudioConnection();

        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> mainView.showError("建立通话失败: " + e.getMessage()));
        }
    }

    /**
     * 结束通话
     */
    private void endCall() {
        if (!callModel.isCalling() || callEnded) {
            return;
        }

        isLocalHangup = true; // 标记为本地挂断
        callEnded = true;
        callModel.setCalling(false);

        // 发送通话结束信令
        sendCallEndSignal();

        // 停止TCP监听线程
        if (tcpListenerThread != null && tcpListenerThread.isAlive()) {
            tcpListenerThread.interrupt();
        }

        cleanupAfterCallEnd();
    }

    /**
     * 发送音频消息
     */
    public void sendAudioMessage() {
        File f = audioController.getRecordedFile();
        if (f == null || !f.exists()) {
            mainView.showError("没有录音文件可发送，请先录音");
            return;
        }

        // 检查目标IP
        String remoteIP = callModel.getRemoteIP();
        if (remoteIP == null || remoteIP.trim().isEmpty()) {
            mainView.showError("请输入目标IP地址");
            return;
        }

        // 显示发送状态
        mainView.showInfo("正在发送音频消息到 " + remoteIP + " ...");

        // 调用音频控制器的发送方法
        audioController.sendAudioMessage(remoteIP, f);

        // 在主界面显示发送状态
        mainView.setRecordingStatus("消息发送中...", Color.BLUE);

        // 重置录音状态
        mainView.setRecordingState(false, false);

        mainView.showInfo("音频消息发送完成");
    }

    /**
     * 获取音频控制器（用于其他类访问）
     */
    public AudioController getAudioController() {
        return audioController;
    }

    /**
     * 获取服务器控制器（用于其他类访问）
     */
    public ServerController getServerController() {
        return serverController;
    }

    /**
     * 获取聊天控制器（用于其他类访问）
     */
    public ChatController getChatController() {
        return chatController;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        endCall();
        if (serverController != null) {
            serverController.stopServer();
        }
        if (audioDetectionTimer != null) {
            audioDetectionTimer.stop();
        }
        if (tcpListenerThread != null && tcpListenerThread.isAlive()) {
            tcpListenerThread.interrupt();
        }
        if (chatController != null) {
            chatController.closeChat();
        }
    }
    public void diagnoseChat() {
        StringBuilder diagnosis = new StringBuilder();
        diagnosis.append("=== 聊天功能诊断 ===\n\n");

        // 检查本地IP
        String localIP = networkModel.getLocalIP();
        diagnosis.append("1. 本地IP: ").append(localIP).append("\n");

        // 检查聊天控制器
        if (chatController == null) {
            diagnosis.append("2. 聊天控制器: 未初始化\n");
        } else {
            diagnosis.append("2. 聊天控制器: 已初始化\n");
            diagnosis.append("3. 聊天服务器状态: ").append(chatController.isServerRunning() ? "运行中" : "已停止").append("\n");
            diagnosis.append("4. 聊天状态: ").append(chatController.isChatting() ? "正在聊天" : "未在聊天").append("\n");
        }

        // 检查目标IP
        String targetIP = callModel.getRemoteIP();
        diagnosis.append("5. 目标IP: ").append(targetIP != null ? targetIP : "未设置").append("\n");

        // 测试端口连接
        if (targetIP != null && !targetIP.isEmpty()) {
            diagnosis.append("\n6. 网络连接测试:\n");
            try (Socket testSocket = new Socket()) {
                testSocket.connect(new InetSocketAddress(targetIP, 8283), 3000);
                diagnosis.append("   - 端口8283连接: 成功\n");
                testSocket.close();
            } catch (Exception e) {
                diagnosis.append("   - 端口8283连接: 失败 (").append(e.getMessage()).append(")\n");
            }
        }

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                    mainView,
                    diagnosis.toString(),
                    "聊天功能诊断",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });
    }
}