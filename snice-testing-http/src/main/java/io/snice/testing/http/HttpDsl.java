package io.snice.testing.http;


import io.snice.codecs.codec.http.HttpMethod;
import io.snice.networking.common.Transport;
import io.snice.networking.config.NetworkInterfaceConfiguration;
import io.snice.testing.core.Session;
import io.snice.testing.http.check.HttpCheckSupport;
import io.snice.testing.http.protocol.HttpProtocol;
import io.snice.testing.http.protocol.HttpProtocol.HttpProtocolBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static io.snice.preconditions.PreConditions.assertNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotNull;
import static io.snice.preconditions.PreConditions.ensureNotEmpty;

/**
 * Functions as a simple DSL to "kick-start" the creation of various HTTP
 * related objects, such as requests, configure the HTTP stack etc.
 */
public class HttpDsl extends HttpCheckSupport {

    private HttpDsl() {
        // No instantiation of this class
    }

    public static HttpProtocolBuilder http() {
        return http(new HttpConfig());
    }

    public static HttpProtocolBuilder http(final HttpConfig config) {
        // TODO: how do you turn the SniceConfig into a http config?
        // TODO: this is wrong. We are mixing configuration of the underlying stack
        // TODO: with the configuration the user wants as part of their http protocol.
        // TODO: SniceConfig is really as part of the running system and nothing the user
        // TODO: should really concern themselves with.

        // TODO: note that I changed the aboce from SniceConfig to HttpConfig and then
        // we'll need to figure out how to get that HttpConfig into this DSL...
        try {
            // final var ip = NetworkingUtils.findPrimaryAddress().getHostAddress();
            final var ip = "127.0.0.1";
            final var listen = new URI("https://" + ip + ":1234");

            // TODO: just hit the local ngrok api to fetch this automatically
            final var vipAddress = new URI("https://ce10-135-180-42-215.ngrok.io");
            final var lp = new NetworkInterfaceConfiguration("default", listen, vipAddress, Transport.tcp);
            config.setNetworkInterfaces(List.of(lp));

            return HttpProtocol.from(config);
        } catch (final URISyntaxException e) {
            // TODO
            throw new RuntimeException(e);
        }
    }

    /**
     * When accepting incoming traffic, you may need to know what the FQDN of your "accept endpoint" is. You may need
     * this information because, as part of your test, you will "install" a webhook over which you then later expect
     * HTTP traffic to. As such, you will need to inform your system under test of where the webhook will be delivered
     * and this information is available to your test as part of the {@link Session} and stored under the key that you
     * specify here.
     */
    public interface SaveAcceptFqdnAsStep {
        AcceptHttpRequestBuilder saveAs(String name);
    }

    /**
     * We need to know whether the action being specified is a request we intend to construct and send or, if we
     * are expecting to accept an incoming HTTP request. Since this is a mandatory step, we are using the so-called
     * step builder pattern to enforce mandatory information to be collected by "guiding" the user through those
     * steps.
     * <p>
     * After this step, there is a split in b
     */
    public interface InitiateOrAcceptStep {

        /**
         * Initiate a new HTTP request towards the given target
         *
         * @param method
         * @param uri    the target URI, which can be an FQDN, a segment or null, in which case the {@link HttpProtocolBuilder#baseUrl()}
         *               will be used to build an FQDN.
         * @return
         */
        InitiateHttpRequestBuilder initiate(HttpMethod method, String uri);

        /**
         * Accept incoming HTTP traffic destined to the given path.
         *
         * @param method
         * @param path   you can ONLY specify a path and not a full FQDN because the full FQDN is owned and
         *               controlled by the underlying network stack and the Snice Testing execution environment.
         *               The path is, however, allowed to be null or empty, in which case the FQDN will be the "raw"
         *               as generated by the execution environment. If you need to know what the FQDN is (as you will
         *               if you intend to use this as a webhook and you need to "install" that webhook through, e.g.,
         *               an HTTP Post to the infrastructure you're testing) then it will be saved in the {@link Session}
         *               under the key you'll specify in the {@link SaveAcceptFqdnAsStep#saveAs(String)}.
         *
         *               OPEN QUESTIONS: not sure you should be allowed to have the path as a dynamic expression
         *               since we may need to know the entire FQDN for this particular "accept" when the Scenario
         *               starts running. After all, you typically install the webhook before you call accept. Or
         *               should you have to start all "accepts" at the beginning of the Scenario?
         * @return
         */
        SaveAcceptFqdnAsStep accept(HttpMethod method, String path);

        default SaveAcceptFqdnAsStep accept(final HttpMethod method) {
            return accept(method, null);
        }

        /**
         * Same as {@link #get(String)} but allows you to not specify any further information in the URI,
         * in which case the {@link InitiateHttpRequestBuilder#baseUrl(String)} MUST have been specified when the HTTP request
         * eventually is constructed.
         *
         * @return a new instance of the {@link InitiateHttpRequestBuilder} with the URI for the
         * GET set to that of the {@link InitiateHttpRequestBuilder#baseUrl(String)}.
         */
        default InitiateHttpRequestBuilder get() {
            return initiate(HttpMethod.GET, null);
        }

        /**
         * Specify that this an outgoing HTTP GET request and the URI to fetch.
         * <p>
         * You can specify a FQDN, in which case the {@link InitiateHttpRequestBuilder#baseUrl(String)}
         * will have no effect on this request. If you just specify a path, the
         * {@link InitiateHttpRequestBuilder#baseUrl(String)} will be pre-pended to form the FQDN.
         * <p>
         * Also, the URI is allowed to contain variable expression, which will be expanded
         * when the test executes. This also means that the validity of the resulting URI
         * cannot be verified (if it is a variable expression) until the execution starts.
         *
         * @param uri the URI to fetch, which is either a FQDN or a path that will be appended to the base URL.
         * @return a new instance of the {@link InitiateHttpRequestBuilder} with the new URI for the GET.
         * @throws IllegalArgumentException in case the URI is null or malformed.
         */
        default InitiateHttpRequestBuilder get(final String uri) throws IllegalArgumentException {
            assertNotEmpty(uri, "The URI to GET cannot be null or the empty string. If you " +
                    "truly wish to not specify the URI then use the overloaded method that does not accept" +
                    "a URI to begin with");
            return initiate(HttpMethod.GET, uri);
        }

        default InitiateHttpRequestBuilder post() {
            return initiate(HttpMethod.POST, null);
        }

        default InitiateHttpRequestBuilder post(final String uri) {
            return initiate(HttpMethod.POST, uri);
        }
    }

    public static InitiateOrAcceptStep http(final String requestName) {
        ensureNotEmpty(requestName, "The name of the HTTP request cannot be empty");

        return new InitiateOrAcceptStep() {
            @Override
            public InitiateHttpRequestBuilder initiate(final HttpMethod method, final String uri) {
                return InitiateHttpRequestDef.of(requestName, method, uri);
            }

            @Override
            public SaveAcceptFqdnAsStep accept(final HttpMethod method, final String path) {
                assertNotNull(method);
                return saveAsName -> AcceptHttpRequestDef.of(requestName, method, path, saveAsName);
            }
        };
    }

}
