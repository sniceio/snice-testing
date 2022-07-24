package io.snice.testing.examples.twilio;

import io.snice.codecs.codec.http.HttpHeader;
import io.snice.codecs.codec.http.HttpMethod;
import io.snice.testing.core.CoreDsl;
import io.snice.testing.core.Snice;
import io.snice.testing.core.SniceConfig;
import io.snice.testing.core.scenario.Scenario;

import java.util.List;
import java.util.Map;

import static io.snice.preconditions.PreConditions.assertNotEmpty;
import static io.snice.testing.http.HttpDsl.http;
import static io.snice.testing.http.check.HttpCheckSupport.header;
import static io.snice.testing.http.check.HttpCheckSupport.status;

public record InitiateCall(String username,
                           String password,
                           String to,
                           String from) {

    private static String produceTwiml(final String msg) {
        return "<Response><Say>" + msg + "</Say></Response>";
    }

    public Scenario scenario() {

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
                // "StatusCallbackEvent", "completed",
                "StatusCallbackMethod", "POST",
                "Timeout", 17,
                "To", to,
                "From", from);

        final var createCall = http("Create New Call")
                .post("Calls")
                .auth(username, password)
                .content(content)
                .check(status().is(200));

        return CoreDsl.scenario("Twilio Initiate Call")
                .executeAsync(statusCallback)
                .executeAsync(twimlHook)
                .execute(createCall);
    }

    public static void main(final String... args) throws Exception {
        // Please do NOT check in sensitive information! :-)
        final var accountSid = System.getenv().get("TWILIO_ACCOUNT_SID");
        final var authToken = System.getenv().get("TWILIO_AUTH_TOKEN");
        final var to = System.getenv().get("TWILIO_TO_NUMBER");
        final var from = System.getenv().get("TWILIO_FROM_NUMBER");

        assertNotEmpty(accountSid, "You must specify the Twilio Account Sid by setting the environment variable TWILIO_ACCOUNT_SID");
        assertNotEmpty(authToken, "You must specify the Twilio AuthToken by setting the environment variable TWILIO_AUTH_TOKEN");
        assertNotEmpty(to, "You must specify the To-number we are going to call by setting the environment variable TWILIO_TO_NUMBER");
        assertNotEmpty(from, "You must specify the From-number we are going to call from by setting the environment variable TWILIO_FROM_NUMBER");

        // TODO: instead of passing to and from in here, use a Feeder and variables in
        //  the content map... like Gatling
        final var scenario = new InitiateCall(accountSid, authToken, to, from).scenario();

        // NOTE: that last slash is SUPER SUPER important!
        // TODO: document this once we get to that.
        final var config = new SniceConfig();
        final var http = http(config)
                .baseUrl("https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/");
        // .auth(accountSid, authToken); TODO

        Snice.run(scenario)
                .configuration(config)
                .protocols(http)
                .start()
                .sync();

        System.err.println("I'm out of here");

    }

}
