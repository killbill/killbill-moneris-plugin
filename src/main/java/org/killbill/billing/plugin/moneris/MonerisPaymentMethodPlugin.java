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

import java.util.List;
import java.util.UUID;

import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.payment.api.PluginProperty;

public class MonerisPaymentMethodPlugin implements PaymentMethodPlugin {

    private final UUID kbPaymentMethodId;
    private final String externalPaymentMethodId;
    private final List<PluginProperty> properties;

    public MonerisPaymentMethodPlugin(final UUID kbPaymentMethodId, final String externalPaymentMethodId, final List<PluginProperty> properties) {
        this.kbPaymentMethodId = kbPaymentMethodId;
        this.externalPaymentMethodId = externalPaymentMethodId;
        this.properties = properties;
    }

    @Override
    public UUID getKbPaymentMethodId() {
        return kbPaymentMethodId;
    }

    @Override
    public String getExternalPaymentMethodId() {
        return externalPaymentMethodId;
    }

    @Override
    public boolean isDefaultPaymentMethod() {
        return false;
    }

    @Override
    public List<PluginProperty> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MonerisPaymentMethodPlugin{");
        sb.append("kbPaymentMethodId=").append(kbPaymentMethodId);
        sb.append(", externalPaymentMethodId='").append(externalPaymentMethodId).append('\'');
        sb.append(", properties=").append(properties);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MonerisPaymentMethodPlugin that = (MonerisPaymentMethodPlugin) o;

        if (externalPaymentMethodId != null ? !externalPaymentMethodId.equals(that.externalPaymentMethodId) : that.externalPaymentMethodId != null) {
            return false;
        }
        if (kbPaymentMethodId != null ? !kbPaymentMethodId.equals(that.kbPaymentMethodId) : that.kbPaymentMethodId != null) {
            return false;
        }
        if (properties != null ? !properties.equals(that.properties) : that.properties != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = kbPaymentMethodId != null ? kbPaymentMethodId.hashCode() : 0;
        result = 31 * result + (externalPaymentMethodId != null ? externalPaymentMethodId.hashCode() : 0);
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        return result;
    }
}
