package model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Transaction {
    private int transactionId;
    private int accountId;
    private String transactionType; // DEPOSIT, WITHDRAW, TRANSFER_SENT, TRANSFER_RECEIVED
    private BigDecimal amount;
    private Integer relatedAccountId; // for transfers
    private Timestamp transactionDate;
    private String description;

    // Constructor, getters, setters...
    public Transaction(int transactionId, int accountId, String transactionType, BigDecimal amount,
                       Integer relatedAccountId, Timestamp transactionDate, String description) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.relatedAccountId = relatedAccountId;
        this.transactionDate = transactionDate;
        this.description = description;
    }

    // Getters
    public int getTransactionId() { return transactionId; }
    public int getAccountId() { return accountId; }
    public String getTransactionType() { return transactionType; }
    public BigDecimal getAmount() { return amount; }
    public Integer getRelatedAccountId() { return relatedAccountId; }
    public Timestamp getTransactionDate() { return transactionDate; }
    public String getDescription() { return description; }
}