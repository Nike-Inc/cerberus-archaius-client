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

import com.netflix.config.ConcurrentMapConfiguration;
import com.netflix.config.PollResult;
import com.netflix.config.PolledConfigurationSource;
import com.nike.cerberus.client.CerberusClient;
import com.nike.cerberus.client.model.CerberusResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link PolledConfigurationSource} that reads configuration from one or more
 * paths in Cerberus
 */
public class CerberusConfigurationSource extends BaseCerberusConfigurationSource {

    private static final Logger logger = LoggerFactory.getLogger(CerberusConfigurationSource.class);

    /**
     * Constructor that accepts a Set&lt;String&gt; for paths.
     *
     * @param cerberusClient Instance of {@link CerberusClient}
     * @param paths Set&lt;String&gt; containing cerberus paths where configuration is stored
     * @throws IllegalArgumentException if cerberusClient is null or if paths is null/empty
     */
    public CerberusConfigurationSource(
            final CerberusClient cerberusClient, final Set<String> paths) {
        super(cerberusClient, paths);
    }

    /**
     * Constructor that accepts var args for paths.
     *
     * @param cerberusClient Instance of {@link CerberusClient}
     * @param paths one or more cerberus paths where configuration is stored
     * @throws IllegalArgumentException if cerberusClient is null or if paths is null/empty
     */
    public CerberusConfigurationSource(final CerberusClient cerberusClient, final String... paths) {
        super(cerberusClient, paths);
    }

    /** {@inheritDoc} */
    @Override
    public PollResult poll(final boolean initial, final Object checkPoint) {
        logger.debug("poll() initial={}", initial);
        final Map<String, Object> config = getMap();
        logger.info("poll() successfully read {} keys from Cerberus", config.size());
        return PollResult.createFull(getMap());
    }

    /**
     * Returns config pulled from Cerberus, keyed exactly the same way they are in Cerberus.
     *
     * @return Cerberus config
     */
    public ConcurrentMapConfiguration getConfig() {
        return new ConcurrentMapConfiguration(getMap());
    }

    private Map<String, Object> getMap() {
        final Map<String, Object> config = new HashMap<>();
        for (final String path : getPaths()) {
            logger.debug("poll: reading cerberus path '{}'...", path);
            final CerberusResponse cerberusResponse = getCerberusClient().read(path);
            config.putAll(cerberusResponse.getData());
        }
        return config;
    }
}
