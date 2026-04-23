package ru.muiv.diplom.ui;

import org.springframework.stereotype.Component;
import ru.muiv.diplom.service.AuthService;

import javax.swing.*;
import java.awt.*;

@Component
public class RegisterFrame extends JFrame {

    private final AuthService authService;

    private final JTextField nameField = new JTextField(22);
    private final JTextField emailField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JPasswordField password2Field = new JPasswordField(22);

    public RegisterFrame(AuthService authService) {
        super("Регистрация");
        this.authService = authService;
        buildUi();
    }

    private void buildUi() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(440, 320);
        setLocationRelativeTo(null);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        int row = 0;
        addRow(form, c, row++, "ФИО:", nameField);
        addRow(form, c, row++, "E-mail:", emailField);
        addRow(form, c, row++, "Пароль:", passwordField);
        addRow(form, c, row++, "Повтор пароля:", password2Field);

        JButton submit = new JButton("Зарегистрироваться");
        submit.addActionListener(e -> doRegister());

        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        c.anchor = GridBagConstraints.EAST;
        form.add(submit, c);

        getRootPane().setDefaultButton(submit);
        setContentPane(form);
    }

    private void addRow(JPanel form, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        form.add(field, c);
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

        OtpDialog dialog = new OtpDialog(this, "Подтверждение e-mail",
                code -> authService.confirmRegistration(token, code) != null);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            JOptionPane.showMessageDialog(this,
                    "Регистрация завершена. Теперь можно войти.",
                    "Готово", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            // OTP не прошёл или окно закрыли — сбрасываем pending-заявку,
            // чтобы в памяти (и тем более в БД) не осталось данных.
            authService.cancelRegistration(token);
        }
    }
}
