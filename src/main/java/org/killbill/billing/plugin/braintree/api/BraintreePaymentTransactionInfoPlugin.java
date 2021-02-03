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

package org.killbill.billing.plugin.braintree.api;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.braintreegateway.Transaction;
import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.api.payment.PluginPaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.braintree.core.BraintreePluginProperties;
import org.killbill.billing.plugin.braintree.dao.BraintreeDao;
import org.killbill.billing.plugin.braintree.dao.gen.tables.records.BraintreeResponsesRecord;

import javax.annotation.Nullable;

public class BraintreePaymentTransactionInfoPlugin extends PluginPaymentTransactionInfoPlugin {

    // Kill Bill limits the field size to 32
    private static final int ERROR_CODE_MAX_LENGTH = 32;

    private final BraintreeResponsesRecord braintreeResponsesRecord;

    public static BraintreePaymentTransactionInfoPlugin build(final BraintreeResponsesRecord braintreeResponsesRecord) {
        final Map additionalData = BraintreeDao.mapFromAdditionalDataString(braintreeResponsesRecord.getAdditionalData());

        final DateTime responseDate = new DateTime(braintreeResponsesRecord.getCreatedDate()
                .atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(), DateTimeZone.UTC);
        return new BraintreePaymentTransactionInfoPlugin(braintreeResponsesRecord,
                UUID.fromString(braintreeResponsesRecord.getKbPaymentId()),
                UUID.fromString(braintreeResponsesRecord.getKbPaymentTransactionId()),
                TransactionType.valueOf(braintreeResponsesRecord.getTransactionType()),
                braintreeResponsesRecord.getAmount(),
                Strings.isNullOrEmpty(braintreeResponsesRecord.getCurrency()) ? null : Currency.valueOf(braintreeResponsesRecord.getCurrency()),
                getPaymentPluginStatus(additionalData),
                getGatewayError(additionalData),
                truncate(getGatewayErrorCode(additionalData)),
                getFirstPaymentReferenceID(additionalData),
                getSecondPaymentReferenceID(additionalData),
                responseDate,
                responseDate,
                PluginProperties.buildPluginProperties(additionalData));
    }

    public BraintreePaymentTransactionInfoPlugin(final BraintreeResponsesRecord braintreeResponsesRecord,
                                                 final UUID kbPaymentId, final UUID kbTransactionPaymentPaymentId,
                                                 final TransactionType transactionType, final BigDecimal amount, final Currency currency,
                                                 final PaymentPluginStatus pluginStatus, final String gatewayError, final String gatewayErrorCode,
                                                 final String firstPaymentReferenceId, final String secondPaymentReferenceId, final DateTime createdDate,
                                                 final DateTime effectiveDate, final List<PluginProperty> properties) {
        super(kbPaymentId, kbTransactionPaymentPaymentId, transactionType, amount, currency, pluginStatus, gatewayError,
                gatewayErrorCode, firstPaymentReferenceId, secondPaymentReferenceId, createdDate, effectiveDate, properties);
        this.braintreeResponsesRecord = braintreeResponsesRecord;
    }

    public BraintreeResponsesRecord getBraintreeResponsesRecord() {
        return braintreeResponsesRecord;
    }

    public static PaymentPluginStatus getPaymentPluginStatus(final String braintreeStatus){
        if(Transaction.Status.SETTLED.toString().equals(braintreeStatus)
                || Transaction.Status.AUTHORIZING.toString().equals(braintreeStatus)
                || Transaction.Status.AUTHORIZED.toString().equals(braintreeStatus)
                || Transaction.Status.SETTLING.toString().equals(braintreeStatus)
                || Transaction.Status.SETTLEMENT_CONFIRMED.toString().equals(braintreeStatus)
                || Transaction.Status.SUBMITTED_FOR_SETTLEMENT.toString().equals(braintreeStatus)
                || Transaction.Status.VOIDED.toString().equals(braintreeStatus)){
            return PaymentPluginStatus.PROCESSED;
        }
        else if(Transaction.Status.SETTLEMENT_PENDING.toString().equals(braintreeStatus)){
            return PaymentPluginStatus.PENDING;
        }
        else if(Transaction.Status.FAILED.toString().equals(braintreeStatus)
                || Transaction.Status.SETTLEMENT_DECLINED.toString().equals(braintreeStatus)
                || Transaction.Status.AUTHORIZATION_EXPIRED.toString().equals(braintreeStatus)
                || Transaction.Status.PROCESSOR_DECLINED.toString().equals(braintreeStatus)
                || Transaction.Status.GATEWAY_REJECTED.toString().equals(braintreeStatus)){
            return PaymentPluginStatus.ERROR;
        }

        return PaymentPluginStatus.UNDEFINED;
    }

    public static boolean isDoneProcessingInBraintree(final String braintreeTransactionStatus){
        return braintreeTransactionStatus.equals(Transaction.Status.SETTLED.toString())
                || braintreeTransactionStatus.equals(Transaction.Status.VOIDED.toString());
    }

    private static PaymentPluginStatus getPaymentPluginStatus(final Map additionalData) {
        final String braintreeStatus = (String) additionalData.get(BraintreePluginProperties.PROPERTY_BT_TRANSACTION_STATUS);
        return getPaymentPluginStatus(braintreeStatus);
    }

    private static String getGatewayError(final Map additionalData) {
        return (String) additionalData.get(BraintreePluginProperties.PROPERTY_BT_GATEWAY_ERROR_MESSAGE);
    }

    private static String getGatewayErrorCode(final Map additionalData) {
        return (String) additionalData.get(BraintreePluginProperties.PROPERTY_BT_GATEWAY_ERROR_CODE);
    }

    private static String getFirstPaymentReferenceID(final Map additionalData){
        return (String) additionalData.get(BraintreePluginProperties.PROPERTY_BT_FIRST_PAYMENT_REFERENCE_ID);
    }

    private static String getSecondPaymentReferenceID(final Map additionalData){
        return (String) additionalData.get(BraintreePluginProperties.PROPERTY_BT_SECOND_PAYMENT_REFERENCE_ID);
    }

    private static String truncate(@Nullable final String string) {
        if (string == null) {
            return null;
        } else if (string.length() <= ERROR_CODE_MAX_LENGTH) {
            return string;
        } else {
            return string.substring(0, ERROR_CODE_MAX_LENGTH);
        }
    }




}
