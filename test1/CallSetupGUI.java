import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class CallSetupGUI extends JFrame {
    private IPPhoneGUI mainApp;
    private JComboBox<String> callTypeCombo;
    private JComboBox<String> targetIPCombo;
    private JTextArea descriptionArea;
    private java.util.List<String> ipHistory;

    public CallSetupGUI(IPPhoneGUI mainApp) {
        this.mainApp = mainApp;
        this.ipHistory = mainApp.getIPHistory();
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("设置通话 - Java IP Phone");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(mainApp);
        setResizable(false);

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(240, 240, 240));

        // 标题
        JLabel titleLabel = new JLabel("设置新通话", JLabel.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        titleLabel.setForeground(new Color(0, 100, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // 内容面板
        JPanel contentPanel = new JPanel(new GridLayout(4, 1, 10, 15));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 1. 通话方式选择
        JPanel callTypePanel = createCallTypePanel();

        // 2. 目标IP选择
        JPanel ipSelectionPanel = createIPSelectionPanel();

        // 3. 通话描述
        JPanel descriptionPanel = createDescriptionPanel();

        // 4. 按钮面板
        JPanel buttonPanel = createButtonPanel();

        contentPanel.add(callTypePanel);
        contentPanel.add(ipSelectionPanel);
        contentPanel.add(descriptionPanel);
        contentPanel.add(buttonPanel);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        add(mainPanel);
    }

    private JPanel createCallTypePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.WHITE);

        JLabel label = new JLabel("通话方式:");
        label.setFont(new Font("微软雅黑", Font.BOLD, 14));
        label.setPreferredSize(new Dimension(80, 25));

        String[] callTypes = {
                "语音通话",
                "仅文字聊天",
                "语音+文字"
        };

        callTypeCombo = new JComboBox<>(callTypes);
        callTypeCombo.setPreferredSize(new Dimension(150, 30));
        callTypeCombo.setFont(new Font("微软雅黑", Font.PLAIN, 12));

        // 添加图标或说明
        JLabel typeHelp = new JLabel("(语音通话需要麦克风和扬声器)");
        typeHelp.setFont(new Font("微软雅黑", Font.ITALIC, 11));
        typeHelp.setForeground(Color.GRAY);

        panel.add(label);
        panel.add(callTypeCombo);
        panel.add(typeHelp);

        return panel;
    }

    private JPanel createIPSelectionPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);

        JLabel label = new JLabel("目标IP地址:");
        label.setFont(new Font("微软雅黑", Font.BOLD, 14));

        // IP选择区域
        JPanel ipPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ipPanel.setBackground(Color.WHITE);

        targetIPCombo = new JComboBox<>(ipHistory.toArray(new String[0]));
        targetIPCombo.setEditable(true);
        targetIPCombo.setPreferredSize(new Dimension(150, 30));
        targetIPCombo.setFont(new Font("微软雅黑", Font.PLAIN, 12));

        JButton scanButton = new JButton("扫描网络");
        scanButton.setBackground(new Color(70, 130, 180));
        scanButton.setForeground(Color.WHITE);
        scanButton.addActionListener(e -> scanNetwork());

        JButton addButton = new JButton("添加IP");
        addButton.setBackground(new Color(46, 139, 87));
        addButton.setForeground(Color.WHITE);
        addButton.addActionListener(e -> addIP());

        ipPanel.add(targetIPCombo);
        ipPanel.add(scanButton);
        ipPanel.add(addButton);

        panel.add(label, BorderLayout.NORTH);
        panel.add(ipPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createDescriptionPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);

        JLabel label = new JLabel("通话描述 (可选):");
        label.setFont(new Font("微软雅黑", Font.BOLD, 14));

        descriptionArea = new JTextArea(3, 30);
        descriptionArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setText("您好，我想与您通话...");
        descriptionArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JScrollPane scrollPane = new JScrollPane(descriptionArea);

        panel.add(label, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panel.setBackground(Color.WHITE);

        JButton startButton = createStyledButton("开始通话", new Color(34, 139, 34));
        JButton testButton = createStyledButton("测试连接", new Color(70, 130, 180));
        JButton cancelButton = createStyledButton("取消", new Color(178, 34, 34));

        startButton.addActionListener(e -> startCall());
        testButton.addActionListener(e -> testConnection());
        cancelButton.addActionListener(e -> dispose());

        panel.add(testButton);
        panel.add(startButton);
        panel.add(cancelButton);

        return panel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("微软雅黑", Font.BOLD, 12));
        button.setPreferredSize(new Dimension(100, 35));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker()),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        return button;
    }

    private void scanNetwork() {
        // 调用主程序的扫描网络功能
        mainApp.scanNetwork();

        // 更新IP列表
        SwingUtilities.invokeLater(() -> {
            targetIPCombo.setModel(new DefaultComboBoxModel<>(
                    mainApp.getIPHistory().toArray(new String[0])
            ));
        });

        JOptionPane.showMessageDialog(this,
                "网络扫描已完成，请查看IP列表更新。",
                "扫描完成", JOptionPane.INFORMATION_MESSAGE);
    }

    private void addIP() {
        String ip = JOptionPane.showInputDialog(this,
                "请输入要添加的IP地址:", "添加IP", JOptionPane.QUESTION_MESSAGE);

        if (ip != null && !ip.trim().isEmpty()) {
            ip = ip.trim();
            if (isValidIP(ip)) {
                mainApp.saveToIPHistory(ip);
                targetIPCombo.addItem(ip);
                targetIPCombo.setSelectedItem(ip);
                JOptionPane.showMessageDialog(this,
                        "IP地址 " + ip + " 已添加到列表", "成功", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "IP地址格式不正确", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void testConnection() {
        Object selectedItem = targetIPCombo.getSelectedItem();
        if (selectedItem == null || selectedItem.toString().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "请先选择或输入目标IP地址", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String ip = selectedItem.toString().trim();
        if (!isValidIP(ip)) {
            JOptionPane.showMessageDialog(this,
                    "IP地址格式不正确", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 显示测试进度
        JDialog testDialog = new JDialog(this, "测试连接", true);
        testDialog.setLayout(new BorderLayout());
        testDialog.setSize(300, 120);
        testDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel label = new JLabel("正在测试连接到 " + ip + "...", JLabel.CENTER);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);

        panel.add(label, BorderLayout.CENTER);
        panel.add(progressBar, BorderLayout.SOUTH);
        testDialog.add(panel);

        // 在后台测试连接
        new Thread(() -> {
            boolean success = mainApp.testConnection(ip);

            SwingUtilities.invokeLater(() -> {
                testDialog.dispose();
                if (success) {
                    JOptionPane.showMessageDialog(this,
                            "连接测试成功！\nIP: " + ip + " 可以访问",
                            "测试结果", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "连接测试失败！\nIP: " + ip + " 无法访问\n请检查IP地址或对方是否在线",
                            "测试结果", JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();

        testDialog.setVisible(true);
    }

    private void startCall() {
        Object selectedItem = targetIPCombo.getSelectedItem();
        if (selectedItem == null || selectedItem.toString().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "请选择或输入目标IP地址", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String ip = selectedItem.toString().trim();
        if (!isValidIP(ip)) {
            JOptionPane.showMessageDialog(this,
                    "IP地址格式不正确", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String callType = (String) callTypeCombo.getSelectedItem();
        String description = descriptionArea.getText().trim();

        // 保存到历史记录
        mainApp.saveToIPHistory(ip);

        // 关闭设置窗口
        dispose();

        // 调用主程序开始通话
        mainApp.startCallFromSetup(ip, callType, description);
    }

    private boolean isValidIP(String ip) {
        return ip.matches("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    }
}