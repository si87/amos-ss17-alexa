/**
 * Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at
 * <p>
 * http://aws.amazon.com/apache2.0/
 * <p>
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package amosalexa.services.pricequery;

import amosalexa.SpeechletSubject;
import amosalexa.services.AbstractSpeechService;
import amosalexa.services.SpeechService;
import amosalexa.services.pricequery.aws.model.Item;
import amosalexa.services.pricequery.aws.model.Offer;
import amosalexa.services.pricequery.aws.request.AWSLookup;
import amosalexa.services.pricequery.aws.util.AWSUtil;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.SpeechletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * This feature lets alexa request product information from amazon
 */
public class PriceQueryService extends AbstractSpeechService implements SpeechService {

    private static final Logger log = LoggerFactory.getLogger(PriceQueryService.class);
    /**
     * slot values
     */
    private static final String KEYWORD_SLOT = "ProductKeyword";
    /**
     * card titles
     */
    private static final String CARD_TITLE = "PriceQuery";
    /**
     * intents
     */
    private static final String PRICE_QUERY_INTENT = "ProductRequestIntent";

    public PriceQueryService(SpeechletSubject speechletSubject) {
        subscribe(speechletSubject);
    }

    @Override
    public String getDialogName() {
        return this.getClass().getName();
    }

    @Override
    public List<String> getStartIntents() {
        return Arrays.asList(
                PRICE_QUERY_INTENT
        );
    }

    @Override
    public List<String> getHandledIntents() {
        return Arrays.asList(
                PRICE_QUERY_INTENT
        );
    }

    @Override
    public void subscribe(SpeechletSubject speechletSubject) {
        speechletSubject.attachSpeechletObserver(this, PRICE_QUERY_INTENT);
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {

        Intent intent = requestEnvelope.getRequest().getIntent();
        String keyword = intent.getSlot(KEYWORD_SLOT) != null ? intent.getSlot(KEYWORD_SLOT).getValue().toLowerCase() : null;

        log.info("Amazon Price Query -  Keyword: " + keyword);

        if(keyword == null){
            return getErrorResponse("Ich konnte deine Suche nicht verstehen");
        }

        return getProductInformation(keyword);
    }

    private SpeechletResponse getProductInformation(String keyword) {
        if (keyword != null) {

            List<Item> items = AWSLookup.itemSearch(keyword, 1, null);

            if (items.isEmpty()) {
                log.error("no results by keyword: " + keyword);
                return getErrorResponse("Die Suche ergab keine Ergebnisse");
            }

            StringBuilder text = new StringBuilder();

            for (int i = 0; i < 3; i++) {

                Offer offer = AWSLookup.offerLookup(items.get(i).getASIN());
                String productTitle = AWSUtil.shortTitle(items.get(i).getTitle());

                text
                    .append(productTitle)
                    .append("kostet <say-as interpret-as=\"unit\">€")
                    .append(offer.getLowestNewPrice() / 100)
                    .append("</say-as> ")
                    .append("<break time=\"1s\"/>");
            }
            return getSSMLResponse(CARD_TITLE, text.toString());
        }
        return getErrorResponse();
    }
}
