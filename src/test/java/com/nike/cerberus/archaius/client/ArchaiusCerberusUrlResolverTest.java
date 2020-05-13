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

import com.nike.cerberus.client.auth.CerberusCredentialsProvider;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.lang3.StringUtils;
import com.netflix.config.ConfigurationManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the ArchaiusCerberusUrlResolver class
 */
public class ArchaiusCerberusUrlResolverTest {

    private AbstractConfiguration config;
    private ArchaiusCerberusUrlResolver arch;

    @Before
    public void setUp() {
        config = mock(AbstractConfiguration.class);
        arch = new ArchaiusCerberusUrlResolver();
    }

    @Test
    public void testResolveUrlHappyEnv() {
        when(config.getString(ArchaiusCerberusUrlResolver.CERBERUS_ADDR_ENV_PROPERTY))
            .thenReturn("https://foo.bar");
        when(config.getString(ArchaiusCerberusUrlResolver.CERBERUS_ADDR_SYS_PROPERTY))
                .thenReturn("");
        String result = arch.resolveUrl(config);
        assertEquals("https://foo.bar", result);
    }

    @Test
    public void testResolveUrlHappySys() {
        when(config.getString(ArchaiusCerberusUrlResolver.CERBERUS_ADDR_ENV_PROPERTY))
                .thenReturn("");
        when(config.getString(ArchaiusCerberusUrlResolver.CERBERUS_ADDR_SYS_PROPERTY))
                .thenReturn("https://foo.bar");
        String result = arch.resolveUrl(config);
        assertEquals("https://foo.bar", result);
    }

    @Test
    public void testResolveUrlNull() {
        when(config.getString(ArchaiusCerberusUrlResolver.CERBERUS_ADDR_ENV_PROPERTY))
                .thenReturn(null);
        when(config.getString(ArchaiusCerberusUrlResolver.CERBERUS_ADDR_SYS_PROPERTY))
                .thenReturn(null);
        String result = arch.resolveUrl(config);
        assertEquals(null, result);
    }

    @Test
    public void testResolveBadUrl() {
        when(config.getString(ArchaiusCerberusUrlResolver.CERBERUS_ADDR_ENV_PROPERTY))
                .thenReturn(null);
        when(config.getString(ArchaiusCerberusUrlResolver.CERBERUS_ADDR_SYS_PROPERTY))
                .thenReturn("httphttp://foo.bar");
        String result = arch.resolveUrl(config);
        assertEquals(null, result);
    }

    @Test
    public void testResolveUrlEnvPrecendence() {
        when(config.getString(ArchaiusCerberusUrlResolver.CERBERUS_ADDR_ENV_PROPERTY))
                .thenReturn("https://piyo.hoge");
        when(config.getString(ArchaiusCerberusUrlResolver.CERBERUS_ADDR_SYS_PROPERTY))
                .thenReturn("http://foo.bar");
        String result = arch.resolveUrl(config);
        assertEquals("https://piyo.hoge", result);
    }

    @Test
    public void testResolveRegionHappyEnv() {
        when(config.getString(ArchaiusCerberusUrlResolver.CERBERUS_REGION_ENV_PROPERTY))
                .thenReturn("us-west-1");
        when(config.getString(ArchaiusCerberusUrlResolver.CERBERUS_REGION_SYS_PROPERTY))
                .thenReturn(null);
        String result = arch.resolveRegion(config);
        assertEquals("us-west-1", result);
    }

    @Test
    public void testResolveRegionHappySys() {
        when(config.getString(ArchaiusCerberusUrlResolver.CERBERUS_REGION_ENV_PROPERTY))
                .thenReturn("");
        when(config.getString(ArchaiusCerberusUrlResolver.CERBERUS_REGION_SYS_PROPERTY))
                .thenReturn("us-west-1");
        String result = arch.resolveRegion(config);
        assertEquals("us-west-1", result);
    }

    @Test
    public void testResolveRegionNull() {
        when(config.getString(ArchaiusCerberusUrlResolver.CERBERUS_REGION_ENV_PROPERTY))
                .thenReturn("");
        when(config.getString(ArchaiusCerberusUrlResolver.CERBERUS_REGION_SYS_PROPERTY))
                .thenReturn(null);
        String result = arch.resolveRegion(config);
        assertEquals(null, result);
    }

    @Test
    public void testResolveRegionBadRegionName() {
        when(config.getString(ArchaiusCerberusUrlResolver.CERBERUS_REGION_ENV_PROPERTY))
                .thenReturn("us-west-11");
        when(config.getString(ArchaiusCerberusUrlResolver.CERBERUS_REGION_SYS_PROPERTY))
                .thenReturn(null);
        String result = arch.resolveRegion(config);
        assertEquals(null, result);
    }

    @Test
    public void testResolveRegionEnvPrecedence() {
        when(config.getString(ArchaiusCerberusUrlResolver.CERBERUS_REGION_ENV_PROPERTY))
                .thenReturn("us-west-1");
        when(config.getString(ArchaiusCerberusUrlResolver.CERBERUS_REGION_SYS_PROPERTY))
                .thenReturn("us-east-1");
        String result = arch.resolveRegion(config);
        assertEquals("us-west-1", result);
    }
}
