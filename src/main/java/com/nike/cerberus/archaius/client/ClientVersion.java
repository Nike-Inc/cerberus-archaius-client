/*
 * Copyright (c) 2017 Nike, Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class to get the version of the current Cerberus Archaius Client */
public class ClientVersion {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientVersion.class);

    protected static final String CLIENT_VERSION_PROPERTY_FILE =
            "cerberus-archaius-client.properties";

    private static final String ARCHAIUS_CLIENT_VERSION_PROPERTY =
            "cerberus_archaius_client.version";

    public static final String CERBERUS_CLIENT_HEADER = "X-Cerberus-Client";

    public static final String HEADER_VALUE_PREFIX = "CerberusArchaiusClient";

    public static final String UNKNOWN = "unknown";

    public static String getVersion() {

        InputStream propsStream = null; // NOPMD
        try {
            propsStream =
                    Thread.currentThread()
                            .getContextClassLoader()
                            .getResourceAsStream(CLIENT_VERSION_PROPERTY_FILE);
            Properties properties = new Properties();
            properties.load(propsStream);
            return properties.getProperty(ARCHAIUS_CLIENT_VERSION_PROPERTY);
        } catch (Exception e) {
            LOGGER.error("Failed to load client properties file", e);
            return UNKNOWN;
        } finally {
            try {
                if (propsStream != null) {
                    propsStream.close();
                }
            } catch (IOException e) {
                LOGGER.error("Failed to close input stream", e);
            }
        }
    }

    public static String getClientHeaderValue() {
        String version = getVersion();

        try {
            String cerberusClientHeaderValue =
                    com.nike.cerberus.client.ClientVersion.getClientHeaderValue();
            return String.format(
                    "%s/%s %s", HEADER_VALUE_PREFIX, version, cerberusClientHeaderValue);
        } catch (Exception e) {
            LOGGER.error("Failed to get Cerberus Client version", e);
            return String.format("%s/%s %s", HEADER_VALUE_PREFIX, version, UNKNOWN);
        }
    }
}
