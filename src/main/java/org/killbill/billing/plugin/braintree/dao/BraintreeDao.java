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

package org.killbill.billing.plugin.braintree.dao;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import com.braintreegateway.Result;
import com.braintreegateway.Transaction;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.jooq.impl.DSL;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.braintree.client.BraintreeClient;
import org.killbill.billing.plugin.braintree.core.BraintreePluginProperties;
import org.killbill.billing.plugin.braintree.dao.gen.tables.records.BraintreeHppRequestsRecord;
import org.killbill.billing.plugin.dao.payment.PluginPaymentDao;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.killbill.billing.plugin.braintree.dao.gen.tables.BraintreePaymentMethods;
import org.killbill.billing.plugin.braintree.dao.gen.tables.BraintreeResponses;
import org.killbill.billing.plugin.braintree.dao.gen.tables.records.BraintreePaymentMethodsRecord;
import org.killbill.billing.plugin.braintree.dao.gen.tables.records.BraintreeResponsesRecord;


import static org.killbill.billing.plugin.braintree.dao.gen.tables.BraintreeHppRequests.BRAINTREE_HPP_REQUESTS;
import static org.killbill.billing.plugin.braintree.dao.gen.tables.BraintreePaymentMethods.BRAINTREE_PAYMENT_METHODS;
import static org.killbill.billing.plugin.braintree.dao.gen.tables.BraintreeResponses.BRAINTREE_RESPONSES;

public class BraintreeDao extends PluginPaymentDao<BraintreeResponsesRecord, BraintreeResponses, BraintreePaymentMethodsRecord, BraintreePaymentMethods> {

    public BraintreeDao(final DataSource dataSource) throws SQLException {
        super(BRAINTREE_RESPONSES, BRAINTREE_PAYMENT_METHODS, dataSource);
        // Save space in the database
        objectMapper.setSerializationInclusion(Include.NON_EMPTY);
    }

    // Payment methods
    @Override
    public void addPaymentMethod(final UUID kbAccountId,
                                 final UUID kbPaymentMethodId,
                                 final boolean isDefault,
                                 final Map<String, String> additionalDataMap,
                                 final DateTime utcNow,
                                 final UUID kbTenantId) throws SQLException {
        execute(dataSource.getConnection(),
                new WithConnectionCallback<BraintreeResponsesRecord>() {
                    @Override
                    public BraintreeResponsesRecord withConnection(final Connection conn) throws SQLException {
                        DSL.using(conn, dialect, settings)
                                .insertInto(BRAINTREE_PAYMENT_METHODS,
                                        BRAINTREE_PAYMENT_METHODS.KB_ACCOUNT_ID,
                                        BRAINTREE_PAYMENT_METHODS.KB_PAYMENT_METHOD_ID,
                                        BRAINTREE_PAYMENT_METHODS.BRAINTREE_ID,
                                        BRAINTREE_PAYMENT_METHODS.IS_DEFAULT,
                                        BRAINTREE_PAYMENT_METHODS.IS_DELETED,
                                        BRAINTREE_PAYMENT_METHODS.ADDITIONAL_DATA,
                                        BRAINTREE_PAYMENT_METHODS.CREATED_DATE,
                                        BRAINTREE_PAYMENT_METHODS.UPDATED_DATE,
                                        BRAINTREE_PAYMENT_METHODS.KB_TENANT_ID)
                                .values(kbAccountId.toString(),
                                        kbPaymentMethodId.toString(),
                                        kbPaymentMethodId.toString(),
                                        (short) (isDefault? TRUE : FALSE),
                                        (short) FALSE,
                                        asString(additionalDataMap),
                                        toLocalDateTime(utcNow),
                                        toLocalDateTime(utcNow),
                                        kbTenantId.toString()
                                )
                                .execute();

                        return null;
                    }
                });
    }

    public void updatePaymentMethod(final UUID kbPaymentMethodId,
                                    final Map<String, Object> additionalDataMap,
                                    final String braintreeId,
                                    final DateTime utcNow,
                                    final UUID kbTenantId) throws SQLException {
        execute(dataSource.getConnection(),
                new WithConnectionCallback<BraintreeResponsesRecord>() {
                    @Override
                    public BraintreeResponsesRecord withConnection(final Connection conn) throws SQLException {
                        DSL.using(conn, dialect, settings)
                                .update(BRAINTREE_PAYMENT_METHODS)
                                .set(BRAINTREE_PAYMENT_METHODS.ADDITIONAL_DATA, asString(additionalDataMap))
                                .set(BRAINTREE_PAYMENT_METHODS.UPDATED_DATE, toLocalDateTime(utcNow))
                                .where(BRAINTREE_PAYMENT_METHODS.KB_PAYMENT_METHOD_ID.equal(kbPaymentMethodId.toString()))
                                .and(BRAINTREE_PAYMENT_METHODS.BRAINTREE_ID.equal(braintreeId))
                                .and(BRAINTREE_PAYMENT_METHODS.KB_TENANT_ID.equal(kbTenantId.toString()))
                                .execute();
                        return null;
                    }
                });
    }

