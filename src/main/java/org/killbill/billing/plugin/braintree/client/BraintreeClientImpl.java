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

import com.braintreegateway.*;
import com.braintreegateway.exceptions.BraintreeException;
import com.braintreegateway.exceptions.NotFoundException;
import org.killbill.billing.plugin.braintree.core.BraintreePluginProperties.PaymentMethodType;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;

public class BraintreeClientImpl implements BraintreeClient {

    private final BraintreeGateway gateway;

    public BraintreeClientImpl(BraintreeGateway braintreeGateway) {
        gateway = braintreeGateway;
    }

    @Override
    public Result<Transaction> saleTransaction(BigDecimal amount, @Nullable String braintreeCustomerId, String braintreePaymentMethodNonce, boolean submitForSettlement) throws BraintreeException {
        Result<Transaction> result;
        try {
            TransactionRequest request = new TransactionRequest()
                    .amount(amount)
                    .paymentMethodNonce(braintreePaymentMethodNonce)
                    .options()
                        .submitForSettlement(submitForSettlement)
                        .done();

            if(braintreeCustomerId != null) request = request.customerId(braintreeCustomerId);

            result = gateway.transaction().sale(request);
        }
        catch(Throwable t){
            throw new BraintreeException("Could not complete sale transaction", t);
        }
        return result;
    }

    @Override
    public Result<Transaction> submitTransactionForSettlement(String braintreeTransactionId, BigDecimal amount) throws BraintreeException {
        Result<Transaction> result;
        try{
            result = gateway.transaction().submitForSettlement(braintreeTransactionId, amount);
        }
        catch(Throwable t){
            throw new BraintreeException("Could not capture transaction " + braintreeTransactionId, t);
        }
        return result;
    }

    @Override
    public Result<Transaction> voidTransaction(String braintreeTransactionId) throws BraintreeException {
        Result<Transaction> result;
        try{
            result = gateway.transaction().voidTransaction(braintreeTransactionId);
        }
        catch(Throwable t){
            throw new BraintreeException("Could not void transaction " + braintreeTransactionId, t);
        }
        return result;
    }

    @Override
    public Result<Transaction> refundTransaction(String braintreeTransactionId, BigDecimal amount) throws BraintreeException {
        Result<Transaction> result;
        try{
            Transaction currentTransaction = gateway.transaction().find(braintreeTransactionId);
            if(currentTransaction.getStatus().equals(Transaction.Status.SETTLED) ||
                    currentTransaction.getStatus().equals(Transaction.Status.SETTLING)){
                //Refund transaction that is already settled or currently settling
                result = gateway.transaction().refund(braintreeTransactionId, amount);
            }
            else if(currentTransaction.getAmount().compareTo(amount) == 0){
                //Transaction still not settled. Since the refund is for the full amount we just void it
                result = gateway.transaction().voidTransaction(braintreeTransactionId);
            }
            else{
                throw new BraintreeException("Cannot refund transaction that has not yet begun settlement, and partial voids are not supported.");
            }
        }
        catch(Throwable t){
            throw new BraintreeException("Could not refund transaction " + braintreeTransactionId, t);
        }
        return result;
    }

    @Override
    public Result<Transaction> creditTransaction(BigDecimal amount, @Nullable String braintreeCustomerId, String braintreePaymentMethodNonce) throws BraintreeException {
        Result<Transaction> result;
        try {
            TransactionRequest request = new TransactionRequest()
                    .amount(amount)
                    .paymentMethodNonce(braintreePaymentMethodNonce);

            if(braintreeCustomerId != null) request = request.customerId(braintreeCustomerId);

            result = gateway.transaction().credit(request);
        }
        catch(Throwable t){
            throw new BraintreeException("Could not credit transaction in Braintree", t);
        }

        return result;
    }


