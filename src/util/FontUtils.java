package util;

import java.awt.Font;
import java.util.Locale;

public class FontUtils {

    /**
     * 获取支持中文的字体
     */
    public static Font getChineseFont(int style, float size) {
        // 尝试多种支持中文的字体
        String[] preferredFonts = {
                "Microsoft YaHei",     // 微软雅黑
                "SimSun",              // 宋体
                "SimHei",              // 黑体
                "KaiTi",               // 楷体
                "FangSong",            // 仿宋
                "NSimSun",             // 新宋体
                "Microsoft JhengHei",  // 微软正黑体
                "Meiryo"               // 明瞭
        };

        // 通用字体族
        String[] fallbackFonts = {
                "Dialog",
                "SansSerif",
                "Serif",
                "Monospaced"
        };

        // 先尝试中文专用字体
        for (String fontName : preferredFonts) {
            Font font = new Font(fontName, style, (int) size);
            if (isChineseSupported(font)) {
                return font.deriveFont(size);
            }
        }

        // 再尝试通用字体
        for (String fontName : fallbackFonts) {
            Font font = new Font(fontName, style, (int) size);
            if (isChineseSupported(font)) {
                return font.deriveFont(size);
            }
        }

        // 最后返回默认字体
        return new Font("Dialog", style, (int) size).deriveFont(size);
    }

    /**
     * 获取普通中文字体
     */
    public static Font getChineseFont(float size) {
        return getChineseFont(Font.PLAIN, size);
    }

    /**
     * 获取粗体中文字体
     */
    public static Font getChineseBoldFont(float size) {
        return getChineseFont(Font.BOLD, size);
    }

    /**
     * 检查字体是否支持中文
     */
    private static boolean isChineseSupported(Font font) {
        return font.canDisplay('中') &&
                font.canDisplay('文') &&
                font.canDisplay('通') &&
                font.canDisplay('话');
    }

    /**
     * 设置组件的字体，包括所有子组件
     */
    public static void setComponentFont(java.awt.Component component, Font font) {
        component.setFont(font);
        if (component instanceof java.awt.Container) {
            for (java.awt.Component child : ((java.awt.Container) component).getComponents()) {
                setComponentFont(child, font);
            }
        }
    }
}