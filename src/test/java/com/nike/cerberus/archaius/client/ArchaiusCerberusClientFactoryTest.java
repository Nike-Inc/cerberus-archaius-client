/*
 * Copyright (c) 2020 Nike, Inc.
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cerberus.okhttp3.HttpUrl;
import com.nike.cerberus.client.CerberusClient;
import com.nike.cerberus.client.CerberusClientException;
import org.apache.commons.configuration.AbstractConfiguration;
import org.junit.Before;
import org.junit.Test;

/** Tests the ArchaiusCerberusUrlResolver class */
public class ArchaiusCerberusClientFactoryTest {

    private AbstractConfiguration config;
    private ArchaiusCerberusUrlResolver arch;

    @Before
    public void setUp() {
        arch = mock(ArchaiusCerberusUrlResolver.class);
        config = mock(AbstractConfiguration.class);
    }

    @Test
    public void testGetClient() {
        when(arch.resolveRegion(config)).thenReturn("us-west-2");
        when(arch.resolveUrl(config)).thenReturn("http://foo.bar");
        CerberusClient client = ArchaiusCerberusClientFactory.getClient(arch, config);

        HttpUrl actualUrl = client.getCerberusUrl();
        HttpUrl expectedUrl = HttpUrl.parse("http://foo.bar");
        assertEquals(expectedUrl, actualUrl);
    }

    @Test(expected = CerberusClientException.class)
    public void testNullGetClient() {
        CerberusClient client = null;
        ArchaiusCerberusClientFactory.getClient();
    }

    @Test
    public void testGetClientNoRegion() {
        when(arch.resolveRegion(config)).thenReturn(null);
        when(arch.resolveUrl(config)).thenReturn("http://foo.bar");
        try {
            CerberusClient client = ArchaiusCerberusClientFactory.getClient(arch, config);
        } catch (Exception exc) {
            assertEquals(CerberusClientException.class, exc.getClass());
        }
    }

    @Test
    public void testGetClientNoUrl() {
        when(arch.resolveRegion(config)).thenReturn("us-west-2");
        when(arch.resolveUrl(config)).thenReturn(null);
        try {
            CerberusClient client = ArchaiusCerberusClientFactory.getClient(arch, config);
        } catch (Exception exc) {
            assertEquals(CerberusClientException.class, exc.getClass());
        }
    }
}
