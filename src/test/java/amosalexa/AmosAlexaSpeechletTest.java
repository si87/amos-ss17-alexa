package amosalexa;

import api.banking.AccountAPI;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.OutputSpeech;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.SsmlOutputSpeech;
import model.banking.StandingOrder;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AmosAlexaSpeechletTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmosAlexaSpeechletTest.class);

    private Session session;
    private final String SESSION_ID = "SessionId.2682fed6-193f-48b3-afd7-c6185d075ddf";

    // FIXME: Get the current account number from the session
    private static final String ACCOUNT_NUMBER = "9999999999";

    private static Integer savingsPlanTestStandingOrderId;

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
    public void bankTransferIntentTest() throws Throwable {
        newSession();

        testIntentMatches("BankTransferIntent", "name:anne", "amount:2",
                "Aktuell betraegt dein Kontostand (.*) Euro\\. Bist du sicher, dass du 2 Euro an anne ueberweisen willst\\?");

        testIntentMatches("AMAZON.YesIntent",
                "Ok, (.*) Euro wurden an anne ueberwiesen\\. Dein neuer Kontostand betraegt (.*) Euro\\.");


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
        }};
        testIntentMatches(
                "StandingOrdersInfoIntent", StringUtils.join(possibleAnswers, "|"));
        testIntentMatches(
                "AMAZON.YesIntent",
                "Dauerauftrag Nummer \\d+: Ueberweise monatlich \\d+\\.\\d+ Euro auf dein Sparkonto.(.*)");
        testIntentMatches(
                "AMAZON.NoIntent", "Okay, tschuess!");
    }

    @Test
    public void savingsPlanTest() throws Exception {
        newSession();

        testIntentMatches(
                "SavingsPlanIntent", "AnzahlJahre:2", "EinzahlungMonat:150", "Grundbetrag:1500",
                "Bei einem Zinssatz von zwei Prozent waere der Gesamtsparbetrag am Ende des Zeitraums insgesamt (.*) Euro\\. Soll ich diesen Sparplan fuer dich anlegen\\?"
        );

        Calendar calendar = Calendar.getInstance();
        String nextPayin = String.format("01.%02d.%d", calendar.get(Calendar.MONTH) + 2, calendar.get(Calendar.YEAR));

        testIntent(
                "AMAZON.YesIntent",
                "Okay! Ich habe den Sparplan angelegt. Der Grundbetrag von 1500 Euro wird deinem Sparkonto gutgeschrieben. Die erste regelmaeßige Einzahlung von 150 Euro erfolgt am " + nextPayin + ".");

        Collection<StandingOrder> allStandingOrders = AccountAPI.getStandingOrdersForAccount(ACCOUNT_NUMBER);
        final Comparator<StandingOrder> comp = Comparator.comparingInt(s -> s.getStandingOrderId().intValue());
        int latestStandingOrderId = allStandingOrders.stream().max(comp).get().getStandingOrderId().intValue();
        LOGGER.info("Latest standing order ID: " + latestStandingOrderId);
        savingsPlanTestStandingOrderId = latestStandingOrderId;

        testIntent(
                "StandingOrdersDeleteIntent",
                "Number:" + latestStandingOrderId, "Moechtest du den Dauerauftrag mit der Nummer "
                        + latestStandingOrderId + " wirklich loeschen?");

        testIntent(
                "AMAZON.YesIntent",
                "Dauerauftrag Nummer " + latestStandingOrderId + " wurde geloescht.");
    }

    @Test
    public void replacementCardDialogTest() throws Exception {
        newSession();

        ArrayList<String> possibleAnswers = new ArrayList<String>() {{
            add("Bestellung einer Ersatzkarte. Es wurden folgende Karten gefunden: (.*)");
            add("Es wurden keine Kredit- oder EC-Karten gefunden.");
        }};

        String response = testIntentMatches("ReplacementCardIntent", StringUtils.join(possibleAnswers, "|"));

        if (response.equals("Es wurden keine Kredit- oder EC-Karten gefunden.")) {
            //Fallback
            return;
        }

        Pattern p = Pattern.compile("karte mit den Endziffern ([0-9]+)\\.");
        Matcher m = p.matcher(response);

        if (m.find()) {
            String endDigits = m.group(1);
            testIntent("FourDigitNumberIntent", "FourDigits:" + endDigits,
                    "Wurde die Karte gesperrt oder wurde sie beschädigt?");

            testIntent("ReplacementCardReasonIntent", "ReplacementReason:beschaedigt",
                    "Soll ein Ersatz für die beschädigte Karte mit den Endziffern " + endDigits + " bestellt werden?");

            testIntent(
                    "AMAZON.YesIntent",
                    "Okay, eine Ersatzkarte wurde bestellt.");
        } else {
            assertTrue(false);
        }
    }

    /************************************
     *          Helper methods          *
     ************************************/

    private String testIntentMatches(String intent, String... params) throws IOException, NoSuchFieldException, IllegalAccessException {
        String[] slots = new String[params.length - 1];
        String expectedOutput = null;

        int i = 0;
        for (String param : params) {
            if (i == params.length - 1) {
                expectedOutput = param;
            } else {
                slots[i] = param;
                i++;
            }
        }

        String actual = performIntent(intent, slots);

        boolean condition = actual.matches(expectedOutput);

        assertTrue("[MATCHING]\nActual: " + actual + "\nExpected: " + expectedOutput, condition);

        return actual;
    }

    private void testIntent(String intent, String... params) throws IOException, NoSuchFieldException, IllegalAccessException {
        String[] slots = new String[params.length - 1];
        String expectedOutput = null;

        int i = 0;
        for (String param : params) {
            if (i == params.length - 1) {
                expectedOutput = param;
            } else {
                slots[i] = param;
                i++;
            }
        }

        //AmosAlexaSpeechlet amosAlexaSpeechlet = AmosAlexaSpeechlet.getInstance();
        //SpeechletResponse response = amosAlexaSpeechlet.onIntent(getEnvelope(intent, slots));
        assertEquals(expectedOutput, performIntent(intent, slots));
    }

    private String performIntent(String intent, String... params) throws IOException, NoSuchFieldException, IllegalAccessException {
        String[] slots = new String[params.length];

        int i = 0;
        for (String param : params) {
            slots[i] = param;
            i++;
        }

        AmosAlexaSpeechlet amosAlexaSpeechlet = AmosAlexaSpeechlet.getInstance();
        SpeechletResponse response = amosAlexaSpeechlet.onIntent(getEnvelope(intent, slots));
        return getOutputSpeechText(response.getOutputSpeech())
                .replaceAll("\\<.*?>", ""); // Remove all markup since it is not really relevant for our tests
    }

    private String getOutputSpeechText(OutputSpeech outputSpeech) {
        if (outputSpeech instanceof SsmlOutputSpeech) {
            SsmlOutputSpeech ssmlOutputSpeech = (SsmlOutputSpeech) outputSpeech;
            return ssmlOutputSpeech.getSsml();
        }
        if (outputSpeech instanceof PlainTextOutputSpeech) {
            PlainTextOutputSpeech plainTextOutputSpeech = (PlainTextOutputSpeech) outputSpeech;
            return plainTextOutputSpeech.getText();
        }

        return null;
    }

    private void newSession() {
        Session.Builder builder = Session.builder();
        builder.withSessionId(SESSION_ID);
        session = builder.build();
    }

    private SpeechletRequestEnvelope<IntentRequest> getEnvelope(String intent, String... slots) throws IOException, NoSuchFieldException, IllegalAccessException {
        SpeechletRequestEnvelope<IntentRequest> envelope = (SpeechletRequestEnvelope<IntentRequest>) SpeechletRequestEnvelope.fromJson(buildJson(intent, slots));

        // Set session via reflection

        Field f1 = envelope.getClass().getDeclaredField("session");
        f1.setAccessible(true);
        f1.set(envelope, session);

        return envelope;
    }

    private boolean isExpected(SpeechletResponse response, String expected) {
        return false;
    }

    private String buildJson(String intent, String... slots) {
        Calendar cal = Calendar.getInstance();
        Date time = cal.getTime();
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        StringBuilder slotsJson = new StringBuilder();

        boolean first = true;
        for (String slot : slots) {
            if (first) {
                first = false;
            } else {
                slotsJson.append(',');
            }

            String[] slotParts = slot.split(":");
            slotsJson.append("\"").append(slotParts[0]).append("\":");
            slotsJson.append("{");
            slotsJson.append("\"name\":\"").append(slotParts[0]).append("\",");
            slotsJson.append("\"value\":\"").append(slotParts[1]).append("\"");
            slotsJson.append("}");
        }

        String json = "{\n" +
                "  \"session\": {\n" +
                "    \"sessionId\": \"" + SESSION_ID + "\",\n" +
                "    \"application\": {\n" +
                "      \"applicationId\": \"amzn1.ask.skill.38e33c69-1510-43cd-be1d-929f08a966b4\"\n" +
                "    },\n" +
                "    \"attributes\": {},\n" +
                "    \"user\": {\n" +
                "      \"userId\": \"amzn1.ask.account.AHCD37TFVGP2S3OHTPFQTU2CVLBJMIVD3IIU6OZRGBTITENQO7W76SR5TRJMS5NDYJ4HQJTX726C4KMYHYZCOV5ONNFWFGH434UF4GUZQXKX2MEK2QE2B275MDM6YITSPWB3PAAFA2JKLQAJJXRJ65F2LXGDKP524L4YVA53IAA3CA6TVZCTBCLPVHBDIC3SLZJPT7PDZN4YUQA\"\n" +
                "    },\n" +
                "    \"new\": true\n" +
                "  },\n" +
                "  \"request\": {\n" +
                "    \"type\": \"IntentRequest\",\n" +
                "    \"requestId\": \"EdwRequestId.09495460-038e-4394-9a83-12115fba09b7\",\n" +
                "    \"locale\": \"de-DE\",\n" +
                "    \"timestamp\": \"" + formatter.format(time) + "\",\n" +
                "    \"intent\": {\n" +
                "      \"name\": \"" + intent + "\",\n" +
                "      \"slots\": {\n" +
                slotsJson.toString() +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"version\": \"1.0\"\n" +
                "}";

        return json;
    }

}