package com.banking.db.service;

import com.banking.db.model.Transaction;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class represents the database tier of the three-tier architecture using MongoDB.
 * It has been adapted to work as a standalone service.
 */
@Service
public class MongoDBManager {
    
    private static final Logger logger = LogManager.getLogger(MongoDBManager.class);
    
    // MongoDB connection settings injected from properties
    @Value("${mongodb.connection_string}")
    private String connectionString;
    
    @Value("${mongodb.database_name}")
    private String databaseName;
    
    // MongoDB client
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> accountsCollection;
    private MongoCollection<Document> transactionsCollection;
    
    /**
     * Initialize the MongoDB connection after bean creation.
     */
    @PostConstruct
    public void initialize() {
        logger.info("Initializing MongoDB connection");
        try {
            // Create the MongoDB client
            logger.debug("Creating MongoDB client with connection string: {}", 
                    connectionString.replaceAll("mongodb\\+srv://.*@", "mongodb+srv://[REDACTED]@"));
            mongoClient = MongoClients.create(connectionString);
            
            // Get database and collections
            database = mongoClient.getDatabase(databaseName);
            accountsCollection = database.getCollection("accounts");
            transactionsCollection = database.getCollection("transactions");
            
            // Test the connection
            logger.debug("Testing MongoDB connection by listing collection names");
            String firstCollection = database.listCollectionNames().first();
            logger.info("Connected to MongoDB successfully, found collection: {}", 
                    firstCollection != null ? firstCollection : "none");
            
            // Initialize indexes
            initializeDatabase();
            
        } catch (Exception e) {
            logger.error("Failed to connect to MongoDB: {}", e.getMessage(), e);
            logger.debug("Connection failure details", e);
            throw new RuntimeException("Failed to connect to MongoDB", e);
        }
    }
    
