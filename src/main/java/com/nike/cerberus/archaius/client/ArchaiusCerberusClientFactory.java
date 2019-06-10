/*
 * Copyright (c) 2019 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.archaius.client;

import com.amazonaws.regions.Regions;
import com.netflix.config.ConfigurationManager;
import com.nike.cerberus.client.auth.DefaultCerberusCredentialsProviderChain;
import com.nike.cerberus.client.CerberusClient;
import com.nike.cerberus.client.CerberusClientFactory;
import okhttp3.HttpUrl;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Client factory for creating a Cerberus client
 */
public class ArchaiusCerberusClientFactory {

    private static final String CERBERUS_ADDR_ENV_PROPERTY = "CERBERUS_ADDR";

    private static final String CERBERUS_ADDR_SYS_PROPERTY = "cerberus.addr";

    private static final String CERBERUS_REGION_ENV_PROPERTY = "CERBERUS_REGION";

    private static final String CERBERUS_REGION_SYS_PROPERTY = "cerberus.region";

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchaiusCerberusClientFactory.class);

    /**
     * Creates a new {@link CerberusClient} with the {@link DefaultCerberusCredentialsProviderChain}.
     *
     * @return Cerberus client
     */
    public static CerberusClient getClient() {
        final Map<String, String> defaultHeaders = new HashMap<>();
        final String xCerberusClientHeaderValue = ClientVersion.getClientHeaderValue();
        defaultHeaders.put(ClientVersion.CERBERUS_CLIENT_HEADER, xCerberusClientHeaderValue);

        return CerberusClientFactory.getClient(
                resolveUrl(),
                // pass the client HTTP header value to be used in authenticate calls to Cerberus
                new DefaultCerberusCredentialsProviderChain(resolveUrl(), getRegion(), xCerberusClientHeaderValue),
                // pass the client header to be used in all other calls to Cerberus (e.g. read, write, etc.)
                defaultHeaders);
    }

    /**
     * Attempts to acquire Cerberus URL from Archaius.
     *
     * @return Cerberus URL
     */
    private static String resolveUrl() {
        final AbstractConfiguration configuration = ConfigurationManager.getConfigInstance();
        final String envUrl = configuration.getString(CERBERUS_ADDR_ENV_PROPERTY);
        final String sysUrl = configuration.getString(CERBERUS_ADDR_SYS_PROPERTY);

        if (StringUtils.isNotBlank(envUrl) && HttpUrl.parse(envUrl) != null) {
            return envUrl;
        } else if (StringUtils.isNotBlank(sysUrl) && HttpUrl.parse(sysUrl) != null) {
            return sysUrl;
        } else {
            LOGGER.warn("Unable to resolve the Cerberus URL.");
            throw new IllegalArgumentException("Cerberus Address property is not configured correctly.");
        }
    }

    /**
     * Attempts to acquire Cerberus region from Archaius.
     *
     * @return Cerberus region
     */
    private static String getRegion() {
        final AbstractConfiguration configuration = ConfigurationManager.getConfigInstance();
        final String envRegion = configuration.getString(CERBERUS_REGION_ENV_PROPERTY);
        final String sysRegion = configuration.getString(CERBERUS_REGION_SYS_PROPERTY);

        if (StringUtils.isNotBlank(envRegion) && Regions.fromName(envRegion) != null) {
            return envRegion;
        } else if (StringUtils.isNotBlank(sysRegion) && Regions.fromName(sysRegion) != null) {
            return sysRegion;
        } else {
            LOGGER.warn("Unable to get the Cerberus region.");
            throw new IllegalArgumentException("Cerberus Region property is not configured correctly.");
        }
    }
}