    // HPP requests

    public void addHppRequest(final UUID kbAccountId,
                              final UUID kbPaymentId,
                              final UUID kbPaymentTransactionId,
                              final Map<String, String> additionalDataMap,
                              final DateTime utcNow,
                              final UUID kbTenantId) throws SQLException {

        execute(dataSource.getConnection(),
                new WithConnectionCallback<Void>() {
                    @Override
                    public Void withConnection(final Connection conn) throws SQLException {
                        DSL.using(conn, dialect, settings)
                                .insertInto(BRAINTREE_HPP_REQUESTS,
                                        BRAINTREE_HPP_REQUESTS.KB_ACCOUNT_ID,
                                        BRAINTREE_HPP_REQUESTS.KB_PAYMENT_ID,
                                        BRAINTREE_HPP_REQUESTS.KB_PAYMENT_TRANSACTION_ID,
                                        BRAINTREE_HPP_REQUESTS.ADDITIONAL_DATA,
                                        BRAINTREE_HPP_REQUESTS.CREATED_DATE,
                                        BRAINTREE_HPP_REQUESTS.KB_TENANT_ID)
                                .values(kbAccountId.toString(),
                                        kbPaymentId == null ? null : kbPaymentId.toString(),
                                        kbPaymentTransactionId == null ? null : kbPaymentTransactionId.toString(),
                                        asString(additionalDataMap),
                                        toLocalDateTime(utcNow),
                                        kbTenantId.toString())
                                .execute();
                        return null;
                    }
                });
    }

    public BraintreeHppRequestsRecord getHppRequest(final String kbTransactionId,
                                                    final String kbTenantId) throws SQLException {
        return execute(dataSource.getConnection(),
                new WithConnectionCallback<BraintreeHppRequestsRecord>() {
                    @Override
                    public BraintreeHppRequestsRecord withConnection(final Connection conn) throws SQLException {
                        return DSL.using(conn, dialect, settings)
                                .selectFrom(BRAINTREE_HPP_REQUESTS)
                                .where(BRAINTREE_HPP_REQUESTS.KB_PAYMENT_TRANSACTION_ID.equal(kbTransactionId))
                                .and(BRAINTREE_HPP_REQUESTS.KB_TENANT_ID.equal(kbTenantId))
                                .orderBy(BRAINTREE_HPP_REQUESTS.RECORD_ID.desc())
                                .limit(1)
                                .fetchOne();
                    }
                });
    }

    // Responses

    public BraintreeResponsesRecord addResponse(final UUID kbAccountId,
                                                final UUID kbPaymentId,
                                                final UUID kbPaymentTransactionId,
                                                final TransactionType transactionType,
                                                final BigDecimal amount,
                                                final Currency currency,
                                                final Result<Transaction> braintreeResult,
                                                final DateTime utcNow,
                                                final UUID kbTenantId) throws SQLException {
        final Map<String, Object> additionalDataMap = BraintreePluginProperties.toAdditionalDataMap(braintreeResult);

        return execute(dataSource.getConnection(),
                new WithConnectionCallback<BraintreeResponsesRecord>() {
                    @Override
                    public BraintreeResponsesRecord withConnection(final Connection conn) throws SQLException {
                        return DSL.using(conn, dialect, settings)
                                .insertInto(BRAINTREE_RESPONSES,
                                        BRAINTREE_RESPONSES.KB_ACCOUNT_ID,
                                        BRAINTREE_RESPONSES.KB_PAYMENT_ID,
                                        BRAINTREE_RESPONSES.KB_PAYMENT_TRANSACTION_ID,
                                        BRAINTREE_RESPONSES.TRANSACTION_TYPE,
                                        BRAINTREE_RESPONSES.AMOUNT,
                                        BRAINTREE_RESPONSES.CURRENCY,
                                        BRAINTREE_RESPONSES.BRAINTREE_ID,
                                        BRAINTREE_RESPONSES.ADDITIONAL_DATA,
                                        BRAINTREE_RESPONSES.CREATED_DATE,
                                        BRAINTREE_RESPONSES.KB_TENANT_ID)
                                .values(kbAccountId.toString(),
                                        kbPaymentId.toString(),
                                        kbPaymentTransactionId.toString(),
                                        transactionType.toString(),
                                        amount,
                                        currency == null ? null : currency.name(),
                                        BraintreeClient.getTransactionInstance(braintreeResult).getId(),
                                        asString(additionalDataMap),
                                        toLocalDateTime(utcNow),
                                        kbTenantId.toString())
                                .returning()
                                .fetchOne();
                    }
                });
    }

