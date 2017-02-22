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
import com.nike.vault.client.model.VaultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link PolledConfigurationSource} that reads configuration
 * from one or more paths in Cerberus
 */
public class CerberusConfigurationSource extends BaseCerberusConfigurationSource {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Constructor that accepts a Set&lt;String&gt; for paths.
     *
     * @param vaultClient Instance of {@link VaultClient}
     * @param paths Set&lt;String&gt; containing vault paths where configuration is stored
     * @throws IllegalArgumentException if vaultClient is null or if paths is null/empty
     */
    public CerberusConfigurationSource(final VaultClient vaultClient, final Set<String> paths) {
        super(vaultClient, paths);
    }

    /**
     * Constructor that accepts var args for paths.
     *
     * @param vaultClient Instance of {@link VaultClient}
     * @param paths one or more vault paths where configuration is stored
     * @throws IllegalArgumentException if vaultClient is null or if paths is null/empty
     */
    public CerberusConfigurationSource(final VaultClient vaultClient, final String... paths) {
        super(vaultClient, paths);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PollResult poll(final boolean initial, final Object checkPoint) {
        logger.debug("poll() initial={}", initial);

        final Map<String, Object> config = Maps.newHashMap();
        for (final String path : getPaths()) {
            final VaultResponse vaultResponse = getVaultClient().read(path);
            config.putAll(vaultResponse.getData());
        }
        return PollResult.createFull(config);
    }

}
