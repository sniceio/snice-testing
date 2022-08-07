package io.snice.testing.http;

import io.snice.testing.core.MessageBuilder;
import io.snice.testing.core.protocol.ProtocolRegistry;
import io.snice.testing.http.protocol.HttpProtocol;

public interface HttpMessageDefBuilder extends MessageBuilder {

    @Override
    default ProtocolRegistry.Key protocol() {
        return HttpProtocol.httpProtocolKey;
    }

}
