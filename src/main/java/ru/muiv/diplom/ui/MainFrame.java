package ru.muiv.diplom.ui;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.muiv.diplom.domain.*;
import ru.muiv.diplom.repo.AccountRepository;
import ru.muiv.diplom.repo.BankTransactionRepository;
import ru.muiv.diplom.repo.PostingRepository;
import ru.muiv.diplom.repo.UserRepository;
import ru.muiv.diplom.service.AccountService;
import ru.muiv.diplom.service.AuthService;
import ru.muiv.diplom.service.TransactionService;
import ru.muiv.diplom.util.CsvExporter;
import ru.muiv.diplom.util.Money;
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
    private final PostingRepository postingRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final CsvExporter csvExporter;
    private final ApplicationContext ctx;

    private final DefaultListModel<Account> accountsModel = new DefaultListModel<>();
    private final JList<Account> accountsList = new JList<>(accountsModel);
    private final DefaultTableModel historyModel =
            new DefaultTableModel(new Object[]{"Дата", "Тип", "Направление", "Сумма", "Контрагент", "Описание"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
    private final JTable historyTable = new JTable(historyModel);
    private final JLabel userLabel = new JLabel();
    private final JLabel balanceLabel = new JLabel();

    public MainFrame(Session session,
                     AccountService accountService,
                     TransactionService transactionService,
                     BankTransactionRepository txRepository,
                     PostingRepository postingRepository,
                     AccountRepository accountRepository,
                     UserRepository userRepository,
                     AuthService authService,
                     CsvExporter csvExporter,
                     ApplicationContext ctx) {
        super("Банковский модуль");
        this.session = session;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.txRepository = txRepository;
        this.postingRepository = postingRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.csvExporter = csvExporter;
        this.ctx = ctx;
        buildUi();
    }

    private void buildUi() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1180, 720);
        setLocationRelativeTo(null);

        setJMenuBar(buildMenu());

        accountsList.setCellRenderer(new AccountListRenderer());
        accountsList.setBackground(BankTheme.PANEL_BG);
        accountsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) reloadHistory();
        });

        balanceLabel.setFont(BankTheme.FONT_MONO);
        balanceLabel.setForeground(BankTheme.TEXT);
        userLabel.setFont(BankTheme.FONT_UI_MEDIUM);

        historyTable.setRowHeight(26);
        historyTable.setShowVerticalLines(false);
        historyTable.getTableHeader().setReorderingAllowed(false);

        // Универсальный рендерер: подсветка строки по колонке "Тип" (индекс 1)
        // + для колонки "Сумма" (индекс 3) — моноширинный шрифт справа.
        javax.swing.table.DefaultTableCellRenderer rowRenderer =
                new javax.swing.table.DefaultTableCellRenderer() {
                    @Override
                    public java.awt.Component getTableCellRendererComponent(
                            JTable table, Object value, boolean isSelected, boolean hasFocus,
                            int row, int col) {
                        super.getTableCellRendererComponent(
                                table, value, isSelected, hasFocus, row, col);
                        if (col == 3) {
                            setFont(BankTheme.FONT_MONO);
                            setHorizontalAlignment(SwingConstants.RIGHT);
                            setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 10));
                        } else {
                            setFont(BankTheme.FONT_UI);
                            setHorizontalAlignment(SwingConstants.LEFT);
                            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                        }
                        if (!isSelected) {
                            Object typeCell = table.getModel().getValueAt(row, 1);
                            setBackground(colorForType(String.valueOf(typeCell)));
                            setForeground(BankTheme.TEXT);
                        }
                        return this;
                    }
                };
        for (int i = 0; i < historyTable.getColumnCount(); i++) {
            historyTable.getColumnModel().getColumn(i).setCellRenderer(rowRenderer);
        }

        JScrollPane accountsScroll = new JScrollPane(accountsList);
        accountsScroll.setPreferredSize(new Dimension(320, 0));
        accountsScroll.setBorder(BorderFactory.createTitledBorder("Мои счета"));

        JScrollPane historyScroll = new JScrollPane(historyTable);
        historyScroll.setBorder(BorderFactory.createTitledBorder("История операций за 90 дней"));

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(12, 16, 4, 16));
        top.add(userLabel, BorderLayout.WEST);
        top.add(balanceLabel, BorderLayout.EAST);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, accountsScroll, historyScroll);
        split.setDividerLocation(320);
        split.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        split.setOpaque(false);

        JPanel toolbar = buildToolbar();

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(toolbar, BorderLayout.NORTH);
        bottom.add(BankTheme.statusBar(
                "Java 21 · Spring Boot 3 · Swing + FlatLaf 3.5 · H2 file · Flyway"),
                BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BankTheme.WINDOW_BG);
        root.add(top, BorderLayout.NORTH);
        root.add(split, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
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
        JButton copyNumber = new JButton("Копировать № счёта");

        // Главная операция — crimson primary
        BankTheme.makePrimary(transfer);

        deposit.addActionListener(e -> operation(TransactionType.DEPOSIT));
        withdraw.addActionListener(e -> operation(TransactionType.WITHDRAW));
        transfer.addActionListener(e -> operation(TransactionType.TRANSFER));
        copyNumber.addActionListener(e -> copySelectedAccountNumber());

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        p.add(deposit);
        p.add(withdraw);
        p.add(transfer);
        p.add(Box.createHorizontalStrut(12));
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
        balanceLabel.setText("Баланс: " + Money.format(acc.getBalance(), acc.getCurrency())
                + "   Статус: " + acc.getStatus());

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
                    Money.format(p.getAmount()),
                    counterpartyLabel(tx, p),
                    tx.getDescription()
            });
        }
    }

    /**
     * Для перевода возвращает «ФИО · номер счёта» второй стороны:
     *   • если текущая проводка DEBIT (списание) — показываем получателя;
     *   • если CREDIT (зачисление) — отправителя.
     * Для пополнения/списания — прочерк.
     */
    private String counterpartyLabel(BankTransaction tx, Posting mine) {
        if (tx.getType() != TransactionType.TRANSFER) return "—";
        List<Posting> all = postingRepository.findByTransactionId(tx.getId());
        Posting other = all.stream()
                .filter(x -> !x.getId().equals(mine.getId()))
                .findFirst().orElse(null);
        if (other == null) return "—";
        Account otherAcc = accountRepository.findById(other.getAccountId()).orElse(null);
        if (otherAcc == null) return "—";
        String name = userRepository.findById(otherAcc.getUserId())
                .map(User::getFullName)
                .orElse("—");
        String arrow = mine.getDirection() == PostingDirection.DEBIT ? "→ " : "← ";
        return arrow + name + " · " + otherAcc.getAccountNumber();
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

    private Color colorForType(String typeTitle) {
        if (typeTitle == null) return BankTheme.PANEL_BG;
        return switch (typeTitle) {
            case "Пополнение" -> new Color(0xE6, 0xF4, 0xEA); // зелёный
            case "Списание"   -> new Color(0xFA, 0xDF, 0xE2); // красный
            case "Перевод"    -> new Color(0xFE, 0xF7, 0xE0); // жёлтый
            default           -> BankTheme.PANEL_BG;
        };
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
                // JetBrains Mono для номера счёта и суммы — как в дизайне
                String monoFamily = BankTheme.FONT_MONO.getFamily();
                String typeLabel = a.getType().getTitle();
                boolean closed = a.getStatus() == AccountStatus.CLOSED;
                String statusBadge = closed
                        ? " <span style='color:#C5221F'>· закрыт</span>"
                        : "";
                String text = "<html>"
                        + "<div style='padding:6px 10px'>"
                        + "<div style='font-family:" + monoFamily + "; font-size:13px; font-weight:bold'>"
                        + a.getAccountNumber() + "</div>"
                        + "<div style='color:#6B6B6B; font-size:11px; padding-top:2px'>"
                        + typeLabel + statusBadge + "</div>"
                        + "<div style='font-family:" + monoFamily + "; font-size:14px; padding-top:4px'>"
                        + Money.format(a.getBalance(), a.getCurrency()) + "</div>"
                        + "</div></html>";
                setText(text);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BankTheme.BORDER_LIGHT),
                        BorderFactory.createEmptyBorder(2, 2, 2, 2)));
                if (!isSelected) setBackground(BankTheme.PANEL_BG);
            }
            return this;
        }
    }
}
