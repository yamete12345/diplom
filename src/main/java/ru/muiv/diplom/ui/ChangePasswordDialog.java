package ru.muiv.diplom.ui;

import ru.muiv.diplom.service.AuthService;

import javax.swing.*;
import java.awt.*;

public class ChangePasswordDialog extends JDialog {

    private final AuthService authService;
    private final Long userId;

    private final JPasswordField oldPwd = new JPasswordField(20);
    private final JPasswordField newPwd = new JPasswordField(20);
    private final JPasswordField newPwd2 = new JPasswordField(20);

    public ChangePasswordDialog(Window owner, AuthService authService, Long userId) {
        super(owner, "Смена пароля", ModalityType.APPLICATION_MODAL);
        this.authService = authService;
        this.userId = userId;
        buildUi();
    }

    private void buildUi() {
        setSize(420, 250);
        setLocationRelativeTo(getOwner());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        int row = 0;
        addRow(form, c, row++, "Текущий пароль:", oldPwd);
        addRow(form, c, row++, "Новый пароль:", newPwd);
        addRow(form, c, row++, "Повтор нового:", newPwd2);

        JButton submit = new JButton("Сменить");
        submit.addActionListener(e -> doSubmit());
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

    private void doSubmit() {
        String n1 = new String(newPwd.getPassword());
        String n2 = new String(newPwd2.getPassword());
        if (!n1.equals(n2)) {
            JOptionPane.showMessageDialog(this, "Новые пароли не совпадают",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            authService.requestPasswordChange(userId, new String(oldPwd.getPassword()));
            OtpDialog otp = new OtpDialog(this, "Код подтверждения смены пароля",
                    code -> authService.confirmPasswordChange(userId, code, n1));
            otp.setVisible(true);
            if (otp.isConfirmed()) {
                JOptionPane.showMessageDialog(this, "Пароль изменён",
                        "Готово", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }
}
