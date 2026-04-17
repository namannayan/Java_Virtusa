package service;

import db.DBConnection;
import model.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BankService {

    // ---------- User Registration & Authentication ----------
    public boolean registerUser(String username, String password, String fullName) {
        String sql = "INSERT INTO users (username, password, full_name) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, fullName);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement pstmt = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("full_name")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ---------- Account Creation ----------
    public boolean createAccount(int userId, String accountType, BigDecimal initialDeposit) {
        // Check if user exists
        if (!userExists(userId)) return false;

        String accountNumber = generateAccountNumber();
        String sql = "INSERT INTO accounts (user_id, account_number, account_type, balance) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, accountNumber);
            pstmt.setString(3, accountType.toUpperCase());
            pstmt.setBigDecimal(4, initialDeposit);
            int rows = pstmt.executeUpdate();
            if (rows > 0 && initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
                // Record initial deposit as a transaction
                int accountId = getAccountIdByNumber(accountNumber);
                if (accountId != -1) {
                    recordTransaction(accountId, "DEPOSIT", initialDeposit, null, "Initial deposit");
                }
            }
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ---------- Deposit ----------
    public boolean deposit(int accountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        Connection conn = DBConnection.getInstance().getConnection();
        try {
            conn.setAutoCommit(false);
            // Update balance
            String updateSql = "UPDATE accounts SET balance = balance + ? WHERE account_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setBigDecimal(1, amount);
                pstmt.setInt(2, accountId);
                pstmt.executeUpdate();
            }
            // Record transaction
            recordTransaction(conn, accountId, "DEPOSIT", amount, null, "Cash deposit");
            conn.commit();
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // ---------- Withdraw (with account-specific validation) ----------
    public boolean withdraw(int accountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        Connection conn = DBConnection.getInstance().getConnection();
        try {
            conn.setAutoCommit(false);
            // Fetch current account details
            Account acc = getAccountById(conn, accountId);
            if (acc == null) return false;

            // Use polymorphic validation
            if (!acc.canWithdraw(amount)) {
                System.out.println("Withdrawal violates account rules (minimum balance / overdraft limit).");
                return false;
            }

            // Update balance
            String updateSql = "UPDATE accounts SET balance = balance - ? WHERE account_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setBigDecimal(1, amount);
                pstmt.setInt(2, accountId);
                pstmt.executeUpdate();
            }
            recordTransaction(conn, accountId, "WITHDRAW", amount, null, "Cash withdrawal");
            conn.commit();
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // ---------- Transfer (between two accounts) ----------
    public boolean transfer(int fromAccountId, int toAccountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        if (fromAccountId == toAccountId) return false;

        Connection conn = DBConnection.getInstance().getConnection();
        try {
            conn.setAutoCommit(false);
            Account fromAcc = getAccountById(conn, fromAccountId);
            Account toAcc = getAccountById(conn, toAccountId);
            if (fromAcc == null || toAcc == null) return false;

            // Validate source account's withdrawal rules
            if (!fromAcc.canTransferOut(amount)) {
                System.out.println("Transfer failed: insufficient funds or violates account rules.");
                return false;
            }

            // Deduct from source
            String deductSql = "UPDATE accounts SET balance = balance - ? WHERE account_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deductSql)) {
                pstmt.setBigDecimal(1, amount);
                pstmt.setInt(2, fromAccountId);
                pstmt.executeUpdate();
            }
            // Add to target
            String addSql = "UPDATE accounts SET balance = balance + ? WHERE account_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(addSql)) {
                pstmt.setBigDecimal(1, amount);
                pstmt.setInt(2, toAccountId);
                pstmt.executeUpdate();
            }

            // Record transactions
            recordTransaction(conn, fromAccountId, "TRANSFER_SENT", amount, toAccountId, "Transfer to account " + toAcc.getAccountNumber());
            recordTransaction(conn, toAccountId, "TRANSFER_RECEIVED", amount, fromAccountId, "Transfer from account " + fromAcc.getAccountNumber());

            conn.commit();
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // ---------- Balance Inquiry ----------
    public BigDecimal getBalance(int accountId) {
        String sql = "SELECT balance FROM accounts WHERE account_id = ?";
        try (PreparedStatement pstmt = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, accountId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("balance");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ---------- Transaction History ----------
    public List<Transaction> getTransactionHistory(int accountId) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE account_id = ? ORDER BY transaction_date DESC";
        try (PreparedStatement pstmt = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, accountId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Transaction t = new Transaction(
                        rs.getInt("transaction_id"),
                        rs.getInt("account_id"),
                        rs.getString("transaction_type"),
                        rs.getBigDecimal("amount"),
                        rs.getObject("related_account_id") != null ? rs.getInt("related_account_id") : null,
                        rs.getTimestamp("transaction_date"),
                        rs.getString("description")
                );
                transactions.add(t);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    // ---------- Helper Methods ----------
    private boolean userExists(int userId) {
        String sql = "SELECT 1 FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private int getAccountIdByNumber(String accountNumber) {
        String sql = "SELECT account_id FROM accounts WHERE account_number = ?";
        try (PreparedStatement pstmt = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setString(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("account_id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private Account getAccountById(Connection conn, int accountId) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE account_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, accountId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String type = rs.getString("account_type");
                BigDecimal balance = rs.getBigDecimal("balance");
                int userId = rs.getInt("user_id");
                String accNumber = rs.getString("account_number");
                if ("SAVINGS".equals(type)) {
                    return new SavingsAccount(accountId, userId, accNumber, balance);
                } else {
                    return new CurrentAccount(accountId, userId, accNumber, balance);
                }
            }
        }
        return null;
    }

    private void recordTransaction(Connection conn, int accountId, String type, BigDecimal amount, Integer relatedAccId, String desc) throws SQLException {
        String sql = "INSERT INTO transactions (account_id, transaction_type, amount, related_account_id, description) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, accountId);
            pstmt.setString(2, type);
            pstmt.setBigDecimal(3, amount);
            if (relatedAccId != null) pstmt.setInt(4, relatedAccId);
            else pstmt.setNull(4, Types.INTEGER);
            pstmt.setString(5, desc);
            pstmt.executeUpdate();
        }
    }

    private void recordTransaction(int accountId, String type, BigDecimal amount, Integer relatedAccId, String desc) {
        String sql = "INSERT INTO transactions (account_id, transaction_type, amount, related_account_id, description) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, accountId);
            pstmt.setString(2, type);
            pstmt.setBigDecimal(3, amount);
            if (relatedAccId != null) pstmt.setInt(4, relatedAccId);
            else pstmt.setNull(4, Types.INTEGER);
            pstmt.setString(5, desc);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String generateAccountNumber() {
        return "ACC" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Additional: list accounts for a user (for menu)
    public List<Account> getAccountsForUser(int userId) {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT * FROM accounts WHERE user_id = ?";
        try (PreparedStatement pstmt = DBConnection.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int accId = rs.getInt("account_id");
                String type = rs.getString("account_type");
                BigDecimal balance = rs.getBigDecimal("balance");
                String accNumber = rs.getString("account_number");
                if ("SAVINGS".equals(type)) {
                    accounts.add(new SavingsAccount(accId, userId, accNumber, balance));
                } else {
                    accounts.add(new CurrentAccount(accId, userId, accNumber, balance));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return accounts;
    }
}