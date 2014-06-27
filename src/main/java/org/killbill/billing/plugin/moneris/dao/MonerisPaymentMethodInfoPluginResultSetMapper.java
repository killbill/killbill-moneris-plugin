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

import org.killbill.billing.payment.plugin.api.PaymentMethodInfoPlugin;
import org.killbill.billing.plugin.moneris.MonerisPaymentMethodInfoPlugin;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

class MonerisPaymentMethodInfoPluginResultSetMapper implements ResultSetMapper<PaymentMethodInfoPlugin> {

    @Override
    public PaymentMethodInfoPlugin map(final int index, final ResultSet r, final StatementContext ctx) throws SQLException {
        final String kbAccountId = r.getString("kb_account_id");
        final String kbPaymentMethodId = r.getString("kb_payment_method_id");
        final String externalPaymentMethodId = r.getString("external_payment_method_id");

        return new MonerisPaymentMethodInfoPlugin(kbAccountId == null ? null : UUID.fromString(kbAccountId),
                                                  kbPaymentMethodId == null ? null : UUID.fromString(kbPaymentMethodId),
                                                  externalPaymentMethodId);
    }
}
