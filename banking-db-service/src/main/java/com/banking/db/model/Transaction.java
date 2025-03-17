package com.banking.db.model;

import java.util.Date;

/**
 * Model class for Transaction data.
 * This is adapted from the Transaction class in the full application.
 */
public class Transaction {
    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT
    }
    
    private String transactionId;
    private Date timestamp;
    private TransactionType type;
    private double amount;
    private double resultingBalance;
    private String description;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    
    // Default constructor for JSON deserialization
    public Transaction() {
    }
    
    public Transaction(String transactionId, TransactionType type, double amount, 
                       double resultingBalance, String description,
                       String sourceAccountNumber, String destinationAccountNumber) {
        this.transactionId = transactionId;
        this.timestamp = new Date();
        this.type = type;
        this.amount = amount;
        this.resultingBalance = resultingBalance;
        this.description = description;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
    }
    
    // Constructor with timestamp for reconstructing transactions from DB
    public Transaction(String transactionId, TransactionType type, double amount, 
                       double resultingBalance, String description,
                       String sourceAccountNumber, String destinationAccountNumber,
                       Date timestamp) {
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.type = type;
        this.amount = amount;
        this.resultingBalance = resultingBalance;
        this.description = description;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
    }
    
    // Getters and setters
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    public TransactionType getType() {
        return type;
    }
    
    public void setType(TransactionType type) {
        this.type = type;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public double getResultingBalance() {
        return resultingBalance;
    }
    
    public void setResultingBalance(double resultingBalance) {
        this.resultingBalance = resultingBalance;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }
    
    public void setSourceAccountNumber(String sourceAccountNumber) {
        this.sourceAccountNumber = sourceAccountNumber;
    }
    
    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }
    
    public void setDestinationAccountNumber(String destinationAccountNumber) {
        this.destinationAccountNumber = destinationAccountNumber;
    }
    
    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", timestamp=" + timestamp +
                ", type=" + type +
                ", amount=" + amount +
                ", resultingBalance=" + resultingBalance +
                ", description='" + description + '\'' +
                '}';
    }
}
