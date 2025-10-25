import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

public class DatabaseManager {

    // 1. **MYSQL CONNECTION DETAILS**
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/expense_tracker_db";
    private static final String USER = "root";       
    private static final String PASSWORD = "Parthonjdbc#1"; 
    public DatabaseManager() {
        createDatabaseIfNotExists();
        initializeTable();
    }


    private void createDatabaseIfNotExists() {
        try (Connection rootConn = DriverManager.getConnection("jdbc:mysql://localhost:3306/", USER, PASSWORD);
             Statement stmt = rootConn.createStatement()) {

            String dbName = "expense_tracker_db";
            // MySQL standard SQL to create database if missing
            String sql = "CREATE DATABASE IF NOT EXISTS " + dbName;
            stmt.executeUpdate(sql);
            System.out.println("Database '" + dbName + "' checked/created successfully.");

        } catch (SQLException e) {
            System.err.println("Error connecting to MySQL or creating database: " + e.getMessage());
        }
    }

    private Connection connect() throws SQLException {
        // Connect directly to the specific database
        return DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
    }

    private void initializeTable() {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            
            // 2. **MYSQL-SPECIFIC CREATE TABLE STATEMENT**
            String sql = "CREATE TABLE IF NOT EXISTS expenses (" +
                          "id INT PRIMARY KEY AUTO_INCREMENT," + 
                          "amount DECIMAL(10, 2) NOT NULL," + 
                          "category VARCHAR(100) NOT NULL," +
                          "description VARCHAR(255)," +
                          "date DATE NOT NULL);"; 
            stmt.execute(sql);
            System.out.println("Table 'expenses' initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Table initialization failed: " + e.getMessage());
        }
    }

    /**
     * Inserts a new expense and returns the generated unique ID.
     * **ACCEPTS java.time.LocalDate**
     */
    public int addExpense(double amount, String category, String description, LocalDate date) throws SQLException {
        String sql = "INSERT INTO expenses (amount, category, description, date) VALUES (?, ?, ?, ?)";
        int generatedId = -1;
        
        try (Connection conn = connect();
             // Statement.RETURN_GENERATED_KEYS works the same for MySQL
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setDouble(1, amount);
            pstmt.setString(2, category);
            pstmt.setString(3, description);
            
            // CONVERSION: Convert LocalDate to the required java.sql.Date for JDBC
            pstmt.setDate(4, Date.valueOf(date)); 
            
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    generatedId = rs.getInt(1); 
                }
            }
        }
        return generatedId;
    }

    /**
     * Deletes an expense using its unique ID.
     */
    public void deleteExpense(int id) throws SQLException {
        String sql = "DELETE FROM expenses WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Clears all data from the expenses table.
     */
    public void clearAllExpenses() throws SQLException {
        // TRUNCATE is faster and resets the AUTO_INCREMENT counter in MySQL
        String sql = "TRUNCATE TABLE expenses"; 
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate(sql);
        }
    }

    /**
     * Loads all expenses from the database.
     * Returns a list of Object arrays: {id, amount, category, description, date}
     */
    public List<Object[]> loadAllExpenses() throws SQLException {
        List<Object[]> expenses = new ArrayList<>();
        String sql = "SELECT id, amount, category, description, date FROM expenses ORDER BY id DESC";
        
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                double amount = rs.getDouble("amount");
                String category = rs.getString("category");
                String description = rs.getString("description");
                String dateStr = rs.getString("date"); // MySQL date can be read as string
                
                expenses.add(new Object[]{id, amount, category, description, dateStr});
            }
        }
        return expenses;
    }
}
