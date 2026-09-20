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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Validates the flat JSON shape of a Google service-account key before it is persisted. */
public final class ServiceAccountCredentialValidator {
    private static final String[] REQUIRED_FIELDS = new String[]{
            "type",
            "project_id",
            "private_key_id",
            "private_key",
            "client_email",
            "client_id",
            "auth_uri",
            "token_uri",
            "auth_provider_x509_cert_url",
            "client_x509_cert_url"
    };

    private ServiceAccountCredentialValidator() {
    }

    public static void validate(byte[] credential) throws InvalidCredentialException {
        if (credential == null || credential.length == 0) {
            throw new InvalidCredentialException("Credential is empty");
        }

        final Map<String, String> fields;
        try {
            fields = new FlatJsonParser(new String(credential, StandardCharsets.UTF_8)).parse();
        } catch (IllegalArgumentException e) {
            throw new InvalidCredentialException("Credential is not valid JSON", e);
        }

        for (String field : REQUIRED_FIELDS) {
            String value = fields.get(field);
            if (value == null || value.trim().length() == 0) {
                throw new InvalidCredentialException("Credential is missing " + field);
            }
        }
        if (!"service_account".equals(fields.get("type"))) {
            throw new InvalidCredentialException("Credential is not a service account");
        }
        String privateKey = fields.get("private_key");
        if (!privateKey.startsWith("-----BEGIN PRIVATE KEY-----")
                || !privateKey.contains("-----END PRIVATE KEY-----")) {
            throw new InvalidCredentialException("Credential has no private key");
        }
        if (!fields.get("client_email").contains("@")
                || !fields.get("token_uri").startsWith("https://")) {
            throw new InvalidCredentialException("Credential has invalid account endpoints");
        }
    }

    static final class InvalidCredentialException extends IOException {
        InvalidCredentialException(String message) {
            super(message);
        }

        InvalidCredentialException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Google service-account files are flat objects whose values are strings. Keeping this parser
     * Java-only lets validation run in local unit tests without Android framework dependencies.
     */
    private static final class FlatJsonParser {
        private final String value;
        private int position;

        FlatJsonParser(String value) {
            this.value = value;
        }

        Map<String, String> parse() {
            Map<String, String> fields = new HashMap<>();
            skipWhitespace();
            expect('{');
            skipWhitespace();
            if (consume('}')) {
                ensureEnd();
                return fields;
            }

            while (true) {
                String key = readString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                fields.put(key, readString());
                skipWhitespace();
                if (consume('}')) {
                    ensureEnd();
                    return fields;
                }
                expect(',');
                skipWhitespace();
            }
        }

        private String readString() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (position < value.length()) {
                char current = value.charAt(position++);
                if (current == '"') {
                    return result.toString();
                }
                if (current != '\\') {
                    if (current < 0x20) {
                        throw new IllegalArgumentException("Unescaped control character");
                    }
                    result.append(current);
                    continue;
                }
                if (position == value.length()) {
                    throw new IllegalArgumentException("Unterminated escape");
                }
                char escaped = value.charAt(position++);
                switch (escaped) {
                    case '"': result.append('"'); break;
                    case '\\': result.append('\\'); break;
                    case '/': result.append('/'); break;
                    case 'b': result.append('\b'); break;
                    case 'f': result.append('\f'); break;
                    case 'n': result.append('\n'); break;
                    case 'r': result.append('\r'); break;
                    case 't': result.append('\t'); break;
                    case 'u': result.append(readUnicodeEscape()); break;
                    default: throw new IllegalArgumentException("Invalid escape");
                }
            }
            throw new IllegalArgumentException("Unterminated string");
        }

        private char readUnicodeEscape() {
            if (position + 4 > value.length()) {
                throw new IllegalArgumentException("Incomplete unicode escape");
            }
            int codePoint = 0;
            for (int index = 0; index < 4; index++) {
                int digit = Character.digit(value.charAt(position++), 16);
                if (digit < 0) {
                    throw new IllegalArgumentException("Invalid unicode escape");
                }
                codePoint = (codePoint << 4) + digit;
            }
            return (char) codePoint;
        }

        private boolean consume(char expected) {
            if (position < value.length() && value.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!consume(expected)) {
                throw new IllegalArgumentException("Unexpected JSON token");
            }
        }

        private void skipWhitespace() {
            while (position < value.length() && Character.isWhitespace(value.charAt(position))) {
                position++;
            }
        }

        private void ensureEnd() {
            skipWhitespace();
            if (position != value.length()) {
                throw new IllegalArgumentException("Trailing content");
            }
        }
    }
}
