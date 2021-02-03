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

import java.util.Map;

import javax.annotation.Nullable;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Environment;
import org.killbill.billing.osgi.api.Healthcheck;
import org.killbill.billing.tenant.api.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BraintreeHealthcheck implements Healthcheck {

    private static final Logger logger = LoggerFactory.getLogger(BraintreeHealthcheck.class);

    private final BraintreeConfigPropertiesConfigurationHandler braintreeConfigPropertiesConfigurationHandler;

    public BraintreeHealthcheck(final BraintreeConfigPropertiesConfigurationHandler braintreeConfigPropertiesConfigurationHandler) {
        this.braintreeConfigPropertiesConfigurationHandler = braintreeConfigPropertiesConfigurationHandler;
    }

    public HealthStatus getHealthStatus(@Nullable final Tenant tenant, @Nullable final Map properties) {
        if (tenant == null) {
            // The plugin is running
            return HealthStatus.healthy("Braintree OK");
        } else {
            // Specifying the tenant lets you also validate the tenant configuration
            final BraintreeConfigProperties braintreeConfigProperties = braintreeConfigPropertiesConfigurationHandler.getConfigurable(tenant.getId());
            return pingBraintree(braintreeConfigProperties);
        }
    }

    private HealthStatus pingBraintree(final BraintreeConfigProperties braintreeConfigProperties) {
        final BraintreeGateway gateway = new BraintreeGateway(
                Environment.parseEnvironment(braintreeConfigProperties.getBtEnvironment()),
                braintreeConfigProperties.getBtMerchantId(),
                braintreeConfigProperties.getBtPublicKey(),
                braintreeConfigProperties.getBtPrivateKey()
        );

        try {
            gateway.getConfiguration().getBaseURL();
            return HealthStatus.healthy("Braintree OK");
        } catch (final Throwable e) {
            logger.warn("Healthcheck error", e);
            return HealthStatus.unHealthy("Braintree error: " + e.getMessage());
        }
    }
}
