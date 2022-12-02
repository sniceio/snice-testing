package io.snice.testing.core.protocol;

import io.snice.networking.common.docker.DockerSupport;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The {@link ProtocolProvider} is a way to dynamically load a provider for a given protocol
 * and, in particular, allow it to supply a default {@link Protocol} stack for some given
 * protocol (such as HTTP, SIP etc)
 */
public interface ProtocolProvider {

    /**
     * Supply a default {@link Protocol} stack, such as an HTTP stack.
     * This is useful when the user doesn't need to configure a given stack themselves but
     * are happy with the defaults. It primarily works for basic test scenarios and is really
     * part of the "instant gratification" for new Snice Test developers.
     */
    Protocol createDefaultProtocol(DockerSupport dockerSupport);

    ProtocolRegistry.Key key();

    static Map<ProtocolRegistry.Key, ProtocolProvider> load() {
        return ServiceLoader.load(ProtocolProvider.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toMap(ProtocolProvider::key, Function.identity()));
    }
}
