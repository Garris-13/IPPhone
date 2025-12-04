package controller;

import model.NetworkModel;
import view.ChatView;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatController {
    private String localIP;
    private String remoteIP;
    private ChatView chatView;
    private NetworkModel networkModel;

    private Socket chatSocket;
    private ServerSocket chatServerSocket;
    private BufferedReader in;
    private PrintWriter out;

    private Thread chatListenerThread;
    private Thread chatServerThread;
    private final AtomicBoolean isChatting = new AtomicBoolean(false);
    private final AtomicBoolean isChatClosed = new AtomicBoolean(true);
    private final AtomicBoolean isServerRunning = new AtomicBoolean(false);

    // 聊天端口 - 使用新的端口8283
    private static final int CHAT_PORT = 8283;

    // 回调接口，用于通知主界面聊天请求到达
    public interface ChatRequestCallback {
        void onChatRequest(String remoteIP, ChatController controller);
        void onChatAccepted(String remoteIP);
        void onChatRejected(String remoteIP);
        void onChatClosed(String remoteIP);
    }

    private ChatRequestCallback callback;

    public ChatController(String localIP, String remoteIP, NetworkModel networkModel) {
        this.localIP = localIP;
        this.remoteIP = remoteIP;
        this.networkModel = networkModel;
    }

    public void setCallback(ChatRequestCallback callback) {
        this.callback = callback;
    }

    // 添加setRemoteIP方法
    public void setRemoteIP(String remoteIP) {
        this.remoteIP = remoteIP;
    }

    /**
     * 启动聊天服务器（被动等待连接）
     */
    public boolean startChatServer() {
        if (isServerRunning.get()) {
            System.out.println("聊天服务器已经在运行");
            return true;
        }

        try {
            // 设置SO_REUSEADDR，允许端口重用
            chatServerSocket = new ServerSocket(CHAT_PORT, 50);
            chatServerSocket.setReuseAddress(true);
            isServerRunning.set(true);

            System.out.println("聊天服务器启动成功，监听端口: " + CHAT_PORT);

            // 在新线程中等待连接
            chatServerThread = new Thread(() -> {
                while (isServerRunning.get() && chatServerSocket != null && !chatServerSocket.isClosed()) {
                    try {
                        System.out.println("等待聊天连接...");
                        Socket incomingSocket = chatServerSocket.accept();
                        incomingSocket.setReuseAddress(true);
                        incomingSocket.setSoTimeout(5000);

                        System.out.println("收到新连接: " + incomingSocket.getInetAddress().getHostAddress());

                        // 在新线程中处理连接请求
                        new Thread(() -> {
                            handleIncomingChatRequest(incomingSocket);
                        }, "ChatRequestHandler").start();

                    } catch (SocketException e) {
                        if (chatServerSocket != null && chatServerSocket.isClosed()) {
                            System.out.println("聊天服务器套接字已关闭");
                        } else {
                            System.err.println("聊天服务器接受连接异常: " + e.getMessage());
                        }
                    } catch (Exception e) {
                        System.err.println("聊天服务器异常: " + e.getMessage());
                    }
                }
                System.out.println("聊天服务器线程结束");
            }, "ChatServerThread");

            chatServerThread.start();
            return true;

        } catch (Exception e) {
            System.err.println("启动聊天服务器失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 停止聊天服务器
     */
    public void stopChatServer() {
        isServerRunning.set(false);

        try {
            if (chatServerSocket != null && !chatServerSocket.isClosed()) {
                chatServerSocket.close();
                System.out.println("聊天服务器已关闭");
            }
        } catch (Exception e) {
            System.err.println("关闭聊天服务器失败: " + e.getMessage());
        }

        // 等待服务器线程结束
        if (chatServerThread != null && chatServerThread.isAlive()) {
            try {
                chatServerThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 处理传入的聊天请求
     */
    private void handleIncomingChatRequest(Socket socket) {
        String clientIP = socket.getInetAddress().getHostAddress();
        System.out.println("处理聊天请求来自: " + clientIP);

        BufferedReader reader = null;
        PrintWriter writer = null;

        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            // 读取连接请求
            String request = reader.readLine();
            String requestRemoteIP = reader.readLine();

            System.out.println("收到聊天请求: " + request + " 来自: " + requestRemoteIP);

            if ("CHAT_REQUEST".equals(request) && requestRemoteIP != null) {
                this.remoteIP = requestRemoteIP;

                // 检查是否已经在聊天中
                if (isChatting.get()) {
                    System.out.println("已经在聊天中，拒绝新请求");
                    writer.println("CHAT_REJECT");
                    writer.flush();
                    socket.close();
                    return;
                }

                // 通知主界面有聊天请求到达
                if (callback != null) {
                    final String finalRemoteIP = requestRemoteIP;
                    final BufferedReader finalReader = reader;
                    final PrintWriter finalWriter = writer;
                    final Socket finalSocket = socket;

                    SwingUtilities.invokeLater(() -> {
                        // 显示聊天请求对话框
                        int choice = JOptionPane.showConfirmDialog(
                                null,
                                "来自 " + finalRemoteIP + " 的聊天邀请，是否接受？",
                                "聊天请求",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE
                        );

                        if (choice == JOptionPane.YES_OPTION) {
                            // 接受聊天请求
                            finalWriter.println("CHAT_ACCEPT");
                            finalWriter.flush();

                            // 保存连接
                            this.chatSocket = finalSocket;
                            this.in = finalReader;
                            this.out = finalWriter;

                            isChatting.set(true);
                            isChatClosed.set(false);

                            // 通知回调
                            callback.onChatAccepted(finalRemoteIP);

                            // 启动聊天监听器
                            startChatListener();

                        } else {
                            // 拒绝聊天请求
                            finalWriter.println("CHAT_REJECT");
                            finalWriter.flush();
                            try {
                                finalSocket.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            // 通知回调
                            callback.onChatRejected(finalRemoteIP);
                        }
                    });
                } else {
                    // 没有回调，直接拒绝
                    writer.println("CHAT_REJECT");
                    writer.flush();
                    socket.close();
                }
            } else {
                System.out.println("无效的聊天请求");
                socket.close();
            }

        } catch (Exception e) {
            System.err.println("处理聊天请求异常: " + e.getMessage());
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception ex) {
                // 忽略关闭异常
            }
        }
    }

    /**
     * 发起聊天请求
     */
    public boolean sendChatRequest(String remoteIP) {
        if (isChatting.get()) {
            System.out.println("已经在聊天中，无法发送新请求");
            return false;
        }

        // 设置远程IP
        this.remoteIP = remoteIP;

        System.out.println("尝试连接到: " + remoteIP + ":" + CHAT_PORT);

        try {
            // 创建新的Socket，设置超时和重用选项
            chatSocket = new Socket();
            chatSocket.setReuseAddress(true);
            chatSocket.setSoTimeout(10000); // 10秒超时
            chatSocket.connect(new InetSocketAddress(remoteIP, CHAT_PORT), 5000);

            System.out.println("已连接到聊天服务器");

            // 获取输入输出流
            in = new BufferedReader(new InputStreamReader(chatSocket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(chatSocket.getOutputStream(), "UTF-8"), true);

            // 发送聊天请求
            out.println("CHAT_REQUEST");
            out.println(localIP);
            out.flush();

            System.out.println("已发送聊天请求到: " + remoteIP);

            // 等待对方响应
            String response = in.readLine();
            System.out.println("收到聊天响应: " + response);

            if ("CHAT_ACCEPT".equals(response)) {
                // 对方接受了聊天
                chatSocket.setSoTimeout(5000); // 设置正常超时
                isChatting.set(true);
                isChatClosed.set(false);

                // 启动聊天监听器
                startChatListener();

                System.out.println("聊天请求被接受");
                return true;

            } else if ("CHAT_REJECT".equals(response)) {
                // 对方拒绝了聊天
                System.out.println("对方拒绝了聊天请求");
                closeChatConnection();
                return false;
            } else {
                // 未知响应
                System.out.println("收到未知响应: " + response);
                closeChatConnection();
                return false;
            }

        } catch (SocketTimeoutException e) {
            System.err.println("连接超时: " + e.getMessage());
            closeChatConnection();
            return false;
        } catch (ConnectException e) {
            System.err.println("连接被拒绝，对方可能没有启动聊天服务器: " + e.getMessage());
            closeChatConnection();
            return false;
        } catch (Exception e) {
            System.err.println("发送聊天请求失败: " + e.getMessage());
            e.printStackTrace();
            closeChatConnection();
            return false;
        }
    }

    /**
     * 启动聊天消息监听器
     */
    private void startChatListener() {
        if (chatListenerThread != null && chatListenerThread.isAlive()) {
            chatListenerThread.interrupt();
            try {
                chatListenerThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        chatListenerThread = new Thread(() -> {
            System.out.println("聊天监听器启动");
            try {
                while (isChatting.get() && !Thread.currentThread().isInterrupted()) {
                    try {
                        String message = in.readLine();
                        if (message == null) {
                            // 连接已关闭
                            System.out.println("聊天连接已关闭（null）");
                            handleRemoteChatClose();
                            break;
                        }

                        System.out.println("收到聊天消息: " + message);

                        if ("CHAT_CLOSE".equals(message)) {
                            // 对方关闭了聊天
                            handleRemoteChatClose();
                            break;
                        } else if (message.startsWith("CHAT_MSG:")) {
                            // 收到聊天消息
                            String[] parts = message.split(":", 2);
                            if (parts.length == 2) {
                                String msgContent = parts[1];
                                if (chatView != null) {
                                    SwingUtilities.invokeLater(() -> {
                                        chatView.appendMessage(remoteIP, msgContent);
                                    });
                                }
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        // 超时继续循环
                        continue;
                    } catch (IOException e) {
                        // 连接异常
                        System.err.println("聊天连接异常: " + e.getMessage());
                        if (isChatting.get()) {
                            handleRemoteChatClose();
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("聊天监听器异常: " + e.getMessage());
                if (isChatting.get()) {
                    handleRemoteChatClose();
                }
            }
            System.out.println("聊天监听器结束");
        }, "ChatListenerThread");

        chatListenerThread.start();
    }

    /**
     * 发送聊天消息
     */
    public void sendChatMessage(String message) {
        if (!isChatting.get() || out == null) {
            System.out.println("无法发送消息，聊天未连接");
            return;
        }

        try {
            // 发送消息格式：CHAT_MSG:消息内容
            out.println("CHAT_MSG:" + message);
            out.flush();

            System.out.println("已发送消息: " + message);

            // 在本地聊天界面显示
            if (chatView != null) {
                SwingUtilities.invokeLater(() -> {
                    chatView.appendMessage(localIP, message);
                });
            }

        } catch (Exception e) {
            System.err.println("发送聊天消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理对方关闭聊天
     */
    private void handleRemoteChatClose() {
        System.out.println("处理对方关闭聊天");

        if (!isChatting.get() || isChatClosed.get()) {
            System.out.println("聊天已经关闭，无需重复处理");
            return;
        }

        isChatting.set(false);
        isChatClosed.set(true);

        SwingUtilities.invokeLater(() -> {
            if (chatView != null && chatView.isVisible()) {
                chatView.updateStatus("对方已退出聊天",
                        Color.RED);
                chatView.appendMessage("系统", "对方(" + remoteIP + ")已退出聊天");

                // 显示通知
                JOptionPane.showMessageDialog(
                        chatView,
                        "对方已退出聊天，聊天窗口将自动关闭",
                        "聊天结束",
                        JOptionPane.INFORMATION_MESSAGE
                );

                // 关闭聊天窗口
                closeChatWindow();
            }

            // 通知回调
            if (callback != null) {
                callback.onChatClosed(remoteIP);
            }
        });

        // 关闭连接
        closeChatConnection();
    }

    /**
     * 关闭聊天
     */
    public void closeChat() {
        System.out.println("关闭聊天请求");

        if (isChatClosed.get()) {
            System.out.println("聊天已经关闭");
            return;
        }

        isChatting.set(false);
        isChatClosed.set(true);

        System.out.println("正在关闭聊天连接...");

        // 通知对方关闭聊天
        if (out != null) {
            try {
                out.println("CHAT_CLOSE");
                out.flush();
                System.out.println("已发送CHAT_CLOSE信号");
            } catch (Exception e) {
                System.err.println("发送聊天关闭通知失败: " + e.getMessage());
            }
        }

        // 关闭连接
        closeChatConnection();

        // 关闭聊天窗口
        SwingUtilities.invokeLater(() -> {
            closeChatWindow();

            // 通知回调
            if (callback != null) {
                callback.onChatClosed(remoteIP);
            }
        });
    }

    /**
     * 关闭聊天窗口
     */
    private void closeChatWindow() {
        System.out.println("关闭聊天窗口");

        if (chatView != null) {
            try {
                if (chatView.isVisible()) {
                    chatView.dispose();
                }
                chatView = null;
            } catch (Exception e) {
                System.err.println("关闭聊天窗口失败: " + e.getMessage());
            }
        }
    }

    /**
     * 关闭聊天连接
     */
    private void closeChatConnection() {
        System.out.println("关闭聊天连接...");

        // 停止监听线程
        if (chatListenerThread != null && chatListenerThread.isAlive()) {
            try {
                chatListenerThread.interrupt();
                chatListenerThread.join(1000);
                System.out.println("聊天监听线程已停止");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 关闭流和套接字
        try {
            if (in != null) {
                in.close();
                in = null;
            }
        } catch (Exception e) {
            System.err.println("关闭输入流失败: " + e.getMessage());
        }

        try {
            if (out != null) {
                out.close();
                out = null;
            }
        } catch (Exception e) {
            System.err.println("关闭输出流失败: " + e.getMessage());
        }

        try {
            if (chatSocket != null && !chatSocket.isClosed()) {
                chatSocket.close();
                chatSocket = null;
                System.out.println("聊天套接字已关闭");
            }
        } catch (Exception e) {
            System.err.println("关闭聊天套接字失败: " + e.getMessage());
        }

        System.out.println("聊天连接清理完成");
    }

    /**
     * 完全重置聊天控制器
     */
    public void reset() {
        System.out.println("重置聊天控制器");

        isChatting.set(false);
        isChatClosed.set(true);

        // 关闭现有连接
        closeChatConnection();

        // 停止服务器
        stopChatServer();

        // 清理视图
        closeChatWindow();

        // 重新启动服务器
        startChatServer();

        System.out.println("聊天控制器重置完成");
    }

    /**
     * 设置聊天视图
     */
    public void setChatView(ChatView chatView) {
        this.chatView = chatView;
    }

    /**
     * 检查是否正在聊天
     */
    public boolean isChatting() {
        return isChatting.get() && !isChatClosed.get();
    }

    /**
     * 检查服务器是否在运行
     */
    public boolean isServerRunning() {
        return isServerRunning.get();
    }

    /**
     * 获取本地IP
     */
    public String getLocalIP() {
        return localIP;
    }

    /**
     * 获取远程IP
     */
    public String getRemoteIP() {
        return remoteIP;
    }
}