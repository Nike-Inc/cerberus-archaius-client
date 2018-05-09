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

import com.nike.cerberus.client.auth.DefaultCerberusCredentialsProviderChain;
import com.nike.cerberus.client.CerberusClient;
import com.nike.cerberus.client.CerberusClientFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Client factory for creating a Cerberus client with URL resolved using the ArchaiusCerberusUrlResolver
 */
public class ArchaiusCerberusClientFactory {

    /**
     * Resolves the Cerberus/Cerberus URL via the {@link ArchaiusCerberusUrlResolver} and creates a new {@link CerberusClient}
     * with the {@link DefaultCerberusCredentialsProviderChain}.
     *
     * @return Cerberus client
     */
    public static CerberusClient getClient() {
        final ArchaiusCerberusUrlResolver archaiusUrlResolver = new ArchaiusCerberusUrlResolver();

        final Map<String, String> defaultHeaders = new HashMap<>();
        final String xCerberusClientHeaderValue = ClientVersion.getClientHeaderValue();
        defaultHeaders.put(ClientVersion.CERBERUS_CLIENT_HEADER, xCerberusClientHeaderValue);

        return CerberusClientFactory.getClient(
                archaiusUrlResolver,
                // pass the client HTTP header value to be used in authenticate calls to Cerberus
                new DefaultCerberusCredentialsProviderChain(archaiusUrlResolver, xCerberusClientHeaderValue),
                // pass the client header to be used in all other calls to Cerberus (e.g. read, write, etc.)
                defaultHeaders);
    }
}
