package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class DialingView extends BackgroundPanel {
    private JLabel statusLabel;
    private JButton cancelButton;
    private JLabel localIPLabel;
    private JLabel microphoneStatusLabel;

    // 舒适的字体
    private Font titleFont;
    private Font headerFont;
    private Font normalFont;
    private Font smallFont;
    private Font largeFont;

    public DialingView(String localIP) {
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

        JLabel titleLabel = new JLabel("正在拨号", JLabel.CENTER);
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(new Color(40, 90, 160));
        topPanel.add(titleLabel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // 中心面板
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(40, 0, 40, 0));

        // 创建状态信息面板
        JPanel statusInfoPanel = new JPanel(new GridLayout(3, 1, 15, 15));
        statusInfoPanel.setOpaque(false);

        // 主状态标签
        statusLabel = new JLabel("正在拨号...", JLabel.CENTER);
        statusLabel.setFont(headerFont);
        statusLabel.setForeground(new Color(40, 40, 40));
        statusInfoPanel.add(statusLabel);

        // 麦克风状态标签
        microphoneStatusLabel = new JLabel("检查麦克风...", JLabel.CENTER);
        microphoneStatusLabel.setFont(normalFont);
        microphoneStatusLabel.setForeground(new Color(220, 140, 40));
        statusInfoPanel.add(microphoneStatusLabel);

        // 连接状态标签
        JLabel connectionStatusLabel = new JLabel("建立连接中...", JLabel.CENTER);
        connectionStatusLabel.setFont(smallFont);
        connectionStatusLabel.setForeground(new Color(100, 100, 100));
        statusInfoPanel.add(connectionStatusLabel);

        centerPanel.add(statusInfoPanel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // 底部面板
        JPanel bottomPanel = new JPanel(new FlowLayout());
        bottomPanel.setOpaque(false);

        cancelButton = createStyledButton("取消", normalFont, new Color(220, 53, 69, 220));
        cancelButton.setPreferredSize(new Dimension(140, 45));
        bottomPanel.add(cancelButton);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    // Action listeners
    public void addCancelButtonListener(ActionListener listener) {
        cancelButton.addActionListener(listener);
    }

    // Status update methods
    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    // 更新本地IP显示
    public void updateLocalIP(String localIP) {
        localIPLabel.setText("本地IP: " + localIP);
    }

    // 新增：更新麦克风状态显示
    public void setMicrophoneStatus(boolean available, String message) {
        SwingUtilities.invokeLater(() -> {
            if (available) {
                microphoneStatusLabel.setText("麦克风: 就绪 ✓");
                microphoneStatusLabel.setForeground(new Color(40, 160, 60));
            } else {
                microphoneStatusLabel.setText("麦克风: " + message);
                microphoneStatusLabel.setForeground(Color.RED);
            }
        });
    }

    // 新增：更新连接状态
    public void setConnectionStatus(String status, boolean isSuccess) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
            if (isSuccess) {
                statusLabel.setForeground(new Color(40, 160, 60));
            } else {
                statusLabel.setForeground(Color.RED);
            }
        });
    }

    // 新增：设置拨号进度
    public void setProgress(String progress) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("正在拨号 - " + progress);
        });
    }
}