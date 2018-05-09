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

package com.nike.cerberus.archaius.client.provider;

import com.google.common.collect.Maps;
import com.netflix.config.ConcurrentMapConfiguration;
import com.netflix.config.PollResult;
import com.netflix.config.PolledConfigurationSource;
import com.nike.cerberus.client.CerberusClient;
import com.nike.cerberus.client.model.CerberusListResponse;
import com.nike.cerberus.client.model.CerberusResponse;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link PolledConfigurationSource} that will recursively traverse one or more
 * full Cerberus safe deposit boxes.
 *
 * Properties will be available in Archaius in the convention of the full path with each node separated with a period
 * with the property name being appended to the end
 *
 * Ex. app/myApplication/testPath/myProperty will be available as app.myApplication.testPath.myProperty
 */
public class NamespacedCerberusConfigurationSource extends BaseCerberusConfigurationSource {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Constructor that accepts a Set&lt;String&gt; for paths
     * @param cerberusClient An already configured cerberus client.  May not be null.
     * @param paths Set containing cerberus paths where configuration is stored.  May not be null.
     * @throws IllegalArgumentException if cerberusClient is null or if paths is null/empty
     */
    public NamespacedCerberusConfigurationSource(final CerberusClient cerberusClient, final Set<String> paths) {
        super(cerberusClient, paths);
    }

    /**
     * Constructor that accepts var args for paths
     * @param cerberusClient An already configured cerberus client.  May not be null.
     * @param paths One or more cerberus paths where configuration is stored.  May not be null.
     * @throws IllegalArgumentException If cerberusClient is null or if paths is null/empty
     */
    public NamespacedCerberusConfigurationSource(final CerberusClient cerberusClient, final String... paths) {
        super(cerberusClient, paths);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PollResult poll(final boolean initial, final Object checkPoint) {
        logger.debug("poll() initial={}", initial);

        final Map<String, Object> config = getMap();
        return PollResult.createFull(config);
    }

    /**
     * Returns config pulled from Cerberus, keyed using the full path to the property.
     * @return Cerberus config
     */
    public AbstractConfiguration getConfig() {
        return new ConcurrentMapConfiguration(getMap());
    }

    /**
     * Returns true if a path meets the definition of a folder according to cerberus's convention (e.g. path ends with a
     * forward slash).
     *
     * See https://www.cerberusproject.io/docs/secrets/generic/index.html under the List API documentation for details as
     * to Cerberus's convention
     *
     * @param path - The cerberus path we are checking to determine if it is a folder or a leaf.
     * @return A boolean value, true if the path is a folder.
     */
    private boolean isFolder(final String path) {
        return StringUtils.endsWith(path, "/");
    }

    /**
     * Given a path, replaces the "/" with "." to match standard property name notation for Java.
     *
     * Example: shared/foo/bar becomes shared.foo.bar
     *
     * @param path Path to do replacement on
     * @return The modified path
     */
    private String getPathPrefix(final String path) {
        String pathPrefix = StringUtils.replace(path, "/", ".");

        if (!StringUtils.endsWith(pathPrefix, ".")) {
            pathPrefix += ".";
        }

        return pathPrefix;
    }

    /**
     * Traverses the cerberus path from the provided path down through it's leaves. Places all properties into the map that
     * will be used to populate the Archaius configuration.
     *
     * @param path - The parent path, all properties under this path will be populated.
     * @return - A map containing all the properties contained under the parent path.
     */
    private Map<String, Object> buildEntriesMap(final String path) {
        final Map<String, Object> config = Maps.newHashMap();
        if (isFolder(path)) {
            final CerberusListResponse listResponse = getCerberusClient().list(path);
            for(final String subpath : listResponse.getKeys()) {
                final String fullPath = path + subpath;
                config.putAll(buildEntriesMap(fullPath));
            }
        } else {
            final CerberusResponse cerberusResponse = getCerberusClient().read(path);
            final Map<String, String> dataFromCerberus = cerberusResponse.getData();
            for (final Map.Entry<String, String> pair : dataFromCerberus.entrySet()) {
                config.put(getPathPrefix(path) + pair.getKey(), pair.getValue());
            }
        }
        return config;
    }

    private Map<String, Object> getMap() {
        final Map<String, Object> config = Maps.newHashMap();
        for (final String path : getPaths()) {
            logger.debug("poll: reading cerberus path '{}'...", path);
            config.putAll(buildEntriesMap(path));
        }
        return config;
    }
}