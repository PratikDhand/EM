import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Random;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.sql.SQLException;
// import java.sql.Date;  <-- REMOVED THIS CONFLICTING IMPORT

public class ExpenseManagerUI extends JFrame {

    private JTextField amountField, descriptionField;
    private JComboBox<String> categoryBox;
    private JLabel budgetLabel, totalSpentLabel;
    private JTable expenseTable;
    private DefaultTableModel tableModel;
    private HashMap<String, Double> categoryTotals;
    private HashMap<Integer, Double> dailySpending;
    private JPanel monthlyOverviewPanel;

    private DatabaseManager dbManager;

    private double totalSpent = 0.0, monthlyBudget = 5000.0;

    public ExpenseManagerUI() {
        setTitle("💸 Personal Expense Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(new Color(240, 247, 255));
        setMinimumSize(new Dimension(1250, 720));

        categoryTotals = new HashMap<>();
        dailySpending = new HashMap<>();
        dbManager = new DatabaseManager();

        loadInitialData();

        initializeUI();
        setVisible(true);
    }

    private void initializeUI() {
        createTopPanel();
        createCenterPanel();
        createBottomPanel();
        pack();
        setLocationRelativeTo(null);
    }

    private void loadInitialData() {
        try {
            List<Object[]> expenses = dbManager.loadAllExpenses();

            totalSpent = 0.0;
            categoryTotals.clear();
            dailySpending.clear();

            for (Object[] expense : expenses) {
                // expense[0]=ID, expense[1]=amount, expense[2]=category, expense[4]=dateStr
                double amount = (Double) expense[1];
                String category = (String) expense[2];
                LocalDate date = LocalDate.parse((String) expense[4]);
                int day = date.getDayOfMonth();

                totalSpent += amount;
                categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + amount);
                dailySpending.put(day, dailySpending.getOrDefault(day, 0.0) + amount);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading initial data: " + e.getMessage() + "\nCheck MySQL connection and credentials.",
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(25, 42, 86));
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        JLabel title = new JLabel("Expense Manager Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(Color.WHITE);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 5));
        infoPanel.setOpaque(false);

        budgetLabel = new JLabel("Budget: $" + String.format("%.2f", monthlyBudget));
        totalSpentLabel = new JLabel("Total Spent: $" + String.format("%.2f", totalSpent));

        for (JLabel lbl : new JLabel[] { budgetLabel, totalSpentLabel }) {
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
            lbl.setForeground(new Color(46, 204, 113));
        }

        infoPanel.add(budgetLabel);
        infoPanel.add(totalSpentLabel);

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(infoPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);
    }

    private void createCenterPanel() {
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 20, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));
        centerPanel.setOpaque(false);

        JPanel leftPanel = new JPanel(new BorderLayout(10, 15));
        leftPanel.setOpaque(false);
        leftPanel.add(createAddExpensePanel(), BorderLayout.NORTH);
        leftPanel.add(createExpenseTablePanel(), BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 15, 15));
        rightPanel.setOpaque(false);
        rightPanel.add(createCategoryBreakdownPanel());
        rightPanel.add(createMonthlyOverviewPanel());

        centerPanel.add(leftPanel);
        centerPanel.add(rightPanel);

        JScrollPane scrollPane = new JScrollPane(centerPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createAddExpensePanel() {
        JPanel panel = createCardPanel("➕ Add New Expense");

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel amountLabel = new JLabel("💰 Amount:");
        amountField = new JTextField(10);

        JLabel categoryLabel = new JLabel("📂 Category:");
        categoryBox = new JComboBox<>(new String[] {
                "Food & Dining", "Transport", "Shopping", "Health", "Entertainment", "Utilities", "Others"
        });

        JLabel descriptionLabel = new JLabel("📝 Description:");
        descriptionField = new JTextField(10);

        JButton addButton = createButton("Add Expense", new Color(52, 152, 219));
        addButton.addActionListener(e -> addExpense());

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(amountLabel, gbc);
        gbc.gridx = 1;
        form.add(amountField, gbc);

        gbc.gridx = 2;
        form.add(categoryLabel, gbc);
        gbc.gridx = 3;
        form.add(categoryBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(descriptionLabel, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        form.add(descriptionField, gbc);

        gbc.gridx = 3;
        gbc.gridwidth = 1;
        form.add(addButton, gbc);

        panel.add(form);
        return panel;
    }

    private JPanel createExpenseTablePanel() {
        JPanel panel = createCardPanel("📜 Recent Expenses");
        panel.setLayout(new BorderLayout());

        String[] cols = { "ID", "Amount ($)", "Category", "Description", "Date" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1)
                    return Double.class;
                return super.getColumnClass(columnIndex);
            }
        };

        try {
            List<Object[]> expenses = dbManager.loadAllExpenses();
            for (Object[] expense : expenses) {
                tableModel.addRow(expense);
            }
        } catch (SQLException e) {
            System.err.println("Error populating table: " + e.getMessage());
        }

        expenseTable = new JTable(tableModel);
        expenseTable.setRowHeight(25);
        expenseTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        expenseTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));

