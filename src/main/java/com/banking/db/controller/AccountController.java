package com.banking.db.controller;

import com.banking.db.model.Account;
import com.banking.db.service.MongoDBManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for account-related operations.
 * This exposes the account functionality of the database tier as REST endpoints.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private static final Logger logger = LogManager.getLogger(AccountController.class);
    
    @Autowired
    private MongoDBManager dbManager;
    
    /**
     * Create a new account.
     */
    @PostMapping
    public ResponseEntity<?> createAccount(@RequestBody Account account) {
        logger.info("REST request to create account: {}", account.getAccountNumber());
        
        boolean created = dbManager.createAccount(
            account.getAccountNumber(),
            account.getAccountHolderName(),
            account.getBalance()
        );
        
        if (created) {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Account created successfully", 
                             "accountNumber", account.getAccountNumber()));
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Account already exists or creation failed", 
                             "accountNumber", account.getAccountNumber()));
        }
    }
    
    /**
     * Get all accounts.
     */
    @GetMapping
    public ResponseEntity<List<Account>> getAllAccounts() {
        logger.info("REST request to get all accounts");
        
        List<Document> accountDocs = dbManager.getAllAccounts();
        List<Account> accounts = new ArrayList<>();
        
        for (Document doc : accountDocs) {
            Account account = new Account();
            account.setAccountNumber(doc.getString("account_number"));
            account.setAccountHolderName(doc.getString("account_holder_name"));
            account.setBalance(doc.getDouble("balance"));
            account.setCreationDate(doc.getDate("creation_date"));
            accounts.add(account);
        }
        
        return ResponseEntity.ok(accounts);
    }
    
    /**
     * Get a specific account by account number.
     */
    @GetMapping("/{accountNumber}")
    public ResponseEntity<?> getAccount(@PathVariable String accountNumber) {
        logger.info("REST request to get account: {}", accountNumber);
        
        Document accountDoc = dbManager.getAccount(accountNumber);
        if (accountDoc == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Account not found", 
                             "accountNumber", accountNumber));
        }
        
        Account account = new Account();
        account.setAccountNumber(accountDoc.getString("account_number"));
        account.setAccountHolderName(accountDoc.getString("account_holder_name"));
        account.setBalance(accountDoc.getDouble("balance"));
        account.setCreationDate(accountDoc.getDate("creation_date"));
        
        return ResponseEntity.ok(account);
    }
    
    /**
     * Update an account's balance.
     */
    @PutMapping("/{accountNumber}/balance")
    public ResponseEntity<?> updateBalance(
            @PathVariable String accountNumber, 
            @RequestBody Map<String, Double> balanceData) {
        
        Double newBalance = balanceData.get("balance");
        if (newBalance == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Balance value is required"));
        }
        
        logger.info("REST request to update balance for account {}: new balance = {}", 
                accountNumber, newBalance);
        
        // First check if account exists
        Document account = dbManager.getAccount(accountNumber);
        if (account == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Account not found", 
                             "accountNumber", accountNumber));
        }
        
        boolean updated = dbManager.updateBalance(accountNumber, newBalance);
        if (updated) {
            return ResponseEntity.ok(Map.of("message", "Balance updated successfully",
                                          "accountNumber", accountNumber,
                                          "newBalance", newBalance));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update balance",
                           "accountNumber", accountNumber));
        }
    }
    
    /**
     * Delete an account (not implemented - just for API completeness).
     */
    @DeleteMapping("/{accountNumber}")
    public ResponseEntity<?> deleteAccount(@PathVariable String accountNumber) {
        // For security reasons, account deletion is not implemented
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(Map.of("error", "Account deletion is not supported",
                       "accountNumber", accountNumber));
    }
}
