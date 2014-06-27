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

package org.killbill.billing.plugin.moneris;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.GatewayNotification;
import org.killbill.billing.payment.plugin.api.HostedPaymentPageFormDescriptor;
import org.killbill.billing.payment.plugin.api.PaymentMethodInfoPlugin;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.moneris.dao.MonerisDao;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.entity.Pagination;
import org.osgi.service.log.LogService;

import JavaAPI.AvsInfo;
import JavaAPI.Completion;
import JavaAPI.CvdInfo;
import JavaAPI.HttpsPostRequest;
import JavaAPI.IndependentRefund;
import JavaAPI.PreAuth;
import JavaAPI.Purchase;
import JavaAPI.PurchaseCorrection;
import JavaAPI.ReAuth;
import JavaAPI.Receipt;
import JavaAPI.Refund;
import JavaAPI.Transaction;

/**
 * Note: this assumes automatic close is enabled. Otherwise, you will need to close the
 * batch manually every day.
 * TODO: make this a configuration option and use the notification queue to schedule closes
 * <p/>
 * TODO CustInfo fields are currently not used. Should we populate some of these?
 */
public class MonerisPaymentPluginApi implements PaymentPluginApi {

    private final String host;
    private final String storeId;
    private final String apiToken;
    private final MonerisDao monerisDao;
    private final LogService logService;

    public MonerisPaymentPluginApi(final String host, final String storeId, final String apiToken, final MonerisDao monerisDao, final LogService logService) {
        this.host = host;
        this.storeId = storeId;
        this.apiToken = apiToken;
        this.monerisDao = monerisDao;
        this.logService = logService;
    }

    @Override
    public PaymentTransactionInfoPlugin authorizePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        final MonerisProperties monerisProperties = new MonerisProperties(properties);
        // Merchant defined unique transaction identifier - must be unique for every Purchase, PreAuth and Independent Refund attempt
        final String orderId = monerisSafeUUID(kbTransactionId);
        final String monerisAmount = toMonerisAmount(amount);
        // This is an optional field that can be sent as part of a Purchase or PreAuth request.
        // It is searchable from the Moneris Merchant Resource Centre. It is commonly used for policy number,
        // membership number, student ID or invoice number.
        final String custId = monerisSafeUUID(kbAccountId);
        // Merchant defined description sent on a per-transaction basis that will appear on the credit card statement.
        final String dynamicDescriptor = monerisProperties.getDynamicDescriptor(monerisSafeUUID(kbPaymentId));
        final AvsInfo avsInfo = monerisProperties.getAvsInfo();
        final CvdInfo cvdInfo = monerisProperties.getCvdInfo();

        // Is it pre-auth or re-auth?
        final PaymentTransactionInfoPlugin origTransaction = findOrigTransaction(kbPaymentId, TransactionType.AUTHORIZE, context);

        final Transaction transaction;
        if (origTransaction == null) {
            // Pre-auth
            final PreAuth preAuth = new PreAuth(orderId, custId, monerisAmount, monerisProperties.getPan(), monerisProperties.getExpDate(), monerisProperties.getCrypt());
            preAuth.setDynamicDescriptor(dynamicDescriptor);
            preAuth.setAvsInfo(avsInfo);
            preAuth.setCvdInfo(cvdInfo);

            transaction = preAuth;
        } else {
            // Re-auth
            final String origOrderId = findOrigOrderId(origTransaction);
            final String txnNumber = findOrigTxnNumber(origTransaction);
            final ReAuth reAuth = new ReAuth(orderId, custId, monerisAmount, origOrderId, txnNumber, monerisProperties.getCrypt());
            reAuth.setDynamicDescriptor(dynamicDescriptor);
            reAuth.setAvsInfo(avsInfo);
            reAuth.setCvdInfo(cvdInfo);

            transaction = reAuth;
        }

        final HttpsPostRequest request = new HttpsPostRequest(host, storeId, apiToken, transaction);
        final Receipt receipt = request.getReceipt();

        final MonerisPaymentTransactionInfoPlugin monerisPaymentTransactionInfoPlugin = new MonerisPaymentTransactionInfoPlugin(kbPaymentId, kbTransactionId, currency, receipt);
        monerisDao.createTransaction(kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, TransactionType.AUTHORIZE, monerisPaymentTransactionInfoPlugin, context);

