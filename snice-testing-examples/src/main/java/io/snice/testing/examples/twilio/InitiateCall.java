package io.snice.testing.examples.twilio;

import io.snice.codecs.codec.http.HttpHeader;
import io.snice.codecs.codec.http.HttpMethod;
import io.snice.testing.core.CoreDsl;
import io.snice.testing.core.scenario.Scenario;
import io.snice.testing.runtime.Snice;

import java.util.List;
import java.util.Map;

import static io.snice.testing.http.HttpDsl.http;
import static io.snice.testing.http.check.HttpCheckSupport.header;
import static io.snice.testing.http.check.HttpCheckSupport.status;

public class InitiateCall {

    private static String produceTwiml(final String msg) {
        return "<Response><Say>" + msg + "</Say></Response>";
    }

    public static Scenario scenario() {

        final var twimlHook = http("Twiml Hook")
                .accept(HttpMethod.POST, "/demo")
                .saveAs("twiml generator")
                .respond(200)
                .content(produceTwiml("Hi and welcome to this amazing demo"))
                .header(HttpHeader.CONTENT_TYPE, "application/xml");

        /**
         * This is where we expect all the call events to be pushed as the call progresses.
         */
        final var statusCallback = http("Call Status")
                .accept(HttpMethod.POST)
                .saveAs("status callback")
                .check(header("CallStatus").is("initiated"))
                .respond(200)
                .acceptNextRequest("ringing")
                .check(header("CallStatus").is("ringing"))
                .respond(200)
                .acceptNextRequest("answered")
                .check(header("CallStatus").is("answered"))
                .respond(200)
                .acceptNextRequest("completed")
                .check(header("CallStatus").is("completed"))
                .respond(200);

        final Map<String, Object> content = Map.of(
                "Url", "${twiml generator}",
                "StatusCallback", "${status callback}",
                "StatusCallbackEvent", List.of("initiated", "answered", "ringing", "completed"),
                "StatusCallbackMethod", "POST",
                "Timeout", 17,
                "To", "${env.TWILIO_TO_NUMBER}",
                "From", "${env.TWILIO_FROM_NUMBER}");

        final var createCall = http("Create New Call")
                .post("Calls")
                .auth("${env.TWILIO_ACCOUNT_SID}", "${env.TWILIO_AUTH_TOKEN}")
                .content(content)
                .check(status().is(200));

        return CoreDsl.scenario("Twilio Initiate Call")
                .executeAsync(statusCallback)
                .executeAsync(twimlHook)
                .execute(createCall);
    }

    public static void main(final String... args) throws Exception {

        // Please do NOT check in sensitive information! :-)
        // Snice Testing allows you to reference environment variables
        // as part of your scenario. Use that!

        final var scenario = InitiateCall.scenario();

        // NOTE: that last slash is SUPER SUPER important!
        // TODO: document this once we get to that.
        final var http = http().baseUrl("https://api.twilio.com/2010-04-01/Accounts/${env.TWILIO_ACCOUNT_SID}/");

        Snice.run(scenario, http);
    }

}
