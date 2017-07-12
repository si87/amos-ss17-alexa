package amosalexa;

import amosalexa.server.Launcher;
import amosalexa.services.bankaccount.ContactTransferService;
import amosalexa.services.financing.AffordabilityService;
import api.aws.DynamoDbClient;
import api.banking.AccountAPI;
import model.banking.Card;
import model.banking.StandingOrder;
import model.db.Category;
import model.db.Contact;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Server;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Use simple tests for tests that only test speechlet input and output. Therefore use testIntent resp. testIntentMatches
 * methods.
 * (Call simple tests explicitly by executing 'gradle simpleTests')
 */
@org.junit.experimental.categories.Category(AmosAlexaSimpleTest.class)
public class AmosAlexaSimpleTestImpl extends AbstractAmosAlexaSpeechletTest implements AmosAlexaSimpleTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmosAlexaSimpleTestImpl.class);

    /*************************************
     *          Testing section          *
     *************************************/

    // Needed to ensure that the account balance is sufficient
    @BeforeClass
    public static void setUpAccount() {
        Calendar cal = Calendar.getInstance();
        Date time = cal.getTime();
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String openingDate = formatter.format(time);

        AccountAPI.createAccount("9999999999", 1250000, openingDate);
    }


    @Test
    public void affordabilityTest() throws Exception {

        // sometimes there is a amazon api problem - nothing we could handle -- comment out test if failing continues

        ArrayList<String> buyAskAnswers = new ArrayList<String>() {{
            add("Produkt a (.*) kostet (.*) Produkt b (.*) kostet (.*) Produkt c (.*) kostet (.*) Möchtest du ein Produkt kaufen");
            add(AffordabilityService.NO_RESULTS);
            add(AffordabilityService.TOO_FEW_RESULTS);
            add("Ein Fehler ist aufgetreten. " + AffordabilityService.ERROR);
        }};

        ArrayList<String> productSelectionAskAnswers = new ArrayList<String>() {{
            add(AffordabilityService.SELECTION_ASK);
            add(AffordabilityService.ERROR + " " + AffordabilityService.SELECTION_ASK);
        }};

        ArrayList<String> byeAnswers = new ArrayList<String>() {{
            add(AffordabilityService.BYE);
            add("Ein Fehler ist aufgetreten. " + AffordabilityService.ERROR);
        }};

        ArrayList<String> balanceCheckAnswers = new ArrayList<String>() {{
            add("(.*) Das Produkt kannst du dir nicht leisten! Dein kontostand beträgt €(.*) Möchtest du nach etwas anderem suchen");
            add(AffordabilityService.BYE);
            add("Ein Fehler ist aufgetreten. " + AffordabilityService.ERROR);
            add("Produkt (.*) " + AffordabilityService.NOTE_ASK);
        }};

        ArrayList<String> emailAnswers = new ArrayList<String>() {{
            add(AffordabilityService.BYE);
            add(AffordabilityService.EMAIL_ACK);
            add(AffordabilityService.SEARCH_ASK);
        }};


        newSession();
        testIntentMatches("AffordProduct", "ProductKeyword:Samsung", StringUtils.join(buyAskAnswers, "|"));
        testIntentMatches("AMAZON.YesIntent", StringUtils.join(productSelectionAskAnswers, "|"));
        testIntentMatches("AffordProduct", "ProductSelection:a", StringUtils.join(balanceCheckAnswers, "|"));
        testIntentMatches("AMAZON.YesIntent", StringUtils.join(emailAnswers, "|"));

        newSession();
        testIntentMatches("AffordProduct", "ProductKeyword:Samsung", StringUtils.join(buyAskAnswers, "|"));
        testIntentMatches("AMAZON.YesIntent", StringUtils.join(productSelectionAskAnswers, "|"));
        testIntentMatches("AffordProduct", "ProductSelection:a", StringUtils.join(balanceCheckAnswers, "|"));
        testIntentMatches("AMAZON.NoIntent", StringUtils.join(byeAnswers, "|"));

        newSession();
        testIntentMatches("AffordProduct", "ProductKeyword:Samsung", StringUtils.join(buyAskAnswers, "|"));
        testIntentMatches("AMAZON.NoIntent", StringUtils.join(byeAnswers, "|"));

        newSession();
        testIntentMatches("AffordProduct", "ProductKeyword:Samsung", StringUtils.join(buyAskAnswers, "|"));
        testIntentMatches("AMAZON.YesIntent", StringUtils.join(productSelectionAskAnswers, "|"));
        testIntentMatches("AffordProduct", "ProductSelection:randomtext", StringUtils.join(productSelectionAskAnswers, "|"));
    }


    @Test
    public void bankAccountTransactionIntentTest() throws IllegalAccessException, NoSuchFieldException, IOException {
        newSession();

        ArrayList<String> possibleAnswers = new ArrayList<String>() {{
            add("Du hast keine Transaktionen in deinem Konto");
            add("Du hast (.*) Transaktionen. Nummer (.*) Von (.*) in Höhe von €(.*)" +
                    "Nummer (.*) Von (.*) in Höhe von €(.*)" +
                    "Nummer (.*) Von (.*) in Höhe von €(.*)" +
                    " Möchtest du weitere Transaktionen hören");
            add("Möchtest du weitere Transaktionen hören");
        }};

        testIntentMatches("AccountInformation", "AccountInformationSlots:überweisungen", StringUtils.join(possibleAnswers, "|"));

        ArrayList<String> possibleAnswersYES = new ArrayList<String>() {{
            add("Du hast keine Überweisungen in deinem Konto");
            add("Nummer (.*) Von (.*) in Höhe von €(.*)" +
                    " Möchtest du weitere Transaktionen hören");
        }};

        testIntentMatches(
                "AMAZON.YesIntent", StringUtils.join(possibleAnswersYES, "|"));
    }

    @Test
    public void bankAccountInformationIntentTest() throws IllegalAccessException, NoSuchFieldException, IOException {
        newSession();
        testIntentMatches("AccountInformation", "AccountInformationSlots:zinssatz", "Dein zinssatz ist aktuell (.*)");
        testIntentMatches("AccountInformation", "AccountInformationSlots:kontostand", "Dein kontostand beträgt €(.*)");
        testIntentMatches("AccountInformation", "AccountInformationSlots:eröffnungsdatum", "Dein eröffnungsdatum war (.*)");
        testIntentMatches("AccountInformation", "AccountInformationSlots:kreditlimit", "Dein kreditlimit beträgt €(.*)");
        testIntentMatches("AccountInformation", "AccountInformationSlots:kreditkartenlimit", "Dein kreditkartenlimit beträgt €(.*)");
        testIntentMatches("AccountInformation", "AccountInformationSlots:kontonummer", "Deine kontonummer lautet (.*)");
        testIntentMatches("AccountInformation", "AccountInformationSlots:abhebegebühr", "Deine abhebegebühr beträgt (.*)");
        testIntentMatches("AccountInformation", "AccountInformationSlots:iban", "Deine iban lautet (.*)");
    }

    @Test
    public void blockCardIntentTest() throws Exception {
        newSession();

        testIntent(
                "BlockCardIntent", "BankCardNumber:123",
                "Möchten Sie die Karte 123 wirklich sperren?");

        testIntent(
                "AMAZON.YesIntent",
                "Karte 123 wurde gesperrt.");
    }

    @Test
    public void standingOrdersInfoTest() throws IllegalAccessException, NoSuchFieldException, IOException {
        newSession();

        ArrayList<String> possibleAnswers = new ArrayList<String>() {{
            add("Keine Dauerauftraege vorhanden.");
            add("Du hast momentan einen Dauerauftrag. " +
                    "Dauerauftrag Nummer \\d+: Ueberweise monatlich \\d+\\.\\d+ Euro auf dein Sparkonto.(.*)");
            add("Du hast momentan (.*) Dauerauftraege. " +
                    "Dauerauftrag Nummer \\d+: Ueberweise monatlich \\d+\\.\\d+ Euro auf dein Sparkonto.(.*)");
            add("Du hast momentan (.*) Dauerauftraege. " +
                    "Dauerauftrag Nummer \\d+: Ueberweise monatlich \\d+\\.\\d+ Euro an (.*)");
        }};
        //TODO this test expects to be at least 4 standing orders existent in the system
        testIntentMatches(
                "StandingOrdersInfoIntent", StringUtils.join(possibleAnswers, "|"));
        //Test speech answer with some securities in the depot
        String response = performIntent("AMAZON.YesIntent");
        Pattern p = Pattern.compile("Dauerauftrag Nummer \\d+: Ueberweise monatlich \\d+\\.\\d+ Euro auf dein Sparkonto." +
                " Moechtest du einen weiteren Eintrag hoeren?" +
                "|Dauerauftrag Nummer \\d+: Ueberweise monatlich \\d+\\.\\d+ Euro an (.*)");
        Matcher m = p.matcher(response);
        if (!m.find()) {
            fail("Unexpected speech output.\nExcpected: " + p.pattern() + "\nAcutual: " + response);
        }

        testIntentMatches(
                "AMAZON.NoIntent", "Okay, tschuess!");
    }

    @Test
    public void standingOrderUpdateSmartIntentTest() throws IllegalAccessException, NoSuchFieldException, IOException {
        newSession();

        Collection<StandingOrder> standingOrdersCollection = AccountAPI.getStandingOrdersForAccount("9999999999");

        List<StandingOrder> standingOrders = new ArrayList<>(standingOrdersCollection);
        for (StandingOrder standingOrder : standingOrders) {
            if (standingOrder.getPayee().toLowerCase().equals("max mustermann")) {
                String orderAmount = standingOrder.getAmount().toString();
                testIntent(
                        "StandingOrderSmartIntent", "Payee:max", "PayeeSecondName:mustermann", "orderAmount:10",
                        "Der Dauerauftrag für max mustermann über " + orderAmount + " Euro existiert schon. Möchtest du diesen aktualisieren");
//                testIntent(
//                        "AMAZON.YesIntent",
//                        "Der Dauerauftrag Nummer " + standingOrder.getStandingOrderId() + " für Max Mustermann über 10 euro wurde erfolgreich aktualisiert");
                break;
            }
        }
    }

    @Test
    public void standingOrderCreateSmartIntentTest() throws IllegalAccessException, NoSuchFieldException, IOException {
        newSession();

        Collection<StandingOrder> standingOrdersCollection = AccountAPI.getStandingOrdersForAccount("9999999999");

        List<StandingOrder> standingOrders = new ArrayList<>(standingOrdersCollection);
        for (StandingOrder standingOrder : standingOrders) {
            if (standingOrder.getPayee().toLowerCase().equals("max mustermann")) {
                String orderAmount = standingOrder.getAmount().toString();
                testIntent(
                        "StandingOrderSmartIntent", "Payee:max", "PayeeSecondName:mustermann", "orderAmount:10",
                        "Der Dauerauftrag für max mustermann über " + orderAmount + " Euro existiert schon. Möchtest du diesen aktualisieren");
                testIntent(
                        "StandingOrderSmartIntent",
                        "Der neue Dauerauftrag für max mustermann über 10 Euro wurde erfolgreich eingerichtet");
                break;
            }
        }
    }

    @Test
    public void contactTest() throws IllegalAccessException, NoSuchFieldException, IOException {
        newSession();

        //Get today´s date in the right format
        Date now = new Date();
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String todayDate = formatter.format(now);

        //Create a transaction that we can use for ContactAddIntent (ingoing transaction)
        /*
        Transaction transaction = TransactionAPI.createTransaction(1, "DE60100000000000000001",
                "DE50100000000000000001", todayDate,
                "Ueberweisung fuer Unit Test", null, "Sandra");
        */

        int transactionId1 = 18877; //transaction.getTransactionId().intValue();
        int transactionId2 = 6328; //transaction without remitter or payee
        int transactionId3 = 23432423; //transaction not existent

        String transactionRemitter = "Sandra";//for transactionId1 //transaction.getRemitter();

        //Test add contact no transaction number given
        testIntent(
                "ContactAddIntent",
                "TransactionNumber: ", "Das habe ich nicht ganz verstanden. Bitte wiederhole deine Eingabe.");

        //Test add contact successful
        testIntent(
                "ContactAddIntent",
                "TransactionNumber:" + transactionId1, "Moechtest du " + transactionRemitter + " als " +
                        "Kontakt speichern?");
        testIntent(
                "AMAZON.YesIntent",
                "Okay! Der Kontakt Sandra wurde angelegt.");

        //Test add contact unknown payee/remitter
        testIntent(
                "ContactAddIntent",
                "TransactionNumber:" + transactionId2, "Ich kann fuer diese Transaktion keine Kontaktdaten speichern, weil der Name des Auftraggebers" +
                        " nicht bekannt ist. Bitte wiederhole deine Eingabe oder breche ab, indem du \"Alexa, Stop!\" sagst.");

        //Test add contact transaction not existent
        testIntent(
                "ContactAddIntent",
                "TransactionNumber:" + transactionId3, "Ich habe keine Transaktion mit dieser Nummer gefunden." +
                        " Bitte wiederhole deine Eingabe.");


        //Get the contact that we just created by searching for the contact with highest id
        List<Contact> allContacts = DynamoDbClient.instance.getItems(Contact.TABLE_NAME, Contact::new);
        final Comparator<Contact> comp2 = Comparator.comparingInt(c -> c.getId());
        Contact latestContact = allContacts.stream().max(comp2).get();
        LOGGER.info("Contact: " + latestContact.getName());
        String latestContactId = String.valueOf(latestContact.getId());

        //Test contact list
        testIntentMatches(
                "ContactListInfoIntent",
                "Kontakt \\d+: Name: (.*), IBAN: (.*).(.*)");

        //Test delete contact. Therefore delete the contact that we just created.
        testIntent(
                "ContactDeleteIntent",
                "ContactID:" + latestContactId, "Moechtest du Kontakt Nummer " + latestContactId + " wirklich " +
                        "loeschen?");
        testIntent(
                "AMAZON.YesIntent",
                "Kontakt wurde geloescht.");
    }

    @Test
    public void savingsPlanTest() throws Exception {
        newSession();

        List<Category> categories = DynamoDbClient.instance.getItems(Category.TABLE_NAME, Category::new);
        Category category = categories.get(0);

        testIntent("SavingsPlanIntroIntent",
                "Was moechtest du als Grundbetrag anlegen?"
        );
        testIntent("PlainNumberIntent", "Number:1500",
                "Wie viele Jahre moechtest du das Geld anlegen?"
        );
        testIntent("PlainNumberIntent", "Number:2",
                "Welchen Geldbetrag moechtest du monatlich investieren?"
        );
        testIntentMatches("PlainNumberIntent", "Number:150",
                "Bei einem Zinssatz von zwei Prozent waere der Gesamtsparbetrag am Ende des Zeitraums insgesamt (.*) Euro\\. Soll ich diesen Sparplan fuer dich anlegen\\?"
        );

        //Calculate what first payment date of the savings plan should be (depending on today´s date)
        Calendar calendar = Calendar.getInstance();
        String nextPayin = String.format("01.%02d.%d", calendar.get(Calendar.MONTH) + 2, calendar.get(Calendar.YEAR));

        //Test NoIntent handling
        testIntent(
                "AMAZON.NoIntent",
                "Nenne einen der Parameter, die du aendern willst " +
                        "oder beginne neu, indem du \"Neu\" sagst.");
        testIntent(
                "SavingsPlanChangeParameterIntent", "SavingsPlanParameter:laufzeit",
                "Wie viele Jahre moechtest du das Geld anlegen?");
        testIntentMatches("PlainNumberIntent", "Number:6",
                "Bei einem Zinssatz von zwei Prozent waere der Gesamtsparbetrag am Ende des Zeitraums insgesamt (.*) Euro\\. " +
                        "Soll ich diesen Sparplan fuer dich anlegen\\?");

        //Test with YesIntent, savings plan should be actually created
        testIntent(
                "AMAZON.YesIntent",
                "Okay! Ich habe den Sparplan angelegt. Der Grundbetrag von 1500 Euro wird deinem Sparkonto gutgeschrieben. Die erste regelmaeßige Einzahlung von 150 Euro erfolgt am " + nextPayin + "."
                        + " Zu welcher Kategorie soll der Dauerauftrag hinzugefügt werden. Sag zum Beispiel Kategorie " + Category.categoryListText());

        testIntentMatches("PlainCategoryIntent", "Category:" + category.getName(),
                "Verstanden. Der Dauerauftrag wurde zur Kategorie " + category.getName() + " hinzugefügt");

        Collection<StandingOrder> allStandingOrders = AccountAPI.getStandingOrdersForAccount(TEST_ACCOUNT_NUMBER);
        final Comparator<StandingOrder> comp = Comparator.comparingInt(s -> s.getStandingOrderId().intValue());
        int latestStandingOrderId = allStandingOrders.stream().max(comp).get().getStandingOrderId().intValue();
        LOGGER.info("Latest standing order ID: " + latestStandingOrderId);

        // We need to start a new session here because the dialog ends after the YesIntent
        newSession();

        testIntent(
                "StandingOrdersDeleteIntent",
                "Number:" + latestStandingOrderId, "Moechtest du den Dauerauftrag mit der Nummer "
                        + latestStandingOrderId + " wirklich loeschen?");

        testIntent(
                "AMAZON.YesIntent",
                "Dauerauftrag Nummer " + latestStandingOrderId + " wurde geloescht.");
    }

    @Test
    public void categoryStatusInfoIntentTest() throws IllegalAccessException, NoSuchFieldException, IOException {
        newSession();
        //Test assumes that category 'lebensmittel' already exists

        //Fetch category object 'lebensmittel' previously to be able to set it back afterwards, so that the test
        //does not affect our data
        List<Category> categories = DynamoDbClient.instance.getItems(Category.TABLE_NAME, Category::new);
        Category category = null;
        Double originalLimit = null;
        Double originialSpending = null;
        //We assume that 'lebensmittel' category exists!
        for (Category cat : categories) {
            if (cat.getName().equals("lebensmittel")) {
                category = cat;
                originalLimit = cat.getLimit();
                originialSpending = cat.getSpending();
            }
        }

        //Set spending to 0 and limit to 100 for test purpose
        category.setSpending(0);
        category.setLimit(100);
        category.setSpending(5);
        LOGGER.info("getSpending: " + category.getSpending());
        LOGGER.info("getLimit: " + category.getLimit());
        DynamoDbClient.instance.putItem(Category.TABLE_NAME, category);

        //Simple status test
        testIntent("CategoryStatusInfoIntent", "Category:lebensmittel",
                "Du hast bereits 5% des Limits von 100.0 Euro fuer die Kategorie lebensmittel ausgegeben. Du kannst noch" +
                        " 95.0 Euro für diese Kategorie ausgeben.");

        category.setLimit(100);
        category.setSpending(150);
        LOGGER.info("getSpending: " + category.getSpending());
        LOGGER.info("getLimit: " + category.getLimit());
        DynamoDbClient.instance.putItem(Category.TABLE_NAME, category);

        //Simple status test
        testIntent("CategoryStatusInfoIntent", "Category:lebensmittel",
                "Du hast bereits 150% des Limits von 100.0 Euro fuer die Kategorie lebensmittel ausgegeben. Du kannst noch " +
                        "0.0 Euro für diese Kategorie ausgeben.");
    }

    @Test
    public void categoryLimitTest() throws IllegalAccessException, NoSuchFieldException, IOException {
        newSession();

        //Fetch category object 'lebensmittel' previously to be able to set it back afterwards, so that the test
        //does not affect our data
        List<Category> categories = DynamoDbClient.instance.getItems(Category.TABLE_NAME, Category::new);
        Category category = null;
        //We assume that 'lebensmittel' category exists!
        for (Category cat : categories) {
            if (cat.getName().equals("lebensmittel")) {
                category = cat;
            }
        }

        //Test limit info with unknown category name
        testIntent("CategoryLimitInfoIntent", "Category:dsafoihdsfzzzzzssdf",
                "Es gibt keine Kategorie mit diesem Namen. Waehle eine andere Kategorie oder" +
                        " erhalte eine Info zu den verfuegbaren Kategorien.");

        //Test plain category name input
        testIntentMatches("PlainCategoryIntent", "Category:lebensmittel",
                "Das Limit fuer die Kategorie lebensmittel liegt bei (.*) Euro.");

        //Test limit setting
        testIntent("CategoryLimitSetIntent", "Category:lebensmittel", "CategoryLimit:250",
                "Moechtest du das Ausgabelimit fuer die Kategorie lebensmittel wirklich auf 250 Euro setzen?");

        //Setting confirmation (yes)
        testIntent("AMAZON.YesIntent", "Limit fuer lebensmittel wurde gesetzt.");

        //Test limit info after setting
        testIntent("CategoryLimitInfoIntent", "Category:lebensmittel",
                "Das Limit fuer die Kategorie lebensmittel liegt bei 250.0 Euro.");

        //Reset the category lebensmittel (as it was before)
        List<Category> categories2 = DynamoDbClient.instance.getItems(Category.TABLE_NAME, Category::new);
        for (Category c : categories2) {
            DynamoDbClient.instance.deleteItem(Category.TABLE_NAME, c);
        }
        Predicate<Category> categoryPredicate = c -> c.getName().equals("lebensmittel");
        categories2.removeIf(categoryPredicate);
        LOGGER.info("Categories2: " + categories2);
        for (Category c : categories2) {
            DynamoDbClient.instance.putItem(Category.TABLE_NAME, c);
        }
        DynamoDbClient.instance.putItem(Category.TABLE_NAME, category);
    }

    @Test
    public void categorySpendingTestIntent() throws IllegalAccessException, NoSuchFieldException, IOException {
        newSession();
        //Test assumes that category 'lebensmittel' already exists

        //Fetch category object 'lebensmittel' previously to be able to set it back afterwards, so that the test
        //does not affect our data
        List<Category> categories = DynamoDbClient.instance.getItems(Category.TABLE_NAME, Category::new);
        Category category = null;
        Double originalLimit = null;
        Double originialSpending = null;
        //We assume that 'lebensmittel' category exists!
        for (Category cat : categories) {
            if (cat.getName().equals("lebensmittel")) {
                category = cat;
                originalLimit = cat.getLimit();
                originialSpending = cat.getSpending();
            }
        }

        //Set spending to 0 and limit to 100 for test purpose
        category.setSpending(0);
        category.setLimit(100);
        LOGGER.info("getSpending: " + category.getSpending());
        LOGGER.info("getLimit: " + category.getLimit());
        DynamoDbClient.instance.putItem(Category.TABLE_NAME, category);

        //Simple spending test
        testIntent("CategorySpendingIntent", "Category:lebensmittel", "Amount:5",
                "Okay. Ich habe 5 Euro fuer lebensmittel notiert.");

        //Spending 89 OK!
        testIntent("CategorySpendingIntent", "Category:lebensmittel", "Amount:84",
                "Okay. Ich habe 84 Euro fuer lebensmittel notiert.");

        //Spending 90 Warning!
        testIntent("CategorySpendingIntent", "Category:lebensmittel", "Amount:1",
                "Okay. Ich habe 1 Euro fuer lebensmittel notiert. Warnung! Du hast in diesem Monat bereits 90 Euro" +
                        " fuer lebensmittel ausgegeben. Das sind 90% des Limits fuer diese Kategorie.");

        //Limit overrun warning
        testIntent("CategorySpendingIntent", "Category:lebensmittel", "Amount:20",
                "Warnung! Durch diese Aktion wirst du das Limit von 100 fuer die Kategorie lebensmittel ueberschreiten." +
                        " Soll ich den Betrag trotzdem notieren?");

        testIntent("AMAZON.YesIntent", "Okay. Ich habe 20 Euro fuer lebensmittel notiert. Warnung! Du hast" +
                " in diesem Monat bereits 110 Euro fuer lebensmittel ausgegeben. Das sind 110% des Limits fuer diese Kategorie.");

        //Reset category lebensmittel to previous state
        category.setSpending(0);
        category.setLimit(originalLimit);
        DynamoDbClient.instance.putItem(Category.TABLE_NAME, category);
    }

    @Test
    public void replacementCardDialogTest() throws Exception {
        newSession();

        final String accountNumber = "9999999999";

        Collection<Card> cards = AccountAPI.getCardsForAccount(accountNumber);
        for (Card card : cards) {
            AccountAPI.deleteCard(accountNumber, card.getCardId());
        }

        AccountAPI.createCardForAccount(accountNumber, Card.CardType.DEBIT, accountNumber,
                Card.Status.ACTIVE, new DateTime(2018, 5, 1, 12, 0).toLocalDate().toString());

        ArrayList<String> possibleAnswers = new ArrayList<String>() {{
            add("Bestellung einer Ersatzkarte. Es wurden folgende Karten gefunden: (.*)");
            add("Es wurden keine Kredit- oder EC-Karten gefunden.");
        }};

        String response = testIntentMatches("ReplacementCardIntent", StringUtils.join(possibleAnswers, "|"));

        Pattern p = Pattern.compile("karte mit den Endziffern ([0-9]+)\\.");
        Matcher m = p.matcher(response);

        if (m.find()) {
            String endDigits = m.group(1);
            testIntent("PlainNumberIntent", "Number:" + endDigits,
                    "Wurde die Karte gesperrt oder wurde sie beschädigt?");

            testIntent("ReplacementCardReasonIntent", "ReplacementReason:beschaedigt",
                    "Soll ein Ersatz für die beschädigte Karte mit den Endziffern " + endDigits + " bestellt werden?");

            testIntent(
                    "AMAZON.YesIntent",
                    "Okay, eine Ersatzkarte wurde bestellt.");
        } else {
            fail("Cannot find credit card.");
        }

        newSession();

        response = testIntentMatches("ReplacementCardIntent", StringUtils.join(possibleAnswers, "|"));

        p = Pattern.compile("karte mit den Endziffern ([0-9]+)\\.");
        m = p.matcher(response);

        if (m.find()) {
            String endDigits = m.group(1);
            testIntent("PlainNumberIntent", "Number:" + endDigits,
                    "Wurde die Karte gesperrt oder wurde sie beschädigt?");

            testIntent("ReplacementCardReasonIntent", "ReplacementReason:gesperrt",
                    "Soll ein Ersatz für die gesperrte Karte mit den Endziffern " + endDigits + " bestellt werden?");

            testIntent(
                    "AMAZON.NoIntent",
                    "");
        } else {
            fail("Cannot find credit card.");
        }
    }

    @Test
    public void transferTemplatesTest() throws Exception {
        newSession();

        String response = performIntent("ListTransferTemplatesIntent");

        Pattern p = Pattern.compile("Vorlage ([0-9]+) vom ([0-9\\.]+): Überweise ([0-9\\.]+) Euro an ([a-zA-Z]+).");
        Matcher m = p.matcher(response);

        if (m.find()) {
            int templateId = Integer.parseInt(m.group(1));
            double amount = Double.parseDouble(m.group(3));

            testIntent("EditTransferTemplateIntent", "TemplateID:" + templateId, "NewAmount:" + (amount * 2),
                    "Möchtest du den Betrag von Vorlage " + templateId + " von " + amount + " auf " + (amount * 2) + " ändern?");

            testIntent(
                    "AMAZON.YesIntent",
                    "Vorlage wurde erfolgreich gespeichert.");

            response = performIntent("ListTransferTemplatesIntent");

            p = Pattern.compile("Vorlage ([0-9]+) vom ([0-9\\.]+): Überweise ([0-9\\.]+) Euro an ([a-zA-Z]+).");
            m = p.matcher(response);

            if (m.find()) {
                assertEquals(templateId, Integer.parseInt(m.group(1)));
                assert (Math.abs(amount * 2 - Double.parseDouble(m.group(3))) < 0.001);

                testIntent("EditTransferTemplateIntent", "TemplateID:" + templateId, "NewAmount:" + amount,
                        "Möchtest du den Betrag von Vorlage " + templateId + " von " + (amount * 2) + " auf " + amount + " ändern?");

                testIntent(
                        "AMAZON.YesIntent",
                        "Vorlage wurde erfolgreich gespeichert.");
            } else {
                fail("Cannot find transfer template.");
            }
        } else {
            fail("Cannot find transfer template.");
        }
    }

    @Test
    public void setBalanceLimitTest() throws Exception {
        int randomNum = ThreadLocalRandom.current().nextInt(100, 200);

        newSession();

        testIntent("SetBalanceLimitIntent", "BalanceLimitAmount:" + randomNum, "Möchtest du dein Kontolimit wirklich auf " + randomNum + " Euro setzen?");
        testIntent("AMAZON.YesIntent", "Okay, dein Kontolimit wurde auf " + randomNum + " Euro gesetzt.");

        newSession();

        testIntent("SetBalanceLimitIntent", "BalanceLimitAmount:" + randomNum, "Möchtest du dein Kontolimit wirklich auf " + randomNum + " Euro setzen?");
        testIntent("AMAZON.NoIntent", "");

        newSession();

        testIntent("GetBalanceLimitIntent", "Dein aktuelles Kontolimit beträgt " + randomNum + " Euro.");
    }

    @Test
    public void sameServiceTest() throws Exception {
        // pretend local environment
        Launcher.server = new Server();
        Launcher.server.start();

        newSession();

        testIntent("SetBalanceLimitIntent", "BalanceLimitAmount:100", "Möchtest du dein Kontolimit wirklich auf 100 Euro setzen?");

        // Switching to another Service should fail because the BalanceLimit dialog is currently active.
        testIntentMatches("SavingsPlanIntroIntent", "Ein Fehler ist aufgetreten.");

        newSession();

        // Switching to another Service works if a new session is started.
        testIntent("SetBalanceLimitIntent", "BalanceLimitAmount:100", "Möchtest du dein Kontolimit wirklich auf 100 Euro setzen?");
        newSession();
        testIntent("SavingsPlanIntroIntent", "Was moechtest du als Grundbetrag anlegen?");
        Launcher.server.stop();
    }

    @Test
    public void contactTransferTest() throws Exception {
        ContactTransferService.contactTable = Contact.TABLE_NAME + "_test";


        List<Category> categories = DynamoDbClient.instance.getItems(Category.TABLE_NAME, Category::new);
        Category category = categories.get(0);


        // Empty test contacts table
        DynamoDbClient.instance.clearItems(ContactTransferService.contactTable, Contact::new);

        DynamoDbClient.instance.putItem(ContactTransferService.contactTable, new Contact("Bob Marley", "UK1"));
        DynamoDbClient.instance.putItem(ContactTransferService.contactTable, new Contact("Bob Ray Simmons", "UK2"));
        DynamoDbClient.instance.putItem(ContactTransferService.contactTable, new Contact("Lucas", "DE1"));
        DynamoDbClient.instance.putItem(ContactTransferService.contactTable, new Contact("Sandra", "DE2"));

        newSession();

        testIntentMatches("ContactTransferIntent", "Contact:sandra", "Amount:1",
                "Dein aktueller Kontostand beträgt ([0-9\\.]+) Euro\\. Möchtest du 1\\.0 Euro an Sandra überweisen\\?");

        testIntentMatches("AMAZON.YesIntent",
                "Erfolgreich\\. 1\\.0 Euro wurden an Sandra überwiesen\\. Dein neuer Kontostand beträgt ([0-9\\.]+) Euro\\. " +
                        "Zu welcher Kategorie soll die Transaktion hinzugefügt werden. Sag zum Beispiel Kategorie " + Category.categoryListText());

        testIntentMatches("PlainCategoryIntent", "Category:" + category.getName(),
                "Verstanden. Die Transaktion wurde zur Kategorie " + category.getName() + " hinzugefügt");

        newSession();

        testIntentMatches("ContactTransferIntent", "Contact:bob", "Amount:1",
                "Ich habe 2 passende Kontakte gefunden\\. Bitte wähle einen aus: Kontakt Nummer 1: Bob ([A-Za-z ]+)\\. Kontakt Nummer 2: Bob ([A-Za-z ]+)\\. ");

        testIntentMatches("ContactChoiceIntent", "ContactIndex:1",
                "Dein aktueller Kontostand beträgt ([0-9\\.]+) Euro\\. Möchtest du 1\\.0 Euro an Bob ([A-Za-z ]+) überweisen\\?");

        testIntent("AMAZON.NoIntent",
                "Okay, verstanden. Dann bis zum nächsten Mal.");
    }

    @Test
    public void budgetReportTest() throws Exception {
        newSession();
        SessionStorage.getInstance().getStorage(sessionId).put("DEBUG", true);
        testIntentMatches("BudgetReportEMailIntent", "Okay, ich habe dir deinen Ausgabenreport per E-Mail gesendet.");
    }

    @Test
    public void categoryTest() throws Exception {
        newSession();

        Category.TABLE_NAME = "category_test";
        DynamoDbClient.instance.clearItems(Category.TABLE_NAME, Category::new);

        testIntent("AddCategoryIntent",
                "CategoryName:Auto",
                "Moechtest du die Kategorie Auto wirklich erstellen?");

        testIntent("AMAZON.YesIntent",
                "Verstanden. Die Kategorie Auto wurde erstellt.");

        testIntent("ShowCategoriesIntent",
                "Aktuell hast du folgende Kategorien: Auto, ");

        testIntent("AddCategoryIntent",
                "CategoryName:Lebensmittel",
                "Moechtest du die Kategorie Lebensmittel wirklich erstellen?");

        testIntent("AMAZON.NoIntent",
                "OK, verstanden. Dann bis bald.");

        testIntent("ShowCategoriesIntent",
                "Aktuell hast du folgende Kategorien: Auto, ");

        //FIXME fix precondition that category Auto must exist for this test
        testIntent("DeleteCategoryIntent",
                "CategoryName:Auto",
                "Möchtest du die Kategorie mit dem Namen 'Auto' und dem Limit von 0.0 Euro wirklich löschen?");

        testIntent("AMAZON.YesIntent",
                "OK, wie du willst. Ich habe die Kategorie mit dem Namen 'Auto' gelöscht. Hoffentlich bereust du es nicht.");

        testIntent("ShowCategoriesIntent",
                "Aktuell hast du keine Kategorien.");

        Category.TABLE_NAME = "category";
    }

    @Test
    public void periodicTransactionTest() throws IllegalAccessException, NoSuchFieldException, IOException {
        newSession();
        testIntent("PeriodicTransactionAddIntent", "TransactionNumber:999999",
                "Ich kann Transaktion Nummer 999999 nicht finden. Bitte aendere deine Eingabe.");
        testIntent("PeriodicTransactionAddIntent", "TransactionNumber: ",
                "Das habe ich nicht verstanden. Bitte wiederhole deine Eingabe.");
        testIntent("PeriodicTransactionAddIntent", "TransactionNumber:31",
                "Moechtest du die Transaktion mit der Nummer 31 wirklich als periodisch markieren?");
        testIntent("AMAZON.YesIntent",
                "Transaktion Nummer 31 wurde als periodisch markiert.");
        testIntent("PeriodicTransactionDeleteIntent", "TransactionNumber:31",
                "Moechtest du die Markierung als periodisch fuer die Transaktion mit der Nummer 31 wirklich entfernen?");
        testIntent("AMAZON.YesIntent",
                "Transaktion Nummer 31 ist nun nicht mehr als periodisch markiert.");
        testIntent("PeriodicTransactionDeleteIntent", "TransactionNumber:999999",
                "Ich kann Transaktion Nummer 999999 nicht finden. Bitte aendere deine Eingabe.");
        testIntent("PeriodicTransactionDeleteIntent", "TransactionNumber:999999",
                "Ich kann Transaktion Nummer 999999 nicht finden. Bitte aendere deine Eingabe.");
        testIntentMatches("PeriodicTransactionListIntent", "(.*)");
    }

}