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

import java.util.HashMap;

import javax.annotation.Nullable;

import org.killbill.billing.payment.api.PluginProperty;

import JavaAPI.AvsInfo;
import JavaAPI.CvdInfo;

/**
 * This class contains the magic Strings to use when passing plugin properties.
 */
public class MonerisProperties extends HashMap<String, String> {

    public MonerisProperties(final Iterable<PluginProperty> properties) {
        super();

        for (final PluginProperty property : properties) {
            if (property.getValue() != null) {
                put(property.getKey(), property.getValue().toString());
            }
        }
    }

    public AvsInfo getAvsInfo() {
        final AvsInfo avs = new AvsInfo();

        avs.setAvsStreetNumber(get("avsStreetNumber"));
        avs.setAvsStreetName(get("avsStreetName"));
        avs.setAvsZipcode(get("avsZipcode"));
        avs.setAvsEmail(get("avsEmail"));
        avs.setAvsHostname(get("avsHostname"));
        avs.setAvsBrowser(get("avsBrowser"));
        avs.setAvsShiptoCountry(get("avsShiptoCountry"));
        avs.setAvsShipMethod(get("avsShipMethod"));
        avs.setAvsMerchProdSku(get("avsMerchProdSku"));
        avs.setAvsCustIp(get("avsCustIp"));
        avs.setAvsCustPhone(get("avsCustPhone"));

        return avs;
    }

    public CvdInfo getCvdInfo() {
        return new CvdInfo(get("cvdIndicator"), get("cvdValue"));
    }

    public String getPan() {
        // Credit Card Number - no spaces or dashes.
        // Most credit card numbers today are 16 digits in length but some 13 digits are still accepted by some issuers.
        // This field has been intentionally expanded to 20 digits in consideration for future expansion and/or potential
        // support of private label card ranges.
        return get("pan");
    }

    public String getExpDate() {
        // Expiry Date - format YYMM no spaces or slashes.
        // PLEASE NOTE THAT THIS IS REVERSED FROM THE DATE DISPLAYED ON THE PHYSICAL CARD WHICH IS MMYY
        return get("expDate");
    }

    public String getCrypt() {
        // E-Commerce Indicator:
        // 1 - Mail Order / Telephone Order - Single
        // 2 - Mail Order / Telephone Order - Recurring
        // 3 - Mail Order / Telephone Order - Instalment
        // 4 - Mail Order / Telephone Order - Unknown Classification 5 - Authenticated E-commerce Transaction (VBV)
        // 6 – Non Authenticated E-commerce Transaction (VBV)
        // 7 - SSL enabled merchant
        // 8 - Non Secure Transaction (Web or Email Based)
        // 9 - SET non - Authenticated transaction
        return get("crypt");
    }

    public String getDynamicDescriptor(@Nullable final String defaultDynamicDescriptor) {
        // Merchant defined description sent on a per-transaction basis that will appear on the credit card statement.
        // Dependent on the card Issuer, the statement will typically show the dynamic desciptor appended to the merchant's
        // existing business name separated by the "/" character. Please note that the combined length of the merchant's
        // business name, forward slash "/" character, and the dynamic descriptor may not exceed 22 characters.
        // -Example-
        // Existing Business Name: ABC Painting
        // Dynamic Descriptor: Booking 12345
        // Cardholder Statement Displays: ABC Painting/Booking 1
        final String dynamicDescriptor = get("dynamicDescriptor");
        return dynamicDescriptor == null ? defaultDynamicDescriptor : dynamicDescriptor;
    }
}
