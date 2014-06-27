/*
 * Copyright 2014 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
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

import java.util.Dictionary;
import java.util.Hashtable;

import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.plugin.moneris.dao.MonerisDao;
import org.killbill.killbill.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;
import org.osgi.framework.BundleContext;

public class MonerisActivator extends KillbillActivatorBase {

    public static final String PLUGIN_NAME = "killbill-moneris";

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        // Configuration system properties
        final String host = configProperties.getString("org.killbill.billing.plugin.moneris.host");
        final String storeId = configProperties.getString("org.killbill.billing.plugin.moneris.storeId");
        final String apiToken = configProperties.getString("org.killbill.billing.plugin.moneris.apiToken");

        final MonerisDao monerisDao = new MonerisDao(dataSource.getDataSource());
        final PaymentPluginApi paymentPluginApi = new MonerisPaymentPluginApi(host, storeId, apiToken, monerisDao, logService);
        registerPaymentPluginApi(context, paymentPluginApi);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
    }

    @Override
    public OSGIKillbillEventHandler getOSGIKillbillEventHandler() {
        return null;
    }

    private void registerPaymentPluginApi(final BundleContext context, final PaymentPluginApi api) {
        final Dictionary props = new Hashtable();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, PaymentPluginApi.class, api, props);
    }
}
