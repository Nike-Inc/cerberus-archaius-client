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

package com.nike.cerberus.archaius.client;

import com.netflix.config.ConfigurationManager;
import com.nike.cerberus.client.CerberusClient;
import com.nike.cerberus.client.CerberusClientException;
import com.nike.cerberus.client.CerberusClientFactory;
import com.nike.cerberus.client.auth.DefaultCerberusCredentialsProviderChain;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.configuration.AbstractConfiguration;

/**
 * Client factory for creating a Cerberus client with URL resolved using the
 * ArchaiusCerberusUrlResolver
 */
public class ArchaiusCerberusClientFactory {

  /**
   * Resolves the Cerberus/Cerberus URL via the {@link ArchaiusCerberusUrlResolver} and creates a
   * new {@link CerberusClient} with the {@link DefaultCerberusCredentialsProviderChain}.
   *
   * @return Cerberus client
   */
  public static CerberusClient getClient() {
    return getClient(null, null);
  }

  /**
   * Resolves the Cerberus/Cerberus URL via the {@link ArchaiusCerberusUrlResolver} and creates a
   * new {@link CerberusClient} with the {@link DefaultCerberusCredentialsProviderChain}.
   *
   * @param aur optional ArchaiusCerbersUrlResolver
   * @param configuration optional AbstractConfiguration
   * @return Cerberus client
   */
  public static CerberusClient getClient(
      ArchaiusCerberusUrlResolver aur, AbstractConfiguration configuration) {
    if (aur == null) {
      aur = new ArchaiusCerberusUrlResolver();
    }

    if (configuration == null) {
      configuration = ConfigurationManager.getConfigInstance();
    }

    final Map<String, String> defaultHeaders = new HashMap<>();
    final String xCerberusClientHeaderValue = ClientVersion.getClientHeaderValue();
    defaultHeaders.put(ClientVersion.CERBERUS_CLIENT_HEADER, xCerberusClientHeaderValue);

    final String url = aur.resolveUrl(configuration);
    final String region = aur.resolveRegion(configuration);

    if (url == null || region == null) {
      throw new CerberusClientException("Missing url or region");
    }

    final DefaultCerberusCredentialsProviderChain dccpc;
    dccpc = new DefaultCerberusCredentialsProviderChain(url, region, xCerberusClientHeaderValue);

    return CerberusClientFactory.getClient(url, dccpc, defaultHeaders);
  }
}
