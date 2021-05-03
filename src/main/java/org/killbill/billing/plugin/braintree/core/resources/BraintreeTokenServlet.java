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

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Environment;
import com.google.inject.Inject;
import org.jooby.mvc.GET;
import org.jooby.mvc.Local;
import org.jooby.mvc.Path;
import org.killbill.billing.plugin.braintree.core.BraintreeConfigProperties;
import org.killbill.billing.plugin.braintree.core.BraintreeConfigPropertiesConfigurationHandler;
import org.killbill.billing.tenant.api.Tenant;
import org.killbill.billing.util.entity.Entity;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
@Path("/clientToken")
public class BraintreeTokenServlet {

    private final BraintreeConfigPropertiesConfigurationHandler braintreeConfigPropertiesConfigurationHandler;

    @Inject
    public BraintreeTokenServlet(final BraintreeConfigPropertiesConfigurationHandler braintreeConfigPropertiesConfigurationHandler) {
        this.braintreeConfigPropertiesConfigurationHandler = braintreeConfigPropertiesConfigurationHandler;
    }

    @GET
    public String getToken(@Local @Named("killbill_tenant") final Optional<Tenant> tenant) {
        final BraintreeConfigProperties config = braintreeConfigPropertiesConfigurationHandler.getConfigurable(tenant.map(Entity::getId).orElse(null));
        final BraintreeGateway braintreeGateway = new BraintreeGateway(
                Environment.parseEnvironment(config.getBtEnvironment()),
                config.getBtMerchantId(),
                config.getBtPublicKey(),
                config.getBtPrivateKey()
        );
        return braintreeGateway.clientToken().generate();
    }
}
