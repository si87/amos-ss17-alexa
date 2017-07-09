package model.db;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAutoGeneratedKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "transaction_category")
public class TransactionDB {

    private String id;
    private String transactionId;
    private String categoryId;


    public TransactionDB(String transactionId, String categoryId) {
        this.transactionId = transactionId;
        this.categoryId = categoryId;
    }

    @DynamoDBHashKey
    @DynamoDBAutoGeneratedKey
    public String getId() {
        return id;
    }

    public TransactionDB setId(String id) {
        this.id = id;
        return this;
    }

    @DynamoDBAttribute
    public String getCategoryId() {
        return categoryId;
    }

    public TransactionDB setCategoryId(String categoryId) {
        this.categoryId = categoryId;
        return this;
    }

    @DynamoDBAttribute
    public String getTransactionId() {
        return transactionId;
    }

    public TransactionDB setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }
}
