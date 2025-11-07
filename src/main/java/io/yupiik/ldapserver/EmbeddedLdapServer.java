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

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFReader;
import io.yupiik.fusion.framework.api.lifecycle.Start;
import io.yupiik.fusion.framework.api.scope.ApplicationScoped;
import io.yupiik.fusion.framework.build.api.event.OnEvent;
import io.yupiik.fusion.framework.build.api.lifecycle.Destroy;
import io.yupiik.fusion.framework.build.api.order.Order;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Logger;

import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;

@ApplicationScoped
public class EmbeddedLdapServer {

    private final Logger logger = Logger.getLogger(EmbeddedLdapServer.class.getName());
    private InMemoryDirectoryServer server;
    private boolean resetSystemProperty;

    private LdapConfiguration ldapConfiguration;

    public EmbeddedLdapServer(LdapConfiguration ldapConfiguration) {
        this.ldapConfiguration = ldapConfiguration;
    }


    public void onEvent(@OnEvent @Order(10) final Start start) {
        if (!ldapConfiguration.active()) {
            return;
        }
        try {
            final InMemoryDirectoryServer ldapServer = createServer(ldapConfiguration);
            if (ldapConfiguration.provisioning()) {
                final Path provisioning = Paths.get(ldapConfiguration.provisioningSource());
                if (Files.exists(provisioning)) {
                    try {
                        doProvisionLDif(ldapServer, provisioning);
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                } else { // hardcode embed data
                    final var resourceAsStream = Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream(ldapConfiguration.provisioningSource());
                    if (resourceAsStream != null) {
                        try (final LDIFReader reader = new LDIFReader(new BufferedReader(new InputStreamReader(
                                resourceAsStream, StandardCharsets.UTF_8)))) {
                            final var entriesAdded = ldapServer.importFromLDIF(false, reader);
                            logger.info(() -> "Imported #" + entriesAdded + " default LDAP entries");
                        } catch (final Exception e) {
                            throw new IllegalStateException(e);
                        }
                    } else {
                        try (final LDIFReader reader = new LDIFReader(new BufferedReader(new InputStreamReader(
                                new ByteArrayInputStream(ldapConfiguration.provisioningSource().getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)))) {
                            final var entriesAdded = ldapServer.importFromLDIF(false, reader);
                            logger.info(() -> "Imported #" + entriesAdded + " default LDAP entries");
                        } catch (final Exception e) {
                            throw new IllegalStateException(e);
                        }
                    }
                }
            }
            ldapServer.startListening();
            logger.info(() -> "Starting LDAP server on port " + ldapServer.getListenPort());
            if (ldapConfiguration.setSystemProperties()) {
                final String key = getClass().getName() + ".port";
                System.setProperty(key, Integer.toString(ldapServer.getListenPort()));
                resetSystemProperty = true;
            }
            server = ldapServer;
        } catch (final LDAPException e) {
            throw new IllegalStateException(e);
        }
    }

    @Destroy
    public void release() {
        if (server == null) {
            return;
        }
        logger.info(() -> "Closing LDAP server");
        server.close();

        if (resetSystemProperty) {
            System.clearProperty(getClass().getName() + ".port");
        }
    }

    private void doProvisionLDif(final InMemoryDirectoryServer ldapServer, final Path provisioning) throws IOException {
        Files.list(provisioning)
                .filter(it -> Files.isRegularFile(it) && it.getFileName().toString().endsWith(".ldif"))
                .sorted(comparing(it -> { // enable to prefix with number, ex: 01_myfile.ldif
                    final String s = it.getFileName().toString();
                    final int sep = s.indexOf('_');
                    if (sep > 0) {
                        try {
                            return Integer.parseInt(s.substring(0, sep));
                        } catch (final NumberFormatException nfe) {
                            // let's default
                        }
                    }
                    return Integer.MAX_VALUE;
                }))
                .forEach(path -> {
                    try {
                        final var entriesAdded = ldapServer.importFromLDIF(false, path.toFile());
                        logger.info(() -> "Imported #" + entriesAdded + " '" + path + "'");
                    } catch (final LDAPException e) {
                        throw new IllegalStateException(e);
                    }
                });
    }

    private InMemoryDirectoryServer createServer(final LdapConfiguration configuration) throws LDAPException {
        final InMemoryDirectoryServerConfig cfg = new InMemoryDirectoryServerConfig(ldapConfiguration.baseDn().split("\\|"));
        Optional.of(ldapConfiguration.port())
                .filter(i -> i > 0)
                .map(p -> Integer.parseInt(String.valueOf(p)))
                .ifPresent(port -> {
                    cfg.getListenerConfigs().clear();
                    try {
                        cfg.getListenerConfigs().add(InMemoryListenerConfig.createLDAPConfig(
                                "default",
                                ofNullable(ldapConfiguration.host())
                                        .map(String::valueOf)
                                        .map(h -> {
                                            try {
                                                return InetAddress.getByName(h);
                                            } catch (final UnknownHostException e) {
                                                throw new IllegalArgumentException(e);
                                            }
                                        })
                                        .orElseGet(InetAddress::getLoopbackAddress),
                                port, null));
                    } catch (final LDAPException e) {
                        throw new IllegalStateException(e);
                    }
                });
        return new InMemoryDirectoryServer(cfg);
    }
}