    public BraintreeResponsesRecord updateResponse(final UUID kbPaymentTransactionId,
                                                final Iterable<PluginProperty> additionalPluginProperties,
                                                final UUID kbTenantId) throws SQLException {
        final Map<String, Object> additionalProperties = PluginProperties.toMap(additionalPluginProperties);
        return updateResponse(kbPaymentTransactionId, additionalProperties, kbTenantId);
    }

    public BraintreeResponsesRecord updateResponse(final UUID kbPaymentTransactionId,
                                                final Map<String, Object> additionalProperties,
                                                final UUID kbTenantId) throws SQLException {
        return execute(dataSource.getConnection(),
                new WithConnectionCallback<BraintreeResponsesRecord>() {
                    @Override
                    public BraintreeResponsesRecord withConnection(final Connection conn) throws SQLException {
                        final BraintreeResponsesRecord response = DSL.using(conn, dialect, settings)
                                .selectFrom(BRAINTREE_RESPONSES)
                                .where(BRAINTREE_RESPONSES.KB_PAYMENT_TRANSACTION_ID.equal(kbPaymentTransactionId.toString()))
                                .and(BRAINTREE_RESPONSES.KB_TENANT_ID.equal(kbTenantId.toString()))
                                .orderBy(BRAINTREE_RESPONSES.RECORD_ID.desc())
                                .limit(1)
                                .fetchOne();

                        if (response == null) {
                            return null;
                        }

                        final Map originalData = new HashMap(mapFromAdditionalDataString(response.getAdditionalData()));
                        originalData.putAll(additionalProperties);

                        DSL.using(conn, dialect, settings)
                                .update(BRAINTREE_RESPONSES)
                                .set(BRAINTREE_RESPONSES.ADDITIONAL_DATA, asString(originalData))
                                .where(BRAINTREE_RESPONSES.RECORD_ID.equal(response.getRecordId()))
                                .execute();
                        return response;
                    }
                });
    }

    public void updateResponse(final BraintreeResponsesRecord braintreeResponsesRecord,
                               final Map additionalMetadata) throws SQLException {
        final Map additionalDataMap = mapFromAdditionalDataString(braintreeResponsesRecord.getAdditionalData());
        additionalDataMap.putAll(additionalMetadata);

        execute(dataSource.getConnection(),
                new WithConnectionCallback<Void>() {
                    @Override
                    public Void withConnection(final Connection conn) throws SQLException {
                        DSL.using(conn, dialect, settings)
                                .update(BRAINTREE_RESPONSES)
                                .set(BRAINTREE_RESPONSES.ADDITIONAL_DATA, asString(additionalDataMap))
                                .where(BRAINTREE_RESPONSES.RECORD_ID.equal(braintreeResponsesRecord.getRecordId()))
                                .execute();
                        return null;
                    }
                });
    }

    @Override
    public BraintreeResponsesRecord getSuccessfulAuthorizationResponse(final UUID kbPaymentId, final UUID kbTenantId) throws SQLException {
        return execute(dataSource.getConnection(),
                new WithConnectionCallback<BraintreeResponsesRecord>() {
                    @Override
                    public BraintreeResponsesRecord withConnection(final Connection conn) throws SQLException {
                        return DSL.using(conn, dialect, settings)
                                .selectFrom(responsesTable)
                                .where(DSL.field(responsesTable.getName() + "." + KB_PAYMENT_ID).equal(kbPaymentId.toString()))
                                .and(
                                        DSL.field(responsesTable.getName() + "." + TRANSACTION_TYPE).equal(TransactionType.AUTHORIZE.toString())
                                                .or(DSL.field(responsesTable.getName() + "." + TRANSACTION_TYPE).equal(TransactionType.PURCHASE.toString()))
                                )
                                .and(DSL.field(responsesTable.getName() + "." + KB_TENANT_ID).equal(kbTenantId.toString()))
                                .orderBy(DSL.field(responsesTable.getName() + "." + RECORD_ID).desc())
                                .limit(1)
                                .fetchOne();
                    }
                });
    }


    public static Map mapFromAdditionalDataString(@Nullable final String additionalData) {
        if (additionalData == null) {
            return ImmutableMap.of();
        }

        try {
            return objectMapper.readValue(additionalData, Map.class);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String stringFromAdditionalDataMap(@Nullable final Map<String, Object> additionalData) {
        if (additionalData == null || additionalData.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(additionalData);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

}
