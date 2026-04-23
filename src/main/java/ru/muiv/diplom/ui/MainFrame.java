package ru.muiv.diplom.ui;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.muiv.diplom.domain.*;
import ru.muiv.diplom.repo.BankTransactionRepository;
import ru.muiv.diplom.service.AccountService;
import ru.muiv.diplom.service.AuthService;
import ru.muiv.diplom.service.TransactionService;
import ru.muiv.diplom.util.CsvExporter;
import ru.muiv.diplom.util.Session;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Component
public class MainFrame extends JFrame {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final Session session;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final BankTransactionRepository txRepository;
    private final AuthService authService;
    private final CsvExporter csvExporter;
    private final ApplicationContext ctx;

    private final DefaultListModel<Account> accountsModel = new DefaultListModel<>();
    private final JList<Account> accountsList = new JList<>(accountsModel);
    private final DefaultTableModel historyModel =
            new DefaultTableModel(new Object[]{"Дата", "Тип", "Направление", "Сумма", "Описание"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
    private final JTable historyTable = new JTable(historyModel);
    private final JLabel userLabel = new JLabel();
    private final JLabel balanceLabel = new JLabel();

    public MainFrame(Session session,
                     AccountService accountService,
                     TransactionService transactionService,
                     BankTransactionRepository txRepository,
                     AuthService authService,
                     CsvExporter csvExporter,
                     ApplicationContext ctx) {
        super("Банковский модуль");
        this.session = session;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.txRepository = txRepository;
        this.authService = authService;
        this.csvExporter = csvExporter;
        this.ctx = ctx;
        buildUi();
    }

    private void buildUi() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1000, 620);
        setLocationRelativeTo(null);

        setJMenuBar(buildMenu());

        accountsList.setCellRenderer(new AccountListRenderer());
        accountsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) reloadHistory();
        });

        JScrollPane accountsScroll = new JScrollPane(accountsList);
        accountsScroll.setPreferredSize(new Dimension(300, 0));
        accountsScroll.setBorder(BorderFactory.createTitledBorder("Мои счета"));

        JScrollPane historyScroll = new JScrollPane(historyTable);
        historyScroll.setBorder(BorderFactory.createTitledBorder("История операций за 90 дней"));

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        top.add(userLabel, BorderLayout.WEST);
        top.add(balanceLabel, BorderLayout.EAST);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, accountsScroll, historyScroll);
        split.setDividerLocation(310);
        split.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel toolbar = buildToolbar();

        JPanel root = new JPanel(new BorderLayout());
        root.add(top, BorderLayout.NORTH);
        root.add(split, BorderLayout.CENTER);
        root.add(toolbar, BorderLayout.SOUTH);
        setContentPane(root);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                logout();
            }
        });
    }

    private JMenuBar buildMenu() {
        JMenuBar mb = new JMenuBar();

        JMenu fileMenu = new JMenu("Файл");
        JMenuItem exportItem = new JMenuItem("Экспорт истории в CSV…");
        exportItem.addActionListener(e -> exportCsv());
        JMenuItem logoutItem = new JMenuItem("Выход");
        logoutItem.addActionListener(e -> logout());
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(logoutItem);

        JMenu accountMenu = new JMenu("Счета");
        JMenuItem openItem = new JMenuItem("Открыть счёт…");
        openItem.addActionListener(e -> openAccount());
        JMenuItem closeItem = new JMenuItem("Закрыть выбранный счёт");
        closeItem.addActionListener(e -> closeAccount());
        accountMenu.add(openItem);
        accountMenu.add(closeItem);

        JMenu profileMenu = new JMenu("Профиль");
        JMenuItem chPwd = new JMenuItem("Сменить пароль…");
        chPwd.addActionListener(e -> new ChangePasswordDialog(this, authService,
                session.requireUserId()).setVisible(true));
        profileMenu.add(chPwd);

        mb.add(fileMenu);
        mb.add(accountMenu);
        mb.add(profileMenu);
        return mb;
    }

    private JPanel buildToolbar() {
        JButton deposit  = new JButton("Пополнить");
        JButton withdraw = new JButton("Списать");
        JButton transfer = new JButton("Перевести");
        JButton copyNumber = new JButton("Копировать номер счёта");

        deposit.addActionListener(e -> operation(TransactionType.DEPOSIT));
        withdraw.addActionListener(e -> operation(TransactionType.WITHDRAW));
        transfer.addActionListener(e -> operation(TransactionType.TRANSFER));
        copyNumber.addActionListener(e -> copySelectedAccountNumber());

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        p.add(deposit);
        p.add(withdraw);
        p.add(transfer);
        p.add(copyNumber);
        return p;
    }

    private void copySelectedAccountNumber() {
        Account acc = accountsList.getSelectedValue();
        if (acc == null) {
            JOptionPane.showMessageDialog(this, "Сначала выберите счёт", "Внимание",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(acc.getAccountNumber()), null);
        JOptionPane.showMessageDialog(this,
                "Номер счёта скопирован в буфер обмена:\n" + acc.getAccountNumber(),
                "Скопировано", JOptionPane.INFORMATION_MESSAGE);
    }

    public void reload() {
        User user = session.getCurrentUser();
        if (user == null) return;
        userLabel.setText("Пользователь: " + user.getFullName() + "  (" + user.getEmail() + ")");
        reloadAccounts();
    }

    private void reloadAccounts() {
        Long userId = session.requireUserId();
        Account selected = accountsList.getSelectedValue();
        accountsModel.clear();
        List<Account> list = accountService.listForUser(userId);
        list.forEach(accountsModel::addElement);

        if (selected != null) {
            for (int i = 0; i < accountsModel.size(); i++) {
                if (accountsModel.get(i).getId().equals(selected.getId())) {
                    accountsList.setSelectedIndex(i);
                    break;
                }
            }
        } else if (!accountsModel.isEmpty()) {
            accountsList.setSelectedIndex(0);
        }
        reloadHistory();
    }

    private void reloadHistory() {
        Account acc = accountsList.getSelectedValue();
        historyModel.setRowCount(0);
        if (acc == null) {
            balanceLabel.setText("");
            return;
        }
        balanceLabel.setText("Баланс: " + acc.getBalance().toPlainString() + " "
                + acc.getCurrency() + "   Статус: " + acc.getStatus());

        Instant to = Instant.now();
        Instant from = to.minus(90, ChronoUnit.DAYS);
        List<Posting> postings =
                transactionService.history(session.requireUserId(), acc.getId(), from, to);
        for (Posting p : postings) {
            Optional<BankTransaction> txOpt = txRepository.findById(p.getTransactionId());
            if (txOpt.isEmpty()) continue;
            BankTransaction tx = txOpt.get();
            historyModel.addRow(new Object[]{
                    DATE_FMT.format(tx.getCreatedAt()),
                    tx.getType().getTitle(),
                    p.getDirection().name(),
                    p.getAmount().toPlainString(),
                    tx.getDescription()
            });
        }
    }

    private void openAccount() {
        Object[] options = AccountType.values();
        AccountType type = (AccountType) JOptionPane.showInputDialog(this,
                "Выберите тип счёта:", "Новый счёт",
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (type == null) return;
        try {
            Account acc = accountService.open(session.requireUserId(), type);
            JOptionPane.showMessageDialog(this,
                    "Открыт счёт: " + acc.getAccountNumber(),
                    "Готово", JOptionPane.INFORMATION_MESSAGE);
            reloadAccounts();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void closeAccount() {
        Account acc = accountsList.getSelectedValue();
        if (acc == null) return;
        int ans = JOptionPane.showConfirmDialog(this,
                "Закрыть счёт " + acc.getAccountNumber() + "?",
                "Подтверждение", JOptionPane.YES_NO_OPTION);
        if (ans != JOptionPane.YES_OPTION) return;
        try {
            accountService.close(session.requireUserId(), acc.getId());
            reloadAccounts();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void operation(TransactionType type) {
        Account acc = accountsList.getSelectedValue();
        if (acc == null) {
            JOptionPane.showMessageDialog(this, "Сначала выберите счёт", "Внимание",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (acc.getStatus() != AccountStatus.ACTIVE) {
            JOptionPane.showMessageDialog(this, "Счёт закрыт", "Внимание",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        OperationDialog dialog = new OperationDialog(this, transactionService,
                session.requireUserId(), acc, type);
        dialog.setVisible(true);
        if (dialog.isDone()) {
            reloadAccounts();
        }
    }

    private void exportCsv() {
        Account acc = accountsList.getSelectedValue();
        if (acc == null) {
            JOptionPane.showMessageDialog(this, "Сначала выберите счёт", "Внимание",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("history-" + acc.getAccountNumber() + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        Instant to = Instant.now();
        Instant from = to.minus(365, ChronoUnit.DAYS);
        try {
            List<Posting> postings =
                    transactionService.history(session.requireUserId(), acc.getId(), from, to);
            csvExporter.exportHistory(postings, Path.of(chooser.getSelectedFile().toURI()));
            JOptionPane.showMessageDialog(this, "Сохранено: "
                            + chooser.getSelectedFile().getAbsolutePath(),
                    "Готово", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void logout() {
        setVisible(false);
        LoginFrame login = ctx.getBean(LoginFrame.class);
        login.returnFromMain();
    }

    private static class AccountListRenderer extends DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                               boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Account a) {
                String text = "<html><b>" + a.getAccountNumber() + "</b><br/>"
                        + a.getType().getTitle() + " · "
                        + a.getBalance().toPlainString() + " " + a.getCurrency()
                        + " · " + a.getStatus() + "</html>";
                setText(text);
            }
            return this;
        }
    }
}