        expenseTable.getColumnModel().getColumn(0).setMinWidth(0);
        expenseTable.getColumnModel().getColumn(0).setMaxWidth(0);
        expenseTable.getColumnModel().getColumn(0).setWidth(0);

        JScrollPane scrollPane = new JScrollPane(expenseTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton deleteButton = createButton("🗑 Delete Selected Expense", new Color(231, 76, 60));
        deleteButton.addActionListener(e -> deleteExpense());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        buttonPanel.add(deleteButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createCategoryBreakdownPanel() {
        JPanel panel = createCardPanel("📊 Category Breakdown");
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        updateCategoryBreakdown(panel);
        return panel;
    }

    private void updateCategoryBreakdown(JPanel panel) {
        panel.removeAll();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        double total = totalSpent == 0 ? 1 : totalSpent;

        for (String cat : categoryTotals.keySet()) {
            double amount = categoryTotals.get(cat);
            double percentage = (amount / total) * 100;

            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            JLabel label = new JLabel(cat + " - $" + String.format("%.2f", amount));
            label.setFont(new Font("Segoe UI", Font.PLAIN, 14));

            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue((int) percentage);
            bar.setStringPainted(true);
            bar.setString(String.format("%.1f%%", percentage));
            bar.setForeground(randomColor());
            bar.setBackground(new Color(230, 230, 230));

            row.add(label, BorderLayout.WEST);
            row.add(bar, BorderLayout.CENTER);
            row.setBorder(new EmptyBorder(5, 10, 5, 10));

            panel.add(row);
        }

        if (categoryTotals.isEmpty()) {
            JLabel placeholder = new JLabel("No expenses added yet.", SwingConstants.CENTER);
            placeholder.setFont(new Font("Segoe UI", Font.ITALIC, 15));
            panel.add(Box.createVerticalGlue());
            panel.add(placeholder);
            panel.add(Box.createVerticalGlue());
        }

        panel.revalidate();
        panel.repaint();
    }

    private JPanel createMonthlyOverviewPanel() {
        monthlyOverviewPanel = createCardPanel("📅 Monthly Spending Days");
        monthlyOverviewPanel.setLayout(new GridLayout(6, 7, 5, 5));
        updateMonthlyOverview(monthlyOverviewPanel);
        return monthlyOverviewPanel;
    }

    private void updateMonthlyOverview(JPanel panel) {
        panel.removeAll();
        LocalDate today = LocalDate.now();
        int currentDayOfMonth = today.getDayOfMonth();
        int daysInMonth = YearMonth.from(today).lengthOfMonth();

        for (int i = 1; i <= 31; i++) {
            JButton day = new JButton(String.valueOf(i));
            day.setFocusPainted(false);
            day.setForeground(Color.BLACK);
            day.setFont(new Font("Segoe UI", Font.BOLD, 12));

            day.setBackground(new Color(245, 245, 245));
            day.setBorder(new LineBorder(new Color(220, 220, 220)));

            if (i > daysInMonth) {
                day.setEnabled(false);
                day.setBackground(new Color(230, 230, 230));
                day.setForeground(new Color(150, 150, 150));
            } else if (dailySpending.containsKey(i)) {
                Color spendingColor = new Color(52, 152, 219);
                day.setBackground(spendingColor);
                day.setForeground(Color.WHITE);
                day.setToolTipText("$" + String.format("%.2f", dailySpending.get(i)) + " spent");
            }

            if (i == currentDayOfMonth) {
                day.setBorder(new LineBorder(new Color(255, 140, 0), 3));
            }

            panel.add(day);
        }

        panel.revalidate();
        panel.repaint();
    }

    private void createBottomPanel() {
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        bottomPanel.setBackground(new Color(236, 240, 241));
        bottomPanel.setBorder(new EmptyBorder(15, 10, 15, 10));

        JButton setBudgetButton = createButton("💰 Set Budget", new Color(46, 204, 113));
        setBudgetButton.addActionListener(e -> setBudget());

        JButton exportButton = createButton("📤 Export Data", new Color(155, 89, 182));
        exportButton.addActionListener(e -> exportData());

        JButton clearButton = createButton("🗑 Clear All", new Color(231, 76, 60));
        clearButton.addActionListener(e -> clearAllData());

        bottomPanel.add(setBudgetButton);
        bottomPanel.add(exportButton);
        bottomPanel.add(clearButton);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createCardPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new TitledBorder(new LineBorder(new Color(200, 200, 200), 1, true),
                title, TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14)));
        return panel;
    }

