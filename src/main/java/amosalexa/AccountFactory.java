package amosalexa;

import api.aws.DynamoDbMapper;
import api.banking.AccountAPI;
import model.banking.Account;
import model.banking.StandingOrder;
import model.db.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class AccountFactory {

    private static final Logger log = LoggerFactory.getLogger(AccountFactory.class);

    /**
     * singleton
     */
    private static AccountFactory accountFactory = new AccountFactory();

    /**
     * balance of demo account
     */
    private static final long ACCOUNT_BALANCE_DEMO = 1000000L;

    /**
     * opening date of demo account
     */
    private static final String ACCOUNT_OPENING_DATE_DEMO = new DateTime().minusMonths(1).toString("yyyy-MM-dd");

    /**
     * date of today
     */
    private static final String TODAY_DATE = new DateTime().toString("yyyy-MM-dd");

    /**
     * dynamo db mapper
     */
    private static DynamoDbMapper dynamoDbMapper = DynamoDbMapper.getInstance();


    public static AccountFactory getInstance(){
        synchronized (AccountFactory.class){
            return accountFactory;
        }
    }

    /**
     * 7 transactions
     * 3 standing orders - with categories
     * 3 stocks
     * 5 categories with 3 keywords
     * 3 contacts with easy names
     */
    public Account createDemo() {

        // user - needed for authenticating all following API calls
        createDemoUser();

        // account + savings account
        Account newDemoAccount = createDemoAccount();
        Account newDemoSavingsAccount = createSavingsAccount();
        saveAccount(newDemoAccount.getNumber(), newDemoSavingsAccount.getNumber(), true);

        // contact accounts
        List<Account> contactAccounts = createContactsAccount();
        saveContactAccounts(contactAccounts);

        // categories
        createCategories(newDemoAccount);

        // standing orders
        createStandingOrders(newDemoAccount, contactAccounts);


        return newDemoAccount;
    }

    private void createDemoUser() {
        User user = new User();
        user.setId(AmosAlexaSpeechlet.USER_ID);
        DynamoDbMapper.getInstance().save(user);
    }

    private void saveContactAccounts(List<Account> contactAccounts) {
        String[] names = {"bob", "sandra", "lucas"};
        int i = 0;
        for(Account contactAccount : contactAccounts){
            dynamoDbMapper.save(new ContactDB(contactAccount.getNumber(), names[i]));
            i++;
        }
    }

    private Account createDemoAccount() {
        String accountNumber = getRandomAccountNumber();
        Account newDemoAccount = null;
        if (!existAccount(accountNumber)) {
            newDemoAccount = AccountAPI.createAccount(accountNumber, ACCOUNT_BALANCE_DEMO, ACCOUNT_OPENING_DATE_DEMO);
        }
        return newDemoAccount;
    }

    private Account createSavingsAccount() {
        String accountNumber = getRandomAccountNumber();
        Account newDemoSavingsAccount = null;
        if (!existAccount(accountNumber)) {
            newDemoSavingsAccount = AccountAPI.createAccount(accountNumber, ACCOUNT_BALANCE_DEMO, ACCOUNT_OPENING_DATE_DEMO);
        }
        return newDemoSavingsAccount;
    }

    private List<Account> createContactsAccount() {
        String accountNumber = getRandomAccountNumber();
        List<Account> contactAccounts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (!existAccount(accountNumber)) {
                Account contactAccount = AccountAPI.createAccount(accountNumber, ACCOUNT_BALANCE_DEMO, ACCOUNT_OPENING_DATE_DEMO);
                contactAccounts.add(contactAccount);
            }
        }
        return contactAccounts;
    }

    private void createCategories(Account newDemoAccount) {
        String[] categoryNames = {"auto", "haushalt", "freizeit", "reisen", "sonstiges"};
        for (String categoryName : categoryNames) {
            dynamoDbMapper.save(new CategoryDB(newDemoAccount.getNumber(), categoryName, 200));
        }
    }

    private void createStandingOrders(Account demoAccount, List<Account> contactAccounts) {
        for(Account contactAccount : contactAccounts){
            StandingOrder standingOrder = AccountAPI.createStandingOrderForAccount(demoAccount.getNumber(), getContactName(contactAccount.getNumber()), 50,
                    contactAccount.getIban(), TODAY_DATE, StandingOrder.ExecutionRate.MONTHLY, "Demo Dauerauftrag");
            dynamoDbMapper.save(new StandingOrderDB(demoAccount.getNumber(), standingOrder.getStandingOrderId().toString(), getRandomCategoryId()));
        }
    }

    private String getRandomCategoryId(){
        List<CategoryDB> categoryDBList = dynamoDbMapper.loadAll(CategoryDB.class);
        int randomNum = ThreadLocalRandom.current().nextInt(0, categoryDBList.size());
        return categoryDBList.get(randomNum).getId();
    }

    private String getContactName(String accountNumber) {
        List<ContactDB> contactDBList = dynamoDbMapper.loadAll(ContactDB.class);
        for(ContactDB contactDB : contactDBList){
            if(contactDB.getAccountNumber().equals(accountNumber))
                return contactDB.getName();
        }
        return null;
    }

    private void removeDemoAccounts() {
        List<AccountDB> accountDBList = dynamoDbMapper.loadAll(AccountDB.class);
        for (AccountDB accountDB : accountDBList) {
            if (accountDB.isDemo()) {
                log.info("Remove old demo account: " + accountDB.getAccountNumber());

                dynamoDbMapper.delete(accountDB);

                removeDemoCategories(accountDB.getAccountNumber());
                removeDemoStandingOrders(accountDB.getAccountNumber());
                removeDemoTransactions(accountDB.getAccountNumber());
                removeDemoSpending(accountDB.getAccountNumber());
                removeDemoContacts(accountDB.getAccountNumber());
            }
        }
    }

    private void removeDemoCategories(String accountNumber) {
        List<CategoryDB> categoryDBList = dynamoDbMapper.loadAll(CategoryDB.class);
        for (CategoryDB categoryDB : categoryDBList) {
            if (categoryDB.getAccountNumber().equals(accountNumber)) {
                dynamoDbMapper.delete(categoryDB);
            }
        }
    }

    private void removeDemoTransactions(String accountNumber) {
        List<TransactionDB> categoryDBListList = dynamoDbMapper.loadAll(TransactionDB.class);
        for (TransactionDB transactionDB : categoryDBListList) {
            if (transactionDB.getAccountNumber().equals(accountNumber)) {
                dynamoDbMapper.delete(transactionDB);
            }
        }
    }

    private void removeDemoStandingOrders(String accountNumber) {
        List<StandingOrderDB> standingOrderDBList = dynamoDbMapper.loadAll(StandingOrderDB.class);
        for (StandingOrderDB standingOrderDB : standingOrderDBList) {
            if (standingOrderDB.getAccountNumber().equals(accountNumber)) {
                dynamoDbMapper.delete(standingOrderDB);
            }
        }
    }

    private void removeDemoSpending(String accountNumber) {
        List<Spending> spendingList = dynamoDbMapper.loadAll(Spending.class);
        for (Spending spending : spendingList) {
            if (spending.getAccountNumber().equals(accountNumber)) {
                dynamoDbMapper.delete(spending);
            }
        }
    }

    private void removeDemoContacts(String accountNumber) {
        List<ContactDB> contactDBList = dynamoDbMapper.loadAll(ContactDB.class);
        for (ContactDB contactDB : contactDBList) {
            if (contactDB.getAccountNumber().equals(accountNumber)) {
                dynamoDbMapper.delete(contactDB);
            }
        }
    }


    private Account getDemoAccount() {
        List<AccountDB> accountDBList = dynamoDbMapper.loadAll(AccountDB.class);
        for (AccountDB accountDB : accountDBList) {
            if (accountDB.isDemo()) {
                return AccountAPI.getAccount(accountDB.getAccountNumber());
            }
        }
        return null;
    }


    public String getDemoAccountNumber() {
        if (getDemoAccount() != null) {
            return getDemoAccount().getNumber();
        }
        return null;
    }

    /**
     * saves account number to db
     *
     * @param accountNumber account number
     * @param isDemo        is valid account for demo
     */
    private void saveAccount(String accountNumber, String savingsAccountNumber, boolean isDemo) {
        removeDemoAccounts();
        dynamoDbMapper.save(new AccountDB(accountNumber, savingsAccountNumber, isDemo));
    }

    private boolean existAccount(String accountNumber) {
        try {
            AccountAPI.getAccount(accountNumber);
            return true;
        } catch (HttpClientErrorException e) {
            return false;
        }
    }

    /**
     * generates random account number
     *
     * @return new account number
     */
    private String getRandomAccountNumber() {
        Random rnd = new Random();
        BigInteger max = new BigInteger("9999999999");
        do {
            BigInteger i = new BigInteger(max.bitLength(), rnd);
            if (i.compareTo(max) <= 0 && !existAccount(i.toString()))
                return i.toString();
        } while (true);
    }
}