    @Override
    public Result<? extends PaymentMethod> createPaymentMethod(String braintreeCustomerId, String braintreePaymentMethodToken, String braintreeNonce, PaymentMethodType paymentMethodType) throws BraintreeException {
        Result<? extends PaymentMethod> result;
        try{
            PaymentMethodRequest request = new PaymentMethodRequest()
                    .token(braintreePaymentMethodToken)
                    .customerId(braintreeCustomerId)
                    .paymentMethodNonce(braintreeNonce);

            switch(paymentMethodType){
                case CARD:
                    request = request.options()
                            .verifyCard(true)
                            .done();
                    result = gateway.paymentMethod().create(request);
                    break;
                case ACH:
                    request = request.options()
                            .usBankAccountVerificationMethod(UsBankAccountVerification.VerificationMethod.NETWORK_CHECK)
                            .done();
                    result = gateway.paymentMethod().create(request);
                    if (result.isSuccess()) {
                        UsBankAccount usBankAccount = (UsBankAccount) result.getTarget();
                        boolean verified = usBankAccount.isVerified();
                        String responseCode = usBankAccount.getVerifications().get(0).getProcessorResponseCode();
                        if(!verified) throw new BraintreeException("Could not verify US bank account for creating ACH payment method. Processor response code: " +responseCode);
                    }
                    break;
                case PAYPAL:
                    result = gateway.paymentMethod().create(request);
                    break;
                default:
                    throw new BraintreeException("Undefined payment method type");
            }

        }
        catch(Throwable t){
            throw new BraintreeException("Error creating payment method in Braintree", t);
        }

        return result;
    }

    @Override
    public Result<? extends PaymentMethod> updatePaymentMethod(String currentBraintreePaymentMethodToken, String newBraintreePaymentMethodToken, String newCustomerId) throws BraintreeException {
        Result<? extends PaymentMethod> result;
        try{
            PaymentMethodRequest request = new PaymentMethodRequest()
                    .customerId(newCustomerId)
                    .token(newBraintreePaymentMethodToken);

            result = gateway.paymentMethod().update(currentBraintreePaymentMethodToken, request);
        }
        catch(Throwable t){
            throw new BraintreeException("Could not synchronize KillBill payment method " + newBraintreePaymentMethodToken + " with Braintree payment method " + currentBraintreePaymentMethodToken, t);
        }

        return result;
    }

    @Override
    public List<? extends PaymentMethod> getPaymentMethods(String braintreeCustomerId) throws BraintreeException {
        List<? extends PaymentMethod> paymentMethods;
        try{
            paymentMethods = gateway.customer().find(braintreeCustomerId).getPaymentMethods();
        }
        catch(Throwable t){
            throw new BraintreeException("Could not fetch payment methods for Braintree customer " + braintreeCustomerId, t);
        }
        return paymentMethods;
    }

    @Nullable
    @Override
    public String createNonceFromPaymentMethodToken(String braintreePaymentMethodToken) {
        String nonceFromPaymentMethodToken;
        try{
            Result<PaymentMethodNonce> result  = gateway.paymentMethodNonce().create(braintreePaymentMethodToken);
            nonceFromPaymentMethodToken = result.getTarget().getNonce();
        }
        catch (NotFoundException e){
            return null;
        }
        catch (Throwable t){
            throw new BraintreeException("Could not create Braintree nonce from payment method token " + braintreePaymentMethodToken, t);
        }
        return nonceFromPaymentMethodToken;
    }

    @Override
    public Result<? extends PaymentMethod> deletePaymentMethod(String braintreePaymentMethodToken) throws BraintreeException {
        Result<? extends PaymentMethod> result;
        try{
            result = gateway.paymentMethod().delete(braintreePaymentMethodToken);
        }
        catch(Throwable t){
            throw new BraintreeException("Could not delete payment method in Braintree", t);
        }

        return result;
    }

    @Override
    public Transaction.Status getTransactionStatus(String braintreeTransactionId) {
        Transaction.Status transactionStatus;
        try{
            transactionStatus = gateway.transaction().find(braintreeTransactionId).getStatus();
        }
        catch(Throwable t){
            throw new BraintreeException("Could not obtain the Braintree status for transaction " + braintreeTransactionId , t);
        }
        return transactionStatus;
    }

}

