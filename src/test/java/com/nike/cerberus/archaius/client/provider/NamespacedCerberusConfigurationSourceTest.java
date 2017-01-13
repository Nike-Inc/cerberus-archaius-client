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

import com.netflix.config.PollResult;
import com.nike.vault.client.VaultClient;
import com.nike.vault.client.VaultServerException;
import com.nike.vault.client.model.VaultListResponse;
import com.nike.vault.client.model.VaultResponse;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for NamespacedCerberusConfigurationSource
 */
public class NamespacedCerberusConfigurationSourceTest {

    private NamespacedCerberusConfigurationSource subject;

    private VaultClient vaultClient;

    private static final String PATH_1 = "app/foobinator/";

    private static final String PATH_1_SUBPATH_1 = "properties";

    private static final String PATH_2 = "shared/artemis/";

    private static final String PATH_2_SUBPATH_1 = "architecture/";

    private static final String PATH_2_SUBPATH_2 = "config";

    private static final String FOOBINATOR_CONFIG_KEY = "jdbcPassword";

    private static final String FOOBINATOR_CONFIG_VALUE = "password123";

    private static final String FOOBINATOR_CONFIG_NAMESPACED_KEY = "app.foobinator.properties.jdbcPassword";

    private static final String ARTEMIS_CONFIG_KEY = "apiKey";

    private static final String ARTEMIS_CONFIG_VALUE = "123abc";

    private static final String ARTEMIS_CONFIG_NAMESPACED_KEY = "shared.artemis.architecture.config.apiKey";

    @Before
    public void setup(){
        vaultClient = mock(VaultClient.class);
        subject = new NamespacedCerberusConfigurationSource(vaultClient, PATH_1, PATH_2);
    }

    @Test
    public void poll_successfully_reads_all_paths() {
        // mock dependencies
        final VaultListResponse path1ListResponse = new VaultListResponse()
                .setKeys(Collections.singletonList(PATH_1_SUBPATH_1));
        when(vaultClient.list(PATH_1)).thenReturn(path1ListResponse);
        final Map<String, String> path1Map = new HashMap<>();
        path1Map.put(FOOBINATOR_CONFIG_KEY, FOOBINATOR_CONFIG_VALUE);
        final VaultResponse path1Response = new VaultResponse().setData(path1Map);
        when(vaultClient.read(PATH_1 + PATH_1_SUBPATH_1)).thenReturn(path1Response);

        final VaultListResponse path2FirstListResponse = new VaultListResponse()
                .setKeys(Collections.singletonList(PATH_2_SUBPATH_1));
        when(vaultClient.list(PATH_2)).thenReturn(path2FirstListResponse);
        final VaultListResponse path2SecondListResponse = new VaultListResponse()
                .setKeys(Collections.singletonList(PATH_2_SUBPATH_2));
        when(vaultClient.list(PATH_2 + PATH_2_SUBPATH_1)).thenReturn(path2SecondListResponse);
        final Map<String, String> path2Map = new HashMap<>();
        path2Map.put(ARTEMIS_CONFIG_KEY, ARTEMIS_CONFIG_VALUE);
        final VaultResponse path2Response = new VaultResponse().setData(path2Map);
        when(vaultClient.read(PATH_2 + PATH_2_SUBPATH_1 + PATH_2_SUBPATH_2)).thenReturn(path2Response);

        // call the method under test
        PollResult result = subject.poll(true, null);

        // verify results
        assertThat(result).isNotNull();
        assertThat(result.getComplete()).isNotNull();
        assertThat(result.getComplete()).containsOnlyKeys(FOOBINATOR_CONFIG_NAMESPACED_KEY, ARTEMIS_CONFIG_NAMESPACED_KEY);
    }

    @Test(expected = VaultServerException.class)
    public void poll_fails_to_read_path1_but_is_successful_on_path2() {
        // mock dependencies to ensure an error
        final VaultListResponse path1ListResponse = new VaultListResponse()
                .setKeys(Collections.singletonList(PATH_1_SUBPATH_1));
        when(vaultClient.list(PATH_1)).thenReturn(path1ListResponse);
        when(vaultClient.read(PATH_1 + PATH_1_SUBPATH_1))
                .thenThrow(new VaultServerException(500, Collections.singletonList("Internal error.")));

        final VaultListResponse path2FirstListResponse = new VaultListResponse()
                .setKeys(Collections.singletonList(PATH_2_SUBPATH_1));
        when(vaultClient.list(PATH_2)).thenReturn(path2FirstListResponse);
        final VaultListResponse path2SecondListResponse = new VaultListResponse()
                .setKeys(Collections.singletonList(PATH_2_SUBPATH_2));
        when(vaultClient.list(PATH_2 + PATH_2_SUBPATH_1)).thenReturn(path2SecondListResponse);
        final Map<String, String> path2Map = new HashMap<>();
        path2Map.put(ARTEMIS_CONFIG_KEY, ARTEMIS_CONFIG_VALUE);
        final VaultResponse path2Response = new VaultResponse().setData(path2Map);
        when(vaultClient.read(PATH_2 + PATH_2_SUBPATH_1 + PATH_2_SUBPATH_2)).thenReturn(path2Response);

        // call the method under test
        PollResult result = subject.poll(true, null);
    }
}