    /**
     * Initialize the database by creating necessary indexes for performance.
     */
    public void initializeDatabase() {
        logger.info("Initializing database indexes");
        try {
            // Create index on account_number field in accounts collection
            logger.debug("Creating unique index on account_number field in accounts collection");
            IndexOptions uniqueOption = new IndexOptions().unique(true);
            accountsCollection.createIndex(Indexes.ascending("account_number"), uniqueOption);
            logger.info("Created unique index on account_number field in accounts collection");
            
            // Create index on account_number field in transactions collection
            logger.debug("Creating index on account_number field in transactions collection");
            transactionsCollection.createIndex(Indexes.ascending("account_number"));
            logger.info("Created index on account_number field in transactions collection");
            
            // Create index on transaction_id field in transactions collection
            logger.debug("Creating unique index on transaction_id field in transactions collection");
            transactionsCollection.createIndex(Indexes.ascending("transaction_id"), uniqueOption);
            logger.info("Created unique index on transaction_id field in transactions collection");
            
            // Create index on timestamp field in transactions collection
            logger.debug("Creating index on timestamp field in transactions collection");
            transactionsCollection.createIndex(Indexes.ascending("timestamp"));
            logger.info("Created index on timestamp field in transactions collection");
            
            logger.info("Database indexes initialized successfully");
            
        } catch (MongoException e) {
            logger.error("Failed to initialize database indexes: {}", e.getMessage());
            logger.debug("Index creation error details", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
    
    /**
     * Create a new account in the database.
     * 
     * @param accountNumber The account number
     * @param accountHolderName The name of the account holder
     * @param initialBalance The initial balance
     * @return true if the account was created successfully
     */
    public boolean createAccount(String accountNumber, String accountHolderName, double initialBalance) {
        logger.info("Creating new account: accountNumber={}, holder={}, initialBalance={}",
                accountNumber, accountHolderName, initialBalance);
        try {
            // Check if account already exists
            logger.debug("Checking if account {} already exists", accountNumber);
            Document existingAccount = accountsCollection.find(
                    Filters.eq("account_number", accountNumber)).first();
            
            if (existingAccount != null) {
                logger.warn("Account creation failed: account {} already exists", accountNumber);
                return false;
            }
            
            // Create the account document
            logger.debug("Creating account document for account {}", accountNumber);
            Document accountDoc = new Document("_id", new ObjectId())
                    .append("account_number", accountNumber)
                    .append("account_holder_name", accountHolderName)
                    .append("balance", initialBalance)
                    .append("creation_date", new Date());
            
            // Insert the account
            logger.debug("Inserting account document into database");
            InsertOneResult result = accountsCollection.insertOne(accountDoc);
            
            boolean success = result.wasAcknowledged();
            
            // If initial balance is positive, record it as a transaction
            if (success && initialBalance > 0) {
                logger.debug("Recording initial deposit transaction of {} for account {}", 
                        initialBalance, accountNumber);
                String transactionId = java.util.UUID.randomUUID().toString();
                recordTransaction(
                    transactionId,
                    accountNumber,
                    "DEPOSIT",
                    initialBalance,
                    initialBalance,
                    "Initial deposit",
                    null,
                    accountNumber
                );
                logger.debug("Initial deposit transaction {} recorded", transactionId);
            }
            
            logger.info("Account {} created successfully: {}", accountNumber, success);
            return success;
        } catch (MongoException e) {
            logger.error("Failed to create account {}: {}", accountNumber, e.getMessage());
            logger.debug("Account creation error details", e);
            return false;
        }
    }
    
    /**
     * Get an account from the database by account number.
     * 
     * @param accountNumber The account number to retrieve
     * @return Account data as a Document, or null if not found
     */
    public Document getAccount(String accountNumber) {
        logger.info("Retrieving account: {}", accountNumber);
        try {
            logger.debug("Executing database query for account {}", accountNumber);
            Document account = accountsCollection.find(Filters.eq("account_number", accountNumber)).first();
            if (account == null) {
                logger.warn("Account {} not found in database", accountNumber);
                return null;
            }
            logger.info("Successfully retrieved account {}", accountNumber);
            logger.debug("Account data: {}", account.toJson());
            return account;
        } catch (MongoException e) {
            logger.error("Failed to retrieve account {}: {}", accountNumber, e.getMessage());
            logger.debug("Account retrieval error details", e);
            return null;
        }
    }
    
    /**
     * Get all accounts from the database.
     * 
     * @return List of account documents
     */
    public List<Document> getAllAccounts() {
        logger.info("Retrieving all accounts");
        try {
            List<Document> accounts = new ArrayList<>();
            accountsCollection.find().into(accounts);
            logger.info("Successfully retrieved {} accounts", accounts.size());
            return accounts;
        } catch (MongoException e) {
            logger.error("Failed to retrieve accounts: {}", e.getMessage());
            logger.debug("Account retrieval error details", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Update the balance of an account.
     * 
     * @param accountNumber The account number
     * @param newBalance The new balance
     * @return true if the update was successful
     */
    public boolean updateBalance(String accountNumber, double newBalance) {
        logger.info("Updating balance for account {}: new balance = {}", accountNumber, newBalance);
        try {
            logger.debug("Executing database update for account {}", accountNumber);
            UpdateResult result = accountsCollection.updateOne(
                    Filters.eq("account_number", accountNumber),
                    Updates.set("balance", newBalance));
            
            boolean success = result.getModifiedCount() > 0;
            if (success) {
                logger.info("Successfully updated balance for account {} to {}", accountNumber, newBalance);
            } else {
                logger.warn("Failed to update balance for account {}: account not found or balance unchanged", 
                        accountNumber);
            }
            return success;
        } catch (MongoException e) {
            logger.error("Failed to update balance for account {}: {}", accountNumber, e.getMessage());
            logger.debug("Balance update error details", e);
            return false;
        }
    }
    
    /**
     * Record a transaction in the database.
     * 
     * @param transactionId Unique transaction ID
     * @param accountNumber Account number associated with the transaction
     * @param transactionType Type of transaction (DEPOSIT, WITHDRAWAL, etc.)
     * @param amount Transaction amount
     * @param resultingBalance The balance after the transaction
     * @param description Transaction description
     * @param sourceAccount Source account for transfers (can be null)
     * @param destinationAccount Destination account for transfers (can be null)
     * @return true if the transaction was recorded successfully
     */
    public boolean recordTransaction(String transactionId, String accountNumber, String transactionType,
                                    double amount, double resultingBalance, String description,
                                    String sourceAccount, String destinationAccount) {
        logger.info("Recording transaction: id={}, account={}, type={}, amount={}", 
                transactionId, accountNumber, transactionType, amount);
        try {
            // Create the transaction document
            logger.debug("Creating transaction document for transaction {}", transactionId);
            Document transactionDoc = new Document("_id", new ObjectId())
                    .append("transaction_id", transactionId)
                    .append("account_number", accountNumber)
                    .append("transaction_type", transactionType)
                    .append("amount", amount)
                    .append("resulting_balance", resultingBalance)
                    .append("description", description)
                    .append("source_account", sourceAccount)
                    .append("destination_account", destinationAccount)
                    .append("timestamp", new Date());
            
            // Insert the transaction
            logger.debug("Inserting transaction document into database");
            InsertOneResult result = transactionsCollection.insertOne(transactionDoc);
            
            boolean success = result.wasAcknowledged();
            if (success) {
                logger.info("Successfully recorded transaction {} for account {}", 
                        transactionId, accountNumber);
                logger.debug("Transaction details: type={}, amount={}, balance={}, description='{}'", 
                        transactionType, amount, resultingBalance, description);
            } else {
                logger.warn("Failed to record transaction {} for account {}", 
                        transactionId, accountNumber);
            }
            return success;
        } catch (MongoException e) {
            logger.error("Failed to record transaction {} for account {}: {}", 
                    transactionId, accountNumber, e.getMessage());
            logger.debug("Transaction recording error details", e);
            return false;
        }
    }
    
    /**
     * Get transaction history for an account.
     * 
     * @param accountNumber The account number
     * @return List of transaction documents
     */
    public List<Document> getTransactionHistory(String accountNumber) {
        logger.info("Retrieving transaction history for account: {}", accountNumber);
        try {
            List<Document> transactions = new ArrayList<>();
            
            // Query for transactions where the account is involved (as source or destination)
            logger.debug("Building query for transactions related to account {}", accountNumber);
            Bson query = Filters.or(
                    Filters.eq("account_number", accountNumber),
                    Filters.eq("source_account", accountNumber),
                    Filters.eq("destination_account", accountNumber)
            );
            
            // Sort by timestamp in ascending order
            logger.debug("Executing database query for transactions");
            transactionsCollection.find(query)
                    .sort(Indexes.ascending("timestamp"))
                    .into(transactions);
            
            logger.info("Retrieved {} transactions for account {}", transactions.size(), accountNumber);
            if (transactions.isEmpty()) {
                logger.debug("No transactions found for account {}", accountNumber);
            } else {
                logger.debug("Transaction date range: from {} to {}", 
                        transactions.get(0).getDate("timestamp"),
                        transactions.get(transactions.size() - 1).getDate("timestamp"));
            }
            return transactions;
        } catch (MongoException e) {
            logger.error("Failed to retrieve transaction history for account {}: {}", 
                    accountNumber, e.getMessage());
            logger.debug("Transaction history retrieval error details", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Convert a MongoDB document to a Transaction object.
     * 
     * @param doc The MongoDB document
     * @return A Transaction object
     */
    public Transaction documentToTransaction(Document doc) {
        logger.debug("Converting document to Transaction object: {}", doc.getString("transaction_id"));
        try {
            String transactionId = doc.getString("transaction_id");
            String typeStr = doc.getString("transaction_type");
            Transaction.TransactionType type = Transaction.TransactionType.valueOf(typeStr);
            double amount = doc.getDouble("amount");
            double resultingBalance = doc.getDouble("resulting_balance");
            String description = doc.getString("description");
            String sourceAccount = doc.getString("source_account");
            String destinationAccount = doc.getString("destination_account");
            Date timestamp = doc.getDate("timestamp");
            
            Transaction transaction = new Transaction(
                    transactionId, 
                    type, 
                    amount, 
                    resultingBalance, 
                    description, 
                    sourceAccount, 
                    destinationAccount,
                    timestamp);
            
            logger.debug("Successfully converted document to Transaction: {} ({})", 
                    transactionId, timestamp);
            return transaction;
        } catch (Exception e) {
            logger.error("Failed to convert document to Transaction: {}", e.getMessage());
            logger.debug("Document conversion error details", e);
            throw new RuntimeException("Failed to convert document to Transaction", e);
        }
    }
    
    /**
     * Close the MongoDB client connection.
     */
    @PreDestroy
    public void close() {
        logger.info("Closing MongoDB connection");
        if (mongoClient != null) {
            try {
                mongoClient.close();
                logger.info("MongoDB connection closed successfully");
            } catch (Exception e) {
                logger.error("Failed to close MongoDB connection: {}", e.getMessage());
                logger.debug("Connection closure error details", e);
            }
        } else {
            logger.warn("Cannot close MongoDB connection: client is null");
        }
    }
}
