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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.moneris.MonerisPaymentTransactionInfoPlugin;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

class MonerisPaymentTransactionInfoPluginResultSetMapper implements ResultSetMapper<PaymentTransactionInfoPlugin> {

    @Override
    public MonerisPaymentTransactionInfoPlugin map(final int index, final ResultSet r, final StatementContext ctx) throws SQLException {
        final String kbPaymentId = r.getString("kb_payment_id");
        final String kbTransactionId = r.getString("kb_transaction_id");
        final String currency = r.getString("currency");
        final String receiptIsVisaDebit = r.getString("receipt_is_visa_debit");
        final String receiptStatusMessage = r.getString("receipt_status_message");
        final String receiptStatusCode = r.getString("receipt_status_code");
        final String receiptCavvResultCode = r.getString("receipt_cavv_result_code");
        final String receiptCvdResultCode = r.getString("receipt_cvd_result_code");
        final String receiptAvsResultCode = r.getString("receipt_avs_result_code");
        final String receiptRecurSuccess = r.getString("receipt_recur_success");
        final String receiptTicket = r.getString("receipt_ticket");
        final String receiptTimedOut = r.getString("receipt_timed_out");
        final String receiptTxnNumber = r.getString("receipt_txn_number");
        final String receiptCardType = r.getString("receipt_card_type");
        final String receiptTransAmount = r.getString("receipt_trans_amount");
        final String receiptMessage = r.getString("receipt_message");
        final String receiptComplete = r.getString("receipt_complete");
        final String receiptTransType = r.getString("receipt_trans_type");
        final String receiptTransDate = r.getString("receipt_trans_date");
        final String receiptTransTime = r.getString("receipt_trans_time");
        final String receiptAuthCode = r.getString("receipt_auth_code");
        final String receiptIso = r.getString("receipt_iso");
        final String receiptResponseCode = r.getString("receipt_response_code");
        final String receiptReferenceNum = r.getString("receipt_reference_num");
        final String receiptReceiptId = r.getString("receipt_receipt_id");

        return new MonerisPaymentTransactionInfoPlugin(kbPaymentId == null ? null : UUID.fromString(kbPaymentId),
                                                       kbTransactionId == null ? null : UUID.fromString(kbTransactionId),
                                                       currency == null ? null : Currency.valueOf(currency),
                                                       receiptIsVisaDebit,
                                                       receiptStatusMessage,
                                                       receiptStatusCode,
                                                       receiptCavvResultCode,
                                                       receiptCvdResultCode,
                                                       receiptAvsResultCode,
                                                       receiptRecurSuccess,
                                                       receiptTicket,
                                                       receiptTimedOut,
                                                       receiptTxnNumber,
                                                       receiptCardType,
                                                       receiptTransAmount,
                                                       receiptMessage,
                                                       receiptComplete,
                                                       receiptTransType,
                                                       receiptTransDate,
                                                       receiptTransTime,
                                                       receiptAuthCode,
                                                       receiptIso,
                                                       receiptResponseCode,
                                                       receiptReferenceNum,
                                                       receiptReceiptId);
    }
}
