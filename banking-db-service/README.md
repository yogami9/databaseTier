# Banking Database Service

This is a standalone MongoDB database service for a three-tier banking application. It exposes REST APIs for account and transaction operations.

## Project Structure

```
banking-db-service/
├── pom.xml                                # Maven configuration
├── Dockerfile                             # Docker configuration for Render
├── render.yaml                            # Render blueprint configuration
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── banking/
│   │   │           └── db/
│   │   │               ├── BankingDbServiceApplication.java  # Spring Boot main class
│   │   │               ├── controller/
│   │   │               │   ├── AccountController.java        # REST API for accounts
│   │   │               │   └── TransactionController.java    # REST API for transactions
│   │   │               ├── model/
│   │   │               │   ├── Account.java                  # Account data model
│   │   │               │   └── Transaction.java              # Transaction data model
│   │   │               └── service/
│   │   │                   └── MongoDBManager.java           # Database access layer
│   │   └── resources/
│   │       └── application.properties                        # Application configuration
│   └── test/                                                 # Test directory
```

## API Endpoints

### Account APIs:
- `GET /api/accounts` - Get all accounts
- `GET /api/accounts/{accountNumber}` - Get a specific account
- `POST /api/accounts` - Create a new account
- `PUT /api/accounts/{accountNumber}/balance` - Update account balance

### Transaction APIs:
- `POST /api/transactions` - Record a new transaction
- `GET /api/transactions/account/{accountNumber}` - Get transaction history for an account

## Running Locally

```bash
# Build the project
mvn clean package

# Run the application
java -jar target/banking-db-service.jar
```

## Deploying to Render

1. Push this project to a Git repository
2. Log in to your Render account
3. Create a new Web Service using the "Blueprint" option
4. Connect your repository
5. The service will be automatically deployed based on render.yaml
