package ru.muiv.diplom.ui;

import ru.muiv.diplom.domain.Account;
import ru.muiv.diplom.domain.TransactionType;
import ru.muiv.diplom.service.TransactionService;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

public class OperationDialog extends JDialog {

    private final TransactionService txService;
    private final Long userId;
    private final Account sourceAccount;
    private final TransactionType mode;

    private final JTextField amountField = new JTextField(15);
    private final JTextField targetAccountField = new JTextField(22);
    private final JTextField descriptionField = new JTextField(22);
    private boolean done;

    public OperationDialog(Window owner,
                           TransactionService txService,
                           Long userId,
                           Account sourceAccount,
                           TransactionType mode) {
        super(owner, titleFor(mode), ModalityType.APPLICATION_MODAL);
        this.txService = txService;
        this.userId = userId;
        this.sourceAccount = sourceAccount;
        this.mode = mode;
        buildUi();
    }

    private static String titleFor(TransactionType mode) {
        return switch (mode) {
            case DEPOSIT  -> "Пополнение счёта";
            case WITHDRAW -> "Списание со счёта";
            case TRANSFER -> "Перевод средств";
        };
    }

    private void buildUi() {
        setSize(460, 260);
        setLocationRelativeTo(getOwner());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        int row = 0;
        c.gridx = 0; c.gridy = row;
        form.add(new JLabel("Счёт:"), c);
        c.gridx = 1;
        form.add(new JLabel(sourceAccount.getAccountNumber()
                + "   баланс: " + sourceAccount.getBalance().toPlainString() + " "
                + sourceAccount.getCurrency()), c);

        row++;
        c.gridx = 0; c.gridy = row;
        form.add(new JLabel("Сумма:"), c);
        c.gridx = 1;
        form.add(amountField, c);

        if (mode == TransactionType.TRANSFER) {
            row++;
            c.gridx = 0; c.gridy = row;
            form.add(new JLabel("Счёт получателя:"), c);
            c.gridx = 1;
            form.add(targetAccountField, c);
        }

        row++;
        c.gridx = 0; c.gridy = row;
        form.add(new JLabel("Описание:"), c);
        c.gridx = 1;
        form.add(descriptionField, c);

        JButton ok = new JButton("Выполнить");
        JButton cancel = new JButton("Отмена");
        ok.addActionListener(e -> doSubmit());
        cancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(ok);

        row++;
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        c.anchor = GridBagConstraints.EAST;
        form.add(buttons, c);

        getRootPane().setDefaultButton(ok);
        setContentPane(form);
    }

    private void doSubmit() {
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountField.getText().trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Введите корректную сумму",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String desc = descriptionField.getText();
        try {
            switch (mode) {
                case DEPOSIT  -> txService.deposit(userId, sourceAccount.getId(), amount, desc);
                case WITHDRAW -> txService.withdraw(userId, sourceAccount.getId(), amount, desc);
                case TRANSFER -> txService.transfer(userId, sourceAccount.getId(),
                        targetAccountField.getText(), amount, desc);
            }
            done = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Операция не выполнена", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isDone() {
        return done;
    }
}
