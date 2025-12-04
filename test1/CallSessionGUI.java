import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CallSessionGUI extends JFrame {
    private IPPhoneGUI mainApp;
    private String remoteIP;
    private String callType;
    private boolean isInCall;

    private JTextArea chatArea;
    private JTextField messageField;
    private JButton hangupButton;
    private JButton sendButton;
    private JLabel statusLabel;
    private JLabel timerLabel;

    public CallSessionGUI(IPPhoneGUI mainApp, String remoteIP, String callType) {
        this.mainApp = mainApp;
        this.remoteIP = remoteIP;
        this.callType = callType;
        this.isInCall = true;
        initializeGUI();
        startTimer();
    }

    private void initializeGUI() {
        setTitle("通话中 - " + remoteIP + " - Java IP Phone");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(mainApp);

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(240, 240, 240));

        // 状态栏
        JPanel statusPanel = createStatusPanel();

        // 聊天区域
        JPanel chatPanel = createChatPanel();

        // 控制面板
        JPanel controlPanel = createControlPanel();

        mainPanel.add(statusPanel, BorderLayout.NORTH);
        mainPanel.add(chatPanel, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // 窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                endCall();
            }
        });
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("通话状态"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        statusLabel = new JLabel("正在与 " + remoteIP + " 进行 " + callType);
        statusLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        statusLabel.setForeground(Color.RED);

        timerLabel = new JLabel("00:00", JLabel.RIGHT);
        timerLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        timerLabel.setForeground(Color.BLUE);

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(timerLabel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("聊天窗口"));
        panel.setBackground(Color.WHITE);

        chatArea = new JTextArea(15, 40);
        chatArea.setEditable(false);
        chatArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        chatArea.setBackground(new Color(248, 248, 255));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(chatArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);

        // 消息发送区域
        JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
        messagePanel.add(new JLabel("消息:"), BorderLayout.WEST);

        messageField = new JTextField();
        sendButton = new JButton("发送");
        sendButton.setBackground(new Color(70, 130, 180));
        sendButton.setForeground(Color.WHITE);

        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);

        // 挂断按钮
        hangupButton = new JButton("结束通话");
        hangupButton.setBackground(new Color(178, 34, 34));
        hangupButton.setForeground(Color.WHITE);
        hangupButton.setFont(new Font("微软雅黑", Font.BOLD, 14));
        hangupButton.setPreferredSize(new Dimension(120, 40));

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(hangupButton);

        panel.add(messagePanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // 事件监听
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        hangupButton.addActionListener(e -> endCall());

        return panel;
    }

    private void startTimer() {
        Timer timer = new Timer(1000, new ActionListener() {
            private int seconds = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                seconds++;
                int minutes = seconds / 60;
                int secs = seconds % 60;
                timerLabel.setText(String.format("%02d:%02d", minutes, secs));
            }
        });
        timer.start();
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        messageField.setText("");
        appendMessage("我", message);

        // 这里调用主程序的发送消息方法
        mainApp.sendMessageFromCallSession(message);
    }

    public void appendMessage(String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append("[" + getCurrentTime() + "] " + sender + ": " + message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void endCall() {
        if (!isInCall) return;

        isInCall = false;
        mainApp.endCallFromCallSession();
        dispose();
    }

    private String getCurrentTime() {
        return new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
    }

    public void onCallEnded() {
        isInCall = false;
        SwingUtilities.invokeLater(() -> {
            appendMessage("系统", "通话已结束");
            statusLabel.setText("通话已结束");
            statusLabel.setForeground(Color.GRAY);
            hangupButton.setEnabled(false);
            sendButton.setEnabled(false);
            messageField.setEnabled(false);
        });
    }
}