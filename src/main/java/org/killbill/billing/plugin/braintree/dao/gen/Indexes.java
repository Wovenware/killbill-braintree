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


import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.Internal;
import org.killbill.billing.plugin.braintree.dao.gen.tables.BraintreePaymentMethods;
import org.killbill.billing.plugin.braintree.dao.gen.tables.BraintreeResponses;


/**
 * A class modelling indexes of tables of the <code>killbill</code> schema.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Indexes {

    // -------------------------------------------------------------------------
    // INDEX definitions
    // -------------------------------------------------------------------------

    public static final Index BRAINTREE_PAYMENT_METHODS_BRAINTREE_PAYMENT_METHODS_BRAINTREE_ID = Indexes0.BRAINTREE_PAYMENT_METHODS_BRAINTREE_PAYMENT_METHODS_BRAINTREE_ID;
    public static final Index BRAINTREE_RESPONSES_BRAINTREE_RESPONSES_BRAINTREE_ID = Indexes0.BRAINTREE_RESPONSES_BRAINTREE_RESPONSES_BRAINTREE_ID;
    public static final Index BRAINTREE_RESPONSES_BRAINTREE_RESPONSES_KB_PAYMENT_ID = Indexes0.BRAINTREE_RESPONSES_BRAINTREE_RESPONSES_KB_PAYMENT_ID;
    public static final Index BRAINTREE_RESPONSES_BRAINTREE_RESPONSES_KB_PAYMENT_TRANSACTION_ID = Indexes0.BRAINTREE_RESPONSES_BRAINTREE_RESPONSES_KB_PAYMENT_TRANSACTION_ID;

    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class Indexes0 {
        public static Index BRAINTREE_PAYMENT_METHODS_BRAINTREE_PAYMENT_METHODS_BRAINTREE_ID = Internal.createIndex("braintree_payment_methods_braintree_id", BraintreePaymentMethods.BRAINTREE_PAYMENT_METHODS, new OrderField[] { BraintreePaymentMethods.BRAINTREE_PAYMENT_METHODS.BRAINTREE_ID }, false);
        public static Index BRAINTREE_RESPONSES_BRAINTREE_RESPONSES_BRAINTREE_ID = Internal.createIndex("braintree_responses_braintree_id", BraintreeResponses.BRAINTREE_RESPONSES, new OrderField[] { BraintreeResponses.BRAINTREE_RESPONSES.BRAINTREE_ID }, false);
        public static Index BRAINTREE_RESPONSES_BRAINTREE_RESPONSES_KB_PAYMENT_ID = Internal.createIndex("braintree_responses_kb_payment_id", BraintreeResponses.BRAINTREE_RESPONSES, new OrderField[] { BraintreeResponses.BRAINTREE_RESPONSES.KB_PAYMENT_ID }, false);
        public static Index BRAINTREE_RESPONSES_BRAINTREE_RESPONSES_KB_PAYMENT_TRANSACTION_ID = Internal.createIndex("braintree_responses_kb_payment_transaction_id", BraintreeResponses.BRAINTREE_RESPONSES, new OrderField[] { BraintreeResponses.BRAINTREE_RESPONSES.KB_PAYMENT_TRANSACTION_ID }, false);
    }
}
