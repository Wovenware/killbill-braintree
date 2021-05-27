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

import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.killbill.billing.osgi.api.Healthcheck;
import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.plugin.api.notification.PluginConfigurationEventHandler;
import org.killbill.billing.plugin.braintree.api.BraintreePaymentPluginApi;
import org.killbill.billing.plugin.braintree.core.resources.BraintreeHealthcheckServlet;
import org.killbill.billing.plugin.braintree.core.resources.BraintreeTokenServlet;
import org.killbill.billing.plugin.braintree.dao.BraintreeDao;
import org.killbill.billing.plugin.core.config.PluginEnvironmentConfig;
import org.killbill.billing.plugin.core.resources.jooby.PluginApp;
import org.killbill.billing.plugin.core.resources.jooby.PluginAppBuilder;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BraintreeActivator extends KillbillActivatorBase {

	private static final Logger logger = LoggerFactory.getLogger(BraintreePaymentPluginApi.class);
	public static final String PLUGIN_NAME = "killbill-braintree";

	private BraintreeConfigPropertiesConfigurationHandler braintreeConfigurationHandler;

	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		final String region = PluginEnvironmentConfig.getRegion(configProperties.getProperties());


		// Register an event listener for plugin configuration
		braintreeConfigurationHandler = new BraintreeConfigPropertiesConfigurationHandler(region, PLUGIN_NAME, killbillAPI);
		
		
		final BraintreeConfigProperties globalConfiguration = braintreeConfigurationHandler
				.createConfigurable(configProperties.getProperties());
		braintreeConfigurationHandler.setDefaultConfigurable(globalConfiguration);

		final BraintreeDao braintreeDao = new BraintreeDao(dataSource.getDataSource());
		final PaymentPluginApi paymentPluginApi = new BraintreePaymentPluginApi(braintreeConfigurationHandler,
				killbillAPI, configProperties, clock.getClock(), braintreeDao);
		registerPaymentPluginApi(context, paymentPluginApi);

		// Expose a healthcheck, so other plugins can check on the plugin status
		final Healthcheck healthcheck = new BraintreeHealthcheck(braintreeConfigurationHandler);
		registerHealthcheck(context, healthcheck);

		// Register a servlet
		final PluginApp pluginApp = new PluginAppBuilder(PLUGIN_NAME, killbillAPI, dataSource, super.clock, configProperties)
						.withRouteClass(BraintreeTokenServlet.class)
						.withRouteClass(BraintreeHealthcheckServlet.class).withService(healthcheck)
						.withService(braintreeConfigurationHandler)
						.build();
		final HttpServlet httpServlet = PluginApp.createServlet(pluginApp);
		registerServlet(context, httpServlet);

		registerHandlers();
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		super.stop(context);
	}

	private void registerHandlers() {
		final PluginConfigurationEventHandler configHandler = new PluginConfigurationEventHandler(
				braintreeConfigurationHandler);

		dispatcher.registerEventHandlers(configHandler);
	}

	private void registerServlet(final BundleContext context, final Servlet servlet) {
		final Hashtable<String, String> props = new Hashtable<>();
		props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
		registrar.registerService(context, Servlet.class, servlet, props);
	}

	private void registerPaymentPluginApi(final BundleContext context, final PaymentPluginApi api) {
		final Hashtable<String, String> props = new Hashtable<>();
		props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
		registrar.registerService(context, PaymentPluginApi.class, api, props);
	}

	private void registerHealthcheck(final BundleContext context, final Healthcheck healthcheck) {
		final Hashtable<String, String> props = new Hashtable<>();
		props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
		registrar.registerService(context, Healthcheck.class, healthcheck, props);
	}
}
