package main;

import model.Account;
import model.Transaction;
import model.User;
import service.BankService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static BankService bankService = new BankService();
    private static Scanner scanner = new Scanner(System.in);
    private static User currentUser = null;

    public static void main(String[] args) {
        while (true) {
            if (currentUser == null) {
                showLoginMenu();
            } else {
                showBankingMenu();
            }
        }
    }

    private static void showLoginMenu() {
        System.out.println("\n=== BANKING SYSTEM ===");
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("3. Exit");
        System.out.print("Choose: ");
        int choice = Integer.parseInt(scanner.nextLine());

        switch (choice) {
            case 1 -> register();
            case 2 -> login();
            case 3 -> { System.out.println("Goodbye!"); System.exit(0); }
            default -> System.out.println("Invalid option");
        }
    }

    private static void register() {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        System.out.print("Full Name: ");
        String fullName = scanner.nextLine();
        if (bankService.registerUser(username, password, fullName)) {
            System.out.println("Registration successful! Please login.");
        } else {
            System.out.println("Registration failed. Username may already exist.");
        }
    }

    private static void login() {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        User user = bankService.authenticate(username, password);
        if (user != null) {
            currentUser = user;
            System.out.println("Welcome, " + user.getFullName() + "!");
        } else {
            System.out.println("Invalid credentials.");
        }
    }

    private static void showBankingMenu() {
        System.out.println("\n=== Welcome " + currentUser.getFullName() + " ===");
        System.out.println("1. Create Account (Savings/Current)");
        System.out.println("2. Deposit");
        System.out.println("3. Withdraw");
        System.out.println("4. Transfer");
        System.out.println("5. Balance Inquiry");
        System.out.println("6. Transaction History");
        System.out.println("7. List My Accounts");
        System.out.println("8. Logout");
        System.out.print("Choose: ");
        int choice = Integer.parseInt(scanner.nextLine());

        switch (choice) {
            case 1 -> createAccount();
            case 2 -> deposit();
            case 3 -> withdraw();
            case 4 -> transfer();
            case 5 -> balanceInquiry();
            case 6 -> transactionHistory();
            case 7 -> listAccounts();
            case 8 -> { currentUser = null; System.out.println("Logged out."); }
            default -> System.out.println("Invalid option");
        }
    }

    private static void createAccount() {
        System.out.print("Account type (SAVINGS/CURRENT): ");
        String type = scanner.nextLine().toUpperCase();
        System.out.print("Initial deposit amount: ");
        BigDecimal amount = new BigDecimal(scanner.nextLine());
        if (bankService.createAccount(currentUser.getUserId(), type, amount)) {
            System.out.println("Account created successfully!");
        } else {
            System.out.println("Failed to create account.");
        }
    }

    private static void deposit() {
        int accId = selectAccount();
        if (accId == -1) return;
        System.out.print("Amount to deposit: ");
        BigDecimal amount = new BigDecimal(scanner.nextLine());
        if (bankService.deposit(accId, amount)) {
            System.out.println("Deposit successful.");
        } else {
            System.out.println("Deposit failed.");
        }
    }

    private static void withdraw() {
        int accId = selectAccount();
        if (accId == -1) return;
        System.out.print("Amount to withdraw: ");
        BigDecimal amount = new BigDecimal(scanner.nextLine());
        if (bankService.withdraw(accId, amount)) {
            System.out.println("Withdrawal successful.");
        } else {
            System.out.println("Withdrawal failed. Check balance or account rules.");
        }
    }

    private static void transfer() {
        System.out.println("Source Account:");
        int fromAccId = selectAccount();
        if (fromAccId == -1) return;
        System.out.println("Target Account (enter account ID):");
        listAccounts();
        System.out.print("Target Account ID: ");
        int toAccId = Integer.parseInt(scanner.nextLine());
        System.out.print("Amount to transfer: ");
        BigDecimal amount = new BigDecimal(scanner.nextLine());
        if (bankService.transfer(fromAccId, toAccId, amount)) {
            System.out.println("Transfer successful.");
        } else {
            System.out.println("Transfer failed.");
        }
    }

    private static void balanceInquiry() {
        int accId = selectAccount();
        if (accId == -1) return;
        BigDecimal balance = bankService.getBalance(accId);
        System.out.println("Current balance: " + balance);
    }

    private static void transactionHistory() {
        int accId = selectAccount();
        if (accId == -1) return;
        List<Transaction> history = bankService.getTransactionHistory(accId);
        if (history.isEmpty()) {
            System.out.println("No transactions found.");
        } else {
            System.out.println("\nTransaction History:");
            for (Transaction t : history) {
                System.out.printf("%s | %-15s | %10.2f | %s\n",
                        t.getTransactionDate(), t.getTransactionType(), t.getAmount(), t.getDescription());
            }
        }
    }

    private static void listAccounts() {
        List<Account> accounts = bankService.getAccountsForUser(currentUser.getUserId());
        if (accounts.isEmpty()) {
            System.out.println("You have no accounts. Create one first.");
        } else {
            System.out.println("Your accounts:");
            for (Account acc : accounts) {
                System.out.printf("ID: %d | %s | Number: %s | Balance: %.2f\n",
                        acc.getAccountId(), acc.getAccountType(), acc.getAccountNumber(), acc.getBalance());
            }
        }
    }

   private static int selectAccount() {
    List<Account> accounts = bankService.getAccountsForUser(currentUser.getUserId());
    if (accounts.isEmpty()) {
        System.out.println("No accounts available. Please create an account first.");
        return -1;
    }
    System.out.println("Select account:");
    for (Account acc : accounts) {
        System.out.printf("%d - %s (%s)\n", acc.getAccountId(), acc.getAccountType(), acc.getAccountNumber());
    }
    System.out.print("Enter Account ID: ");
    int enteredId = Integer.parseInt(scanner.nextLine());

    for (Account acc : accounts) {
        if (acc.getAccountId() == enteredId) {
            return enteredId;
        }
    }
    System.out.println("Invalid account ID or account does not belong to you.");
    return -1;
}
}
