package ru.muiv.diplom.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Predicate;

/**
 * Модальный диалог ввода OTP. На ОК вызывает verifier.test(code):
 *   true  → confirmed=true, закрываемся;
 *   false → показываем ошибку, остаёмся.
 */
public class OtpDialog extends JDialog {

    private final JTextField codeField = new JTextField(10);
    private final Predicate<String> verifier;
    private boolean confirmed;
    private boolean submitted;

    public OtpDialog(Window owner, String title, Predicate<String> verifier) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.verifier = verifier;
        buildUi();
    }

    private void buildUi() {
        setSize(340, 170);
        setLocationRelativeTo(getOwner());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        panel.add(new JLabel("На вашу почту отправлен код подтверждения."), c);

        c.gridy = 1; c.gridwidth = 1;
        panel.add(new JLabel("Код:"), c);
        c.gridx = 1;
        panel.add(codeField, c);

        JButton ok = new JButton("Подтвердить");
        JButton cancel = new JButton("Отмена");
        ok.addActionListener(e -> submit());
        cancel.addActionListener(e -> {
            confirmed = false;
            submitted = false;
            dispose();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(ok);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        c.anchor = GridBagConstraints.EAST;
        panel.add(buttons, c);

        getRootPane().setDefaultButton(ok);
        setContentPane(panel);
    }

    private void submit() {
        submitted = true;
        String code = codeField.getText();
        if (verifier.test(code)) {
            confirmed = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Неверный или просроченный код", "Ошибка", JOptionPane.ERROR_MESSAGE);
            codeField.selectAll();
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean wasSubmitted() {
        return submitted;
    }
}
