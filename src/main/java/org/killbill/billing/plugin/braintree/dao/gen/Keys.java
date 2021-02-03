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

package org.killbill.billing.plugin.braintree.dao.gen;


import org.jooq.Identity;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.Internal;
import org.jooq.types.ULong;
import org.killbill.billing.plugin.braintree.dao.gen.tables.BraintreeHppRequests;
import org.killbill.billing.plugin.braintree.dao.gen.tables.BraintreePaymentMethods;
import org.killbill.billing.plugin.braintree.dao.gen.tables.BraintreeResponses;
import org.killbill.billing.plugin.braintree.dao.gen.tables.records.BraintreeHppRequestsRecord;
import org.killbill.billing.plugin.braintree.dao.gen.tables.records.BraintreePaymentMethodsRecord;
import org.killbill.billing.plugin.braintree.dao.gen.tables.records.BraintreeResponsesRecord;


/**
 * A class modelling foreign key relationships and constraints of tables of 
 * the <code>killbill</code> schema.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // IDENTITY definitions
    // -------------------------------------------------------------------------

    public static final Identity<BraintreeHppRequestsRecord, ULong> IDENTITY_BRAINTREE_HPP_REQUESTS = Identities0.IDENTITY_BRAINTREE_HPP_REQUESTS;
    public static final Identity<BraintreePaymentMethodsRecord, ULong> IDENTITY_BRAINTREE_PAYMENT_METHODS = Identities0.IDENTITY_BRAINTREE_PAYMENT_METHODS;
    public static final Identity<BraintreeResponsesRecord, ULong> IDENTITY_BRAINTREE_RESPONSES = Identities0.IDENTITY_BRAINTREE_RESPONSES;

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<BraintreeHppRequestsRecord> KEY_BRAINTREE_HPP_REQUESTS_PRIMARY = UniqueKeys0.KEY_BRAINTREE_HPP_REQUESTS_PRIMARY;
    public static final UniqueKey<BraintreeHppRequestsRecord> KEY_BRAINTREE_HPP_REQUESTS_RECORD_ID = UniqueKeys0.KEY_BRAINTREE_HPP_REQUESTS_RECORD_ID;
    public static final UniqueKey<BraintreePaymentMethodsRecord> KEY_BRAINTREE_PAYMENT_METHODS_PRIMARY = UniqueKeys0.KEY_BRAINTREE_PAYMENT_METHODS_PRIMARY;
    public static final UniqueKey<BraintreePaymentMethodsRecord> KEY_BRAINTREE_PAYMENT_METHODS_RECORD_ID = UniqueKeys0.KEY_BRAINTREE_PAYMENT_METHODS_RECORD_ID;
    public static final UniqueKey<BraintreePaymentMethodsRecord> KEY_BRAINTREE_PAYMENT_METHODS_BRAINTREE_PAYMENT_METHODS_KB_PAYMENT_ID = UniqueKeys0.KEY_BRAINTREE_PAYMENT_METHODS_BRAINTREE_PAYMENT_METHODS_KB_PAYMENT_ID;
    public static final UniqueKey<BraintreeResponsesRecord> KEY_BRAINTREE_RESPONSES_PRIMARY = UniqueKeys0.KEY_BRAINTREE_RESPONSES_PRIMARY;
    public static final UniqueKey<BraintreeResponsesRecord> KEY_BRAINTREE_RESPONSES_RECORD_ID = UniqueKeys0.KEY_BRAINTREE_RESPONSES_RECORD_ID;

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class Identities0 {
        public static Identity<BraintreeHppRequestsRecord, ULong> IDENTITY_BRAINTREE_HPP_REQUESTS = Internal.createIdentity(BraintreeHppRequests.BRAINTREE_HPP_REQUESTS, BraintreeHppRequests.BRAINTREE_HPP_REQUESTS.RECORD_ID);
        public static Identity<BraintreePaymentMethodsRecord, ULong> IDENTITY_BRAINTREE_PAYMENT_METHODS = Internal.createIdentity(BraintreePaymentMethods.BRAINTREE_PAYMENT_METHODS, BraintreePaymentMethods.BRAINTREE_PAYMENT_METHODS.RECORD_ID);
        public static Identity<BraintreeResponsesRecord, ULong> IDENTITY_BRAINTREE_RESPONSES = Internal.createIdentity(BraintreeResponses.BRAINTREE_RESPONSES, BraintreeResponses.BRAINTREE_RESPONSES.RECORD_ID);
    }

    private static class UniqueKeys0 {
        public static final UniqueKey<BraintreeHppRequestsRecord> KEY_BRAINTREE_HPP_REQUESTS_PRIMARY = Internal.createUniqueKey(BraintreeHppRequests.BRAINTREE_HPP_REQUESTS, "KEY_braintree_hpp_requests_PRIMARY", new TableField[] { BraintreeHppRequests.BRAINTREE_HPP_REQUESTS.RECORD_ID }, true);
        public static final UniqueKey<BraintreeHppRequestsRecord> KEY_BRAINTREE_HPP_REQUESTS_RECORD_ID = Internal.createUniqueKey(BraintreeHppRequests.BRAINTREE_HPP_REQUESTS, "KEY_braintree_hpp_requests_record_id", new TableField[] { BraintreeHppRequests.BRAINTREE_HPP_REQUESTS.RECORD_ID }, true);
        public static final UniqueKey<BraintreePaymentMethodsRecord> KEY_BRAINTREE_PAYMENT_METHODS_PRIMARY = Internal.createUniqueKey(BraintreePaymentMethods.BRAINTREE_PAYMENT_METHODS, "KEY_braintree_payment_methods_PRIMARY", new TableField[] { BraintreePaymentMethods.BRAINTREE_PAYMENT_METHODS.RECORD_ID }, true);
        public static final UniqueKey<BraintreePaymentMethodsRecord> KEY_BRAINTREE_PAYMENT_METHODS_RECORD_ID = Internal.createUniqueKey(BraintreePaymentMethods.BRAINTREE_PAYMENT_METHODS, "KEY_braintree_payment_methods_record_id", new TableField[] { BraintreePaymentMethods.BRAINTREE_PAYMENT_METHODS.RECORD_ID }, true);
        public static final UniqueKey<BraintreePaymentMethodsRecord> KEY_BRAINTREE_PAYMENT_METHODS_BRAINTREE_PAYMENT_METHODS_KB_PAYMENT_ID = Internal.createUniqueKey(BraintreePaymentMethods.BRAINTREE_PAYMENT_METHODS, "KEY_braintree_payment_methods_braintree_payment_methods_kb_payment_id", new TableField[] { BraintreePaymentMethods.BRAINTREE_PAYMENT_METHODS.KB_PAYMENT_METHOD_ID }, true);
        public static final UniqueKey<BraintreeResponsesRecord> KEY_BRAINTREE_RESPONSES_PRIMARY = Internal.createUniqueKey(BraintreeResponses.BRAINTREE_RESPONSES, "KEY_braintree_responses_PRIMARY", new TableField[] { BraintreeResponses.BRAINTREE_RESPONSES.RECORD_ID }, true);
        public static final UniqueKey<BraintreeResponsesRecord> KEY_BRAINTREE_RESPONSES_RECORD_ID = Internal.createUniqueKey(BraintreeResponses.BRAINTREE_RESPONSES, "KEY_braintree_responses_record_id", new TableField[] { BraintreeResponses.BRAINTREE_RESPONSES.RECORD_ID }, true);
    }
}
