package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CallView extends BackgroundPanel {
    private JLabel statusLabel;
    private JButton muteButton;
    private JButton endCallButton;
    private JLabel localIPLabel;
    private boolean isMuted = false;
    private JTextArea messageArea;
    private JLabel audioStatusLabel;
    private javax.swing.Timer audioDetectionTimer;
    private JLabel callEndLabel;
    private JLabel connectionStatusLabel;

    // 舒适的字体
    private Font titleFont;
    private Font headerFont;
    private Font normalFont;
    private Font smallFont;
    private Font largeFont;

    public CallView(String localIP) {
        initializeUI(localIP);
    }

    private void initializeUI(String localIP) {
        // 初始化舒适的字体
        titleFont = new Font("Microsoft YaHei", Font.BOLD, 28);
        headerFont = new Font("Microsoft YaHei", Font.BOLD, 20);
        largeFont = new Font("Microsoft YaHei", Font.BOLD, 18);
        normalFont = new Font("Microsoft YaHei", Font.PLAIN, 16);
        smallFont = new Font("Microsoft YaHei", Font.PLAIN, 14);

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // 顶部面板
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        localIPLabel = new JLabel("本地IP: " + localIP, JLabel.LEFT);
        localIPLabel.setFont(smallFont);
        localIPLabel.setForeground(new Color(30, 100, 180));
        topPanel.add(localIPLabel, BorderLayout.WEST);

        JLabel titleLabel = new JLabel("通话中", JLabel.CENTER);
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(new Color(40, 90, 160));
        topPanel.add(titleLabel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // 中心面板：状态 + 音频状态 + 消息区域
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(30, 0, 30, 0));

        // 状态标签
        statusLabel = new JLabel("通话中...", JLabel.CENTER);
        statusLabel.setFont(headerFont);
        statusLabel.setForeground(new Color(40, 40, 40));
        centerPanel.add(statusLabel, BorderLayout.NORTH);

        // 连接状态显示
        JPanel connectionStatusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        connectionStatusPanel.setOpaque(false);
        connectionStatusLabel = new JLabel("连接正常", JLabel.CENTER);
        connectionStatusLabel.setFont(smallFont);
        connectionStatusLabel.setForeground(new Color(40, 160, 60));
        connectionStatusPanel.add(connectionStatusLabel);
        centerPanel.add(connectionStatusPanel, BorderLayout.CENTER);

        // 音频状态显示
        JPanel audioStatusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        audioStatusPanel.setOpaque(false);
        audioStatusLabel = new JLabel("等待音频输入...", JLabel.CENTER);
        audioStatusLabel.setFont(normalFont);
        audioStatusLabel.setForeground(new Color(80, 80, 80));
        audioStatusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 140, 200, 150)),
                BorderFactory.createEmptyBorder(10, 25, 10, 25)
        ));
        audioStatusLabel.setBackground(new Color(255, 255, 255, 180));
        audioStatusLabel.setOpaque(true);
        audioStatusLabel.setPreferredSize(new Dimension(380, 40));
        audioStatusPanel.add(audioStatusLabel);
        centerPanel.add(audioStatusPanel, BorderLayout.CENTER);

        // 通话结束通知标签（初始隐藏）
        JPanel callEndPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        callEndPanel.setOpaque(false);
        callEndLabel = new JLabel("对方已挂断", JLabel.CENTER);
        callEndLabel.setFont(headerFont);
        callEndLabel.setForeground(new Color(200, 60, 60));
        callEndLabel.setVisible(false);
        callEndPanel.add(callEndLabel);
        centerPanel.add(callEndPanel, BorderLayout.SOUTH);

        // 消息区域
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setOpaque(false);
        messagePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(80, 140, 200, 120), 2), "通话日志",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                largeFont,
                new Color(60, 120, 190)));

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(smallFont);
        messageArea.setBackground(new Color(255, 255, 255, 180));
        messageArea.setForeground(new Color(40, 40, 40));
        messageArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(messageArea);
        scroll.setPreferredSize(new Dimension(450, 120));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(80, 140, 200, 100)));
        setComponentTransparent(scroll);

        messagePanel.add(scroll, BorderLayout.CENTER);
        centerPanel.add(messagePanel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // 底部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 15));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        muteButton = createStyledButton("静音", normalFont, new Color(30, 144, 255, 220));
        muteButton.setPreferredSize(new Dimension(140, 45));

        endCallButton = createStyledButton("结束通话", normalFont, new Color(220, 53, 69, 220));
        endCallButton.setPreferredSize(new Dimension(140, 45));

        buttonPanel.add(muteButton);
        buttonPanel.add(endCallButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    // Action listeners
    public void addMuteButtonListener(ActionListener listener) {
        muteButton.addActionListener(listener);
    }

    public void addEndCallButtonListener(ActionListener listener) {
        endCallButton.addActionListener(listener);
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public void updateMuteButton(boolean muted) {
        isMuted = muted;
        muteButton.setText(muted ? "取消静音" : "静音");
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void updateLocalIP(String localIP) {
        localIPLabel.setText("本地IP: " + localIP);
    }

    // 往消息区追加一行（带时间）
    public void appendMessage(String who, String msg) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String ts = sdf.format(new Date());
        messageArea.append("[" + ts + "] " + who + ": " + msg + "\n");
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }

    // 新增：更新音频状态显示
    public void updateAudioStatus(String ip, long timestamp) {
        SwingUtilities.invokeLater(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String timeStr = sdf.format(new Date(timestamp));

            String statusText;
            if (ip != null && !ip.isEmpty()) {
                statusText = "检测到声音: " + ip + " (" + timeStr + ")";
                audioStatusLabel.setForeground(new Color(0, 100, 180));
                audioStatusLabel.setBackground(new Color(220, 240, 255, 200));
            } else {
                statusText = "等待音频输入...";
                audioStatusLabel.setForeground(new Color(100, 100, 100));
                audioStatusLabel.setBackground(new Color(255, 255, 255, 180));
            }

            audioStatusLabel.setText(statusText);

            // 同时在消息区域记录
            if (ip != null && !ip.isEmpty()) {
                appendMessage("系统", "检测到 " + ip + " 开始说话 (" + timeStr + ")");
            }
        });
    }

    // 新增：更新连接状态
    public void updateConnectionStatus(boolean connected, String message) {
        SwingUtilities.invokeLater(() -> {
            if (connected) {
                connectionStatusLabel.setText("连接正常");
                connectionStatusLabel.setForeground(new Color(40, 160, 60));
            } else {
                connectionStatusLabel.setText("连接异常: " + message);
                connectionStatusLabel.setForeground(Color.RED);
            }
        });
    }

    // 新增：重置音频状态
    public void resetAudioStatus() {
        SwingUtilities.invokeLater(() -> {
            audioStatusLabel.setText("等待音频输入...");
            audioStatusLabel.setForeground(new Color(100, 100, 100));
            audioStatusLabel.setBackground(new Color(255, 255, 255, 180));
            connectionStatusLabel.setText("连接正常");
            connectionStatusLabel.setForeground(new Color(40, 160, 60));
        });
    }

    // 新增：清空消息区域
    public void clearMessages() {
        SwingUtilities.invokeLater(() -> {
            messageArea.setText("");
        });
    }

    // 新增：获取当前音频状态文本
    public String getAudioStatusText() {
        return audioStatusLabel.getText();
    }

    // 新增：显示对方挂断通知
    public void showCallEndNotification(String message) {
        SwingUtilities.invokeLater(() -> {
            callEndLabel.setText(message);
            callEndLabel.setVisible(true);
            statusLabel.setText("通话已结束");
            statusLabel.setForeground(Color.RED);
            connectionStatusLabel.setText("连接已断开");
            connectionStatusLabel.setForeground(Color.RED);

            // 禁用控制按钮
            muteButton.setEnabled(false);
            endCallButton.setEnabled(false);
        });
    }

    // 新增：重置通话结束状态
    public void resetCallEndStatus() {
        SwingUtilities.invokeLater(() -> {
            callEndLabel.setVisible(false);
            statusLabel.setForeground(Color.BLACK);
            muteButton.setEnabled(true);
            endCallButton.setEnabled(true);
        });
    }

    // 新增：设置通话时长
    public void setCallDuration(String duration) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("通话中 (" + duration + ")");
        });
    }
}