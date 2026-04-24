package ru.muiv.diplom.ui;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

/**
 * Визуальная тема приложения — перенос дизайна из handoff-бандла
 * (Bank Module.html, defaults: accent=crimson, density=comfy, font=sf/segoe).
 *
 * <h3>Дизайн-токены</h3>
 * <ul>
 *   <li>Акцент (кнопки/рамки фокуса): {@link #ACCENT} <code>#B32A3A</code></li>
 *   <li>Фон окна: <code>#f2f2f2</code>, панели: <code>#ffffff</code></li>
 *   <li>Текст: <code>#1f1f1f</code>, приглушённый: <code>#6b6b6b</code></li>
 *   <li>Таблица: заголовок <code>#e8e8e8</code>, выделение <code>#F5D4D8</code></li>
 *   <li>Шрифт UI: Segoe UI / SF Pro Text, 13 pt</li>
 *   <li>Шрифт цифр (баланс, № счёта): JetBrains Mono → Consolas fallback, 13 pt</li>
 * </ul>
 */
public final class BankTheme {

    public static final Color ACCENT        = new Color(0xB3, 0x2A, 0x3A);
    public static final Color ACCENT_HOVER  = new Color(0x92, 0x20, 0x30);
    public static final Color ACCENT_SOFT   = new Color(0xFA, 0xDF, 0xE2);
    public static final Color TABLE_SEL     = new Color(0xF5, 0xD4, 0xD8);

    public static final Color WINDOW_BG     = new Color(0xF2, 0xF2, 0xF2);
    public static final Color PANEL_BG      = Color.WHITE;
    public static final Color PANEL_ALT_BG  = new Color(0xF7, 0xF7, 0xF7);
    public static final Color BORDER        = new Color(0xC8, 0xC8, 0xC8);
    public static final Color BORDER_LIGHT  = new Color(0xE1, 0xE1, 0xE1);
    public static final Color TEXT          = new Color(0x1F, 0x1F, 0x1F);
    public static final Color TEXT_DIM      = new Color(0x6B, 0x6B, 0x6B);
    public static final Color TABLE_HEADER  = new Color(0xE8, 0xE8, 0xE8);
    public static final Color STATUS_BAR    = new Color(0xE4, 0xE4, 0xE4);

    public static final Color SUCCESS       = new Color(0x0F, 0x9D, 0x58);
    public static final Color DANGER        = new Color(0xC5, 0x22, 0x1F);

    public static final Font FONT_UI = pickFont(13f, Font.PLAIN,
            "SF Pro Text", "Segoe UI Variable", "Segoe UI", Font.SANS_SERIF);
    public static final Font FONT_UI_BOLD = FONT_UI.deriveFont(Font.BOLD);
    public static final Font FONT_UI_MEDIUM = pickFont(13f, Font.PLAIN,
            "Segoe UI Semibold", "SF Pro Text Semibold", "Segoe UI", Font.SANS_SERIF)
            .deriveFont(Font.BOLD);
    public static final Font FONT_MONO = pickFont(13f, Font.PLAIN,
            "JetBrains Mono", "Consolas", "Menlo", Font.MONOSPACED);
    public static final Font FONT_TITLE = pickFont(18f, Font.PLAIN,
            "SF Pro Display", "Segoe UI", Font.SANS_SERIF).deriveFont(Font.BOLD);

    private BankTheme() {}

    /** Устанавливает FlatLaf и все UIManager-ключи. Вызывается один раз при старте. */
    public static void install() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ignored) {}

        // Акцент и отступы FlatLaf — темизация одним ключом
        UIManager.put("@accentColor", ACCENT);
        UIManager.put("Component.focusColor", ACCENT);
        UIManager.put("Component.focusedBorderColor", ACCENT);
        UIManager.put("Component.arc", 6);
        UIManager.put("Button.arc", 6);
        UIManager.put("TextComponent.arc", 4);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));

        // Плотность "comfy" — увеличенные внутренние отступы
        UIManager.put("Button.margin", new Insets(6, 16, 6, 16));
        UIManager.put("Button.minimumWidth", 88);
        UIManager.put("TextComponent.padding", new Insets(6, 10, 6, 10));

        // Цветовая палитра
        UIManager.put("Panel.background", WINDOW_BG);
        UIManager.put("MenuBar.background", new Color(0xEC, 0xEC, 0xEC));
        UIManager.put("Table.selectionBackground", TABLE_SEL);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.alternateRowColor", PANEL_ALT_BG);
        UIManager.put("Table.gridColor", BORDER_LIGHT);
        UIManager.put("TableHeader.background", TABLE_HEADER);
        UIManager.put("List.selectionBackground", TABLE_SEL);
        UIManager.put("List.selectionForeground", TEXT);

        // Шрифты
        UIManager.put("defaultFont", FONT_UI);
        UIManager.put("Button.font", FONT_UI);
        UIManager.put("Label.font", FONT_UI);
        UIManager.put("TextField.font", FONT_UI);
        UIManager.put("PasswordField.font", FONT_UI);
        UIManager.put("Table.font", FONT_UI);
        UIManager.put("TableHeader.font", FONT_UI_MEDIUM);
        UIManager.put("List.font", FONT_UI);
        UIManager.put("TitledBorder.font", FONT_UI_MEDIUM);

        // TitledBorder как в дизайне
        UIManager.put("TitledBorder.titleColor", TEXT);
    }

    /** Помечает кнопку как первичную (crimson + белый текст). */
    public static void makePrimary(AbstractButton b) {
        b.putClientProperty("JButton.buttonType", "default");
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setFont(FONT_UI_MEDIUM);
    }

    /** Помечает кнопку как опасную (красная обводка). */
    public static void makeDanger(AbstractButton b) {
        b.setForeground(DANGER);
    }

    /** Заголовок раздела (крупный, как в дизайне). */
    public static JLabel titleLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_TITLE);
        l.setForeground(TEXT);
        return l;
    }

    /** Приглушённый вспомогательный текст. */
    public static JLabel dimLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_DIM);
        return l;
    }

    /** Статус-бар в нижней части окна. */
    public static JPanel statusBar(String text) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(STATUS_BAR);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_DIM);
        l.setFont(FONT_UI.deriveFont(12f));
        bar.add(l);
        return bar;
    }

    private static Font pickFont(float size, int style, String... families) {
        String[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        java.util.Set<String> set = new java.util.HashSet<>(java.util.Arrays.asList(installed));
        for (String f : families) {
            if (set.contains(f)) return new Font(f, style, (int) size).deriveFont(size);
        }
        // Фоллбэк — последний аргумент (например, Font.SANS_SERIF)
        return new Font(families[families.length - 1], style, (int) size).deriveFont(size);
    }
}
