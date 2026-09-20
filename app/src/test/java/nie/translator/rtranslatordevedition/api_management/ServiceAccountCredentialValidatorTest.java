/*
 * Copyright 2016 Luca Martino.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copyFile of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nie.translator.rtranslatordevedition.api_management;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class ServiceAccountCredentialValidatorTest {
    @Test
    public void validate_acceptsPlausibleServiceAccount() throws Exception {
        ServiceAccountCredentialValidator.validate(validCredential().getBytes(StandardCharsets.UTF_8));
    }

    @Test(expected = ServiceAccountCredentialValidator.InvalidCredentialException.class)
    public void validate_rejectsUnrelatedJson() throws Exception {
        ServiceAccountCredentialValidator.validate(
                "{\"type\":\"user\",\"name\":\"not-a-service-account\"}"
                        .getBytes(StandardCharsets.UTF_8));
    }

    @Test(expected = ServiceAccountCredentialValidator.InvalidCredentialException.class)
    public void validate_rejectsMissingPrivateKey() throws Exception {
        ServiceAccountCredentialValidator.validate(validCredential()
                .replace("\"private_key\":\"-----BEGIN PRIVATE KEY-----\\nabc\\n-----END PRIVATE KEY-----\\n\",", "")
                .getBytes(StandardCharsets.UTF_8));
    }

    @Test(expected = ServiceAccountCredentialValidator.InvalidCredentialException.class)
    public void validate_rejectsMalformedJson() throws Exception {
        ServiceAccountCredentialValidator.validate("{\"type\":\"service_account\""
                .getBytes(StandardCharsets.UTF_8));
    }

    private static String validCredential() {
        return "{"
                + "\"type\":\"service_account\","
                + "\"project_id\":\"sample-project\","
                + "\"private_key_id\":\"key-id\","
                + "\"private_key\":\"-----BEGIN PRIVATE KEY-----\\nabc\\n-----END PRIVATE KEY-----\\n\","
                + "\"client_email\":\"service@example.iam.gserviceaccount.com\","
                + "\"client_id\":\"1234567890\","
                + "\"auth_uri\":\"https://accounts.google.com/o/oauth2/auth\","
                + "\"token_uri\":\"https://oauth2.googleapis.com/token\","
                + "\"auth_provider_x509_cert_url\":\"https://www.googleapis.com/oauth2/v1/certs\","
                + "\"client_x509_cert_url\":\"https://www.googleapis.com/robot/v1/metadata/x509/test\""
                + "}";
    }
}
