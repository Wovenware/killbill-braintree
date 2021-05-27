/*
 * Copyright 2021 Wovenware, Inc
 *
 * Wovenware licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.braintree.core;

import com.braintreegateway.CreditCard;
import com.braintreegateway.PayPalAccount;
import com.braintreegateway.PaymentMethod;
import com.braintreegateway.Result;
import com.braintreegateway.Transaction;
import com.braintreegateway.UsBankAccount;

import org.killbill.billing.plugin.braintree.client.BraintreeClient;

import java.util.HashMap;
import java.util.Map;

public abstract class BraintreePluginProperties {

    public enum PaymentMethodType{
        CARD,
        ACH,
        PAYPAL
    }

    public static final String PROPERTY_FALLBACK_VALUE = "NULL";

    public static final String PROPERTY_AMOUNT = "amount";
    public static final String PROPERTY_CURRENCY = "currency";
    public static final String PROPERTY_PAYMENT_METHOD_TYPE = "payment_method_type";

    public static final String MAGIC_FIELD_BT_CUSTOMER_ID = "BRAINTREE_CUSTOMER_ID";

    public static final String PROPERTY_BT_CUSTOMER_ID = "bt_customer_id";
    public static final String PROPERTY_BT_NONCE = "bt_nonce";
    public static final String PROPERTY_BT_PAYMENT_INSTRUMENT_TYPE = "bt_payment_instrument_type";
    public static final String PROPERTY_BT_TRANSACTION_STATUS = "bt_transaction_status";
    public static final String PROPERTY_BT_TRANSACTION_SUCCESS = "bt_transaction_success";
    public static final String PROPERTY_BT_GATEWAY_ERROR_MESSAGE = "bt_gateway_error_message";
    public static final String PROPERTY_BT_GATEWAY_ERROR_CODE = "bt_gateway_error_code";
    public static final String PROPERTY_BT_FIRST_PAYMENT_REFERENCE_ID = "bt_first_payment_reference_id";
    public static final String PROPERTY_BT_SECOND_PAYMENT_REFERENCE_ID = "bt_second_payment_reference_id";

    public static final String PROPERTY_KB_TRANSACTION_ID = "kb_transaction_id";
    public static final String PROPERTY_KB_PAYMENT_ID = "kb_payment_id";
    public static final String PROPERTY_KB_TRANSACTION_TYPE = "kb_transaction_type";

    public static final String PROPERTY_OVERRIDDEN_TRANSACTION_STATUS = "overriddenTransactionStatus";

    public static Map<String, Object> toAdditionalDataMap(final Result<Transaction> braintreeResult) {

        final Map<String, Object> additionalDataMap = new HashMap<>();
        Transaction transaction = BraintreeClient.getTransactionInstance(braintreeResult);
        Transaction.Status status = transaction.getStatus();

        additionalDataMap.put(PROPERTY_BT_TRANSACTION_STATUS, status);
        additionalDataMap.put(PROPERTY_BT_TRANSACTION_SUCCESS, braintreeResult.isSuccess());
        additionalDataMap.put(PROPERTY_BT_PAYMENT_INSTRUMENT_TYPE, transaction.getPaymentInstrumentType());

        additionalDataMap.put(PROPERTY_BT_FIRST_PAYMENT_REFERENCE_ID, transaction.getId());
        additionalDataMap.put(PROPERTY_BT_SECOND_PAYMENT_REFERENCE_ID, transaction.getRetrievalReferenceNumber());

        if(!braintreeResult.isSuccess()) {
            String gatewayErrorMessage;
            String gatewayErrorCode = null;
            if(status.equals(Transaction.Status.PROCESSOR_DECLINED)){
                gatewayErrorMessage = transaction.getProcessorResponseText();
                gatewayErrorCode = transaction.getProcessorResponseCode();
            }
            else if(status.equals(Transaction.Status.SETTLEMENT_DECLINED)){
                gatewayErrorMessage = transaction.getProcessorSettlementResponseText();
                gatewayErrorCode = transaction.getProcessorSettlementResponseCode();
            }
            else if(status.equals(Transaction.Status.GATEWAY_REJECTED)){
                gatewayErrorMessage = transaction.getNetworkResponseText() == null?
                        transaction.getGatewayRejectionReason().toString() : transaction.getNetworkResponseText();
                gatewayErrorCode = transaction.getNetworkResponseCode();
            }
            else{
                gatewayErrorMessage = braintreeResult.getMessage();
            }

            additionalDataMap.put(PROPERTY_BT_GATEWAY_ERROR_MESSAGE, gatewayErrorMessage);
            if(gatewayErrorCode != null) additionalDataMap.put(PROPERTY_BT_GATEWAY_ERROR_CODE, gatewayErrorCode);
        }

        return additionalDataMap;
    }

    public static Map<String, Object> toAdditionalDataMap(final PaymentMethod paymentMethod) {
        final Map<String, Object> additionalDataMap = new HashMap<String, Object>();

        additionalDataMap.put("token", paymentMethod.getToken());
        additionalDataMap.put("is_default", paymentMethod.isDefault());
        additionalDataMap.put("image_url", paymentMethod.getImageUrl());
        additionalDataMap.put(PROPERTY_BT_CUSTOMER_ID, paymentMethod.getCustomerId());

        if (paymentMethod instanceof CreditCard) {
            final CreditCard cc = (CreditCard) paymentMethod;
            if (cc.getBillingAddress() != null) {
                additionalDataMap.put("billing_address_country_name", cc.getBillingAddress().getCountryName());
            }
            additionalDataMap.put("bin", cc.getBin());
            additionalDataMap.put("cardholder_name", cc.getCardholderName());
            additionalDataMap.put("cart_type", cc.getCardType());
            if (cc.getCreatedAt() != null) {
                additionalDataMap.put("created_at", cc.getCreatedAt().toInstant().toString());
            }
            additionalDataMap.put("customer_id", cc.getCustomerId());
            additionalDataMap.put("customer_location", cc.getCustomerLocation());
            additionalDataMap.put("expiration_month", cc.getExpirationMonth());
            additionalDataMap.put("expiration_year", cc.getExpirationYear());
            additionalDataMap.put("is_default", cc.isDefault());
            additionalDataMap.put("is_venmo_sdk", cc.isVenmoSdk());
            additionalDataMap.put("is_expired", cc.isExpired());
            additionalDataMap.put("is_network_tokenized", cc.isNetworkTokenized());
            additionalDataMap.put("image_url", cc.getImageUrl());
            additionalDataMap.put("last4", cc.getLast4());
            if (cc.getCommercial() != null) {
                additionalDataMap.put("commercial", cc.getCommercial().toString());
            }
            if (cc.getDebit() != null) {
                additionalDataMap.put("debit", cc.getDebit().toString());
            }
            if (cc.getDurbinRegulated() != null) {
                additionalDataMap.put("durbin_regulated", cc.getDurbinRegulated().toString());
            }
            if (cc.getHealthcare() != null) {
                additionalDataMap.put("healthcare", cc.getHealthcare().toString());
            }
            if (cc.getPayroll() != null) {
                additionalDataMap.put("payroll", cc.getPayroll().toString());
            }
            if (cc.getPrepaid() != null) {
                additionalDataMap.put("prepaid", cc.getPrepaid().toString());
            }
            additionalDataMap.put("product_id", cc.getProductId());
            additionalDataMap.put("country_of_issuance", cc.getCountryOfIssuance());
            additionalDataMap.put("issuing_bank", cc.getIssuingBank());
            additionalDataMap.put("unique_number_identifier", cc.getUniqueNumberIdentifier());
            additionalDataMap.put("token", cc.getToken());
            if (cc.getUpdatedAt() != null) {
                additionalDataMap.put("updated_at", cc.getUpdatedAt().toInstant().toString());
            }
            if (cc.getVerification() != null) {
                additionalDataMap.put("verification_status", cc.getVerification().getStatus().toString());
            }
            additionalDataMap.put("account_type", cc.getAccountType());
        } else if (paymentMethod instanceof PayPalAccount) {
            final PayPalAccount paypal = (PayPalAccount) paymentMethod;
            // GDPR / PII
            // additionalDataMap.put("email", paypal.getEmail());
            additionalDataMap.put("token", paypal.getToken());
            additionalDataMap.put("billing_agreement_id", paypal.getBillingAgreementId());
            additionalDataMap.put("is_default", paypal.isDefault());
            additionalDataMap.put("image_url", paypal.getImageUrl());
            additionalDataMap.put("payer_id", paypal.getPayerId());
            additionalDataMap.put("customer_id", paypal.getCustomerId());
            if (paypal.getCreatedAt() != null) {
                additionalDataMap.put("created_at", paypal.getCreatedAt().toInstant().toString());
            }
            if (paypal.getUpdatedAt() != null) {
                additionalDataMap.put("updated_at", paypal.getUpdatedAt().toInstant().toString());
            }
            if (paypal.getRevokedAt() != null) {
                additionalDataMap.put("revoked_at", paypal.getRevokedAt().toInstant().toString());
            }
        } else if (paymentMethod instanceof UsBankAccount) {
            final UsBankAccount acct = (UsBankAccount) paymentMethod;
            additionalDataMap.put("routing_number", acct.getRoutingNumber());
            additionalDataMap.put("last4", acct.getLast4());
            additionalDataMap.put("account_type", acct.getAccountType());
            additionalDataMap.put("account_holder_name", acct.getAccountHolderName());
            additionalDataMap.put("token", acct.getToken());
            additionalDataMap.put("image_url", acct.getImageUrl());
            additionalDataMap.put("bank_name", acct.getBankName());
            additionalDataMap.put("customer_id", acct.getCustomerId());
            additionalDataMap.put("is_default", acct.isDefault());
            if (acct.getAchMandate() != null) {
                additionalDataMap.put("ach_mandate_accepted_at", acct.getAchMandate().getAcceptedAt().toString());
            }
            additionalDataMap.put("is_verified", acct.isVerified());
        }

        return additionalDataMap;
    }
}
