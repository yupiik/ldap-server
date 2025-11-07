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

import io.yupiik.fusion.framework.build.api.configuration.Property;
import io.yupiik.fusion.framework.build.api.configuration.RootConfiguration;

@RootConfiguration("yupiik.ldap.server")
public record LdapConfiguration(
        @Property(value = "active", documentation = "Activate the server", defaultValue = "true")
        boolean active,

        @Property(value = "setSystemProperties", documentation = "Activate the server", defaultValue = "false")
        boolean setSystemProperties,

        @Property(value = "host", documentation = "Published host of the server", defaultValue = "\"localhost\"")
        String host,

        @Property(value = "port", documentation = "Published port of the server", defaultValue = "4444")
        int port,

        @Property(value = "provisioning", documentation = "Activate the server", defaultValue = "true")
        boolean provisioning,

        @Property(value = "provisioningSource", documentation = "Activate the server", defaultValue = "\"yupiik/ldap/server/default.ldif\"")
        String provisioningSource,

        @Property(value = "active", documentation = "Activate the server", defaultValue = "\"dc=demo,dc=com\"")
        String baseDn) {
}
