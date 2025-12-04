package view;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.net.URL;

public class BackgroundPanel extends JPanel {
    protected Image backgroundImage;

    public BackgroundPanel() {
        loadBackgroundImage();
    }

    protected void loadBackgroundImage() {
        try {
            URL bgUrl = getClass().getResource("background.png");
            if (bgUrl != null) {
                backgroundImage = new ImageIcon(bgUrl).getImage();
            }
        } catch (Exception e) {
            System.err.println("加载背景图片失败: " + e.getMessage());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (backgroundImage != null) {
            // 绘制背景图片，使其适应组件大小
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            // 如果没有背景图片，使用渐变背景
            Graphics2D g2d = (Graphics2D) g;
            GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(245, 248, 250),
                    getWidth(), getHeight(), new Color(235, 242, 248)
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // 设置组件透明的方法
    protected void setComponentTransparent(JComponent component) {
        component.setOpaque(false);
        if (component instanceof JTextComponent) {
            ((JTextComponent) component).setOpaque(false);
            component.setBackground(new Color(255, 255, 255, 180));
        } else if (component instanceof JScrollPane) {
            component.setOpaque(false);
            ((JScrollPane) component).getViewport().setOpaque(false);
        } else if (component instanceof JPanel) {
            component.setOpaque(false);
        }
    }

    // 创建带样式的按钮
    protected JButton createStyledButton(String text, Font font, Color backgroundColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(backgroundColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();

                super.paintComponent(g);
            }
        };

        button.setFont(font);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // 添加鼠标悬停效果
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
                button.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 150), 2));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            }
        });

        return button;
    }
}