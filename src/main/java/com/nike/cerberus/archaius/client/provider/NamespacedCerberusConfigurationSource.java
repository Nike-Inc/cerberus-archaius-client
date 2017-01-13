/*
 * Copyright (c) 2016 Nike, Inc.
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
import com.netflix.config.PollResult;
import com.netflix.config.PolledConfigurationSource;
import com.nike.vault.client.VaultClient;
import com.nike.vault.client.model.VaultListResponse;
import com.nike.vault.client.model.VaultResponse;
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
     * @param vaultClient An already configured vault client.  May not be null.
     * @param paths Set containing vault paths where configuration is stored.  May not be null.
     * @throws IllegalArgumentException if vaultClient is null or if paths is null/empty
     */
    public NamespacedCerberusConfigurationSource(final VaultClient vaultClient, final Set<String> paths) {
        super(vaultClient, paths);
    }

    /**
     * Constructor that accepts var args for paths
     * @param vaultClient An already configured vault client.  May not be null.
     * @param paths One or more vault paths where configuration is stored.  May not be null.
     * @throws IllegalArgumentException If vaultClient is null or if paths is null/empty
     */
    public NamespacedCerberusConfigurationSource(final VaultClient vaultClient, final String... paths) {
        super(vaultClient, paths);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PollResult poll(final boolean initial, final Object checkPoint) {
        final Map<String, Object> config = Maps.newHashMap();
        for (final String path : getPaths()) {
            config.putAll(buildEntriesMap(path));
        }
        return PollResult.createFull(config);
    }

    /**
     * Returns true if a path meets the definition of a folder according to vault's convention (e.g. path ends with a
     * forward slash).
     *
     * See https://www.vaultproject.io/docs/secrets/generic/index.html under the List API documentation for details as
     * to Vault's convention
     *
     * @param path - The vault path we are checking to determine if it is a folder or a leaf.
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
     * Traverses the vault path from the provided path down through it's leaves. Places all properties into the map that
     * will be used to populate the Archaius configuration.
     *
     * @param path - The parent path, all properties under this path will be populated.
     * @return - A map containing all the properties contained under the parent path.
     */
    private Map<String, Object> buildEntriesMap(final String path) {
        final Map<String, Object> config = Maps.newHashMap();
        if (isFolder(path)) {
            final VaultListResponse listResponse = getVaultClient().list(path);
            for(final String subpath : listResponse.getKeys()) {
                final String fullPath = path + subpath;
                config.putAll(buildEntriesMap(fullPath));
            }
        } else {
            final VaultResponse vaultResponse = getVaultClient().read(path);
            final Map<String, String> dataFromVault = vaultResponse.getData();
            for (final Map.Entry<String, String> pair : dataFromVault.entrySet()) {
                config.put(getPathPrefix(path) + pair.getKey(), pair.getValue());
            }
        }
        return config;
    }
}