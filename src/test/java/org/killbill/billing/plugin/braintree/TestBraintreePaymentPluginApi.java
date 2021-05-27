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

package org.killbill.billing.plugin.braintree;

import com.braintreegateway.Customer;
import com.braintreegateway.CustomerRequest;
import com.braintreegateway.Result;
import com.braintreegateway.exceptions.BraintreeException;
import com.google.common.collect.ImmutableList;
import org.killbill.billing.ObjectType;
import org.killbill.billing.payment.api.*;
import org.killbill.billing.payment.plugin.api.PaymentMethodInfoPlugin;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.TestUtils;
import org.killbill.billing.plugin.api.core.PluginCustomField;
import org.killbill.billing.plugin.braintree.api.BraintreePaymentMethodPlugin;
import org.killbill.billing.plugin.braintree.core.BraintreePluginProperties;
import org.killbill.billing.plugin.braintree.core.BraintreePluginProperties.PaymentMethodType;
import org.killbill.billing.util.callcontext.TenantContext;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;

public class TestBraintreePaymentPluginApi extends TestBase {

    public static final String FAKE_VALID_VISA_NONCE = "fake-valid-visa-nonce";
    public static final String FAKE_VALID_PAYPAL_NONCE = "fake-paypal-billing-agreement-nonce";

    @Test(groups = "integration", enabled = true)
    public void testCreatePaymentMethod() throws PaymentPluginApiException {
        final UUID kbAccountId = account.getId();

        Customer customer = createBraintreeCustomer(kbAccountId);
        assertEquals(syncPaymentMethods(kbAccountId).size(), 0);
        addPaymentMethodToCustomer(customer, PaymentMethodType.CARD);
        assertEquals(syncPaymentMethods(kbAccountId).size(), 1);
        addPaymentMethodToCustomer(customer, PaymentMethodType.PAYPAL);
        assertEquals(syncPaymentMethods(kbAccountId).size(), 2);

    }

    @Test(groups = "integration", enabled = true)
    public void testDeletePaymentMethod() throws PaymentPluginApiException {
        final UUID kbAccountId = account.getId();

        Customer customer = createBraintreeCustomer(kbAccountId);
        assertEquals(syncPaymentMethods(kbAccountId).size(), 0);
        PaymentMethodPlugin paymentMethodPlugin = addPaymentMethodToCustomer(customer, PaymentMethodType.CARD);
        assertEquals(syncPaymentMethods(kbAccountId).size(), 1);
        braintreeGateway.paymentMethod().delete(paymentMethodPlugin.getExternalPaymentMethodId());
        assertEquals(syncPaymentMethods(kbAccountId).size(), 0);
    }

    @Test(groups = "integration", enabled = true)
    public void testSyncPaymentMethods() throws PaymentPluginApiException {
        UUID kbAccountId = account.getId();
        Customer customer = createBraintreeCustomer(kbAccountId);
        assertEquals(syncPaymentMethods(kbAccountId).size(), 0);
        //Create payment methods directly in Braintree
        braintreeClient.createPaymentMethod(customer.getId(), UUID.randomUUID().toString(), FAKE_VALID_VISA_NONCE, PaymentMethodType.CARD);
        braintreeClient.createPaymentMethod(customer.getId(), UUID.randomUUID().toString(), FAKE_VALID_PAYPAL_NONCE, PaymentMethodType.PAYPAL);
        List<PaymentMethodInfoPlugin> paymentMethods = syncPaymentMethods(kbAccountId);
        assertEquals(paymentMethods.size(), 2);
        //Delete payment methods directly in Braintree
        braintreeClient.deletePaymentMethod(paymentMethods.get(0).getExternalPaymentMethodId());
        assertEquals(syncPaymentMethods(kbAccountId).size(), 1);
        braintreeClient.deletePaymentMethod(paymentMethods.get(1).getExternalPaymentMethodId());
        assertEquals(syncPaymentMethods(kbAccountId).size(), 0);
    }

