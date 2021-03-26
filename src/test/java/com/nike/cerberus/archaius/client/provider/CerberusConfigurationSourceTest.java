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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.netflix.config.PollResult;
import com.nike.cerberus.client.CerberusClient;
import com.nike.cerberus.client.CerberusServerException;
import com.nike.cerberus.client.model.CerberusResponse;
import java.util.*;
import org.apache.commons.configuration.AbstractConfiguration;
import org.junit.Before;
import org.junit.Test;

/** Test class for CerberusConfigurationSource */
public class CerberusConfigurationSourceTest {

    private CerberusConfigurationSource subject;

    private CerberusClient cerberusClient;

    private static final String PATH_1 = "app/foobinator/config";

    private static final String PATH_2 = "shared/artemis/architecture/config";

    private static final String FOOBINATOR_CONFIG_KEY = "jdbcPassword";

    private static final String FOOBINATOR_CONFIG_VALUE = "password123";

    private static final String ARTEMIS_CONFIG_KEY = "apiKey";

    private static final String ARTEMIS_CONFIG_VALUE = "123abc";

    @Before
    public void setup() {
        cerberusClient = mock(CerberusClient.class);
        subject = new CerberusConfigurationSource(cerberusClient, PATH_1, PATH_2);
    }

    @Test
    public void poll_loads_both_paths_successfully() {
        // mock dependencies
        final Map<String, String> foobinatorMap = new HashMap<>();
        foobinatorMap.put(FOOBINATOR_CONFIG_KEY, FOOBINATOR_CONFIG_VALUE);

        final Map<String, String> artemisMap = new HashMap<>();
        artemisMap.put(ARTEMIS_CONFIG_KEY, ARTEMIS_CONFIG_VALUE);

        final CerberusResponse foobinatorResponse = new CerberusResponse().setData(foobinatorMap);
        final CerberusResponse artemisResponse = new CerberusResponse().setData(artemisMap);

        when(cerberusClient.read(PATH_1)).thenReturn(foobinatorResponse);
        when(cerberusClient.read(PATH_2)).thenReturn(artemisResponse);

        // call the method under test
        PollResult result = subject.poll(true, null);

        // verify results
        assertThat(result).isNotNull();
        assertThat(result.getComplete()).isNotNull();
        assertThat(result.getComplete())
                .containsOnlyKeys(FOOBINATOR_CONFIG_KEY, ARTEMIS_CONFIG_KEY);

        AbstractConfiguration config = subject.getConfig();
        assertThat(config).isNotNull();
        assertThat(config.getKeys()).containsOnly(FOOBINATOR_CONFIG_KEY, ARTEMIS_CONFIG_KEY);
        assertThat(config.getString(ARTEMIS_CONFIG_KEY)).isEqualTo(ARTEMIS_CONFIG_VALUE);
        assertThat(config.getString(FOOBINATOR_CONFIG_KEY)).isEqualTo(FOOBINATOR_CONFIG_VALUE);
    }

    @Test(expected = CerberusServerException.class)
    public void poll_only_loads_data_for_path1() {
        // mock dependencies
        final Map<String, String> foobinatorMap = new HashMap<>();
        foobinatorMap.put(FOOBINATOR_CONFIG_KEY, FOOBINATOR_CONFIG_VALUE);
        final CerberusResponse foobinatorResponse = new CerberusResponse().setData(foobinatorMap);
        when(cerberusClient.read(PATH_1)).thenReturn(foobinatorResponse);

        when(cerberusClient.read(PATH_2))
                .thenThrow(
                        new CerberusServerException(
                                500, Collections.singletonList("Internal error.")));

        // call the method under test
        subject.poll(true, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_validation_cerberus_client_cannot_be_null() {
        new CerberusConfigurationSource(
                null, new HashSet<>(Collections.singletonList("/fake/path")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_validation_paths_cannot_be_null() {
        new CerberusConfigurationSource(cerberusClient, (Set<String>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_validation_paths_cannot_be_empty() {
        new CerberusConfigurationSource(cerberusClient, new HashSet<>());
    }
}
