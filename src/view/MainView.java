package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.net.URL;

import util.FontUtils;

public class MainView extends BackgroundPanel {
    private JTextField ipField;
    private JComboBox<String> modeComboBox;
    private JButton connectButton;
    private JButton chatButton;
    private JLabel localIPLabel;
    private JButton recordButton;
    private JLabel recordingStatusLabel;
    private JButton serverToggleButton;
    private JLabel serverStatusLabel;
    private JTextArea messageArea;
    private JButton diagnoseButton;
    private JLabel imageLabel;

    // 定义舒适的字体
    private Font titleFont;
    private Font headerFont;
    private Font normalFont;
    private Font buttonFont;
    private Font smallFont;

    private boolean isRecording = false;
    private boolean hasRecorded = false;
    private boolean isServerRunning = false;

    public MainView(String localIP) {
        initializeUI(localIP);
    }

    private void initializeUI(String localIP) {
        // 初始化舒适的字体
        initializeFonts();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 25, 20, 25));

        // 顶部面板
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        // 左上角面板 - 包含图片和本地IP
        JPanel leftTopPanel = new JPanel();
        leftTopPanel.setLayout(new BoxLayout(leftTopPanel, BoxLayout.Y_AXIS));
        leftTopPanel.setOpaque(false);
        leftTopPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 20));

        // 添加图片
        try {
            URL imageUrl = getClass().getResource("IP.png");
            if (imageUrl != null) {
                ImageIcon originalIcon = new ImageIcon(imageUrl);
                int imageWidth = 180;
                int imageHeight = 120;

                double widthRatio = (double) imageWidth / originalIcon.getIconWidth();
                double heightRatio = (double) imageHeight / originalIcon.getIconHeight();
                double ratio = Math.min(widthRatio, heightRatio);

                int scaledWidth = (int) (originalIcon.getIconWidth() * ratio);
                int scaledHeight = (int) (originalIcon.getIconHeight() * ratio);

                Image scaledImage = originalIcon.getImage().getScaledInstance(
                        scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledImage);

                imageLabel = new JLabel(scaledIcon);
                imageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                imageLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
                leftTopPanel.add(imageLabel);

                // 添加图片下方的分隔线
                JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
                separator.setMaximumSize(new Dimension(scaledWidth, 2));
                separator.setForeground(new Color(80, 140, 200, 150));
                leftTopPanel.add(separator);

            } else {
                imageLabel = new JLabel("[IP图标]");
                imageLabel.setFont(smallFont);
                imageLabel.setForeground(new Color(80, 140, 200));
                imageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                leftTopPanel.add(imageLabel);
            }
        } catch (Exception e) {
            imageLabel = new JLabel("[图片加载失败]");
            imageLabel.setFont(smallFont);
            imageLabel.setForeground(new Color(100, 100, 100));
            imageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            leftTopPanel.add(imageLabel);
        }

        // 本地IP标签
        JPanel ipPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        ipPanel.setOpaque(false);
        localIPLabel = new JLabel("本地IP: " + localIP);
        localIPLabel.setFont(smallFont);
        localIPLabel.setForeground(new Color(30, 100, 180));
        localIPLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        ipPanel.add(localIPLabel);

        leftTopPanel.add(ipPanel);
        leftTopPanel.add(Box.createVerticalGlue());

        topPanel.add(leftTopPanel, BorderLayout.WEST);

        // 标题
        JLabel titleLabel = new JLabel("IP Phone");
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(new Color(40, 90, 160));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 标题面板
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        topPanel.add(titlePanel, BorderLayout.CENTER);
        topPanel.add(Box.createHorizontalStrut(150), BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // 左侧控制面板
        JPanel controlPanel = createControlPanel();

        // 右侧消息面板
        JPanel messagePanel = createMessagePanel();

        // 使用JSplitPane分割左右面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlPanel, messagePanel);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(450);
        splitPane.setDividerSize(8);
        splitPane.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        setComponentTransparent(splitPane);

        add(splitPane, BorderLayout.CENTER);

        // 底部按钮面板
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);

        // 初始化状态
        updateUIForMode();
    }

    private void initializeFonts() {
        // 使用更舒适的字体方案
        titleFont = new Font("Microsoft YaHei", Font.BOLD, 38);
        headerFont = new Font("Microsoft YaHei", Font.BOLD, 18);
        normalFont = new Font("Microsoft YaHei", Font.PLAIN, 16);
        buttonFont = new Font("Microsoft YaHei", Font.PLAIN, 15);
        smallFont = new Font("Microsoft YaHei", Font.PLAIN, 14);
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new GridBagLayout());
        setComponentTransparent(controlPanel);
        controlPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(80, 140, 200, 120), 2), "控制面板",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                headerFont,
                new Color(60, 120, 190)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 15, 12, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // 服务器状态
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel serverLabel = new JLabel("服务器状态:");
        serverLabel.setFont(normalFont);
        serverLabel.setForeground(new Color(50, 50, 50));
        controlPanel.add(serverLabel, gbc);

        gbc.gridx = 1;
        serverStatusLabel = new JLabel("已停止", JLabel.LEFT);
        serverStatusLabel.setFont(normalFont);
        serverStatusLabel.setForeground(Color.RED);
        controlPanel.add(serverStatusLabel, gbc);

        // 目标IP
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel targetIPLabel = new JLabel("目标IP:");
        targetIPLabel.setFont(normalFont);
        targetIPLabel.setForeground(new Color(50, 50, 50));
        controlPanel.add(targetIPLabel, gbc);

        gbc.gridx = 1;
        ipField = new JTextField();
        ipField.setFont(normalFont);
        ipField.setPreferredSize(new Dimension(180, 32));
        ipField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 140, 200, 150)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        ipField.setBackground(new Color(255, 255, 255, 200));
        ipField.setForeground(new Color(40, 40, 40));
        controlPanel.add(ipField, gbc);

        // 通信方式
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel modeLabel = new JLabel("通信方式:");
        modeLabel.setFont(normalFont);
        modeLabel.setForeground(new Color(50, 50, 50));
        controlPanel.add(modeLabel, gbc);

        gbc.gridx = 1;
        String[] modes = {"拨号通话", "音频消息"};
        modeComboBox = new JComboBox<>(modes);
        modeComboBox.setFont(normalFont);
        modeComboBox.setPreferredSize(new Dimension(150, 32));
        modeComboBox.setBackground(new Color(255, 255, 255, 200));
        modeComboBox.setForeground(new Color(40, 40, 40));
        modeComboBox.addActionListener(e -> updateUIForMode());
        controlPanel.add(modeComboBox, gbc);

        // 录音状态
        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel recordStatusLabel = new JLabel("录音状态:");
        recordStatusLabel.setFont(normalFont);
        recordStatusLabel.setForeground(new Color(50, 50, 50));
        controlPanel.add(recordStatusLabel, gbc);

        gbc.gridx = 1;
        recordingStatusLabel = new JLabel("", JLabel.LEFT);
        recordingStatusLabel.setFont(normalFont);
        recordingStatusLabel.setForeground(Color.RED);
        controlPanel.add(recordingStatusLabel, gbc);

        return controlPanel;
    }

    private JPanel createMessagePanel() {
        JPanel messagePanel = new JPanel(new BorderLayout());
        setComponentTransparent(messagePanel);
        messagePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(80, 140, 200, 120), 2), "消息记录",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                headerFont,
                new Color(60, 120, 190)));

        messageArea = new JTextArea(16, 35);
        messageArea.setEditable(false);
        messageArea.setFont(normalFont);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        messageArea.setBackground(new Color(255, 255, 255, 200));
        messageArea.setForeground(new Color(40, 40, 40));

        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(400, 350));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(80, 140, 200, 100)));
        setComponentTransparent(scrollPane);

        messagePanel.add(scrollPane, BorderLayout.CENTER);
        return messagePanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(80, 140, 200, 100)),
                BorderFactory.createEmptyBorder(10, 0, 5, 0)));

        serverToggleButton = createStyledButton("启动服务器", buttonFont, new Color(76, 175, 80, 220));
        serverToggleButton.setPreferredSize(new Dimension(140, 40));

        recordButton = createStyledButton("开始录音", buttonFont, new Color(220, 53, 69, 220));
        recordButton.setPreferredSize(new Dimension(140, 40));
        recordButton.setVisible(false);

        connectButton = createStyledButton("连接", buttonFont, new Color(0, 123, 255, 220));
        connectButton.setPreferredSize(new Dimension(140, 40));

        chatButton = createStyledButton("在线聊天", buttonFont, new Color(30, 144, 255, 220));
        chatButton.setPreferredSize(new Dimension(140, 40));

        diagnoseButton = createStyledButton("诊断", buttonFont, new Color(255, 165, 0, 220));
        diagnoseButton.setPreferredSize(new Dimension(140, 40));

        buttonPanel.add(serverToggleButton);
        buttonPanel.add(recordButton);
        buttonPanel.add(connectButton);
        buttonPanel.add(chatButton);
        buttonPanel.add(diagnoseButton);

        return buttonPanel;
    }

    public void addDiagnoseButtonListener(ActionListener listener) {
        if (diagnoseButton != null) {
            diagnoseButton.addActionListener(listener);
        }
    }

    private void updateUIForMode() {
        String selectedMode = getSelectedMode();
        if ("音频消息".equals(selectedMode)) {
            recordButton.setVisible(true);
            connectButton.setText("发送消息");
            recordingStatusLabel.setText("点击开始录音");
        } else {
            recordButton.setVisible(false);
            connectButton.setText("连接");
            recordingStatusLabel.setText("");
            isRecording = false;
            hasRecorded = false;
            recordButton.setText("开始录音");
        }
    }

    public void setRecordingState(boolean recording, boolean hasRecordedAudio) {
        isRecording = recording;
        hasRecorded = hasRecordedAudio;

        if (isRecording) {
            recordButton.setText("停止录音");
            recordingStatusLabel.setText("录音中...");
            recordingStatusLabel.setForeground(Color.RED);
        } else {
            recordButton.setText("开始录音");
            if (hasRecorded) {
                recordingStatusLabel.setText("录音完成，点击发送");
                recordingStatusLabel.setForeground(new Color(0, 100, 200));
            } else {
                recordingStatusLabel.setText("点击开始录音");
                recordingStatusLabel.setForeground(new Color(120, 120, 120));
            }
        }
    }

    public void setServerState(boolean running) {
        isServerRunning = running;
        if (running) {
            serverToggleButton.setText("停止服务器");
            serverStatusLabel.setText("运行中");
            serverStatusLabel.setForeground(new Color(40, 167, 69));
        } else {
            serverToggleButton.setText("启动服务器");
            serverStatusLabel.setText("已停止");
            serverStatusLabel.setForeground(Color.RED);
        }
    }

    // Getters for user input
    public String getRemoteIP() {
        return ipField.getText().trim();
    }

    public String getSelectedMode() {
        return (String) modeComboBox.getSelectedItem();
    }

    public boolean hasRecordedAudio() {
        return hasRecorded;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isServerRunning() {
        return isServerRunning;
    }

    // Action listeners
    public void addConnectButtonListener(ActionListener listener) {
        connectButton.addActionListener(listener);
    }

    public void addChatButtonListener(ActionListener listener) {
        chatButton.addActionListener(listener);
    }

    public void addRecordButtonListener(ActionListener listener) {
        recordButton.addActionListener(listener);
    }

    public void addServerToggleListener(ActionListener listener) {
        serverToggleButton.addActionListener(listener);
    }

    // Utility methods
    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "错误", JOptionPane.ERROR_MESSAGE);
    }

    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "信息", JOptionPane.INFORMATION_MESSAGE);
    }

    public void clearFields() {
        ipField.setText("");
        recordingStatusLabel.setText("点击开始录音");
        recordingStatusLabel.setForeground(new Color(120, 120, 120));
        isRecording = false;
        hasRecorded = false;
        recordButton.setText("开始录音");
    }

    public void updateLocalIP(String localIP) {
        localIPLabel.setText("本地IP: " + localIP);
    }

    public void setRecordingStatus(String status, Color color) {
        recordingStatusLabel.setText(status);
        recordingStatusLabel.setForeground(color);
    }

    public void appendMessage(String who, String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String timestamp = sdf.format(new Date());
        String formattedMessage;

        if (message.length() > 60) {
            formattedMessage = message.replaceAll("(.{60})", "$1\n     ");
        } else {
            formattedMessage = message;
        }

        messageArea.append("[" + timestamp + "] " + who + ": " + formattedMessage + "\n\n");
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }

    // 添加清空消息的方法
    public void clearMessages() {
        messageArea.setText("");
    }

    // 添加获取消息区域的方法
    public JTextArea getMessageArea() {
        return messageArea;
    }

    // 添加更新图片的方法
    public void updateImage(ImageIcon newImage) {
        if (imageLabel != null && newImage != null) {
            imageLabel.setIcon(newImage);
        }
    }
}