        return monerisPaymentTransactionInfoPlugin;
    }

    @Override
    public PaymentTransactionInfoPlugin capturePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        final MonerisProperties monerisProperties = new MonerisProperties(properties);
        // For Refunds, Completions and Voids the order_id must reference the original transaction
        final PaymentTransactionInfoPlugin origTransaction = findOrigTransaction(kbPaymentId, TransactionType.AUTHORIZE, context);
        final String orderId = findOrigOrderId(origTransaction);
        final String txnNumber = findOrigTxnNumber(origTransaction);
        final String monerisAmount = toMonerisAmount(amount);
        // Merchant defined description sent on a per-transaction basis that will appear on the credit card statement.
        final String dynamicDescriptor = monerisProperties.getDynamicDescriptor(monerisSafeUUID(kbPaymentId));

        // Build the completion object
        final Completion completion = new Completion(orderId, monerisAmount, txnNumber, monerisProperties.getCrypt());
        completion.setDynamicDescriptor(dynamicDescriptor);

        final HttpsPostRequest request = new HttpsPostRequest(host, storeId, apiToken, completion);
        final Receipt receipt = request.getReceipt();

        final MonerisPaymentTransactionInfoPlugin monerisPaymentTransactionInfoPlugin = new MonerisPaymentTransactionInfoPlugin(kbPaymentId, kbTransactionId, currency, receipt);
        monerisDao.createTransaction(kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, TransactionType.CAPTURE, monerisPaymentTransactionInfoPlugin, context);

        return monerisPaymentTransactionInfoPlugin;
    }

    @Override
    public PaymentTransactionInfoPlugin purchasePayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        final MonerisProperties monerisProperties = new MonerisProperties(properties);
        // Merchant defined unique transaction identifier - must be unique for every Purchase, PreAuth and Independent Refund attempt
        final String orderId = monerisSafeUUID(kbTransactionId);
        final String monerisAmount = toMonerisAmount(amount);
        // This is an optional field that can be sent as part of a Purchase or PreAuth request.
        // It is searchable from the Moneris Merchant Resource Centre. It is commonly used for policy number,
        // membership number, student ID or invoice number.
        final String custId = monerisSafeUUID(kbAccountId);
        // Merchant defined description sent on a per-transaction basis that will appear on the credit card statement.
        final String dynamicDescriptor = monerisProperties.getDynamicDescriptor(monerisSafeUUID(kbPaymentId));
        final AvsInfo avsInfo = monerisProperties.getAvsInfo();
        final CvdInfo cvdInfo = monerisProperties.getCvdInfo();

        // Build the purchase object
        final Purchase preAuth = new Purchase(orderId, custId, monerisAmount, monerisProperties.getPan(), monerisProperties.getExpDate(), monerisProperties.getCrypt());
        preAuth.setDynamicDescriptor(dynamicDescriptor);
        preAuth.setAvsInfo(avsInfo);
        preAuth.setCvdInfo(cvdInfo);

        final HttpsPostRequest request = new HttpsPostRequest(host, storeId, apiToken, preAuth);
        final Receipt receipt = request.getReceipt();

        final MonerisPaymentTransactionInfoPlugin monerisPaymentTransactionInfoPlugin = new MonerisPaymentTransactionInfoPlugin(kbPaymentId, kbTransactionId, currency, receipt);
        monerisDao.createTransaction(kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, TransactionType.PURCHASE, monerisPaymentTransactionInfoPlugin, context);

        return monerisPaymentTransactionInfoPlugin;
    }

    @Override
    public PaymentTransactionInfoPlugin voidPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        final MonerisProperties monerisProperties = new MonerisProperties(properties);
        // For Refunds, Completions and Voids the order_id must reference the original transaction
        final PaymentTransactionInfoPlugin origTransaction = findOrigTransaction(kbPaymentId, TransactionType.AUTHORIZE, context);
        final String orderId = findOrigOrderId(origTransaction);
        final String txnNumber = findOrigTxnNumber(origTransaction);
        // Merchant defined description sent on a per-transaction basis that will appear on the credit card statement.
        final String dynamicDescriptor = monerisProperties.getDynamicDescriptor(monerisSafeUUID(kbPaymentId));

        // Build the void object
        final PurchaseCorrection purchaseCorrection = new PurchaseCorrection(orderId, txnNumber, monerisProperties.getCrypt());
        purchaseCorrection.setDynamicDescriptor(dynamicDescriptor);

        final HttpsPostRequest request = new HttpsPostRequest(host, storeId, apiToken, purchaseCorrection);
        final Receipt receipt = request.getReceipt();

        final MonerisPaymentTransactionInfoPlugin monerisPaymentTransactionInfoPlugin = new MonerisPaymentTransactionInfoPlugin(kbPaymentId, kbTransactionId, null, receipt);
        monerisDao.createTransaction(kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, null, null, TransactionType.VOID, monerisPaymentTransactionInfoPlugin, context);

        return monerisPaymentTransactionInfoPlugin;
    }

    @Override
    public PaymentTransactionInfoPlugin creditPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        final MonerisProperties monerisProperties = new MonerisProperties(properties);
        // Merchant defined unique transaction identifier - must be unique for every Purchase, PreAuth and Independent Refund attempt
        final String orderId = monerisSafeUUID(kbTransactionId);
        final String monerisAmount = toMonerisAmount(amount);
        // This is an optional field that can be sent as part of a Purchase or PreAuth request.
        // It is searchable from the Moneris Merchant Resource Centre. It is commonly used for policy number,
        // membership number, student ID or invoice number.
        final String custId = monerisSafeUUID(kbAccountId);
        // Merchant defined description sent on a per-transaction basis that will appear on the credit card statement.
        final String dynamicDescriptor = monerisProperties.getDynamicDescriptor(monerisSafeUUID(kbPaymentId));

        // Build the credit object
        final IndependentRefund independentRefund = new IndependentRefund(orderId, custId, monerisAmount, monerisProperties.getPan(), monerisProperties.getExpDate(), monerisProperties.getCrypt());
        independentRefund.setDynamicDescriptor(dynamicDescriptor);

        final HttpsPostRequest request = new HttpsPostRequest(host, storeId, apiToken, independentRefund);
        final Receipt receipt = request.getReceipt();

        final MonerisPaymentTransactionInfoPlugin monerisPaymentTransactionInfoPlugin = new MonerisPaymentTransactionInfoPlugin(kbPaymentId, kbTransactionId, currency, receipt);
        monerisDao.createTransaction(kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, TransactionType.CREDIT, monerisPaymentTransactionInfoPlugin, context);

        return monerisPaymentTransactionInfoPlugin;
    }

    @Override
    public PaymentTransactionInfoPlugin refundPayment(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        final MonerisProperties monerisProperties = new MonerisProperties(properties);
        // For Refunds, Completions and Voids the order_id must reference the original transaction
        PaymentTransactionInfoPlugin origTransaction = findOrigTransaction(kbPaymentId, TransactionType.PURCHASE, context);
        if (origTransaction == null) {
            // Maybe we are refunding a capture?
            origTransaction = findOrigTransaction(kbPaymentId, TransactionType.CAPTURE, context);
        }
        final String orderId = findOrigOrderId(origTransaction);
        final String txnNumber = findOrigTxnNumber(origTransaction);
        final String monerisAmount = toMonerisAmount(amount);
        // Merchant defined description sent on a per-transaction basis that will appear on the credit card statement.
        final String dynamicDescriptor = monerisProperties.getDynamicDescriptor(monerisSafeUUID(kbPaymentId));

        // Build the refund object
        final Refund refund = new Refund(orderId, monerisAmount, txnNumber, monerisProperties.getCrypt());
        refund.setDynamicDescriptor(dynamicDescriptor);

        final HttpsPostRequest request = new HttpsPostRequest(host, storeId, apiToken, refund);
        final Receipt receipt = request.getReceipt();

        final MonerisPaymentTransactionInfoPlugin monerisPaymentTransactionInfoPlugin = new MonerisPaymentTransactionInfoPlugin(kbPaymentId, kbTransactionId, currency, receipt);
        monerisDao.createTransaction(kbAccountId, kbPaymentId, kbTransactionId, kbPaymentMethodId, amount, currency, TransactionType.REFUND, monerisPaymentTransactionInfoPlugin, context);

        return monerisPaymentTransactionInfoPlugin;
    }

    @Override
    public List<PaymentTransactionInfoPlugin> getPaymentInfo(final UUID kbAccountId, final UUID kbPaymentId, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        return monerisDao.getTransactions(kbPaymentId, context.getTenantId());
    }

    @Override
    public Pagination<PaymentTransactionInfoPlugin> searchPayments(final String searchKey, final Long offset, final Long limit, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        return monerisDao.searchTransactions(searchKey, offset, limit, context.getTenantId());
    }

    @Override
    public void addPaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final PaymentMethodPlugin paymentMethodProps, final boolean setDefault, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        monerisDao.createPaymentMethod(kbAccountId, kbPaymentMethodId, paymentMethodProps, context);
    }

    @Override
    public void deletePaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        monerisDao.deletePaymentMethod(kbPaymentMethodId, context);
    }

    @Override
    public PaymentMethodPlugin getPaymentMethodDetail(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        return monerisDao.getPaymentMethod(kbPaymentMethodId, context.getTenantId());
    }

    @Override
    public void setDefaultPaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        // No-op, neither the plugin nor Moneris do care
    }

    @Override
    public List<PaymentMethodInfoPlugin> getPaymentMethods(final UUID kbAccountId, final boolean refreshFromGateway, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        return monerisDao.getPaymentMethods(kbAccountId, context.getTenantId());
    }

    @Override
    public Pagination<PaymentMethodPlugin> searchPaymentMethods(final String searchKey, final Long offset, final Long limit, final Iterable<PluginProperty> properties, final TenantContext context) throws PaymentPluginApiException {
        return monerisDao.searchPaymentMethods(searchKey, offset, limit, context.getTenantId());
    }

    @Override
    public void resetPaymentMethods(final UUID kbAccountId, final List<PaymentMethodInfoPlugin> paymentMethods, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        // TODO
    }

    @Override
    public HostedPaymentPageFormDescriptor buildFormDescriptor(final UUID kbAccountId, final Iterable<PluginProperty> customFields, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        throw new UnsupportedOperationException("Moneris hosted payment pages are not (yet) supported");
    }

    @Override
    public GatewayNotification processNotification(final String notification, final Iterable<PluginProperty> properties, final CallContext context) throws PaymentPluginApiException {
        throw new UnsupportedOperationException("Moneris notifications are not (yet) supported");
    }

    private String monerisSafeUUID(final UUID uuid) {
        // TODO Conflicting API documentation.
        // Are - allowed? an definition is supposed to be a-z A-Z 0-9 _ - : . @ spaces but the description of order_id is:
        // The last 10 characters of the order_id will be displayed in the “Invoice Number” field on the Merchant Direct Reports.
        // Only the following character sets will be sent to Merchant Direct (A_Z, a-z, space, 0-9).
        // A minimum of 3 and a maximum of 10 characters will be sent to Merchant Direct.
        // If the order_id has less than 3 characters, it may display a blank or 0000000000 in the Invoice Number field.
        // Only the last characters up to the invalid character will be sent. E.G. 1234-567890, 567890 will be sent to Merchant Direct.
        return uuid.toString().replace("-", "");
    }

    private String toMonerisAmount(@Nullable final BigDecimal realValueInLocalCurrency) {
        if (realValueInLocalCurrency == null) {
            return null;
        } else {
            // Amount of the transaction. This must contain 3 digits with two penny values.
            // The minimum value passed can be 0.01 and the maximum 9999999.99
            return realValueInLocalCurrency.setScale(2, RoundingMode.CEILING).toString();
        }
    }

    private PaymentTransactionInfoPlugin findOrigTransaction(final UUID kbPaymentId, final TransactionType transactionType, final CallContext context) {
        PaymentTransactionInfoPlugin origTransaction = null;

        final List<PaymentTransactionInfoPlugin> previousTransactionsForPayment = monerisDao.getTransactions(kbPaymentId, context.getTenantId());
        for (final PaymentTransactionInfoPlugin paymentTransactionInfoPlugin : previousTransactionsForPayment) {
            if (transactionType.equals(paymentTransactionInfoPlugin.getTransactionType())) {
                origTransaction = paymentTransactionInfoPlugin;
                // Don't break - we want the last one
            }
        }

        return origTransaction;
    }

    private String findOrigOrderId(final UUID kbPaymentId, final TransactionType transactionType, final CallContext context) {
        final PaymentTransactionInfoPlugin origTransaction = findOrigTransaction(kbPaymentId, transactionType, context);
        return findOrigOrderId(origTransaction);
    }

    private String findOrigOrderId(final PaymentTransactionInfoPlugin origTransaction) {
        if (origTransaction == null) {
            return null;
        } else {
            return monerisSafeUUID(origTransaction.getKbTransactionPaymentId());
        }
    }

    private String findOrigTxnNumber(final PaymentTransactionInfoPlugin origTransaction) {
        return ((MonerisPaymentTransactionInfoPlugin) origTransaction).getTxnNumber();
    }
}
