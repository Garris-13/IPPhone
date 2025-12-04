package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

import controller.ChatController;

public class ChatView extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton closeButton;
    private JLabel statusLabel;
    private JLabel localIPLabel;
    private JLabel remoteIPLabel;

    private BackgroundPanel mainPanel; // 使用背景面板作为主面板

    private String localIP;
    private String remoteIP;
    private ChatController chatController;

    // 舒适的字体
    private Font titleFont;
    private Font normalFont;
    private Font smallFont;
    private Font largeFont;

    public ChatView(String localIP, String remoteIP) {
        this.localIP = localIP;
        this.remoteIP = remoteIP;
        initializeUI();
    }

    private void initializeUI() {
        // 初始化舒适的字体
        titleFont = new Font("Microsoft YaHei", Font.BOLD, 22);
        largeFont = new Font("Microsoft YaHei", Font.BOLD, 18);
        normalFont = new Font("Microsoft YaHei", Font.PLAIN, 16);
        smallFont = new Font("Microsoft YaHei", Font.PLAIN, 14);

        setTitle("在线聊天 - 本地IP: " + localIP + " | 对方IP: " + remoteIP);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(750, 600);
        setLocationRelativeTo(null);
        setResizable(true);

        // 添加窗口关闭监听器
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeChat();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                System.out.println("聊天窗口已关闭");
            }
        });

        // 使用背景面板作为内容面板
        mainPanel = new BackgroundPanel();
        mainPanel.setLayout(new BorderLayout());
        setContentPane(mainPanel);

        // 顶部状态面板
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 左侧IP信息面板
        JPanel ipPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        ipPanel.setOpaque(false);

        localIPLabel = new JLabel("本地IP: " + localIP);
        localIPLabel.setFont(smallFont);
        localIPLabel.setForeground(new Color(30, 100, 180));

        remoteIPLabel = new JLabel("对方IP: " + remoteIP);
        remoteIPLabel.setFont(smallFont);
        remoteIPLabel.setForeground(new Color(180, 60, 60));

        ipPanel.add(localIPLabel);
        ipPanel.add(remoteIPLabel);

        topPanel.add(ipPanel, BorderLayout.WEST);

        // 状态标签
        statusLabel = new JLabel("聊天已连接", JLabel.CENTER);
        statusLabel.setFont(titleFont);
        statusLabel.setForeground(new Color(40, 160, 60));
        topPanel.add(statusLabel, BorderLayout.CENTER);

        // 添加右侧空白
        topPanel.add(Box.createHorizontalStrut(100), BorderLayout.EAST);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // 聊天内容区域
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(normalFont);
        chatArea.setBackground(new Color(255, 255, 255, 200));
        chatArea.setForeground(new Color(40, 40, 40));
        chatArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(80, 140, 200, 100)));
        mainPanel.setComponentTransparent(scrollPane);

        centerPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // 底部输入面板
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        inputField = new JTextField();
        inputField.setFont(normalFont);
        inputField.setBackground(new Color(255, 255, 255, 220));
        inputField.setForeground(new Color(40, 40, 40));
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 140, 200, 150)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        inputField.addActionListener(e -> sendMessage());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonPanel.setOpaque(false);

        sendButton = new JButton("发送");
        sendButton.setFont(normalFont);
        sendButton.setBackground(new Color(30, 144, 255, 220));
        sendButton.setForeground(Color.BLACK);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));

        closeButton = new JButton("关闭聊天");
        closeButton.setFont(normalFont);
        closeButton.setBackground(new Color(220, 53, 69, 220));
        closeButton.setForeground(Color.BLACK);
        closeButton.setFocusPainted(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));

        buttonPanel.add(sendButton);
        buttonPanel.add(closeButton);

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // 添加按钮悬停效果
        addButtonHoverEffect(sendButton);
        addButtonHoverEffect(closeButton);
    }

    private void addButtonHoverEffect(JButton button) {
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
    }

    public void setChatController(ChatController chatController) {
        this.chatController = chatController;
    }

    // 添加消息到聊天区域
    public void appendMessage(String ip, String message) {
        SwingUtilities.invokeLater(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String timestamp = sdf.format(new Date());
            String sender = ip.equals(localIP) ? "我" : "对方(" + ip + ")";

            // 添加样式
            chatArea.append("[" + timestamp + "] ");
            chatArea.append(sender + ": ");
            chatArea.append(message + "\n\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    // 更新状态
    public void updateStatus(String status, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
            statusLabel.setForeground(color);
        });
    }

    // 获取输入的消息
    public String getInputMessage() {
        return inputField.getText().trim();
    }

    // 清空输入框
    public void clearInput() {
        SwingUtilities.invokeLater(() -> {
            inputField.setText("");
        });
    }

    // 发送消息
    private void sendMessage() {
        String message = getInputMessage();
        if (!message.isEmpty() && chatController != null) {
            chatController.sendChatMessage(message);
            clearInput();
        }
    }

    // 关闭聊天
    public void closeChat() {
        System.out.println("ChatView.closeChat() 被调用");

        // 如果聊天控制器存在，调用其关闭方法
        if (chatController != null) {
            System.out.println("调用 ChatController.closeChat()");
            chatController.closeChat();
        } else {
            // 如果控制器不存在，直接关闭窗口
            System.out.println("ChatController 为 null，直接关闭窗口");
            dispose();
        }
    }

    // 添加事件监听器
    public void addSendButtonListener(ActionListener listener) {
        sendButton.addActionListener(listener);
        inputField.addActionListener(listener);
    }

    public void addCloseButtonListener(ActionListener listener) {
        closeButton.addActionListener(e -> {
            System.out.println("关闭聊天按钮被点击");
            closeChat();
        });
    }

    // 获取本地IP
    public String getLocalIP() {
        return localIP;
    }

    // 获取对方IP
    public String getRemoteIP() {
        return remoteIP;
    }

    // 清空聊天记录
    public void clearChat() {
        SwingUtilities.invokeLater(() -> {
            chatArea.setText("");
        });
    }

    // 获取聊天区域
    public JTextArea getChatArea() {
        return chatArea;
    }
}