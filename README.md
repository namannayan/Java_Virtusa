#  Banking System Simulation (JDBC + MySQL)

A console-based banking application built with **Java**, **JDBC**, and **MySQL**. This project demonstrates core banking operations, object-oriented design (inheritance, encapsulation), and database integration.

##  Features

- **User Authentication** – Register and login with username/password.
- **Account Management** – Create Savings or Current account with unique account numbers.
- **Deposit & Withdraw** – Add or remove money with account-specific validation:
  - *Savings Account* – Minimum balance of ₹500 required.
  - *Current Account* – Overdraft facility up to ₹5000.
- **Money Transfer** – Transfer funds between accounts (with rollback on failure).
- **Balance Inquiry** – Check real-time balance.
- **Transaction History** – View all deposits, withdrawals, and transfers with timestamps.
- **Polymorphism** – Abstract `Account` class with concrete `SavingsAccount` and `CurrentAccount`.



##  Technology Stack

| Technology      | Purpose                               |
|----------------|---------------------------------------|
| Java 17        | Core language                         |
| MySQL 8.0      | Persistent data storage               |
| JDBC           | Database connectivity                  |
| Maven          | Dependency management                 |
| Eclipse/IntelliJ| IDE (any Java IDE works)              |

##  Database Schema

Run the following SQL script in src/main/resources/schema.file to create the `banking_system` database and tables

##Getting Started

Prerequisites
Java 17 or higher

MySQL Server (8.0 recommended)

Maven (optional – IDE can manage dependencies)



How to Use
Register – Create a new user account.

Login – Use your username and password.

Create Account – Choose Savings or Current with an initial deposit.

Perform Operations:

Deposit / Withdraw / Transfer money.

Check balance and transaction history.

Logout – Exit to main menu.

# BankingSystemJDBC

## Project Structure

```text
BankingSystemJDBC/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── db/
│   │   │   │   └── DBConnection.java
│   │   │   ├── model/
│   │   │   │   ├── Account.java         (abstract)
│   │   │   │   ├── SavingsAccount.java
│   │   │   │   ├── CurrentAccount.java
│   │   │   │   ├── Transaction.java
│   │   │   │   └── User.java
│   │   │   ├── service/
│   │   │   │   └── BankService.java
│   │   │   └── main/
│   │   │       └── Main.java
│   │   └── resources/
│   │       └── schema.sql
│   └── test/
└── README.md


Developed as a medium-level Java project to demonstrate OOP, JDBC, and MySQL integration.
