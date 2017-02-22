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

import com.google.common.collect.Sets;
import com.netflix.config.PolledConfigurationSource;
import com.nike.vault.client.VaultClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Base class for Cerberus configuration sources.
 */
public abstract class BaseCerberusConfigurationSource implements PolledConfigurationSource {

    private final VaultClient vaultClient;

    private final Set<String> paths;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Constructor that accepts a Set&lt;String&gt; for paths.
     *
     * @param vaultClient Instance of {@link VaultClient}
     * @param paths Set&lt;String&gt; containing vault paths where configuration is stored
     * @throws IllegalArgumentException if vaultClient is null or if paths is null/empty
     */
    public BaseCerberusConfigurationSource(VaultClient vaultClient, Set<String> paths) {
        super();
        if (vaultClient == null) {
            throw new IllegalArgumentException("vaultClient cannot be null");
        }
        if (paths == null || paths.isEmpty()) {
            throw new IllegalArgumentException("paths cannot be null or empty");
        }
        this.vaultClient = vaultClient;
        this.paths = Sets.newHashSet(paths);
        logger.info("paths={}",  getPaths());
    }

    /**
     * Constructor that accepts var args for paths.
     *
     * @param vaultClient Instance of {@link VaultClient}
     * @param paths one or more vault paths where configuration is stored
     * @throws IllegalArgumentException if vaultClient is null or if paths is null/empty
     */
    public BaseCerberusConfigurationSource(VaultClient vaultClient, String... paths) {
        super();
        if (vaultClient == null) {
            throw new IllegalArgumentException("vaultClient cannot be null");
        }
        if (paths == null || paths.length == 0) {
            throw new IllegalArgumentException("paths cannot be null or empty");
        }
        this.vaultClient = vaultClient;
        this.paths = Sets.newHashSet(paths);
        logger.info("paths={}", getPaths());
    }

    public VaultClient getVaultClient() {
        return vaultClient;
    }

    public Set<String> getPaths() {
        return paths;
    }
}
