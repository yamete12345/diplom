package ru.muiv.diplom.ui;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.muiv.diplom.domain.User;
import ru.muiv.diplom.service.AuthService;
import ru.muiv.diplom.util.LastLoginStore;
import ru.muiv.diplom.util.Session;

import javax.swing.*;
import java.awt.*;

@Component
public class LoginFrame extends JFrame {

    private final AuthService authService;
    private final Session session;
    private final ApplicationContext ctx;
    private final LastLoginStore lastLoginStore;

    private final JTextField emailField = new JTextField(24);
    private final JPasswordField passwordField = new JPasswordField(24);

    public LoginFrame(AuthService authService, Session session, ApplicationContext ctx,
                      LastLoginStore lastLoginStore) {
        super("Банковский модуль — Вход");
        this.authService = authService;
        this.session = session;
        this.ctx = ctx;
        this.lastLoginStore = lastLoginStore;
        buildUi();
        String last = lastLoginStore.load();
        if (last != null && !last.isBlank()) {
            emailField.setText(last);
            SwingUtilities.invokeLater(passwordField::requestFocusInWindow);
        }
    }

    private void buildUi() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(640, 520);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BankTheme.WINDOW_BG);

        // ───── Центр: карточка формы ─────
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(BankTheme.PANEL_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BankTheme.BORDER_LIGHT, 1, true),
                BorderFactory.createEmptyBorder(28, 36, 28, 36)));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 0, 6, 0);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        c.gridx = 0; c.gridy = 0;
        card.add(BankTheme.titleLabel("Вход в личный кабинет"), c);

        c.gridy++;
        card.add(BankTheme.dimLabel("Введите e-mail и пароль — дальше код придёт на почту"), c);

        c.gridy++; c.insets = new Insets(18, 0, 4, 0);
        card.add(labeled("E-mail", emailField), c);

        c.gridy++; c.insets = new Insets(6, 0, 4, 0);
        card.add(labeled("Пароль", passwordField), c);

        JButton loginBtn = new JButton("Войти");
        BankTheme.makePrimary(loginBtn);
        loginBtn.addActionListener(e -> doLogin());

        JButton registerBtn = new JButton("Регистрация");
        registerBtn.addActionListener(e -> openRegister());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(registerBtn);
        buttons.add(loginBtn);

        c.gridy++; c.insets = new Insets(20, 0, 0, 0);
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        card.add(buttons, c);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        center.add(card);

        root.add(center, BorderLayout.CENTER);
        root.add(BankTheme.statusBar("Java 21 · Spring Boot 3 · Swing + FlatLaf"), BorderLayout.SOUTH);

        getRootPane().setDefaultButton(loginBtn);
        setContentPane(root);
    }

    private JComponent labeled(String label, JComponent field) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = BankTheme.dimLabel(label);
        l.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        field.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height));
        p.add(l);
        p.add(Box.createVerticalStrut(4));
        p.add(field);
        return p;
    }

    private void doLogin() {
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());
        try {
            User user = authService.loginStepPassword(email, password);

            OtpDialog dialog = new OtpDialog(this, "Код из письма (вход)",
                    code -> authService.loginStepOtp(user.getId(), code));
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                session.setCurrentUser(user);
                lastLoginStore.save(user.getEmail());
                MainFrame mainFrame = ctx.getBean(MainFrame.class);
                mainFrame.reload();
                mainFrame.setVisible(true);
                this.setVisible(false);
                passwordField.setText("");
            } else if (dialog.wasSubmitted()) {
                JOptionPane.showMessageDialog(this,
                        "Неверный или просроченный код", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Ошибка входа", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openRegister() {
        RegisterFrame rf = ctx.getBean(RegisterFrame.class);
        rf.reset();
        rf.setVisible(true);
    }

    public void returnFromMain() {
        session.clear();
        String last = lastLoginStore.load();
        emailField.setText(last == null ? "" : last);
        passwordField.setText("");
        setVisible(true);
        SwingUtilities.invokeLater(passwordField::requestFocusInWindow);
    }
}
