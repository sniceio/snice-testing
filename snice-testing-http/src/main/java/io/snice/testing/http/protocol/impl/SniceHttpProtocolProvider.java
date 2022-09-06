package io.snice.testing.http.protocol.impl;

import io.snice.networking.common.Transport;
import io.snice.networking.common.docker.DockerSupport;
import io.snice.networking.config.NetworkInterfaceConfiguration;
import io.snice.testing.core.protocol.Protocol;
import io.snice.testing.core.protocol.ProtocolProvider;
import io.snice.testing.core.protocol.ProtocolRegistry;
import io.snice.testing.http.HttpConfig;
import io.snice.testing.http.protocol.HttpProtocol;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class SniceHttpProtocolProvider implements ProtocolProvider {

    @Override
    public Protocol createDefaultProtocol(final DockerSupport dockerSupport) {
        try {
            final var config = new HttpConfig();

            final var ip = dockerSupport.getPrimaryHostIp();
            final var listen = new URI("http://" + ip + ":8080");
            final var lp = new NetworkInterfaceConfiguration("default", listen, null, Transport.tcp);
            config.setNetworkInterfaces(List.of(lp));
            return HttpProtocol.from(config).build();
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ProtocolRegistry.Key key() {
        return HttpProtocol.httpProtocolKey;
    }
}
