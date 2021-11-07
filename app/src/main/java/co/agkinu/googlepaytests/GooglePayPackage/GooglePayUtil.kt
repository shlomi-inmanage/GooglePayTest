package co.agkinu.googlepaytests.GooglePayPackage

import android.app.Activity
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object GooglePayUtil {

    private val baseRequest = JSONObject().apply {
        put("apiVersion", 2)
        put("apiVersionMinor", 0)
    }

    /*
    Define the card networks that your application accepts.
     */
    private val allowedCardNetworks = JSONArray(GooglePayConstants.SUPPORTED_NETWORKS)

    /*
    The Google Pay API might return cards on file on Google.com (PAN_ONLY) or a device token on an Android device authenticated with a 3-D Secure cryptogram (CRYPTOGRAM_3DS).
     */
    private val allowedCardAuthMethods = JSONArray(GooglePayConstants.SUPPORTED_METHODS)

    /*
    Google encrypts information about a payer's selected card for secure processing by a payment provider.
    Replace example and exampleGatewayMerchantId with the appropriate values for your payment provider.
    Use the following table to find the specific gateway and gatewayMerchantId values for your payment provider:
    https://developers.google.com/pay/api/android/guides/tutorial#tokenization
     */
    private fun gatewayTokenizationSpecification(): JSONObject {
        return JSONObject().apply {
            put("type", "PAYMENT_GATEWAY")
            put("parameters", JSONObject(mapOf(
                "gateway" to "example",
                "gatewayMerchantId" to "exampleGatewayMerchantId")))
        }
    }

    private fun baseCardPaymentMethod(): JSONObject {
        return JSONObject().apply {

            val parameters = JSONObject().apply {
                put("allowedAuthMethods", allowedCardAuthMethods)
                put("allowedCardNetworks", allowedCardNetworks)
                put("billingAddressRequired", true)
                put("billingAddressParameters", JSONObject().apply {
                    put("format", "FULL")
                })
            }

            put("type", "CARD")
            put("parameters", parameters)
        }
    }

    /*
    Extend the base card payment method object to describe information expected to be returned to your application, which must include tokenized payment data.
     */
    private fun cardPaymentMethod(): JSONObject {
        val cardPaymentMethod = baseCardPaymentMethod()
        cardPaymentMethod.put("tokenizationSpecification", gatewayTokenizationSpecification())

        return cardPaymentMethod
    }

    /*
    Create a PaymentsClient instance in the onCreate method in your Activity.
    The PaymentsClient is used for interaction with the Google Pay API.
     */
    fun createPaymentsClient(activity: Activity): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(GooglePayConstants.PAYMENTS_ENVIRONMENT)
            .build()

        return Wallet.getPaymentsClient(activity, walletOptions)
    }

    /*
    Add your allowed payment methods to your base request object with the following code snippet:
     */
    fun isReadyToPayRequest(): JSONObject? {
        return try {
            baseRequest.apply {
                put("allowedPaymentMethods", JSONArray().put(baseCardPaymentMethod()))
            }
        } catch (e: JSONException) { null }
    }

    /*
    A PaymentDataRequest JSON object describes the information that you request from a payer in a Google Pay payment sheet.
    Provide information about the transaction price and the status of the provided price. For more information, see TransactionInfo JSON object documentation.
    The following example shows how to get price, price status, and currency transaction information.
     */
    private fun getTransactionInfo(price: String): JSONObject {
        return JSONObject().apply {
            put("totalPrice", price)
            put("totalPriceStatus", "FINAL")
            put("countryCode", GooglePayConstants.COUNTRY_CODE)
            put("currencyCode", GooglePayConstants.CURRENCY_CODE)
        }
    }

    private val merchantInfo: JSONObject =
        JSONObject().put("merchantName", GooglePayConstants.MERCHANT_NAME)

    /*
    Assign your base request object to a new PaymentDataRequest JSON object.
    Then, add the payment methods supported by your application,
    such as any configuration of additional data expected in the response.
    Finally, add information about the transaction and the merchant who makes the request.
    The following example shows how to request payment data:
     */
    fun getPaymentDataRequest(price: String): JSONObject? {
        try {
            return baseRequest.apply {
                put("allowedPaymentMethods", JSONArray().put(cardPaymentMethod()))
                put("transactionInfo", getTransactionInfo(price))
                put("merchantInfo", merchantInfo)

                // An optional shipping address requirement is a top-level property of the
                // PaymentDataRequest JSON object.
                val shippingAddressParameters = JSONObject().apply {
                    put("phoneNumberRequired", false)
                    put("allowedCountryCodes", JSONArray(GooglePayConstants.SHIPPING_SUPPORTED_COUNTRIES))
                }
                put("shippingAddressParameters", shippingAddressParameters)
                put("shippingAddressRequired", true)
            }
        } catch (e: JSONException) {
            return null
        }
    }
}