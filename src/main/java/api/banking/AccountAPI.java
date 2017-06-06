package api.banking;

import model.banking.Account;
import model.banking.Card;
import model.banking.StandingOrder;
import model.banking.Transaction;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.client.Traverson;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Date;

import static org.springframework.hateoas.client.Hop.rel;

public class AccountAPI {

	/**
	 * Logger
	 */
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(AccountAPI.class);

	private static BankingRESTClient bankingRESTClient = BankingRESTClient.getInstance();

	public static Account createAccount(String accountNumber, Number balance, String openingDate) {
		Account newAccount = new Account();
		newAccount.setNumber(accountNumber);
		newAccount.setBalance(balance);
		newAccount.setOpeningDate(openingDate);

		return createAccount(newAccount);
	}

	public static Account createAccount(Account newAccount) {
		return (Account) bankingRESTClient.postBankingModelObject("/accounts/generate", newAccount, Account.class);
	}

	public static Account getAccount(String accountNumber){
		return (Account) bankingRESTClient.getBankingModelObject("/accounts/" + accountNumber, Account.class);
	}

	public static Card createCardForAccount(String accountNumber, Card.CardType cardType, String cardNumber, Card.Status status, String expirationDate) {
		Card newCard = new Card();
		newCard.setCardType(cardType);
		newCard.setCardNumber(cardNumber);
		newCard.setStatus(status);
		newCard.setExpirationDate(expirationDate);

		return createCardForAccount(accountNumber, newCard);
	}

	public static Card createCardForAccount(String accountNumber, Card newCard) {
		return (Card) bankingRESTClient.postBankingModelObject("/accounts/" + accountNumber + "/cards", newCard, Card.class);
	}

	public static Card getCardForAccount(String accountNumber, Number cardId){
		return (Card) bankingRESTClient.getBankingModelObject("/accounts/" + accountNumber + "/cards/" + cardId, Card.class);
	}

	/**
	 * Get all cards (credit / debit) for the given account
	 * @param accountNumber Account number
	 * @return Collection of Cards
	 * @throws HttpClientErrorException
	 */
	public static Collection<Card> getCardsForAccount(String accountNumber) throws HttpClientErrorException {
		// TODO: Create a generic method for getting embedded JSON-HAL collections (in BankingRESTClient)
		Traverson traverson = null;
		try {
			traverson = new Traverson(new URI(BankingRESTClient.BANKING_API_ENDPOINT + BankingRESTClient.BANKING_API_BASEURL_V1 + "/accounts/" + accountNumber + "/cards"),
						MediaTypes.HAL_JSON);
		} catch (URISyntaxException e) {
			log.error("getCardsForAccount failed", e);
			return null;
		}

		ParameterizedTypeReference<Resources<Card>> typeRefDevices = new ParameterizedTypeReference<Resources<Card>>() {};
		Resources<Card> resResponses = traverson.follow(rel("$._links.self.href")).toObject(typeRefDevices);
		return resResponses.getContent();
	}

	/**
	 * Update a card
	 * @param accountNumber Account number
	 * @param card Card
	 * @return True on success, False otherwise
	 */
	public static boolean updateCard(String accountNumber, Card card) {
		try {
			bankingRESTClient.putBankingModelObject("/accounts/" + accountNumber + "/cards/" + card.getCardNumber(), card);
			return true;
		} catch (RestClientException e) {
			log.error("updateCard failed", e);
			return false;
		}
	}

	/**
	 * Delete a card
	 * @param accountNumber Account number
	 * @param cardId Card id
	 * @return True on success, False otherwise
	 */
	public static boolean deleteCard(String accountNumber, Number cardId) {
		try {
			bankingRESTClient.deleteBankingModelObject("/accounts/" + accountNumber + "/cards/" + cardId);
			return true;
		} catch (RestClientException e) {
			log.error("deleteCard failed", e);
			return false;
		}
	}

