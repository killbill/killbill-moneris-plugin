/*
 * Copyright 2014 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License" + you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.moneris.dao;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import org.h2.jdbcx.JdbcConnectionPool;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentMethodInfoPlugin;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.moneris.MonerisContext;
import org.killbill.billing.plugin.moneris.MonerisPaymentMethodPlugin;
import org.killbill.billing.plugin.moneris.MonerisPaymentTransactionInfoPlugin;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.CallOrigin;
import org.killbill.billing.util.callcontext.UserType;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestMonerisDao {

    private JdbcConnectionPool ds = null;
    private MonerisDao monerisDao = null;

    @BeforeMethod(groups = "slow")
    public void setUp() throws Exception {
        this.ds = JdbcConnectionPool.create("jdbc:h2:mem:moneris", "username", "password");
        this.monerisDao = new MonerisDao(ds);

        final DBI dbi = new DBI(this.ds);
        Handle h = null;
        try {
            h = dbi.open();
            final InputStream inputStream = TestMonerisDao.class.getResourceAsStream("/org/killbill/billing/plugin/moneris/dao/ddl.sql");
            final String ddl = new Scanner(inputStream, "UTF-8").useDelimiter("\\A").next();
            h.execute(ddl);
        } finally {
            if (h != null) {
                h.close();
            }
        }
    }

    @AfterMethod(groups = "slow")
    public void tearDown() throws Exception {
        if (ds != null) {
            ds.dispose();
        }
    }

    @Test(groups = "slow")
    public void testTransactions() throws Exception {
        final UUID kbTenantId = UUID.randomUUID();
        final CallContext context = new MonerisContext(kbTenantId, UUID.randomUUID(), UUID.randomUUID().toString(), CallOrigin.TEST, UserType.TEST, null, null, new DateTime(DateTimeZone.UTC), new DateTime(DateTimeZone.UTC));
        final UUID kbAccountId = UUID.randomUUID();
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbTransactionId = UUID.randomUUID();
        final UUID kbPaymentMethodId = UUID.randomUUID();
        final BigDecimal amount = BigDecimal.TEN;
        final Currency currency = Currency.CAD;
        final MonerisPaymentTransactionInfoPlugin auth = createTransaction(kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, TransactionType.AUTHORIZE, context);

        // Verify multi-tenancy - TODO test NULL tenant id
        Assert.assertEquals(monerisDao.getTransactions(kbPaymentId, UUID.randomUUID()).size(), 0);

        // List transactions for that payment
        final List<PaymentTransactionInfoPlugin> transactions1 = monerisDao.getTransactions(kbPaymentId, kbTenantId);
        Assert.assertEquals(transactions1.size(), 1);
        Assert.assertEquals(transactions1.get(0), auth);

        final MonerisPaymentTransactionInfoPlugin capture = createTransaction(kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, TransactionType.CAPTURE, context);

        // List again the transactions for that payment
        final List<PaymentTransactionInfoPlugin> transactions2 = monerisDao.getTransactions(kbPaymentId, kbTenantId);
        Assert.assertEquals(transactions2.size(), 2);
        Assert.assertEquals(transactions2.get(0), auth);
        Assert.assertEquals(transactions2.get(1), capture);

        // TODO
        // monerisDao.searchTransactions();
    }

    @Test(groups = "slow")
    public void testPaymentMethods() throws Exception {
        final UUID kbTenantId = UUID.randomUUID();
        final CallContext context = new MonerisContext(kbTenantId, UUID.randomUUID(), UUID.randomUUID().toString(), CallOrigin.TEST, UserType.TEST, null, null, new DateTime(DateTimeZone.UTC), new DateTime(DateTimeZone.UTC));
        final UUID kbAccountId = UUID.randomUUID();
        final UUID kbPaymentMethodId1 = UUID.randomUUID();
        final UUID kbPaymentMethodId2 = UUID.randomUUID();

        // Add the first payment method
        final PaymentMethodPlugin paymentMethodPlugin1 = createPaymentMethod(kbAccountId, kbPaymentMethodId1, context);

        // Verify multi-tenancy - TODO test NULL tenant id
        Assert.assertNull(monerisDao.getPaymentMethod(kbPaymentMethodId1, UUID.randomUUID()));
        Assert.assertEquals(monerisDao.getPaymentMethods(kbAccountId, UUID.randomUUID()).size(), 0);

        Assert.assertEquals(monerisDao.getPaymentMethod(kbPaymentMethodId1, kbTenantId), paymentMethodPlugin1);
        final List<PaymentMethodInfoPlugin> paymentMethodInfoPlugins1 = monerisDao.getPaymentMethods(kbAccountId, kbTenantId);
        Assert.assertEquals(paymentMethodInfoPlugins1.size(), 1);
        checkEquals(kbAccountId, paymentMethodInfoPlugins1.get(0), paymentMethodPlugin1);

        // Add a second payment method
        final PaymentMethodPlugin paymentMethodPlugin2 = createPaymentMethod(kbAccountId, kbPaymentMethodId2, context);

        Assert.assertEquals(monerisDao.getPaymentMethod(kbPaymentMethodId2, kbTenantId), paymentMethodPlugin2);
        final List<PaymentMethodInfoPlugin> paymentMethodInfoPlugins2 = monerisDao.getPaymentMethods(kbAccountId, kbTenantId);
        Assert.assertEquals(paymentMethodInfoPlugins2.size(), 2);
        checkEquals(kbAccountId, paymentMethodInfoPlugins2.get(0), paymentMethodPlugin1);
        checkEquals(kbAccountId, paymentMethodInfoPlugins2.get(1), paymentMethodPlugin2);

        // Delete the first payment method
        monerisDao.deletePaymentMethod(kbPaymentMethodId1, context);

        Assert.assertNull(monerisDao.getPaymentMethod(kbPaymentMethodId1, kbTenantId));
        final List<PaymentMethodInfoPlugin> paymentMethodInfoPlugins3 = monerisDao.getPaymentMethods(kbAccountId, kbTenantId);
        Assert.assertEquals(paymentMethodInfoPlugins3.size(), 1);
        checkEquals(kbAccountId, paymentMethodInfoPlugins3.get(0), paymentMethodPlugin2);

        // TODO
        // monerisDao.searchPaymentMethods()
    }

    private void checkEquals(final UUID kbAccountId, final PaymentMethodInfoPlugin paymentMethodInfoPlugin, final PaymentMethodPlugin paymentMethodPlugin) {
        Assert.assertEquals(paymentMethodInfoPlugin.getAccountId(), kbAccountId);
        Assert.assertEquals(paymentMethodInfoPlugin.getPaymentMethodId(), paymentMethodPlugin.getKbPaymentMethodId());
        Assert.assertEquals(paymentMethodInfoPlugin.isDefault(), paymentMethodPlugin.isDefaultPaymentMethod());
        Assert.assertEquals(paymentMethodInfoPlugin.getExternalPaymentMethodId(), paymentMethodPlugin.getExternalPaymentMethodId());
    }

    private MonerisPaymentTransactionInfoPlugin createTransaction(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final TransactionType transactionType, final CallContext context) {
        final MonerisPaymentTransactionInfoPlugin transactionInfoPlugin = new MonerisPaymentTransactionInfoPlugin(kbPaymentId, kbTransactionId, currency,
                                                                                                                  UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                                                                                                                  UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                                                                                                                  UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                                                                                                                  UUID.randomUUID().toString(), UUID.randomUUID().toString(), amount.toString(),
                                                                                                                  UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                                                                                                                  "2014-03-10", "12:42:01", UUID.randomUUID().toString(),
                                                                                                                  UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                                                                                                                  UUID.randomUUID().toString());

        monerisDao.createTransaction(kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, transactionType, transactionInfoPlugin, context);

        return transactionInfoPlugin;
    }

    private MonerisPaymentMethodPlugin createPaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final CallContext context) {
        final MonerisPaymentMethodPlugin paymentMethodPlugin = new MonerisPaymentMethodPlugin(kbPaymentMethodId, UUID.randomUUID().toString(), new LinkedList<PluginProperty>());

        monerisDao.createPaymentMethod(kbAccountId, kbPaymentMethodId, paymentMethodPlugin, context);

        return paymentMethodPlugin;
    }
}
