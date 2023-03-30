FROM registry.snice.io/snice:latest

ARG WORKDIR=/opt/sniceio
WORKDIR $WORKDIR

ARG USER=sniceio
ARG GROUP=sniceio

COPY --chown=$USER:$GROUP target/snice-testing-examples-0.0.1-SNAPSHOT.jar $WORKDIR/lib
COPY --chown=$USER:$GROUP http_app.yml $WORKDIR/config.yml

# ENTRYPOINT ["docker-entrypoint.sh"]
# CMD ["java", "-Djava.net.preferIPv4Stack=true", "-cp", "$WORKDIR/lib/*", "io.snice.testing.runtime.Snice"]
# CMD ['java', '-Djava.net.preferIPv4Stack=true', '-cp', '$WORKDIR/lib/*', 'io.snice.testing.runtime.Snice']
# CMD ['io.snice.testing.runtime.Snice']
