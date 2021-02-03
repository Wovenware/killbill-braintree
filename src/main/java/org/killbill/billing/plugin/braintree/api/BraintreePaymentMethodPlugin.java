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

import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.api.payment.PluginPaymentMethodPlugin;
import org.killbill.billing.plugin.braintree.dao.BraintreeDao;
import org.killbill.billing.plugin.braintree.dao.gen.tables.records.BraintreePaymentMethodsRecord;

import java.util.List;
import java.util.UUID;


public class BraintreePaymentMethodPlugin extends PluginPaymentMethodPlugin {

    public static BraintreePaymentMethodPlugin build(BraintreePaymentMethodsRecord braintreePaymentMethodsRecord){
        final String externalPaymentMethodId = braintreePaymentMethodsRecord.getBraintreeId();
        return new BraintreePaymentMethodPlugin(UUID.fromString(braintreePaymentMethodsRecord.getKbPaymentMethodId()),
                externalPaymentMethodId,
                braintreePaymentMethodsRecord.getIsDefault() == BraintreeDao.TRUE,
                PluginProperties.buildPluginProperties(BraintreeDao.mapFromAdditionalDataString(braintreePaymentMethodsRecord.getAdditionalData())));
    }

    public BraintreePaymentMethodPlugin(final UUID kbPaymentMethodId,
                                     final String externalPaymentMethodId,
                                     final boolean isDefaultPaymentMethod,
                                     final List<PluginProperty> properties) {
        super(kbPaymentMethodId, externalPaymentMethodId, isDefaultPaymentMethod, properties);
    }
}
