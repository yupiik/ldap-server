/*
 * Copyright (c) 2023 / present - Yupiik SAS - https://www.yupiik.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.yupiik.ldapserver;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;
import io.yupiik.fusion.testing.Fusion;
import io.yupiik.fusion.testing.FusionSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@FusionSupport
class EmbeddedLdapServerTest {
    //
    // /!\ don't inject the server, it must start by itself and not cause of the injection!
    //

    @Test
    void ensureServerIsUp() {
        final String portValue = System.getProperty("io.yupiik.ldapserver.EmbeddedLdapServer.port", "4444");
        assertNotNull(portValue);

        final LDAPConnectionOptions connectionOptions = new LDAPConnectionOptions();

        try (final LDAPConnection ldapConnection = new LDAPConnection(
                    connectionOptions, "localhost", Integer.parseInt(portValue))) {
            final SearchResult search = ldapConnection.search(new SearchRequest(
                    "dc=demo,dc=com", SearchScope.BASE, "(dc=demo)"));
            assertEquals(ResultCode.SUCCESS, search.getResultCode());
            assertEquals(1, search.getEntryCount());
        } catch (final LDAPException e) {
            fail(e.getMessage());
        }
    }
}
