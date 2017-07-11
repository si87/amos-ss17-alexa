package api.aws;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;

public class DynamoDbMapper {

    private AmazonDynamoDBClient dynamoDbClient;
    private DynamoDBMapper mapper;
    private DynamoDB dynamoDB;


    public DynamoDbMapper(AmazonDynamoDBClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.mapper = new DynamoDBMapper(dynamoDbClient);
        this.dynamoDB = new DynamoDB(dynamoDbClient);
    }

    /**
     * creates a new table by a pojo class
     * @param cl class which will be mapped to the new table
     */
    public void createTable(Class cl) throws InterruptedException {
        CreateTableRequest tableRequest = mapper.generateCreateTableRequest(cl);
        tableRequest.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));
        Table table = dynamoDB.createTable(tableRequest);
        table.waitForActive();
    }

    /**
     * drops table by a pojo class
     * @param cl class which will be mapped to drop a table
     */
    public void dropTable(Class cl){
        DeleteTableRequest tableRequest = mapper.generateDeleteTableRequest(cl);
        dynamoDbClient.deleteTable(tableRequest);
    }

    /**
     * saves entity in db
     * @param object entity
     */
    public void insert(Object object){
        mapper.save(object);
    }

    /**
     * delete entity from db
     * @param object entity
     */
    public void delete(Object object){
        mapper.delete(object);
    }


    /**
     * load entity from db
     * @param cl mapping class
     * @param objectKey key
     * @return entity
     */
    public Object load(Class cl, Object objectKey){
      return mapper.load(cl, objectKey);
    }
}