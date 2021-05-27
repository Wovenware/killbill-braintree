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

import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Environment;
import org.killbill.billing.ObjectType;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.plugin.TestUtils;
import org.killbill.billing.plugin.braintree.api.BraintreePaymentPluginApi;
import org.killbill.billing.plugin.braintree.client.BraintreeClient;
import org.killbill.billing.plugin.braintree.client.BraintreeClientImpl;
import org.killbill.billing.plugin.braintree.core.BraintreeActivator;
import org.killbill.billing.plugin.braintree.core.BraintreeConfigProperties;
import org.killbill.billing.plugin.braintree.core.BraintreeConfigPropertiesConfigurationHandler;
import org.killbill.billing.plugin.braintree.dao.BraintreeDao;
import org.killbill.billing.util.api.CustomFieldUserApi;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.customfield.CustomField;
import org.killbill.clock.ClockMock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

public class TestBase {

    // Don't use braintree.properties (conflicts with a resource from upstream)
    private static final String PROPERTIES_FILE_NAME = "killbill.properties";

    public static final Currency DEFAULT_CURRENCY = Currency.USD;
    public static final String DEFAULT_COUNTRY = "US";

    protected ClockMock clock;
    protected CallContext context;
    protected Account account;
    protected BraintreePaymentPluginApi braintreePaymentPluginApi;
    protected OSGIKillbillAPI killbillApi;
    protected CustomFieldUserApi customFieldUserApi;
    protected BraintreeConfigPropertiesConfigurationHandler braintreeConfigPropertiesConfigurationHandler;
    protected BraintreeClient braintreeClient;
    protected BraintreeGateway braintreeGateway;
    protected BraintreeDao dao;

    @BeforeMethod(groups = {"slow", "integration"})
    public void setUp() throws Exception {
        EmbeddedDbHelper.instance().resetDB();
        dao = EmbeddedDbHelper.instance().getBraintreeDao();

        clock = new ClockMock();

        context = Mockito.mock(CallContext.class);
        UUID randomTenantId = UUID.randomUUID();
        Mockito.when(context.getTenantId()).thenReturn(randomTenantId);

        account = TestUtils.buildAccount(DEFAULT_CURRENCY, DEFAULT_COUNTRY);
        killbillApi = TestUtils.buildOSGIKillbillAPI(account);
        customFieldUserApi = Mockito.mock(CustomFieldUserApi.class);
        Mockito.when(killbillApi.getCustomFieldUserApi()).thenReturn(customFieldUserApi);

        TestUtils.buildPaymentMethod(account.getId(), account.getPaymentMethodId(), BraintreeActivator.PLUGIN_NAME, killbillApi);

        braintreeConfigPropertiesConfigurationHandler = new BraintreeConfigPropertiesConfigurationHandler("", BraintreeActivator.PLUGIN_NAME, killbillApi);
        setDefaultConfigurable();
        final BraintreeConfigProperties globalConfiguration = braintreeConfigPropertiesConfigurationHandler.getConfigurable(randomTenantId);
        final OSGIConfigPropertiesService configPropertiesService = Mockito.mock(OSGIConfigPropertiesService.class);
        braintreePaymentPluginApi = new BraintreePaymentPluginApi(braintreeConfigPropertiesConfigurationHandler,
                                                            killbillApi,
                                                            configPropertiesService,
                                                            clock,
                                                            dao);

        TestUtils.updateOSGIKillbillAPI(killbillApi, braintreePaymentPluginApi);

        Mockito.when(killbillApi.getPaymentApi()
                                .addPaymentMethod(Mockito.any(Account.class),
                                                  Mockito.anyString(),
                                                  Mockito.eq("killbill-braintree"),
                                                  Mockito.anyBoolean(),
                                                  Mockito.any(PaymentMethodPlugin.class),
                                                  Mockito.any(Iterable.class),
                                                  Mockito.any(CallContext.class)))
               .thenAnswer(new Answer<Object>() {
                   @Override
                   public Object answer(final InvocationOnMock invocation) throws Throwable {
                       braintreePaymentPluginApi.addPaymentMethod(((Account) invocation.getArguments()[0]).getId(),
                                                               UUID.randomUUID(),
                                                               (PaymentMethodPlugin) invocation.getArguments()[4],
                                                               (Boolean) invocation.getArguments()[3],
                                                               (Iterable) invocation.getArguments()[5],
                                                               (CallContext) invocation.getArguments()[6]);
                       return null;
                   }
               });

        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                // A bit simplistic but good enough for now?
                Mockito.when(customFieldUserApi.getCustomFieldsForAccountType(Mockito.eq(account.getId()), Mockito.eq(ObjectType.ACCOUNT), Mockito.any(TenantContext.class)))
                       .thenReturn((List<CustomField>) invocation.getArguments()[0]);
                return null;
            }
        })
               .when(customFieldUserApi).addCustomFields(Mockito.anyList(), Mockito.any(CallContext.class));
    }

    @BeforeMethod(groups = "integration")
    public void setUpIntegration() throws Exception {
        setDefaultConfigurable();
        final BraintreeConfigProperties globalConfiguration = braintreeConfigPropertiesConfigurationHandler.getConfigurable(null);
        braintreeGateway = new BraintreeGateway(
                Environment.parseEnvironment(globalConfiguration.getBtEnvironment()),
                globalConfiguration.getBtMerchantId(),
                globalConfiguration.getBtPublicKey(),
                globalConfiguration.getBtPrivateKey()
        );
        braintreeClient = new BraintreeClientImpl(braintreeGateway);
    }

    @BeforeSuite(groups = {"slow", "integration"})
    public void setUpBeforeSuite() throws Exception {
        EmbeddedDbHelper.instance().startDb();
    }

    @AfterSuite(groups = {"slow", "integration"})
    public void tearDownAfterSuite() throws Exception {
        EmbeddedDbHelper.instance().stopDB();
    }

    private void setDefaultConfigurable() throws  Exception{
        Properties properties = new Properties();
        try {
            properties = TestUtils.loadProperties(PROPERTIES_FILE_NAME);
        } catch (final RuntimeException ignored) {
            // Will use environment variables instead
        }
        final BraintreeConfigProperties braintreeConfigProperties = new BraintreeConfigProperties(properties, "");
        braintreeConfigPropertiesConfigurationHandler.setDefaultConfigurable(braintreeConfigProperties);
    }

}
