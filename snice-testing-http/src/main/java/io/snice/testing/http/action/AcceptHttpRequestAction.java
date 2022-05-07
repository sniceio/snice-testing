package io.snice.testing.http.action;

import io.snice.testing.core.Execution;
import io.snice.testing.core.Session;
import io.snice.testing.core.action.Action;
import io.snice.testing.http.AcceptHttpRequestDef;
import io.snice.testing.http.stack.HttpStack;

import java.util.List;
import java.util.Map;

public record AcceptHttpRequestAction(String name,
                                      HttpStack stack,
                                      AcceptHttpRequestDef def,
                                      Map<String, Object> attributes,
                                      Action next) implements Action {

    @Override
    public void execute(final List<Execution> executions, final Session session) {
        // TODO
        final var t = new Thread(() -> {
            try {
                System.err.println("Got to execute somehow but first I'll sleep 4 seconds");
                Thread.sleep(4000);
                System.err.println("Done sleeping so now moving to next action");
                next.execute(executions, session);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        });

        t.start();
    }

}
