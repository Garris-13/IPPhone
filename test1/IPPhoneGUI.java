import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class IPPhoneGUI extends JFrame {
    private static final int DIAL_PORT = 6060;
    private static final int AUDIO_PORT = 6061;
    private static final int MESSAGE_PORT = 6062;

    private String remoteIP;
    private boolean isInCall = false;
    private String username;
    private AudioManager audioManager;
    private CallManager callManager;
    private CallSessionGUI callSessionGUI;

    // GUI组件
    private JTextArea statusArea;
    private JTextArea chatArea;
    private JTextField messageField;
    private JComboBox<String> targetIPCombo;
    private JButton callButton;
    private JButton hangupButton;
    private JButton sendButton;
    private JButton testAudioButton;
    private JButton scanNetworkButton;
    private JButton addIPButton;
    private JButton newCallButton;
    private JLabel statusLabel;
    private JComboBox<String> callTypeCombo;

    // IP历史记录
    private java.util.List<String> ipHistory;
    private static final String IP_HISTORY_FILE = "ip_history.txt";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new IPPhoneGUI().setVisible(true);
        });
    }

    public IPPhoneGUI() {
        this.username = "User_" + (System.currentTimeMillis() % 1000);
        this.audioManager = new AudioManager(AUDIO_PORT);
        this.callManager = new CallManager();
        this.ipHistory = loadIPHistory();
        initializeGUI();
        startServer();
    }

    private void initializeGUI() {
        setTitle("Java IP Phone - " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 650);
        setLocationRelativeTo(this);

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(240, 240, 240));

        // 状态面板
        JPanel statusPanel = createStatusPanel();

        // 目标IP选择面板
        JPanel ipSelectionPanel = createIPSelectionPanel();

        // 通话控制面板
        JPanel controlPanel = createControlPanel();

        // 聊天和状态面板
        JPanel contentPanel = createContentPanel();

        // 消息发送面板
        JPanel messagePanel = createMessagePanel();

        // 布局组合
        mainPanel.add(statusPanel, BorderLayout.NORTH);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(ipSelectionPanel, BorderLayout.NORTH);
        topPanel.add(controlPanel, BorderLayout.CENTER);
        mainPanel.add(topPanel, BorderLayout.CENTER);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(messagePanel, BorderLayout.SOUTH);

        add(mainPanel);

        // 事件监听器
        setupEventListeners();

        updateStatus("IP Phone GUI 版本已启动");
        updateStatus("用户名: " + username);
        updateStatus("本地IP: " + getLocalIP());
        updateStatus("服务器端口: " + DIAL_PORT + "(拨号), " + AUDIO_PORT + "(音频), " + MESSAGE_PORT + "(消息)");
        updateStatus("等待来电或开始通话...");
    }

    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("状态"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        statusPanel.setBackground(Color.WHITE);

        statusLabel = new JLabel("状态: 就绪");
        statusLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        statusLabel.setForeground(new Color(0, 100, 0));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        testAudioButton = createStyledButton("测试音频", new Color(70, 130, 180));
        buttonPanel.add(testAudioButton);
        statusPanel.add(buttonPanel, BorderLayout.EAST);

        return statusPanel;
    }

    private JPanel createIPSelectionPanel() {
        JPanel ipPanel = new JPanel(new BorderLayout(5, 5));
        ipPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("选择通话目标"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        ipPanel.setBackground(Color.WHITE);

        // IP选择区域
        JPanel ipSelectionArea = new JPanel(new FlowLayout(FlowLayout.LEFT));

        ipSelectionArea.add(new JLabel("目标IP:"));
        targetIPCombo = new JComboBox<>(ipHistory.toArray(new String[0]));
        targetIPCombo.setEditable(true);
        targetIPCombo.setPreferredSize(new Dimension(150, 28));
        targetIPCombo.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        ipSelectionArea.add(targetIPCombo);

        // 预置一些常见的IP段
        JComboBox<String> ipPresetCombo = new JComboBox<>(new String[]{
                "192.168.1.", "192.168.0.", "10.0.0.", "172.16.1."
        });
        ipPresetCombo.addActionListener(e -> {
            String preset = (String) ipPresetCombo.getSelectedItem();
            if (targetIPCombo.getSelectedItem() != null) {
                String currentIP = targetIPCombo.getSelectedItem().toString();
                if (currentIP.matches("\\d+\\.\\d+\\.\\d+\\..*")) {
                    String newIP = preset + currentIP.substring(currentIP.lastIndexOf('.') + 1);
                    targetIPCombo.setSelectedItem(newIP);
                }
            }
        });
        ipSelectionArea.add(new JLabel("IP段:"));
        ipSelectionArea.add(ipPresetCombo);

        ipPanel.add(ipSelectionArea, BorderLayout.CENTER);

        // 按钮区域
        JPanel ipButtonPanel = new JPanel(new FlowLayout());
        scanNetworkButton = createStyledButton("扫描网络", new Color(46, 139, 87));
        addIPButton = createStyledButton("添加IP", new Color(165, 42, 42));
        JButton clearHistoryButton = createStyledButton("清空历史", new Color(178, 34, 34));

        clearHistoryButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(this,
                    "确定要清空所有IP历史记录吗？", "清空历史",
                    JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                ipHistory.clear();
                targetIPCombo.removeAllItems();
                saveIPHistory();
                updateStatus("IP历史记录已清空");
            }
        });

        ipButtonPanel.add(scanNetworkButton);
        ipButtonPanel.add(addIPButton);
        ipButtonPanel.add(clearHistoryButton);

        ipPanel.add(ipButtonPanel, BorderLayout.EAST);

        return ipPanel;
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("通话控制"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        controlPanel.setBackground(Color.WHITE);

        // 新增：新通话按钮
        newCallButton = createStyledButton("新通话", new Color(65, 105, 225));
        controlPanel.add(newCallButton);

        controlPanel.add(new JLabel("通话方式:"));
        String[] callTypes = {"语音通话", "仅文字聊天", "语音+文字"};
        callTypeCombo = new JComboBox<>(callTypes);
        callTypeCombo.setSelectedIndex(0);
        callTypeCombo.setPreferredSize(new Dimension(120, 28));
        controlPanel.add(callTypeCombo);

        callButton = createStyledButton("开始通话", new Color(34, 139, 34));
        hangupButton = createStyledButton("结束通话", new Color(178, 34, 34));
        hangupButton.setEnabled(false);

        controlPanel.add(callButton);
        controlPanel.add(hangupButton);

        return controlPanel;
    }

    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 10, 10));

        // 聊天区域
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("聊天窗口"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        chatPanel.setBackground(Color.WHITE);

        chatArea = new JTextArea(15, 40);
        chatArea.setEditable(false);
        chatArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        chatArea.setBackground(new Color(248, 248, 255));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        // 系统信息区域
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("系统信息"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        infoPanel.setBackground(Color.WHITE);

        statusArea = new JTextArea(15, 30);
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        statusArea.setBackground(new Color(240, 240, 240));
        JScrollPane statusScroll = new JScrollPane(statusArea);
        infoPanel.add(statusScroll, BorderLayout.CENTER);

        contentPanel.add(chatPanel);
        contentPanel.add(infoPanel);

        return contentPanel;
    }

    private JPanel createMessagePanel() {
        JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
        messagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("发送消息"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        messagePanel.setBackground(Color.WHITE);

        messageField = new JTextField();
        messageField.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        sendButton = createStyledButton("发送", new Color(70, 130, 180));

        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);

        return messagePanel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("微软雅黑", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker()),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        return button;
    }

    private void setupEventListeners() {
        callButton.addActionListener(e -> startCall());

        hangupButton.addActionListener(e -> endCall());

        sendButton.addActionListener(e -> sendMessage());

        messageField.addActionListener(e -> sendMessage());

        testAudioButton.addActionListener(e -> testAudio());

        scanNetworkButton.addActionListener(e -> scanNetwork());

        addIPButton.addActionListener(e -> showAddIPDialog());

        newCallButton.addActionListener(e -> openCallSetup());

        // 窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });

        // IP选择框编辑事件
        targetIPCombo.addActionListener(e -> {
            if (e.getActionCommand().equals("comboBoxChanged")) {
                Object selectedItem = targetIPCombo.getSelectedItem();
                if (selectedItem != null) {
                    String ip = selectedItem.toString();
                    if (isValidIP(ip)) {
                        saveToIPHistory(ip);
                    }
                }
            }
        });
    }

    private void openCallSetup() {
        SwingUtilities.invokeLater(() -> {
            CallSetupGUI setupGUI = new CallSetupGUI(this);
            setupGUI.setVisible(true);
        });
    }

    private void startCall() {
        Object selectedItem = targetIPCombo.getSelectedItem();
        if (selectedItem == null) {
            showError("请选择或输入目标IP地址");
            return;
        }

        String ip = selectedItem.toString().trim();
        if (ip.isEmpty()) {
            showError("请选择或输入目标IP地址");
            return;
        }

        if (!isValidIP(ip)) {
            showError("IP地址格式不正确");
            return;
        }

        if (isInCall) {
            showError("当前正在通话中，请先结束当前通话");
            return;
        }

        String callType = (String) callTypeCombo.getSelectedItem();
        startCallFromSetup(ip, callType, "");
    }

    public void startCallFromSetup(String ip, String callType, String description) {
        this.remoteIP = ip;

        // 保存到历史记录
        saveToIPHistory(ip);

        updateChat("系统", "正在建立 " + callType + " 连接到 " + ip);
        if (!description.isEmpty()) {
            updateChat("系统", "通话描述: " + description);
        }

        // 调用CallManager创建通话会话
        String targetUser = "User@" + ip;
        CallManager.CallSession session = callManager.createCallSession(username, targetUser, getLocalIP(), ip);

        new Thread(() -> {
            try (Socket socket = new Socket(ip, DIAL_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                // 发送拨号请求（包含描述）
                if (description.isEmpty()) {
                    out.println("DIAL:" + username + ":" + callType);
                } else {
                    out.println("DIAL:" + username + ":" + callType + ":" + description);
                }
                updateStatus("发送拨号请求到 " + ip + ", 类型: " + callType);

                // 等待响应
                String response = in.readLine();
                if (response != null && response.startsWith("ACCEPT")) {
                    SwingUtilities.invokeLater(() -> {
                        updateChat("系统", callType + " 已连接!");
                        startCallSession(callType);
                    });

                    // 更新通话会话状态
                    session.acceptCall();
                    updateStatus("通话会话已建立: " + session.getSessionId());

                } else if (response != null && response.startsWith("REJECT")) {
                    SwingUtilities.invokeLater(() -> {
                        updateChat("系统", "对方拒绝接听");
                    });
                    session.rejectCall();
                } else {
                    SwingUtilities.invokeLater(() -> {
                        updateChat("系统", "呼叫失败: " + (response != null ? response : "无响应"));
                    });
                }

            } catch (ConnectException e) {
                SwingUtilities.invokeLater(() -> {
                    showError("无法连接到目标IP: " + ip + "\n请检查IP地址是否正确或对方是否在线");
                });
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    showError("拨号失败: " + e.getMessage());
                });
            }
        }).start();
    }

    private void startCallSession(String callType) {
        isInCall = true;

        // 根据通话类型启动相应的功能
        if (callType.contains("语音")) {
            audioManager.startRecording(remoteIP, AUDIO_PORT);
            updateStatus("语音通话已启动");
        }

        // 更新界面状态
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("状态: " + callType + " - " + remoteIP);
            statusLabel.setForeground(Color.RED);
            callButton.setEnabled(false);
            hangupButton.setEnabled(true);
            targetIPCombo.setEnabled(false);
            callTypeCombo.setEnabled(false);
            scanNetworkButton.setEnabled(false);
            addIPButton.setEnabled(false);
            newCallButton.setEnabled(false);
        });

        updateChat("系统", callType + " 正在进行中...");

        // 打开通话会话窗口
        SwingUtilities.invokeLater(() -> {
            callSessionGUI = new CallSessionGUI(this, remoteIP, callType);
            callSessionGUI.setVisible(true);
        });
    }

    private void endCall() {
        if (!isInCall) return;

        isInCall = false;

        // 停止音频传输
        audioManager.stopRecording();

        // 结束通话会话
        String sessionId = username + "-User@" + remoteIP + "-" + System.currentTimeMillis();
        callManager.endCallSession(sessionId);

        // 发送结束通话消息
        new Thread(() -> {
            try (Socket socket = new Socket(remoteIP, MESSAGE_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                out.println("CALL_END");
            } catch (IOException e) {
                // 忽略结束通话时的错误
            }
        }).start();

        // 关闭通话会话窗口
        if (callSessionGUI != null) {
            callSessionGUI.onCallEnded();
        }

        // 更新界面状态
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("状态: 就绪");
            statusLabel.setForeground(new Color(0, 100, 0));
            callButton.setEnabled(true);
            hangupButton.setEnabled(false);
            targetIPCombo.setEnabled(true);
            callTypeCombo.setEnabled(true);
            scanNetworkButton.setEnabled(true);
            addIPButton.setEnabled(true);
            newCallButton.setEnabled(true);
            updateChat("系统", "通话已结束");
        });
    }

    public void endCallFromCallSession() {
        endCall();
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty() || !isInCall) {
            return;
        }

        messageField.setText("");

        // 在聊天窗口显示
        updateChat("我", message);

        // 在通话会话窗口显示
        if (callSessionGUI != null) {
            callSessionGUI.appendMessage("我", message);
        }

        // 发送消息
        new Thread(() -> {
            try (Socket socket = new Socket(remoteIP, MESSAGE_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                out.println("MSG:" + message);

            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    showError("发送消息失败: " + e.getMessage());
                });
            }
        }).start();
    }

    public void sendMessageFromCallSession(String message) {
        // 在主界面聊天窗口显示
        updateChat("我", message);

        // 发送消息
        new Thread(() -> {
            try (Socket socket = new Socket(remoteIP, MESSAGE_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                out.println("MSG:" + message);

            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    showError("发送消息失败: " + e.getMessage());
                });
            }
        }).start();
    }

    private void testAudio() {
        new Thread(() -> {
            updateStatus("开始音频测试...");

            JDialog testDialog = new JDialog(this, "音频测试", true);
            testDialog.setLayout(new BorderLayout());
            testDialog.setSize(300, 150);
            testDialog.setLocationRelativeTo(this);

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JLabel label = new JLabel("正在测试音频设备...", JLabel.CENTER);
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);

            panel.add(label, BorderLayout.CENTER);
            panel.add(progressBar, BorderLayout.SOUTH);
            testDialog.add(panel);

            // 在后台执行测试
            new Thread(() -> {
                // 临时录制并播放自己的声音
                audioManager.startRecording("127.0.0.1", AUDIO_PORT);

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                audioManager.stopRecording();

                SwingUtilities.invokeLater(() -> {
                    testDialog.dispose();
                    updateStatus("音频测试结束");
                    JOptionPane.showMessageDialog(IPPhoneGUI.this,
                            "音频测试完成！\n请检查是否能听到自己的声音。",
                            "测试结果", JOptionPane.INFORMATION_MESSAGE);
                });
            }).start();

            testDialog.setVisible(true);
        }).start();
    }

    public void scanNetwork() {
        new Thread(() -> {
            updateStatus("开始扫描局域网...");

            String localIP = getLocalIP();
            String networkPrefix = localIP.substring(0, localIP.lastIndexOf('.') + 1);

            // 创建进度对话框
            ProgressDialog progressDialog = new ProgressDialog(this, "扫描网络", "正在扫描局域网设备...");
            progressDialog.setVisible(true);

            int foundCount = 0;
            for (int i = 1; i < 255; i++) {
                if (i % 50 == 0) {
                    final int progress = i;
                    SwingUtilities.invokeLater(() -> {
                        progressDialog.updateProgress(progress, 254);
                    });
                }

                String testIP = networkPrefix + i;
                if (!testIP.equals(localIP)) {
                    if (testIPAsync(testIP)) {
                        foundCount++;
                    }
                }

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                }
            }

            progressDialog.dispose();
            updateStatus("局域网扫描完成，找到 " + foundCount + " 个设备");
            JOptionPane.showMessageDialog(this,
                    "扫描完成！\n找到 " + foundCount + " 个在线设备。",
                    "扫描结果", JOptionPane.INFORMATION_MESSAGE);
        }).start();
    }

    private boolean testIPAsync(String ip) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, DIAL_PORT), 1000);
            // 连接成功，添加到历史记录
            if (!ipHistory.contains(ip)) {
                ipHistory.add(0, ip);
                saveIPHistory();
                updateStatus("发现设备: " + ip);
                SwingUtilities.invokeLater(() -> {
                    targetIPCombo.addItem(ip);
                });
                return true;
            }
        } catch (IOException e) {
            // 连接失败，忽略
        }
        return false;
    }

    public boolean testConnection(String ip) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, DIAL_PORT), 3000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void showAddIPDialog() {
        String ip = JOptionPane.showInputDialog(this,
                "请输入要添加的IP地址:", "添加IP", JOptionPane.QUESTION_MESSAGE);

        if (ip != null && !ip.trim().isEmpty()) {
            ip = ip.trim();
            if (isValidIP(ip)) {
                saveToIPHistory(ip);
                JOptionPane.showMessageDialog(this,
                        "IP地址 " + ip + " 已添加到列表", "成功", JOptionPane.INFORMATION_MESSAGE);
            } else {
                showError("IP地址格式不正确");
            }
        }
    }

    private boolean isValidIP(String ip) {
        return ip.matches("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    }

    private void startServer() {
        // 启动拨号服务器
        new Thread(new DialingServer()).start();
        // 启动消息服务器
        new Thread(new MessageServer()).start();

        updateStatus("服务器已启动，等待来电...");
    }

    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            statusArea.append("[" + getCurrentTime() + "] " + message + "\n");
            statusArea.setCaretPosition(statusArea.getDocument().getLength());
        });
    }

    private void updateChat(String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append("[" + getCurrentTime() + "] " + sender + ": " + message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    public void appendToCallSession(String sender, String message) {
        if (callSessionGUI != null) {
            callSessionGUI.appendMessage(sender, message);
        }
    }

    private void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "错误", JOptionPane.ERROR_MESSAGE);
        });
    }

    private void showIncomingCallDialog(String caller, String callerIP, String callType) {
        SwingUtilities.invokeLater(() -> {
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(new JLabel("<html><b>来电信息</b></html>"), BorderLayout.NORTH);

            JTextArea infoArea = new JTextArea();
            infoArea.setEditable(false);
            infoArea.setText("来电用户: " + caller + "\n" +
                    "IP地址: " + callerIP + "\n" +
                    "通话类型: " + callType);
            infoArea.setBackground(new Color(240, 240, 240));
            panel.add(new JScrollPane(infoArea), BorderLayout.CENTER);

            int result = JOptionPane.showConfirmDialog(this, panel, "来电",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

            if (result == JOptionPane.YES_OPTION) {
                remoteIP = callerIP;
                updateChat("系统", "接听 " + callType + " 来电");
                startCallSession(callType);

                // 发送接受响应
                new Thread(() -> {
                    try (Socket socket = new Socket(callerIP, DIAL_PORT);
                         PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                        out.println("ACCEPT");
                    } catch (IOException e) {
                        showError("接听失败: " + e.getMessage());
                    }
                }).start();
            } else {
                // 发送拒绝响应
                new Thread(() -> {
                    try (Socket socket = new Socket(callerIP, DIAL_PORT);
                         PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                        out.println("REJECT");
                    } catch (IOException e) {
                        // 忽略错误
                    }
                }).start();
                updateChat("系统", "已拒绝来电");
            }
        });
    }

    private void shutdown() {
        isInCall = false;
        audioManager.close();
        saveIPHistory();
        if (callSessionGUI != null) {
            callSessionGUI.dispose();
        }
        dispose();
    }

    private String getLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    private String getCurrentTime() {
        return new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
    }

    // IP历史记录管理
    public java.util.List<String> getIPHistory() {
        return ipHistory;
    }

    private java.util.List<String> loadIPHistory() {
        java.util.List<String> history = new ArrayList<>();
        history.add("192.168.1.100");
        history.add("192.168.1.101");
        history.add("127.0.0.1");

        // 从文件加载历史记录（如果存在）
        try (BufferedReader reader = new BufferedReader(new FileReader(IP_HISTORY_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty() && !history.contains(line.trim())) {
                    history.add(line.trim());
                }
            }
        } catch (IOException e) {
            // 文件不存在，使用默认值
        }
        return history;
    }

    public void saveToIPHistory(String ip) {
        if (!ipHistory.contains(ip)) {
            ipHistory.add(0, ip); // 添加到开头
            if (ipHistory.size() > 20) {
                ipHistory.remove(ipHistory.size() - 1); // 保持最多20条记录
            }
            saveIPHistory();
            SwingUtilities.invokeLater(() -> {
                targetIPCombo.insertItemAt(ip, 0);
                targetIPCombo.setSelectedItem(ip);
            });
        }
    }

    private void saveIPHistory() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(IP_HISTORY_FILE))) {
            for (String ip : ipHistory) {
                writer.println(ip);
            }
        } catch (IOException e) {
            System.err.println("保存IP历史记录失败: " + e.getMessage());
        }
    }

    // 进度对话框内部类
    class ProgressDialog extends JDialog {
        private JProgressBar progressBar;
        private JLabel label;

        public ProgressDialog(JFrame parent, String title, String message) {
            super(parent, title, true);
            setLayout(new BorderLayout());
            setSize(300, 120);
            setLocationRelativeTo(parent);
            setResizable(false);

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            label = new JLabel(message, JLabel.CENTER);
            progressBar = new JProgressBar(0, 100);
            progressBar.setIndeterminate(true);

            panel.add(label, BorderLayout.CENTER);
            panel.add(progressBar, BorderLayout.SOUTH);
            add(panel);
        }

        public void updateProgress(int value, int max) {
            progressBar.setIndeterminate(false);
            progressBar.setMaximum(max);
            progressBar.setValue(value);
            label.setText("扫描进度: " + value + "/" + max);
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
                updateStatus("拨号服务器错误: " + e.getMessage());
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
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                String request = in.readLine();
                if (request != null && request.startsWith("DIAL:")) {
                    String[] parts = request.split(":", 3);
                    if (parts.length >= 3) {
                        String caller = parts[1];
                        String callType = parts[2];
                        String callerIP = socket.getInetAddress().getHostAddress();

                        updateStatus("收到 " + callType + " 来电: " + caller + " (" + callerIP + ")");
                        showIncomingCallDialog(caller, callerIP, callType);
                    }
                }

            } catch (IOException e) {
                updateStatus("拨号处理错误: " + e.getMessage());
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
                updateStatus("消息服务器错误: " + e.getMessage());
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
                        SwingUtilities.invokeLater(() -> {
                            updateChat("系统", "对方已挂断通话");
                            endCall();
                        });
                        break;
                    } else if (message.startsWith("MSG:")) {
                        String text = message.substring(4);
                        updateChat("对方", text);
                        appendToCallSession("对方", text);
                    }
                }

            } catch (IOException e) {
                if (isInCall) {
                    updateStatus("消息处理错误: " + e.getMessage());
                }
            }
        }
    }
}