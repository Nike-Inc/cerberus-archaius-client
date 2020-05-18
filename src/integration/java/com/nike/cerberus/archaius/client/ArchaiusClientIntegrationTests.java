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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fieldju.commons.EnvUtils;
import com.nike.cerberus.client.CerberusClient;
import com.nike.cerberus.client.CerberusServerApiException;
import com.nike.cerberus.client.CerberusServerException;
import com.nike.cerberus.client.model.CerberusListFilesResponse;
import com.nike.cerberus.client.model.CerberusListResponse;
import com.nike.cerberus.client.model.CerberusResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class ArchaiusClientIntegrationTests {

    private static final String CERBERUS_ADDR_ENV_PROPERTY = "CERBERUS_ADDR";
    private static final String CERBERUS_REGION_ENV_PROPERTY = "CERBERUS_REGION";
    private static final String CERBERUS_ROOT_SDB_PATH_PROPERTY = "CERBERUS_ROOT_SDB_PATH";

    private static String cerberusUrl;
    private static String region;
    private static String rootSdbPath;

    private static String secretPath;
    private static String sdbFullSecretPath;
    private static Map<String, String> secretData;

    private static CerberusClient cerberusClient;

    @BeforeClass
    public static void setUp() {

        rootSdbPath = EnvUtils.getRequiredEnv(CERBERUS_ROOT_SDB_PATH_PROPERTY);

        secretPath = UUID.randomUUID().toString();
        sdbFullSecretPath = rootSdbPath + secretPath;

        String key = RandomStringUtils.randomAlphabetic(15);
        String value = RandomStringUtils.randomAlphabetic(25);
        secretData = new HashMap<>();
        secretData.put(key, value);
    }

    @Test
    public void test_properties_configured() {
        cerberusUrl = EnvUtils.getRequiredEnv(CERBERUS_ADDR_ENV_PROPERTY);
        region = EnvUtils.getRequiredEnv(CERBERUS_REGION_ENV_PROPERTY);
    }

    @Test
    public void test_create_cerberus_client() {
        cerberusClient = ArchaiusCerberusClientFactory.getClient();
    }

    @Test
    public void test_client_secret_functionality() {

        // create client
        cerberusClient = ArchaiusCerberusClientFactory.getClient();

        // create secret
        cerberusClient.write(sdbFullSecretPath, secretData);

        // read secret
        CerberusResponse cerberusReadResponse = cerberusClient.read(sdbFullSecretPath);
        assertEquals(secretData, cerberusReadResponse.getData());

        // list secrets
        CerberusListResponse cerberusListResponse = cerberusClient.list(rootSdbPath);
        assertTrue(cerberusListResponse.getKeys().contains(secretPath));

        // update secret
        Map<String, String> newSecretData = generateNewSecretData();
        cerberusClient.write(sdbFullSecretPath, newSecretData);
        secretData = newSecretData;

        // confirm updated secret data
        CerberusResponse cerberusReadResponseUpdated = cerberusClient.read(sdbFullSecretPath);
        assertEquals(newSecretData, cerberusReadResponseUpdated.getData());

        // delete secret
        cerberusClient.delete(sdbFullSecretPath);

        // confirm secret is deleted
        try {
            cerberusClient.read(sdbFullSecretPath);
        } catch (CerberusServerException cse) {
            assertEquals(404, cse.getCode());
        }
    }

    @Test
    public void test_client_crud_file_functionality() {

        // create client
        cerberusClient = ArchaiusCerberusClientFactory.getClient();

        String fileContentStr = "file content string!";
        byte[] fileContentArr = fileContentStr.getBytes(StandardCharsets.UTF_8);

        // create file
        cerberusClient.writeFile(sdbFullSecretPath, fileContentArr);

        // read file
        byte[] file = cerberusClient.readFileAsBytes(sdbFullSecretPath);
        String resultContentStr = new String(file, StandardCharsets.UTF_8);
        assertEquals(fileContentStr, resultContentStr);

        // list files
        CerberusListFilesResponse response = cerberusClient.listFiles(rootSdbPath);
        assertEquals(
                StringUtils.substringAfter(sdbFullSecretPath, "/"),
                response.getSecureFileSummaries().get(0).getPath());

        // update file
        String newFileContentStr = "new file content string*";
        byte[] newFileContentArr = newFileContentStr.getBytes(StandardCharsets.UTF_8);
        cerberusClient.writeFile(sdbFullSecretPath, newFileContentArr);

        // confirm updated file data
        byte[] updatedFileResult = cerberusClient.readFileAsBytes(sdbFullSecretPath);
        String updatedFileStr = new String(updatedFileResult, StandardCharsets.UTF_8);
        assertEquals(newFileContentStr, updatedFileStr);

        // delete file
        cerberusClient.deleteFile(sdbFullSecretPath);

        // confirm file is deleted
        try {
            cerberusClient.readFileAsBytes(sdbFullSecretPath);
        } catch (CerberusServerApiException cse) {
            assertEquals(404, cse.getCode());
        }
    }

    private Map<String, String> generateNewSecretData() {
        String key = RandomStringUtils.randomAlphabetic(20);
        String value = RandomStringUtils.randomAlphabetic(30);
        Map<String, String> newSecretData = new HashMap<>();
        newSecretData.put(key, value);

        return newSecretData;
    }
}
