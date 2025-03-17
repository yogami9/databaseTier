package com.banking.db.controller;

import com.banking.db.model.Transaction;
import com.banking.db.service.MongoDBManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for transaction-related operations.
 * This exposes the transaction functionality of the database tier as REST endpoints.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger logger = LogManager.getLogger(TransactionController.class);
    
    @Autowired
    private MongoDBManager dbManager;
    
    /**
     * Record a new transaction.
     */
    @PostMapping
    public ResponseEntity<?> recordTransaction(@RequestBody Map<String, Object> transactionData) {
        logger.info("REST request to record transaction");
        
        try {
            // Extract required fields
            String accountNumber = (String) transactionData.get("accountNumber");
            String transactionType = (String) transactionData.get("transactionType");
            Double amount = ((Number) transactionData.get("amount")).doubleValue();
            Double resultingBalance = ((Number) transactionData.get("resultingBalance")).doubleValue();
            String description = (String) transactionData.get("description");
            String sourceAccount = (String) transactionData.get("sourceAccount");
            String destinationAccount = (String) transactionData.get("destinationAccount");
            
            // Validate required fields
            if (accountNumber == null || transactionType == null || amount == null || resultingBalance == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing required fields"));
            }
            
            // Generate transaction ID if not provided
            String transactionId = (String) transactionData.get("transactionId");
            if (transactionId == null) {
                transactionId = UUID.randomUUID().toString();
            }
            
            boolean recorded = dbManager.recordTransaction(
                transactionId,
                accountNumber,
                transactionType,
                amount,
                resultingBalance,
                description,
                sourceAccount,
                destinationAccount
            );
            
            if (recorded) {
                return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Transaction recorded successfully",
                                "transactionId", transactionId));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to record transaction"));
            }
        } catch (Exception e) {
            logger.error("Error recording transaction: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Invalid transaction data: " + e.getMessage()));
        }
    }
    
    /**
     * Get transaction history for an account.
     */
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<Transaction>> getTransactionHistory(@PathVariable String accountNumber) {
        logger.info("REST request to get transaction history for account: {}", accountNumber);
        
        // First check if account exists
        Document account = dbManager.getAccount(accountNumber);
        if (account == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ArrayList<>());
        }
        
        List<Document> transactionDocs = dbManager.getTransactionHistory(accountNumber);
        List<Transaction> transactions = new ArrayList<>();
        
        for (Document doc : transactionDocs) {
            try {
                Transaction transaction = dbManager.documentToTransaction(doc);
                transactions.add(transaction);
            } catch (Exception e) {
                logger.error("Error converting transaction document: {}", e.getMessage(), e);
                // Continue with next transaction
            }
        }
        
        return ResponseEntity.ok(transactions);
    }
    
    /**
     * Get a specific transaction by ID (Not implemented - for API completeness).
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<?> getTransaction(@PathVariable String transactionId) {
        // This would require an index on transaction_id and a new method in MongoDBManager
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(Map.of("error", "Getting transaction by ID is not implemented"));
    }
}