	/**
	 * Get all transactions for the given account
	 * @param accountNumber Account number
	 * @return Collection of Cards
	 * @throws HttpClientErrorException
	 */
	public static Collection<Transaction> getTransactionsForAccount(String accountNumber) throws HttpClientErrorException {
		// TODO: Create a generic method for getting embedded JSON-HAL collections (in BankingRESTClient)
		Traverson traverson = null;
		try {
			traverson = new Traverson(new URI(BankingRESTClient.BANKING_API_ENDPOINT + BankingRESTClient.BANKING_API_BASEURL_V1 + "/accounts/" + accountNumber + "/transactions"),
					MediaTypes.HAL_JSON);
		} catch (URISyntaxException e) {
			log.error("getTransactionsForAccount failed", e);
			return null;
		}

		ParameterizedTypeReference<Resources<Transaction>> typeRefDevices = new ParameterizedTypeReference<Resources<Transaction>>() {};
		Resources<Transaction> resResponses = traverson.follow(rel("$._links.self.href")).toObject(typeRefDevices);
		return resResponses.getContent();
	}

	/**
	 * Get all standing orders for the given account
	 * @param accountNumber Account number
	 * @return Collection of StandingOrders
	 * @throws HttpClientErrorException
	 */
	public static Collection<StandingOrder> getStandingOrdersForAccount(String accountNumber) throws HttpClientErrorException {
		// TODO: Create a generic method for getting embedded JSON-HAL collections (in BankingRESTClient)
		Traverson traverson = null;
		try {
			traverson = new Traverson(new URI(BankingRESTClient.BANKING_API_ENDPOINT + BankingRESTClient.BANKING_API_BASEURL_V1 + "/accounts/" + accountNumber + "/standingorders"),
					MediaTypes.HAL_JSON);
		} catch (URISyntaxException e) {
			log.error("getStandingOrdersForAccount failed", e);
			return null;
		}

		ParameterizedTypeReference<Resources<StandingOrder>> typeRefDevices = new ParameterizedTypeReference<Resources<StandingOrder>>() {};
		Resources<StandingOrder> resResponses = traverson.follow(rel("$._links.self.href")).toObject(typeRefDevices);
		return resResponses.getContent();
	}

	public static StandingOrder createStandingOrderForAccount(String accountNumber, String payee, Number amount, String destinationAccount,
															  String firstExecution, StandingOrder.ExecutionRate executionRate, String description) {
		StandingOrder newStandingOrder = new StandingOrder();
		newStandingOrder.setPayee(payee);
		newStandingOrder.setAmount(amount);
		newStandingOrder.setDestinationAccount(destinationAccount);
		newStandingOrder.setFirstExecution(firstExecution);
		newStandingOrder.setExecutionRate(executionRate);
		newStandingOrder.setDescription(description);

		return createStandingOrderForAccount(accountNumber, newStandingOrder);
	}

	public static StandingOrder createStandingOrderForAccount(String accountNumber, StandingOrder newStandingOrder) {
		return (StandingOrder) bankingRESTClient.postBankingModelObject("/accounts/" + accountNumber + "/standingorders", newStandingOrder, StandingOrder.class);
	}

	/**
	 * Get a standing order
	 * @param accountNumber Account number
	 * @param standingOrderId Standing order id
	 * @return StandingOrder
	 */
	public static StandingOrder getStandingOrder(String accountNumber, Number standingOrderId) {
		return (StandingOrder) bankingRESTClient.getBankingModelObject("/accounts/" + accountNumber + "/standingorders/" + standingOrderId, StandingOrder.class);
	}

	/**
	 * Delete a standing order
	 * @param accountNumber Account number
	 * @param standingOrderId Standing order id
	 * @return True on success, False otherwise
	 */
	public static boolean deleteStandingOrder(String accountNumber, Number standingOrderId) {
		try {
			bankingRESTClient.deleteBankingModelObject("/accounts/" + accountNumber + "/standingorders/" + standingOrderId);
			return true;
		} catch (RestClientException e) {
			log.error("deleteStandingOrder failed", e);
			return false;
		}
	}

	/**
	 * Update a standing order
	 * @param accountNumber Account number
	 * @param standingOrder Standing order
	 * @return True on success, False otherwise
	 */
	public static boolean updateStandingOrder(String accountNumber, StandingOrder standingOrder) {
		try {
			bankingRESTClient.putBankingModelObject("/accounts/" + accountNumber + "/standingorders/" + standingOrder.getStandingOrderId(), standingOrder);
			return true;
		} catch (RestClientException e) {
			log.error("updateStandingOrder failed", e);
			return false;
		}
	}

}