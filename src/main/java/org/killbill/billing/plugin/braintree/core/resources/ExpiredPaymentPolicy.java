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

package org.killbill.billing.plugin.braintree.core.resources;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import org.joda.time.DateTime;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.braintree.api.BraintreePaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.braintree.core.BraintreeConfigProperties;
import org.killbill.billing.plugin.braintree.core.BraintreePluginProperties;
import org.killbill.billing.plugin.braintree.dao.BraintreeDao;
import org.killbill.clock.Clock;

import java.util.List;
import java.util.Map;

public class ExpiredPaymentPolicy {

    private final Clock clock;

    private final BraintreeConfigProperties braintreeProperties;

    public ExpiredPaymentPolicy(final Clock clock, final BraintreeConfigProperties braintreeProperties) {
        this.clock = clock;
        this.braintreeProperties = braintreeProperties;
    }

    public BraintreePaymentTransactionInfoPlugin isExpired(final List<PaymentTransactionInfoPlugin> paymentTransactions) {
        if (!containOnlyAuthsOrPurchases(paymentTransactions)) {
            return null;
        }

        final BraintreePaymentTransactionInfoPlugin transaction = (BraintreePaymentTransactionInfoPlugin) latestTransaction(paymentTransactions);
        if (transaction.getCreatedDate() == null) {
            return null;
        }

        if (transaction.getStatus() == PaymentPluginStatus.PENDING) {
            final DateTime expirationDate = expirationDateForInitialTransactionType(transaction);
            if (clock.getNow(expirationDate.getZone()).isAfter(expirationDate)) {
                return transaction;
            }
        }

        return null;
    }

    private PaymentTransactionInfoPlugin latestTransaction(final List<PaymentTransactionInfoPlugin> paymentTransactions) {
        return Iterables.getLast(paymentTransactions);
    }

    private boolean containOnlyAuthsOrPurchases(final List<PaymentTransactionInfoPlugin> transactions) {
        for (final PaymentTransactionInfoPlugin transaction : transactions) {
            if (transaction.getTransactionType() != TransactionType.AUTHORIZE &&
                transaction.getTransactionType() != TransactionType.PURCHASE) {
                return false;
            }
        }
        return true;
    }

    private DateTime expirationDateForInitialTransactionType(final BraintreePaymentTransactionInfoPlugin transaction) {
        if (transaction.getBraintreeResponsesRecord() == null) {
            return transaction.getCreatedDate().plus(braintreeProperties.getPendingPaymentExpirationPeriod(null));
        }

        final Map braintreeResponseAdditionalData = BraintreeDao.mapFromAdditionalDataString(transaction.getBraintreeResponsesRecord().getAdditionalData());

        if (isHppBuildFormTransaction(braintreeResponseAdditionalData)) {
            return transaction.getCreatedDate().plus(braintreeProperties.getPendingHppPaymentWithoutCompletionExpirationPeriod());
        }

        final String paymentMethod = getPaymentMethod(braintreeResponseAdditionalData);
        return transaction.getCreatedDate().plus(braintreeProperties.getPendingPaymentExpirationPeriod(paymentMethod));
    }

    private boolean isHppBuildFormTransaction(final Map braintreeResponseAdditionalData) {
        return isHppPayment(braintreeResponseAdditionalData) && !isHppCompletionTransaction(braintreeResponseAdditionalData);
    }

    private boolean isHppCompletionTransaction(final Map braintreeResponseAdditionalData) {
        return Boolean.valueOf(MoreObjects.firstNonNull(braintreeResponseAdditionalData.get(BraintreePluginProperties.PROPERTY_HPP_COMPLETION), false).toString());
    }

    private boolean isHppPayment(final Map braintreeResponseAdditionalData) {
        return Boolean.valueOf(MoreObjects.firstNonNull(braintreeResponseAdditionalData.get(BraintreePluginProperties.PROPERTY_FROM_HPP), false).toString());
    }

    private String getPaymentMethod(final Map braintreeResponseAdditionalData) {
        return (String) braintreeResponseAdditionalData.get(BraintreePluginProperties.PROPERTY_BT_PAYMENT_INSTRUMENT_TYPE);
    }
}