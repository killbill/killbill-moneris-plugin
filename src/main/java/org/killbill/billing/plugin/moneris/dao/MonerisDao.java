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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentMethodInfoPlugin;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.moneris.MonerisPaymentTransactionInfoPlugin;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.entity.Pagination;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionStatus;

public class MonerisDao {

    private final DBI dbi;

    public MonerisDao(final DataSource dataSource) {
        this.dbi = new DBI(dataSource);
    }

    public void createTransaction(final UUID kbAccountId, final UUID kbPaymentId, final UUID kbTransactionId, final UUID kbPaymentMethodId, final BigDecimal amount, final Currency currency, final TransactionType transactionType, final MonerisPaymentTransactionInfoPlugin monerisPaymentTransactionInfoPlugin, final CallContext callContext) {
        dbi.inTransaction(new TransactionCallback<Object>() {
            @Override
            public Object inTransaction(final Handle conn, final TransactionStatus status) throws Exception {
                conn.execute("insert into moneris_transactions (" +
                             "  kb_account_id" +
                             ", kb_payment_id" +
                             ", kb_transaction_id" +
                             ", kb_payment_method_id" +
                             ", transaction_type" +
                             ", amount" +
                             ", currency" +
                             ", transaction_amount" +
                             ", transaction_effective_date" +
                             ", transaction_status" +
                             ", transaction_gateway_error" +
                             ", transaction_gateway_error_code" +
                             ", transaction_first_payment_reference_id" +
                             ", transaction_second_payment_reference_id" +
                             ", receipt_is_visa_debit" +
                             ", receipt_status_message" +
                             ", receipt_status_code" +
                             ", receipt_cavv_result_code" +
                             ", receipt_cvd_result_code" +
                             ", receipt_avs_result_code" +
                             ", receipt_recur_success" +
                             ", receipt_ticket" +
                             ", receipt_timed_out" +
                             ", receipt_txn_number" +
                             ", receipt_card_type" +
                             ", receipt_trans_amount" +
                             ", receipt_message" +
                             ", receipt_complete" +
                             ", receipt_trans_type" +
                             ", receipt_trans_date" +
                             ", receipt_trans_time" +
                             ", receipt_auth_code" +
                             ", receipt_iso" +
                             ", receipt_response_code" +
                             ", receipt_reference_num" +
                             ", receipt_receipt_id" +
                             ", created_by" +
                             ", created_date" +
                             ", updated_by" +
                             ", updated_date" +
                             ", kb_tenant_id" +
                             ")" +
                             "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                             kbAccountId,
                             kbPaymentId,
                             kbTransactionId,
                             kbPaymentMethodId,
                             transactionType,
                             amount,
                             currency == null ? null : currency.toString(),
                             monerisPaymentTransactionInfoPlugin.getAmount(),
                             monerisPaymentTransactionInfoPlugin.getEffectiveDate().toDate(),
                             monerisPaymentTransactionInfoPlugin.getStatus(),
                             monerisPaymentTransactionInfoPlugin.getGatewayError(),
                             monerisPaymentTransactionInfoPlugin.getGatewayErrorCode(),
                             monerisPaymentTransactionInfoPlugin.getFirstPaymentReferenceId(),
                             monerisPaymentTransactionInfoPlugin.getSecondPaymentReferenceId(),
                             monerisPaymentTransactionInfoPlugin.getIsVisaDebit(),
                             monerisPaymentTransactionInfoPlugin.getStatusMessage(),
                             monerisPaymentTransactionInfoPlugin.getStatusCode(),
                             monerisPaymentTransactionInfoPlugin.getCavvResultCode(),
                             monerisPaymentTransactionInfoPlugin.getCvdResultCode(),
                             monerisPaymentTransactionInfoPlugin.getAvsResultCode(),
                             monerisPaymentTransactionInfoPlugin.getRecurSuccess(),
                             monerisPaymentTransactionInfoPlugin.getTicket(),
                             monerisPaymentTransactionInfoPlugin.getTimedOut(),
                             monerisPaymentTransactionInfoPlugin.getTxnNumber(),
                             monerisPaymentTransactionInfoPlugin.getCardType(),
                             monerisPaymentTransactionInfoPlugin.getTransAmount(),
                             monerisPaymentTransactionInfoPlugin.getMessage(),
                             monerisPaymentTransactionInfoPlugin.getComplete(),
                             monerisPaymentTransactionInfoPlugin.getTransType(),
                             monerisPaymentTransactionInfoPlugin.getTransDate(),
                             monerisPaymentTransactionInfoPlugin.getTransTime(),
                             monerisPaymentTransactionInfoPlugin.getAuthCode(),
                             monerisPaymentTransactionInfoPlugin.getIso(),
                             monerisPaymentTransactionInfoPlugin.getResponseCode(),
                             monerisPaymentTransactionInfoPlugin.getReferenceNum(),
                             monerisPaymentTransactionInfoPlugin.getReceiptId(),
                             callContext.getUserName(),
                             callContext.getCreatedDate().toDate(),
                             callContext.getUserName(),
                             callContext.getCreatedDate().toDate(),
                             callContext.getTenantId()
                            );
                return null;
            }
        });
    }

