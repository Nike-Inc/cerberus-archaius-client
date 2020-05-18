/*
 * Copyright (c) 2020 Nike, Inc.
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
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import org.apache.commons.configuration.AbstractConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class for resolving the Cerberus URL via Archaius. */
public class ArchaiusCerberusUrlResolver {
    public static final String CERBERUS_REGION_ENV_PROPERTY = "CERBERUS_REGION";

    public static final String CERBERUS_REGION_SYS_PROPERTY = "cerberus.region";

    public static final String CERBERUS_ADDR_ENV_PROPERTY = "CERBERUS_ADDR";

    public static final String CERBERUS_ADDR_SYS_PROPERTY = "cerberus.addr";

    private static final String INVALID_PROPERY_VALUE_TEMPLATE =
            "Could not find a valid value from the property %s";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Get a valid region name or null given a possible region name
     *
     * @param maybeRegionName
     * @return null or maybeRegionName
     */
    @Nullable
    private String getValidRegionName(String maybeRegionName) {
        try {
            Regions.fromName(maybeRegionName);
            return maybeRegionName;
        } catch (IllegalArgumentException exc) {
            logger.warn(String.format("%s is not a valid region name", maybeRegionName));
        }
        return null;
    }

    /**
     * Get a valid url or null given a possible url string
     *
     * @param maybeUrl
     * @return null or maybeRegion
     */
    @Nullable
    private String getValidUrl(String maybeUrl) {
        if (HttpUrl.parse(maybeUrl) != null) {
            return maybeUrl;
        } else {
            logger.warn(String.format("%s is not a valid URL", maybeUrl));
        }
        return null;
    }

    /**
     * Attempt to get a valid url from testProperty
     *
     * @param testProperty is the name of the property to check
     * @param configuration is the configuration maybe containing testProperty
     * @return valid url or null
     */
    @Nullable
    private String checkPropertyUrl(String testProperty, AbstractConfiguration configuration) {
        final String testUrl = configuration.getString(testProperty);
        if (testUrl == null) {
            logger.warn(String.format("No value for %s, skipping check", testProperty));
        } else if (getValidUrl(testUrl) != null) {
            return testUrl;
        } else {
            logger.warn(String.format(INVALID_PROPERY_VALUE_TEMPLATE, testProperty));
        }
        return null;
    }

    /**
     * Attempts to acquire the Cerberus URL from Archaius.
     *
     * @param configuration an AbstractConfiguration used to get properties used to get the Cerberus
     *     url from either the environment or system properties
     * @return Cerberus URL
     */
    @Nullable
    public String resolveUrl(AbstractConfiguration configuration) {
        final String envUrl = checkPropertyUrl(CERBERUS_ADDR_ENV_PROPERTY, configuration);
        if (envUrl != null) {
            return envUrl;
        }

        final String sysUrl = checkPropertyUrl(CERBERUS_ADDR_SYS_PROPERTY, configuration);
        if (sysUrl != null) {
            return sysUrl;
        }

        logger.warn("Unable to resolve the Cerberus URL.");
        return null;
    }

    /**
     * Attempt to get a valid region name
     *
     * @param testProperty is the name of the property to check
     * @param configuration is the configuration maybe containing testProperty
     * @return valid region name or null
     */
    @Nullable
    private String checkPropertyRegionName(
            String testProperty, AbstractConfiguration configuration) {
        final String testRegionName = configuration.getString(testProperty);
        if (testRegionName == null) {
            logger.warn(String.format("No value for %s, skipping check", testProperty));
        } else if (getValidRegionName(testRegionName) != null) {
            return testRegionName;
        } else {
            logger.warn(String.format(INVALID_PROPERY_VALUE_TEMPLATE, testProperty));
        }
        return null;
    }

    /**
     * Attempts to acquire Cerberus region from Archaius.
     *
     * @param configuration an AbstractConfiguration used to get properties used to get region names
     *     from either the environment or system properties
     * @return Cerberus region
     */
    public String resolveRegion(AbstractConfiguration configuration) {
        final String envRegion =
                checkPropertyRegionName(CERBERUS_REGION_ENV_PROPERTY, configuration);
        if (envRegion != null) {
            return envRegion;
        }

        final String sysRegion =
                checkPropertyRegionName(CERBERUS_REGION_SYS_PROPERTY, configuration);
        if (sysRegion != null) {
            return sysRegion;
        }

        logger.warn("Unable to get the Cerberus region.");
        return null;
    }
}
