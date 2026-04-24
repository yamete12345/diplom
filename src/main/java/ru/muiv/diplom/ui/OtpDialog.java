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

    private final JTextField codeField = new JTextField(12);
    private final Predicate<String> verifier;
    private boolean confirmed;
    private boolean submitted;

    public OtpDialog(Window owner, String title, Predicate<String> verifier) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.verifier = verifier;
        buildUi();
    }

    private void buildUi() {
        setSize(480, 320);
        setLocationRelativeTo(getOwner());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BankTheme.WINDOW_BG);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(BankTheme.PANEL_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BankTheme.BORDER_LIGHT, 1, true),
                BorderFactory.createEmptyBorder(24, 32, 24, 32)));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 6, 0);

        card.add(BankTheme.titleLabel("Код подтверждения"), c);

        c.gridy++;
        card.add(BankTheme.dimLabel("На вашу почту отправлен 6-значный код."), c);

        c.gridy++; c.insets = new Insets(20, 0, 4, 0);
        card.add(BankTheme.dimLabel("Код"), c);

        c.gridy++; c.insets = new Insets(0, 0, 6, 0);
        codeField.setFont(BankTheme.FONT_MONO.deriveFont(18f));
        card.add(codeField, c);

        JButton ok = new JButton("Подтвердить");
        BankTheme.makePrimary(ok);
        ok.addActionListener(e -> submit());

        JButton cancel = new JButton("Отмена");
        cancel.addActionListener(e -> {
            confirmed = false;
            submitted = false;
            dispose();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(ok);

        c.gridy++; c.insets = new Insets(18, 0, 0, 0);
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        card.add(buttons, c);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        center.add(card);

        root.add(center, BorderLayout.CENTER);

        getRootPane().setDefaultButton(ok);
        setContentPane(root);
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
