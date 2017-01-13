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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.netflix.config.PollResult;
import com.nike.vault.client.VaultClient;
import com.nike.vault.client.VaultServerException;
import com.nike.vault.client.model.VaultResponse;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for CerberusConfigurationSource
 */
public class CerberusConfigurationSourceTest {

    private CerberusConfigurationSource subject;

    private VaultClient vaultClient;

    private static final String PATH_1 = "app/foobinator/config";

    private static final String PATH_2 = "shared/artemis/architecture/config";

    private static final String FOOBINATOR_CONFIG_KEY = "jdbcPassword";

    private static final String FOOBINATOR_CONFIG_VALUE = "password123";

    private static final String ARTEMIS_CONFIG_KEY = "apiKey";

    private static final String ARTEMIS_CONFIG_VALUE = "123abc";

    @Before
    public void setup(){
        vaultClient = mock(VaultClient.class);
        subject = new CerberusConfigurationSource(vaultClient, PATH_1, PATH_2);
    }

    @Test
    public void poll_loads_both_paths_successfully() {
        // mock dependencies
        final Map<String, String> foobinatorMap = Maps.newHashMap();
        foobinatorMap.put(FOOBINATOR_CONFIG_KEY, FOOBINATOR_CONFIG_VALUE);

        final Map<String, String> artemisMap = Maps.newHashMap();
        artemisMap.put(ARTEMIS_CONFIG_KEY, ARTEMIS_CONFIG_VALUE);

        final VaultResponse foobinatorResponse = new VaultResponse().setData(foobinatorMap);
        final VaultResponse artemisResponse = new VaultResponse().setData(artemisMap);

        when(vaultClient.read(PATH_1)).thenReturn(foobinatorResponse);
        when(vaultClient.read(PATH_2)).thenReturn(artemisResponse);

        // call the method under test
        PollResult result = subject.poll(true, null);

        // verify results
        assertThat(result).isNotNull();
        assertThat(result.getComplete()).isNotNull();
        assertThat(result.getComplete()).containsOnlyKeys(FOOBINATOR_CONFIG_KEY, ARTEMIS_CONFIG_KEY);

    }

    @Test(expected = VaultServerException.class)
    public void poll_only_loads_data_for_path1() {
        // mock dependencies
        final Map<String, String> foobinatorMap = Maps.newHashMap();
        foobinatorMap.put(FOOBINATOR_CONFIG_KEY, FOOBINATOR_CONFIG_VALUE);
        final VaultResponse foobinatorResponse = new VaultResponse().setData(foobinatorMap);
        when(vaultClient.read(PATH_1)).thenReturn(foobinatorResponse);

        when(vaultClient.read(PATH_2)).thenThrow(new VaultServerException(500, Lists.newArrayList("Internal error.")));

        // call the method under test
        subject.poll(true, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_validation_vault_client_cannot_be_null() {
        new CerberusConfigurationSource(null, Sets.newHashSet("/fake/path"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_validation_paths_cannot_be_null() {
        new CerberusConfigurationSource(vaultClient, (Set<String>)null );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_validation_paths_cannot_be_empty() {
        new CerberusConfigurationSource(vaultClient, Sets.<String>newHashSet());
    }

}
