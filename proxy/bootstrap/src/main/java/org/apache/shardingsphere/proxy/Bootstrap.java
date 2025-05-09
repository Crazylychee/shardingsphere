/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.proxy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.infra.config.props.ConfigurationProperties;
import org.apache.shardingsphere.infra.config.props.ConfigurationPropertyKey;
import org.apache.shardingsphere.proxy.arguments.BootstrapArguments;
import org.apache.shardingsphere.proxy.backend.config.ProxyConfigurationLoader;
import org.apache.shardingsphere.proxy.backend.config.YamlProxyConfiguration;
import org.apache.shardingsphere.proxy.frontend.CDCServer;
import org.apache.shardingsphere.proxy.frontend.ShardingSphereProxy;
import org.apache.shardingsphere.proxy.frontend.ssl.ProxySSLContext;
import org.apache.shardingsphere.proxy.initializer.BootstrapInitializer;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * ShardingSphere-Proxy Bootstrap.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Bootstrap {
    
    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
    
    /**
     * Main entrance.
     *
     * @param args startup arguments
     * @throws IOException IO exception
     * @throws SQLException SQL exception
     */
    public static void main(final String[] args) throws IOException, SQLException {
        BootstrapArguments bootstrapArgs = new BootstrapArguments(args);
        YamlProxyConfiguration yamlConfig = ProxyConfigurationLoader.load(bootstrapArgs.getConfigurationPath());
        int port = bootstrapArgs.getPort().orElseGet(() -> new ConfigurationProperties(yamlConfig.getServerConfiguration().getProps()).getValue(ConfigurationPropertyKey.PROXY_DEFAULT_PORT));
        List<String> addresses = bootstrapArgs.getAddresses();
        checkPort(addresses, port);
        new BootstrapInitializer().init(yamlConfig, port);
        Optional.ofNullable((Integer) yamlConfig.getServerConfiguration().getProps().get(ConfigurationPropertyKey.CDC_SERVER_PORT.getKey()))
                .ifPresent(optional -> new Thread(new CDCServer(addresses, optional)).start());
        ProxySSLContext.init();
        ShardingSphereProxy proxy = new ShardingSphereProxy();
        bootstrapArgs.getSocketPath().ifPresent(proxy::start);
        proxy.start(port, addresses);
    }
    
    private static void checkPort(final List<String> addresses, final int port) throws IOException {
        for (String each : addresses) {
            try (ServerSocket socket = new ServerSocket()) {
                socket.bind(new InetSocketAddress(each, port));
            }
        }
    }
}
