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

import java.util.Properties;

import org.killbill.billing.osgi.api.Healthcheck;
import org.killbill.billing.plugin.braintree.core.BraintreeActivator;
import org.killbill.billing.plugin.braintree.core.BraintreeConfigProperties;
import org.killbill.billing.plugin.braintree.core.BraintreeConfigPropertiesConfigurationHandler;
import org.killbill.billing.plugin.braintree.core.BraintreeHealthcheck;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestBraintreeHealthcheck extends TestBase {

    @Test(groups = "slow")
    public void testHealthcheckNoTenant() {
        final BraintreeConfigPropertiesConfigurationHandler noConfigHandler = new BraintreeConfigPropertiesConfigurationHandler("", BraintreeActivator.PLUGIN_NAME, killbillApi);
        noConfigHandler.setDefaultConfigurable(new BraintreeConfigProperties(new Properties(), ""));
        final Healthcheck healthcheck = new BraintreeHealthcheck(noConfigHandler);
        Assert.assertTrue(healthcheck.getHealthStatus(null, null).isHealthy());
    }

    @Test(groups = "slow")
    public void testHealthcheck() {
        final Healthcheck healthcheck = new BraintreeHealthcheck(braintreeConfigPropertiesConfigurationHandler);
        Assert.assertTrue(healthcheck.getHealthStatus(null, null).isHealthy());
    }
}
