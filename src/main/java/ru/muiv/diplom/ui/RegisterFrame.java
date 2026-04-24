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
public class RegisterFrame extends JFrame {

    private final AuthService authService;
    private final Session session;
    private final ApplicationContext ctx;
    private final LastLoginStore lastLoginStore;

    private final JTextField nameField = new JTextField(24);
    private final JTextField emailField = new JTextField(24);
    private final JPasswordField passwordField = new JPasswordField(24);
    private final JPasswordField password2Field = new JPasswordField(24);

    public RegisterFrame(AuthService authService, Session session, ApplicationContext ctx,
                         LastLoginStore lastLoginStore) {
        super("Банковский модуль — Регистрация");
        this.authService = authService;
        this.session = session;
        this.ctx = ctx;
        this.lastLoginStore = lastLoginStore;
        buildUi();
    }

    private void buildUi() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(640, 560);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BankTheme.WINDOW_BG);

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

        int row = 0;
        c.gridx = 0; c.gridy = row++;
        card.add(BankTheme.titleLabel("Регистрация"), c);

        c.gridy = row++;
        card.add(BankTheme.dimLabel("После ввода данных на указанный e-mail придёт код подтверждения"), c);

        c.insets = new Insets(18, 0, 4, 0);
        c.gridy = row++;  card.add(field("ФИО",           nameField),      c);
        c.insets = new Insets(6, 0, 4, 0);
        c.gridy = row++;  card.add(field("E-mail",        emailField),     c);
        c.gridy = row++;  card.add(field("Пароль",        passwordField),  c);
        c.gridy = row++;  card.add(field("Повтор пароля", password2Field), c);

        JButton submit = new JButton("Зарегистрироваться");
        BankTheme.makePrimary(submit);
        submit.addActionListener(e -> doRegister());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(submit);

        c.gridy = row++; c.insets = new Insets(20, 0, 0, 0);
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        card.add(buttons, c);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        center.add(card);

        root.add(center, BorderLayout.CENTER);
        root.add(BankTheme.statusBar("Java 21 · Swing + FlatLaf"), BorderLayout.SOUTH);

        getRootPane().setDefaultButton(submit);
        setContentPane(root);
    }

    private JComponent field(String label, JComponent f) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = BankTheme.dimLabel(label);
        l.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        f.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, f.getPreferredSize().height));
        p.add(l);
        p.add(Box.createVerticalStrut(4));
        p.add(f);
        return p;
    }

    public void reset() {
        nameField.setText("");
        emailField.setText("");
        passwordField.setText("");
        password2Field.setText("");
    }

    private void doRegister() {
        String pwd = new String(passwordField.getPassword());
        String pwd2 = new String(password2Field.getPassword());
        if (!pwd.equals(pwd2)) {
            JOptionPane.showMessageDialog(this, "Пароли не совпадают",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String token;
        try {
            token = authService.register(nameField.getText(), emailField.getText(), pwd);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Ошибка регистрации", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Подтверждение OTP возвращает созданного пользователя — перехватываем,
        // чтобы сразу открыть MainFrame без повторного входа.
        User[] createdUser = new User[1];
        OtpDialog dialog = new OtpDialog(this, "Подтверждение e-mail",
                code -> {
                    User u = authService.confirmRegistration(token, code);
                    if (u != null) {
                        createdUser[0] = u;
                        return true;
                    }
                    return false;
                });
        dialog.setVisible(true);
        if (dialog.isConfirmed() && createdUser[0] != null) {
            autoLoginAndOpenMain(createdUser[0]);
        } else {
            authService.cancelRegistration(token);
        }
    }

    /** Авто-вход сразу после регистрации: минуем LoginFrame и открываем MainFrame. */
    private void autoLoginAndOpenMain(User user) {
        session.setCurrentUser(user);
        lastLoginStore.save(user.getEmail());
        MainFrame mainFrame = ctx.getBean(MainFrame.class);
        mainFrame.reload();
        mainFrame.setVisible(true);
        // Скрываем LoginFrame (он был видим под нами)
        LoginFrame login = ctx.getBean(LoginFrame.class);
        login.setVisible(false);
        dispose();
    }
}
