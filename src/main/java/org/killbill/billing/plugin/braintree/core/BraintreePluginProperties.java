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

import com.braintreegateway.Result;
import com.braintreegateway.Transaction;
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

    public static final String PROPERTY_FROM_HPP = "fromHPP";
    public static final String PROPERTY_HPP_COMPLETION = "fromHPPCompletion";
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

}