    public List<PaymentTransactionInfoPlugin> getTransactions(final UUID kbPaymentId, final UUID kbTenantId) {
        return dbi.inTransaction(new TransactionCallback<List<PaymentTransactionInfoPlugin>>() {
            @Override
            public List<PaymentTransactionInfoPlugin> inTransaction(final Handle conn, final TransactionStatus status) throws Exception {
                final Query<PaymentTransactionInfoPlugin> query = conn.createQuery("select * " +
                                                                                   "from moneris_transactions " +
                                                                                   "where kb_payment_id = :kbPaymentId and kb_tenant_id = :kbTenantId " +
                                                                                   "order by created_date asc, updated_date asc")
                                                                      .bind("kbPaymentId", kbPaymentId)
                                                                      .bind("kbTenantId", kbTenantId)
                                                                      .map(new MonerisPaymentTransactionInfoPluginResultSetMapper());
                return query.list();
            }
        });
    }

    public Pagination<PaymentTransactionInfoPlugin> searchTransactions(final String searchKey, final Long offset, final Long limit, final UUID kbTenantId) {
        // TODO
        return null;
    }

    public void createPaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final PaymentMethodPlugin paymentMethodProps, final CallContext callContext) {
        dbi.inTransaction(new TransactionCallback<Object>() {
            @Override
            public Object inTransaction(final Handle conn, final TransactionStatus status) throws Exception {
                conn.execute("insert into moneris_payment_methods (" +
                             "  kb_account_id" +
                             ", kb_payment_method_id" +
                             ", external_payment_method_id" +
                             // TODO Extract some plugin properties, like billing address, etc?
                             ", created_by" +
                             ", created_date" +
                             ", updated_by" +
                             ", updated_date" +
                             ", kb_tenant_id" +
                             ")" +
                             "values (?, ?, ?, ?, ?, ?, ?, ?)",
                             kbAccountId,
                             kbPaymentMethodId,
                             paymentMethodProps.getExternalPaymentMethodId(),
                             callContext.getUserName(),
                             callContext.getCreatedDate().toDate(),
                             callContext.getUserName(),
                             callContext.getCreatedDate().toDate(),
                             callContext.getTenantId()
                            );
                return null;
            }
        });
    }

    public void deletePaymentMethod(final UUID kbPaymentMethodId, final CallContext callContext) {
        dbi.inTransaction(new TransactionCallback<Object>() {
            @Override
            public Object inTransaction(final Handle conn, final TransactionStatus status) throws Exception {
                conn.execute("update moneris_payment_methods " +
                             "set is_deleted = true, updated_by = ?, updated_date = ? " +
                             "where kb_payment_method_id = ? and kb_tenant_id = ?",
                             callContext.getUserName(), callContext.getCreatedDate().toDate(),
                             kbPaymentMethodId, callContext.getTenantId()
                            );
                return null;
            }
        });
    }

    public PaymentMethodPlugin getPaymentMethod(final UUID kbPaymentMethodId, final UUID kbTenantId) {
        return dbi.inTransaction(new TransactionCallback<PaymentMethodPlugin>() {
            @Override
            public PaymentMethodPlugin inTransaction(final Handle conn, final TransactionStatus status) throws Exception {
                final Query<PaymentMethodPlugin> query = conn.createQuery("select * " +
                                                                          "from moneris_payment_methods " +
                                                                          "where kb_payment_method_id = :kbPaymentMethodId and kb_tenant_id = :kbTenantId and not is_deleted " +
                                                                          "order by created_date asc, updated_date asc")
                                                             .bind("kbPaymentMethodId", kbPaymentMethodId)
                                                             .bind("kbTenantId", kbTenantId)
                                                             .map(new MonerisPaymentMethodPluginResultSetMapper());
                return query.first();
            }
        });
    }

    public List<PaymentMethodInfoPlugin> getPaymentMethods(final UUID kbAccountId, final UUID kbTenantId) {
        return dbi.inTransaction(new TransactionCallback<List<PaymentMethodInfoPlugin>>() {
            @Override
            public List<PaymentMethodInfoPlugin> inTransaction(final Handle conn, final TransactionStatus status) throws Exception {
                final Query<PaymentMethodInfoPlugin> query = conn.createQuery("select * " +
                                                                              "from moneris_payment_methods " +
                                                                              "where kb_account_id = :kbAccountId and kb_tenant_id = :kbTenantId and not is_deleted " +
                                                                              "order by created_date asc, updated_date asc")
                                                                 .bind("kbAccountId", kbAccountId)
                                                                 .bind("kbTenantId", kbTenantId)
                                                                 .map(new MonerisPaymentMethodInfoPluginResultSetMapper());
                return query.list();
            }
        });
    }

    public Pagination<PaymentMethodPlugin> searchPaymentMethods(final String searchKey, final Long offset, final Long limit, final UUID kbTenantId) {
        // TODO
        return null;
    }
}
