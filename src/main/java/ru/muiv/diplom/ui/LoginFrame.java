package ru.muiv.diplom.ui;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.muiv.diplom.domain.User;
import ru.muiv.diplom.service.AuthService;
import ru.muiv.diplom.util.Session;

import javax.swing.*;
import java.awt.*;

@Component
public class LoginFrame extends JFrame {

    private final AuthService authService;
    private final Session session;
    private final ApplicationContext ctx;

    private final JTextField emailField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);

    public LoginFrame(AuthService authService, Session session, ApplicationContext ctx) {
        super("Банковский модуль — Вход");
        this.authService = authService;
        this.session = session;
        this.ctx = ctx;
        buildUi();
    }

    private void buildUi() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(420, 260);
        setLocationRelativeTo(null);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        form.add(new JLabel("E-mail:"), c);
        c.gridx = 1;
        form.add(emailField, c);

        c.gridx = 0; c.gridy = 1;
        form.add(new JLabel("Пароль:"), c);
        c.gridx = 1;
        form.add(passwordField, c);

        JButton loginBtn = new JButton("Войти");
        JButton registerBtn = new JButton("Регистрация");

        loginBtn.addActionListener(e -> doLogin());
        registerBtn.addActionListener(e -> openRegister());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(registerBtn);
        buttons.add(loginBtn);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        c.anchor = GridBagConstraints.EAST;
        form.add(buttons, c);

        getRootPane().setDefaultButton(loginBtn);
        setContentPane(form);
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
        emailField.setText("");
        passwordField.setText("");
        setVisible(true);
    }
}
