import controller.CallController;
import model.CallModel;
import model.NetworkModel;
import view.MainView;
import view.DialingView;
import view.CallView;
import util.FontUtils;
import javax.swing.*;
import java.awt.*;

public class IPPhone extends JFrame {
    private CallModel callModel;
    private NetworkModel networkModel;
    private MainView mainView;
    private DialingView dialingView;
    private CallView callView;
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private CallController callController;

    public IPPhone() {
        // 设置中文Look and Feel
        setChineseLookAndFeel();
        initializeModels();
        initializeViews();
        initializeController();
        setupFrame();
    }

    private void setChineseLookAndFeel() {
        try {
            // 使用系统默认的Look and Feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // 设置全局字体
            Font chineseFont = FontUtils.getChineseFont(14f);
            UIManager.put("Button.font", chineseFont);
            UIManager.put("Label.font", chineseFont);
            UIManager.put("TextField.font", chineseFont);
            UIManager.put("ComboBox.font", chineseFont);
            UIManager.put("OptionPane.messageFont", chineseFont);
            UIManager.put("OptionPane.buttonFont", chineseFont);

            // 设置全局按钮前景色为黑色
            UIManager.put("Button.foreground", Color.BLACK);

        } catch (Exception e) {
            System.err.println("设置Look and Feel失败: " + e.getMessage());
            // 忽略错误，使用默认设置
        }
    }

    private void initializeModels() {
        callModel = new CallModel();
        networkModel = new NetworkModel();
    }

    private void initializeViews() {
        // 获取本地IP并传递给各个视图
        String localIP = networkModel.getLocalIP();

        mainView = new MainView(localIP);
        dialingView = new DialingView(localIP);
        callView = new CallView(localIP);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(mainView, "MAIN");
        mainPanel.add(dialingView, "DIALING");
        mainPanel.add(callView, "CALL");
    }

    private void initializeController() {
        callController = new CallController(
                callModel, networkModel, mainView, dialingView, callView,
                mainPanel, cardLayout
        );
    }

    private void setupFrame() {
        setTitle("IP Phone - 本地IP: " + networkModel.getLocalIP());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 设置合适的初始大小 - 加大窗口尺寸
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) (screenSize.width * 0.8); // 屏幕宽度的80%
        int height = (int) (screenSize.height * 0.8); // 屏幕高度的80%

        // 确保最小尺寸
        int minWidth = 1000;
        int minHeight = 700;
        width = Math.max(width, minWidth);
        height = Math.max(height, minHeight);

        setSize(width, height);
        setLocationRelativeTo(null); // 居中显示

        add(mainPanel);
        cardLayout.show(mainPanel, "MAIN");

        // 确保窗口以合适大小显示
        packIfNeeded();
    }

    private void packIfNeeded() {
        // 确保窗口内容正确显示
        SwingUtilities.invokeLater(() -> {
            validate();
            repaint();
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            IPPhone phone = new IPPhone();
            phone.setVisible(true);
            // 确保窗口正确显示
            phone.setExtendedState(JFrame.NORMAL);

            // 添加窗口状态监听，确保正确显示
            phone.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    phone.validate();
                    phone.repaint();
                }
            });
        });
    }
}