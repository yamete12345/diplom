package ru.muiv.diplom.ui;

import ru.muiv.diplom.domain.Account;
import ru.muiv.diplom.domain.TransactionType;
import ru.muiv.diplom.service.TransactionService;
import ru.muiv.diplom.util.Money;

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
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        GridBagConstraints lc = new GridBagConstraints();
        lc.insets = new Insets(6, 6, 6, 10);
        lc.anchor = GridBagConstraints.WEST;
        lc.fill = GridBagConstraints.NONE;
        lc.weightx = 0;

        GridBagConstraints fc = new GridBagConstraints();
        fc.insets = new Insets(6, 0, 6, 6);
        fc.anchor = GridBagConstraints.WEST;
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1;

        int row = 0;
        lc.gridx = 0; lc.gridy = row;
        form.add(new JLabel("Счёт:"), lc);
        fc.gridx = 1; fc.gridy = row;
        form.add(new JLabel(sourceAccount.getAccountNumber()
                + "   баланс: "
                + Money.format(sourceAccount.getBalance(), sourceAccount.getCurrency())), fc);

        row++;
        lc.gridx = 0; lc.gridy = row;
        form.add(new JLabel("Сумма:"), lc);
        fc.gridx = 1; fc.gridy = row;
        form.add(amountField, fc);

        if (mode == TransactionType.TRANSFER) {
            row++;
            lc.gridx = 0; lc.gridy = row;
            form.add(new JLabel("Счёт получателя:"), lc);
            fc.gridx = 1; fc.gridy = row;
            form.add(targetAccountField, fc);
        }

        row++;
        lc.gridx = 0; lc.gridy = row;
        form.add(new JLabel("Описание:"), lc);
        fc.gridx = 1; fc.gridy = row;
        form.add(descriptionField, fc);

        JButton ok = new JButton("Выполнить");
        BankTheme.makePrimary(ok);
        JButton cancel = new JButton("Отмена");
        ok.addActionListener(e -> doSubmit());
        cancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(ok);

        GridBagConstraints bc = new GridBagConstraints();
        bc.gridx = 0; bc.gridy = ++row;
        bc.gridwidth = 2;
        bc.insets = new Insets(16, 0, 0, 0);
        bc.anchor = GridBagConstraints.EAST;
        bc.fill = GridBagConstraints.HORIZONTAL;
        bc.weightx = 1;
        form.add(buttons, bc);

        getRootPane().setDefaultButton(ok);
        setContentPane(form);
        pack();
        setMinimumSize(new Dimension(540, getHeight()));
        setSize(Math.max(540, getWidth()), getHeight());
        setLocationRelativeTo(getOwner());
    }

    private void doSubmit() {
        BigDecimal amount;
        try {
            String raw = amountField.getText()
                    .replace("\u00A0", "")
                    .replace(" ", "")
                    .replace(',', '.')
                    .trim();
            amount = new BigDecimal(raw);
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
