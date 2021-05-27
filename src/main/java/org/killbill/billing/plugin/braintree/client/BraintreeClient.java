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

package org.killbill.billing.plugin.braintree.client;

import com.braintreegateway.PaymentMethod;
import com.braintreegateway.Result;
import com.braintreegateway.Transaction;
import com.braintreegateway.exceptions.BraintreeException;
import org.killbill.billing.plugin.braintree.core.BraintreePluginProperties.PaymentMethodType;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;

public interface BraintreeClient {

    Result<Transaction> saleTransaction(String orderId, BigDecimal amount, @Nullable String braintreeCustomerId, String braintreePaymentMethodNonce, boolean submitForSettlement) throws BraintreeException;

    Result<Transaction> submitTransactionForSettlement(String braintreeTransactionId, BigDecimal amount) throws BraintreeException;

    Result<Transaction> voidTransaction(String braintreeTransactionId) throws BraintreeException;

    Result<Transaction> refundTransaction(String braintreeTransactionId, BigDecimal amount) throws BraintreeException;

    Result<Transaction> creditTransaction(BigDecimal amount, @Nullable String braintreeCustomerId, String braintreePaymentMethodNonce) throws BraintreeException;

    Result<? extends PaymentMethod> createPaymentMethod(String braintreeCustomerId, String braintreePaymentMethodToken, String braintreeNonce, PaymentMethodType paymentMethodType) throws BraintreeException;

    Result<? extends PaymentMethod> updatePaymentMethod(String currentBraintreePaymentMethodToken, String newBraintreePaymentMethodToken, String newCustomerId) throws BraintreeException;

    List<? extends PaymentMethod> getPaymentMethods(String braintreeCustomerId) throws BraintreeException;

    Result<? extends PaymentMethod> deletePaymentMethod(String braintreePaymentMethodToken) throws BraintreeException;

    @Nullable String createNonceFromPaymentMethodToken(String braintreePaymentMethodToken);

    Transaction.Status getTransactionStatus(String braintreeTransactionId);

    static Transaction getTransactionInstance(Result<Transaction> result){
        return result.getTransaction() == null? result.getTarget() : result.getTransaction();
    }

}
