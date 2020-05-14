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
import com.nike.cerberus.client.model.CerberusListResponse;
import com.nike.cerberus.client.model.CerberusResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.configuration.AbstractConfiguration;
import org.junit.Before;
import org.junit.Test;

/** Test class for NamespacedCerberusConfigurationSource */
public class NamespacedCerberusConfigurationSourceTest {

  private NamespacedCerberusConfigurationSource subject;

  private CerberusClient cerberusClient;

  private static final String PATH_1 = "app/foobinator/";

  private static final String PATH_1_SUBPATH_1 = "properties";

  private static final String PATH_2 = "shared/artemis/";

  private static final String PATH_2_SUBPATH_1 = "architecture/";

  private static final String PATH_2_SUBPATH_2 = "config";

  private static final String FOOBINATOR_CONFIG_KEY = "jdbcPassword";

  private static final String FOOBINATOR_CONFIG_VALUE = "password123";

  private static final String FOOBINATOR_CONFIG_NAMESPACED_KEY =
      "app.foobinator.properties.jdbcPassword";

  private static final String ARTEMIS_CONFIG_KEY = "apiKey";

  private static final String ARTEMIS_CONFIG_VALUE = "123abc";

  private static final String ARTEMIS_CONFIG_NAMESPACED_KEY =
      "shared.artemis.architecture.config.apiKey";

  @Before
  public void setup() {
    cerberusClient = mock(CerberusClient.class);
    subject = new NamespacedCerberusConfigurationSource(cerberusClient, PATH_1, PATH_2);
  }

  @Test
  public void poll_successfully_reads_all_paths() {
    // mock dependencies
    final CerberusListResponse path1ListResponse =
        new CerberusListResponse().setKeys(Collections.singletonList(PATH_1_SUBPATH_1));
    when(cerberusClient.list(PATH_1)).thenReturn(path1ListResponse);
    final Map<String, String> path1Map = new HashMap<>();
    path1Map.put(FOOBINATOR_CONFIG_KEY, FOOBINATOR_CONFIG_VALUE);
    final CerberusResponse path1Response = new CerberusResponse().setData(path1Map);
    when(cerberusClient.read(PATH_1 + PATH_1_SUBPATH_1)).thenReturn(path1Response);

    final CerberusListResponse path2FirstListResponse =
        new CerberusListResponse().setKeys(Collections.singletonList(PATH_2_SUBPATH_1));
    when(cerberusClient.list(PATH_2)).thenReturn(path2FirstListResponse);
    final CerberusListResponse path2SecondListResponse =
        new CerberusListResponse().setKeys(Collections.singletonList(PATH_2_SUBPATH_2));
    when(cerberusClient.list(PATH_2 + PATH_2_SUBPATH_1)).thenReturn(path2SecondListResponse);
    final Map<String, String> path2Map = new HashMap<>();
    path2Map.put(ARTEMIS_CONFIG_KEY, ARTEMIS_CONFIG_VALUE);
    final CerberusResponse path2Response = new CerberusResponse().setData(path2Map);
    when(cerberusClient.read(PATH_2 + PATH_2_SUBPATH_1 + PATH_2_SUBPATH_2))
        .thenReturn(path2Response);

    // call the method under test
    PollResult result = subject.poll(true, null);

    // verify results
    assertThat(result).isNotNull();
    assertThat(result.getComplete()).isNotNull();
    assertThat(result.getComplete())
        .containsOnlyKeys(FOOBINATOR_CONFIG_NAMESPACED_KEY, ARTEMIS_CONFIG_NAMESPACED_KEY);

    AbstractConfiguration config = subject.getConfig();
    assertThat(config).isNotNull();
    assertThat(config.getKeys())
        .containsOnly(FOOBINATOR_CONFIG_NAMESPACED_KEY, ARTEMIS_CONFIG_NAMESPACED_KEY);
    assertThat(config.getString(ARTEMIS_CONFIG_NAMESPACED_KEY)).isEqualTo(ARTEMIS_CONFIG_VALUE);
    assertThat(config.getString(FOOBINATOR_CONFIG_NAMESPACED_KEY))
        .isEqualTo(FOOBINATOR_CONFIG_VALUE);
  }

  @Test(expected = CerberusServerException.class)
  public void poll_fails_to_read_path1_but_is_successful_on_path2() {
    // mock dependencies to ensure an error
    final CerberusListResponse path1ListResponse =
        new CerberusListResponse().setKeys(Collections.singletonList(PATH_1_SUBPATH_1));
    when(cerberusClient.list(PATH_1)).thenReturn(path1ListResponse);
    when(cerberusClient.read(PATH_1 + PATH_1_SUBPATH_1))
        .thenThrow(new CerberusServerException(500, Collections.singletonList("Internal error.")));

    final CerberusListResponse path2FirstListResponse =
        new CerberusListResponse().setKeys(Collections.singletonList(PATH_2_SUBPATH_1));
    when(cerberusClient.list(PATH_2)).thenReturn(path2FirstListResponse);
    final CerberusListResponse path2SecondListResponse =
        new CerberusListResponse().setKeys(Collections.singletonList(PATH_2_SUBPATH_2));
    when(cerberusClient.list(PATH_2 + PATH_2_SUBPATH_1)).thenReturn(path2SecondListResponse);
    final Map<String, String> path2Map = new HashMap<>();
    path2Map.put(ARTEMIS_CONFIG_KEY, ARTEMIS_CONFIG_VALUE);
    final CerberusResponse path2Response = new CerberusResponse().setData(path2Map);
    when(cerberusClient.read(PATH_2 + PATH_2_SUBPATH_1 + PATH_2_SUBPATH_2))
        .thenReturn(path2Response);

    // call the method under test
    PollResult result = subject.poll(true, null);
  }
}
