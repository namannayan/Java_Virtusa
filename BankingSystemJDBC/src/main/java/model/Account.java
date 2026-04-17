package model;

import java.math.BigDecimal;

public abstract class Account {
    protected int accountId;
    protected int userId;
    protected String accountNumber;
    protected String accountType; // "SAVINGS" or "CURRENT"
    protected BigDecimal balance;

    public Account(int accountId, int userId, String accountNumber, String accountType, BigDecimal balance) {
        this.accountId = accountId;
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = balance;
    }

    // Abstract validation methods (to be overridden by subclasses)
    public abstract boolean canWithdraw(BigDecimal amount);
    public abstract boolean canTransferOut(BigDecimal amount); // same as withdraw for most

    // Getters & Setters
    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}