    private JButton createButton(String text, Color color) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };

        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.setOpaque(false);
        btn.setContentAreaFilled(false);

        final Color originalColor = color;

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(originalColor.darker());
                btn.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(originalColor);
                btn.repaint();
            }
        });

        btn.setBackground(originalColor);

        return btn;
    }

    private void addExpense() {
        try {
            double amount = Double.parseDouble(amountField.getText());
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Amount must be greater than zero!", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            String category = (String) categoryBox.getSelectedItem();
            String desc = descriptionField.getText();
            LocalDate date = LocalDate.now();
            int dayOfMonth = date.getDayOfMonth();
            String dateStr = date.toString();

            // 1. SAVE to database
            // CORRECTED: Passing the LocalDate 'date' variable directly.
            int newId = dbManager.addExpense(amount, category, desc, date);

            if (newId == -1) {
                throw new SQLException("Failed to retrieve generated ID for new expense.");
            }

            // 2. UPDATE UI
            tableModel.insertRow(0, new Object[] { newId, amount, category, desc, dateStr });

            totalSpent += amount;
            totalSpentLabel.setText("Total Spent: $" + String.format("%.2f", totalSpent));

            categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + amount);

            dailySpending.put(dayOfMonth, dailySpending.getOrDefault(dayOfMonth, 0.0) + amount);

            amountField.setText("");
            descriptionField.setText("");

            updateCategoryBreakdown((JPanel) ((JPanel) ((JPanel) ((JScrollPane) getContentPane()
                    .getComponent(1)).getViewport().getView()).getComponent(1)).getComponent(0));
            updateMonthlyOverview(monthlyOverviewPanel);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for Amount!", "Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Database Error: " + e.getMessage()
                            + "\nPlease check your MySQL server, connection details, and credentials.",
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "An unexpected error occurred: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteExpense() {
        int selectedRow = expenseTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an expense to delete.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete the selected expense?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int idToDelete = (Integer) tableModel.getValueAt(selectedRow, 0);

                double amount = (Double) tableModel.getValueAt(selectedRow, 1);
                String category = (String) tableModel.getValueAt(selectedRow, 2);
                String dateStr = (String) tableModel.getValueAt(selectedRow, 4);
                int dayOfMonth = LocalDate.parse(dateStr).getDayOfMonth();

                // 1. DELETE FROM DATABASE using unique ID
                dbManager.deleteExpense(idToDelete);

                // 2. UPDATE UI
                tableModel.removeRow(selectedRow);

                totalSpent -= amount;
                totalSpentLabel.setText("Total Spent: $" + String.format("%.2f", totalSpent));

                double currentCategoryTotal = categoryTotals.getOrDefault(category, 0.0);
                if (currentCategoryTotal - amount > 0.001) {
                    categoryTotals.put(category, currentCategoryTotal - amount);
                } else {
                    categoryTotals.remove(category);
                }

                double currentDailyTotal = dailySpending.getOrDefault(dayOfMonth, 0.0);
                if (currentDailyTotal - amount > 0.001) {
                    dailySpending.put(dayOfMonth, currentDailyTotal - amount);
                } else {
                    dailySpending.remove(dayOfMonth);
                }

                updateCategoryBreakdown((JPanel) ((JPanel) ((JPanel) ((JScrollPane) getContentPane()
                        .getComponent(1)).getViewport().getView()).getComponent(1)).getComponent(0));
                updateMonthlyOverview(monthlyOverviewPanel);

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Database Error",
                        JOptionPane.ERROR_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error deleting expense: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void setBudget() {
        String input = JOptionPane.showInputDialog(this, "Enter Monthly Budget ($):",
                String.format("%.2f", monthlyBudget));
        if (input != null) {
            try {
                double newBudget = Double.parseDouble(input);
                if (newBudget > 0) {
                    monthlyBudget = newBudget;
                    budgetLabel.setText("Budget: $" + String.format("%.2f", monthlyBudget));
                } else {
                    JOptionPane.showMessageDialog(this, "Budget must be a positive number!");
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid number for budget!");
            }
        }
    }

    private void exportData() {
        JOptionPane.showMessageDialog(this, "Export feature coming soon!", "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void clearAllData() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clear all data? This cannot be undone.", "Confirm Clear All",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // 1. CLEAR DATABASE
                dbManager.clearAllExpenses();

                // 2. CLEAR UI & MEMORY
                tableModel.setRowCount(0);
                totalSpent = 0;
                monthlyBudget = 5000.0;
                categoryTotals.clear();
                dailySpending.clear();

                totalSpentLabel.setText("Total Spent: $0.00");
                budgetLabel.setText("Budget: $" + String.format("%.2f", monthlyBudget));

                updateCategoryBreakdown((JPanel) ((JPanel) ((JPanel) ((JScrollPane) getContentPane()
                        .getComponent(1)).getViewport().getView()).getComponent(1)).getComponent(0));
                updateMonthlyOverview(monthlyOverviewPanel);

                repaint();
                JOptionPane.showMessageDialog(this, "All data has been cleared from the application and database.",
                        "Data Cleared", JOptionPane.INFORMATION_MESSAGE);

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Database Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private Color randomColor() {
        Color[] palette = {
                new Color(52, 152, 219), new Color(46, 204, 113),
                new Color(231, 76, 60), new Color(241, 196, 15),
                new Color(155, 89, 182), new Color(26, 188, 156),
                new Color(230, 126, 34)
        };
        return palette[new Random().nextInt(palette.length)];
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        SwingUtilities.invokeLater(ExpenseManagerUI::new);
    }
}
