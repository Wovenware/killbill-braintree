# killbill-braintree-plugin

Plugin to use [Braintree](https://www.braintreepayments.com/) as a gateway.

## Requirements

• An active Braintree account is required for using the plugin. A Braintree Sandbox account may be used for testing purposes.

## Installation

Locally:

```
kpm install_java_plugin braintree-plugin --from-source-file target/braintree-plugin-*-SNAPSHOT.jar --destination /var/tmp/bundles
```

## Configuration

1. The plugin requires that the killbill database includes some additional tables. Connect to the database and execute the ddl.sql file included to create these required tables.

2. Go to your Braintree account and obtain the following values:

* merchantId
* publicKey
* privateKey

See [Braintree - Gateway Credentials](https://articles.braintreepayments.com/control-panel/important-gateway-credentials) for more information regarding how to retrieve these values from your account.

3. Add the Braintree gateway credentials as global properties in the killbill.properties file:

```java
org.killbill.billing.plugin.braintree.btEnvironment=sandbox
org.killbill.billing.plugin.braintree.btMerchantId={merchantId}
org.killbill.billing.plugin.braintree.btPublicKey={publicKey}
org.killbill.billing.plugin.braintree.btPrivateKey={privateKey}
```

For the btEnvironment property, use 'sandbox' only for testing with a Braintree Sandbox account. Other possible values include 'development', 'qa', and 'production'. See Braintree documentation for details.

Note that these four properties can also be set using the following environment variables:

```bash
BRAINTREE_ENVIRONMENT
BRAINTREE_MERCHANT_ID
BRAINTREE_PUBLIC_KEY
BRAINTREE_PRIVATE_KEY
```

Since unit tests run isolated from the KillBill environment, they can only execute properly if the credentials are set using environment variables. The Braintree plugin on the other hand will attempt to load the credentials from the properties file first, and fallback to the environment variables as a second option only if the properties are not found in the file.

4. In order to facilitate automated testing, you should disable all fraud detection within your Braintree Sandbox account. These can generate gateway rejection errors when processing multiple test transactions. In particular make sure to disable [Duplicate Transaction Checking](https://articles.braintreepayments.com/control-panel/transactions/duplicate-checking#configuring-duplicate-transaction-checking).

Once these properties are configured and the plugin restarted it will be ready to be used.

For more information regarding killbill properties see [Kill Bill configuration guide](https://docs.killbill.io/latest/userguide_configuration.html).

## Overview

The plugin generates a token for the client by means of a servlet, the client uses this token to send payment information to Braintree in exchange for a nonce. The nonce is used by the KillBill Braintree plugin to create payment methods, or to perform a one-time purchases without the need to vault the payment method. If the nonce is used to create the payment method, then subsequent transactions that use that payment method will use the payment method token instead of the nonce, which becomes invalid once used for the payment method creation. 

Payment methods that are supported include:

* Credit Card (with or without 3D Secure)
* PayPal
* ACH

Note that most of the differences in processing these payment methods are managed in the client side, and the nonce received by the backend will be handled in the same manner, with only some small differences.

Note that when creating payment methods from the client at least the following properties must be included for the plugin to work correctly:

* bt_nonce: Payment method nonce received from Braintree.
* bt_customer_id: The customer ID assigned to the customer in Braintree's vault. Only required if not already present as an account Custom Field, if provided it will be set as Custom Field.
* payment_method_type: The type of payment method that is being created. Possible values include CARD, PAYPAL or ACH. These values are case insensitive. Not to be confused with Braintree's payment instrument type, which includes subdivisions of these three general types.