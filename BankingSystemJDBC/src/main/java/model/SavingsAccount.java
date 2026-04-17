package model;

import java.math.BigDecimal;

public class SavingsAccount extends Account {
    private static final BigDecimal MINIMUM_BALANCE = new BigDecimal("500.00");

    public SavingsAccount(int accountId, int userId, String accountNumber, BigDecimal balance) {
        super(accountId, userId, accountNumber, "SAVINGS", balance);
    }

    @Override
    public boolean canWithdraw(BigDecimal amount) {
        // After withdrawal, balance must be >= MINIMUM_BALANCE
        return balance.subtract(amount).compareTo(MINIMUM_BALANCE) >= 0;
    }

    @Override
    public boolean canTransferOut(BigDecimal amount) {
        return canWithdraw(amount);
    }
}