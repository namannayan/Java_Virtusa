package model;

import java.math.BigDecimal;

public class CurrentAccount extends Account {
    private static final BigDecimal OVERDRAFT_LIMIT = new BigDecimal("5000.00");

    public CurrentAccount(int accountId, int userId, String accountNumber, BigDecimal balance) {
        super(accountId, userId, accountNumber, "CURRENT", balance);
    }

    @Override
    public boolean canWithdraw(BigDecimal amount) {
        // Overdraft allowed up to -OVERDRAFT_LIMIT
        BigDecimal newBalance = balance.subtract(amount);
        return newBalance.compareTo(OVERDRAFT_LIMIT.negate()) >= 0;
    }

    @Override
    public boolean canTransferOut(BigDecimal amount) {
        return canWithdraw(amount);
    }
}