    @Test(groups = "integration", enabled = true)
    public void testSuccessfulPurchase() throws PaymentPluginApiException, PaymentApiException {
        UUID kbAccountId = account.getId();
        Customer customer = createBraintreeCustomer(kbAccountId);
        PaymentMethodPlugin paymentMethodPlugin  = addPaymentMethodToCustomer(customer, PaymentMethodType.CARD);

        final Payment payment = TestUtils.buildPayment(kbAccountId, paymentMethodPlugin.getKbPaymentMethodId(), account.getCurrency(), killbillApi);
        final PaymentTransaction purchaseTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.PURCHASE, BigDecimal.TEN, payment.getCurrency());

        final PaymentTransactionInfoPlugin purchaseInfoPlugin = braintreePaymentPluginApi.purchasePayment(kbAccountId,
                payment.getId(),
                purchaseTransaction.getId(),
                paymentMethodPlugin.getKbPaymentMethodId(),
                purchaseTransaction.getAmount(),
                purchaseTransaction.getCurrency(),
                ImmutableList.of(),
                context);
        TestUtils.updatePaymentTransaction(purchaseTransaction, purchaseInfoPlugin);
        verifyPaymentTransactionInfoPlugin(payment, purchaseTransaction, purchaseInfoPlugin, PaymentPluginStatus.PROCESSED);
    }

    @Test(groups = "integration", enabled = true)
    public void testSuccessfulAuthCapture() throws PaymentPluginApiException, PaymentApiException {
        UUID kbAccountId = account.getId();
        Customer customer = createBraintreeCustomer(kbAccountId);
        PaymentMethodPlugin paymentMethodPlugin  = addPaymentMethodToCustomer(customer, PaymentMethodType.CARD);

        final Payment payment = TestUtils.buildPayment(kbAccountId, paymentMethodPlugin.getKbPaymentMethodId(), account.getCurrency(), killbillApi);
        final PaymentTransaction authorizationTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.AUTHORIZE, BigDecimal.TEN, payment.getCurrency());
        final PaymentTransaction captureTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.CAPTURE, BigDecimal.TEN, payment.getCurrency());

        final PaymentTransactionInfoPlugin authorizationInfoPlugin = braintreePaymentPluginApi.authorizePayment(kbAccountId,
                payment.getId(),
                authorizationTransaction.getId(),
                paymentMethodPlugin.getKbPaymentMethodId(),
                authorizationTransaction.getAmount(),
                authorizationTransaction.getCurrency(),
                ImmutableList.of(),
                context);
        TestUtils.updatePaymentTransaction(authorizationTransaction, authorizationInfoPlugin);
        verifyPaymentTransactionInfoPlugin(payment, authorizationTransaction, authorizationInfoPlugin, PaymentPluginStatus.PROCESSED);

        final PaymentTransactionInfoPlugin captureInfoPlugin = braintreePaymentPluginApi.capturePayment(kbAccountId,
                payment.getId(),
                captureTransaction.getId(),
                paymentMethodPlugin.getKbPaymentMethodId(),
                captureTransaction.getAmount(),
                captureTransaction.getCurrency(),
                ImmutableList.of(),
                context);
        TestUtils.updatePaymentTransaction(captureTransaction, captureInfoPlugin);
        verifyPaymentTransactionInfoPlugin(payment, captureTransaction, captureInfoPlugin, PaymentPluginStatus.PROCESSED);
    }

    @Test(groups = "integration", enabled = true)
    public void testSuccessfulAuthVoid() throws PaymentPluginApiException, PaymentApiException {
        UUID kbAccountId = account.getId();
        Customer customer = createBraintreeCustomer(kbAccountId);
        PaymentMethodPlugin paymentMethodPlugin  = addPaymentMethodToCustomer(customer, PaymentMethodType.CARD);

        final Payment payment = TestUtils.buildPayment(kbAccountId, paymentMethodPlugin.getKbPaymentMethodId(), account.getCurrency(), killbillApi);
        final PaymentTransaction authorizationTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.AUTHORIZE, BigDecimal.TEN, payment.getCurrency());
        final PaymentTransaction voidTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.VOID, BigDecimal.TEN, payment.getCurrency());

        final PaymentTransactionInfoPlugin authorizationInfoPlugin = braintreePaymentPluginApi.authorizePayment(kbAccountId,
                payment.getId(),
                authorizationTransaction.getId(),
                paymentMethodPlugin.getKbPaymentMethodId(),
                authorizationTransaction.getAmount(),
                authorizationTransaction.getCurrency(),
                ImmutableList.of(),
                context);
        TestUtils.updatePaymentTransaction(authorizationTransaction, authorizationInfoPlugin);
        verifyPaymentTransactionInfoPlugin(payment, authorizationTransaction, authorizationInfoPlugin, PaymentPluginStatus.PROCESSED);

        final PaymentTransactionInfoPlugin voidInfoPlugin = braintreePaymentPluginApi.voidPayment(kbAccountId,
                payment.getId(),
                voidTransaction.getId(),
                paymentMethodPlugin.getKbPaymentMethodId(),
                ImmutableList.of(),
                context);
        TestUtils.updatePaymentTransaction(voidTransaction, voidInfoPlugin);
        verifyPaymentTransactionInfoPlugin(payment, voidTransaction, voidInfoPlugin, PaymentPluginStatus.PROCESSED);
    }

    @Test(groups = "integration", enabled = true)
    public void testSuccessfulPurchaseRefund() throws PaymentPluginApiException, PaymentApiException {
        UUID kbAccountId = account.getId();
        Customer customer = createBraintreeCustomer(kbAccountId);
        PaymentMethodPlugin paymentMethodPlugin  = addPaymentMethodToCustomer(customer, PaymentMethodType.CARD);

        final Payment payment = TestUtils.buildPayment(kbAccountId, paymentMethodPlugin.getKbPaymentMethodId(), account.getCurrency(), killbillApi);
        final PaymentTransaction purchaseTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.PURCHASE, BigDecimal.TEN, payment.getCurrency());
        final PaymentTransaction refundTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.REFUND, BigDecimal.TEN, payment.getCurrency());

        final PaymentTransactionInfoPlugin purchaseInfoPlugin = braintreePaymentPluginApi.purchasePayment(kbAccountId,
                payment.getId(),
                purchaseTransaction.getId(),
                paymentMethodPlugin.getKbPaymentMethodId(),
                purchaseTransaction.getAmount(),
                purchaseTransaction.getCurrency(),
                ImmutableList.of(),
                context);
        TestUtils.updatePaymentTransaction(purchaseTransaction, purchaseInfoPlugin);
        verifyPaymentTransactionInfoPlugin(payment, purchaseTransaction, purchaseInfoPlugin, PaymentPluginStatus.PROCESSED);

        final PaymentTransactionInfoPlugin refundInfoPlugin = braintreePaymentPluginApi.refundPayment(kbAccountId,
                payment.getId(),
                refundTransaction.getId(),
                paymentMethodPlugin.getKbPaymentMethodId(),
                refundTransaction.getAmount(),
                refundTransaction.getCurrency(),
                ImmutableList.of(),
                context);
        TestUtils.updatePaymentTransaction(refundTransaction, refundInfoPlugin);
        verifyPaymentTransactionInfoPlugin(payment, refundTransaction, refundInfoPlugin, PaymentPluginStatus.PROCESSED);
    }

    @Test(groups = "integration", enabled = true)
    public void testGetPaymentInfo() throws PaymentPluginApiException, PaymentApiException{
        UUID kbAccountId = account.getId();
        Customer customer = createBraintreeCustomer(kbAccountId);
        PaymentMethodPlugin paymentMethodPlugin  = addPaymentMethodToCustomer(customer, PaymentMethodType.CARD);

        final Payment payment = TestUtils.buildPayment(kbAccountId, paymentMethodPlugin.getKbPaymentMethodId(), account.getCurrency(), killbillApi);
        final PaymentTransaction authorizationTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.AUTHORIZE, BigDecimal.TEN, payment.getCurrency());
        final PaymentTransaction captureTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.CAPTURE, BigDecimal.ONE, payment.getCurrency());
        final PaymentTransaction voidTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.VOID, BigDecimal.TEN, payment.getCurrency());

        final PaymentTransactionInfoPlugin authorizationInfoPlugin = braintreePaymentPluginApi.authorizePayment(kbAccountId,
                payment.getId(),
                authorizationTransaction.getId(),
                paymentMethodPlugin.getKbPaymentMethodId(),
                authorizationTransaction.getAmount(),
                authorizationTransaction.getCurrency(),
                ImmutableList.of(),
                context);
        TestUtils.updatePaymentTransaction(authorizationTransaction, authorizationInfoPlugin);
        List<PaymentTransactionInfoPlugin> payments = braintreePaymentPluginApi.getPaymentInfo(kbAccountId, payment.getId(), ImmutableList.of(), context);
        assertEquals(payments.size(), 1);
        assertEquals(payments.get(0).getAmount().compareTo(BigDecimal.TEN), 0);
        assertEquals(payments.get(0).getTransactionType(), TransactionType.AUTHORIZE);
        assertEquals(payments.get(0).getStatus(), PaymentPluginStatus.PROCESSED);
        assertEquals(payments.get(0).getKbPaymentId(), payment.getId());

        final PaymentTransactionInfoPlugin captureInfoPlugin = braintreePaymentPluginApi.capturePayment(kbAccountId,
                payment.getId(),
                captureTransaction.getId(),
                paymentMethodPlugin.getKbPaymentMethodId(),
                captureTransaction.getAmount(),
                captureTransaction.getCurrency(),
                ImmutableList.of(),
                context);
        TestUtils.updatePaymentTransaction(captureTransaction, captureInfoPlugin);
        verifyPaymentTransactionInfoPlugin(payment, captureTransaction, captureInfoPlugin, PaymentPluginStatus.PROCESSED);
        payments = braintreePaymentPluginApi.getPaymentInfo(kbAccountId, payment.getId(), ImmutableList.of(), context);
        assertEquals(payments.size(), 2);
        assertEquals(payments.get(1).getAmount().compareTo(BigDecimal.ONE), 0);
        assertEquals(payments.get(1).getTransactionType(), TransactionType.CAPTURE);
        assertEquals(payments.get(1).getStatus(), PaymentPluginStatus.PROCESSED);
        assertEquals(payments.get(1).getKbPaymentId(), payment.getId());

        final PaymentTransactionInfoPlugin voidInfoPlugin = braintreePaymentPluginApi.voidPayment(kbAccountId,
                payment.getId(),
                voidTransaction.getId(),
                paymentMethodPlugin.getKbPaymentMethodId(),
                ImmutableList.of(),
                context);
        TestUtils.updatePaymentTransaction(voidTransaction, voidInfoPlugin);
        verifyPaymentTransactionInfoPlugin(payment, voidTransaction, voidInfoPlugin, PaymentPluginStatus.PROCESSED);
        payments = braintreePaymentPluginApi.getPaymentInfo(kbAccountId, payment.getId(), ImmutableList.of(), context);
        assertEquals(payments.size(), 3);
        assertEquals(payments.get(2).getTransactionType(), TransactionType.VOID);
        assertEquals(payments.get(2).getStatus(), PaymentPluginStatus.PROCESSED);
        assertEquals(payments.get(2).getKbPaymentId(), payment.getId());
    }

    private void verifyPaymentTransactionInfoPlugin(final Payment payment,
                                                    final PaymentTransaction paymentTransaction,
                                                    final PaymentTransactionInfoPlugin paymentTransactionInfoPlugin,
                                                    final PaymentPluginStatus expectedPaymentPluginStatus) {
        assertEquals(paymentTransactionInfoPlugin.getKbPaymentId(), payment.getId());
        assertEquals(paymentTransactionInfoPlugin.getKbTransactionPaymentId(), paymentTransaction.getId());
        assertEquals(paymentTransactionInfoPlugin.getTransactionType(), paymentTransaction.getTransactionType());
        if (TransactionType.VOID.equals(paymentTransaction.getTransactionType())) {
            assertNull(paymentTransactionInfoPlugin.getAmount());
            assertNull(paymentTransactionInfoPlugin.getCurrency());
        } else {
            assertEquals(paymentTransactionInfoPlugin.getAmount().compareTo(paymentTransaction.getAmount()), 0);
            assertEquals(paymentTransactionInfoPlugin.getCurrency(), paymentTransaction.getCurrency());
        }
        assertNotNull(paymentTransactionInfoPlugin.getCreatedDate());
        assertNotNull(paymentTransactionInfoPlugin.getEffectiveDate());

        assertNull(paymentTransactionInfoPlugin.getGatewayErrorCode());
        assertEquals(paymentTransactionInfoPlugin.getStatus(), expectedPaymentPluginStatus, "Unexpected status " + paymentTransactionInfoPlugin.getStatus() + ": " + paymentTransactionInfoPlugin);

        assertNull(paymentTransactionInfoPlugin.getGatewayError());
    }

    private PaymentMethodPlugin addPaymentMethodToCustomer(Customer customer, PaymentMethodType paymentMethodType) throws PaymentPluginApiException {
        final UUID kbAccountId = account.getId();
        final UUID paymentMethodId = UUID.randomUUID();

        String testNonce;
        switch (paymentMethodType){
            case CARD:
                testNonce = FAKE_VALID_VISA_NONCE;
                break;
            case PAYPAL:
                testNonce = FAKE_VALID_PAYPAL_NONCE;
                break;
            default:
                testNonce = "INVALID_NONCE";
        }

        BraintreePaymentMethodPlugin braintreePaymentMethodPlugin = new BraintreePaymentMethodPlugin(paymentMethodId, paymentMethodId.toString(), true, ImmutableList.of());

        braintreePaymentPluginApi.addPaymentMethod(kbAccountId,
                paymentMethodId,
                braintreePaymentMethodPlugin,
                true,
                ImmutableList.of(new PluginProperty(BraintreePluginProperties.PROPERTY_PAYMENT_METHOD_TYPE, paymentMethodType.name(),false),
                        new PluginProperty(BraintreePluginProperties.PROPERTY_BT_NONCE, testNonce, false),
                        new PluginProperty(BraintreePluginProperties.PROPERTY_BT_CUSTOMER_ID, customer.getId(), false)),
                context);

        return braintreePaymentPluginApi.getPaymentMethodDetail(kbAccountId, paymentMethodId, ImmutableList.of(), context);
    }

    private List<PaymentMethodInfoPlugin> syncPaymentMethods(UUID kbAccountId) throws PaymentPluginApiException {
        return braintreePaymentPluginApi.getPaymentMethods(kbAccountId,true, ImmutableList.of(), context);
    }

    private Customer createBraintreeCustomer(final UUID kbAccountId) {
        CustomerRequest request = new CustomerRequest()
                .firstName("John")
                .lastName("Doe");

        final Result<Customer> result = braintreeGateway.customer().create(request);

        if(!result.isSuccess()) throw new BraintreeException("Could not create customer.");

        Customer customer = result.getTarget();

        // Add the magic Custom Field
        final PluginCustomField customField = new PluginCustomField(kbAccountId,
                ObjectType.ACCOUNT,
                BraintreePluginProperties.MAGIC_FIELD_BT_CUSTOMER_ID,
                customer.getId(),
                clock.getUTCNow());
        Mockito.when(customFieldUserApi.getCustomFieldsForAccountType(Mockito.eq(kbAccountId), Mockito.eq(ObjectType.ACCOUNT), Mockito.any(TenantContext.class)))
                .thenReturn(ImmutableList.of(customField));

        return customer;
    }




}
