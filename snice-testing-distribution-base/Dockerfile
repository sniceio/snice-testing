FROM openjdk:17-buster

RUN apt-get update && \
    apt install -y libsctp-dev net-tools && \
    apt install -y tcpdump ngrep && \
    apt install -y sudo && \
    apt install -y vim

# ARG MODULE=snice-testing-distribution-base
ARG MODULE=.
ARG LIB=target/lib

ARG WORKDIR=/opt/sniceio
ARG USER=sniceio
ARG GROUP=sniceio
ARG LOG_CONF=logback.xml

RUN groupadd -r $GROUP && \
    useradd -r -g $USER $GROUP -G sudo

# Insecure, will remove after testing etc has completed
RUN echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers

WORKDIR $WORKDIR

COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chown $USER:$GROUP /usr/local/bin/docker-entrypoint.sh && \
    chmod 755 /usr/local/bin/docker-entrypoint.sh && \
    chown -R $USER:$GROUP $WORKDIR


COPY --chown=$USER:$GROUP $MODULE/$LIB $WORKDIR/lib
COPY --chown=$USER:$GROUP $MODULE/$LOG_CONF $WORKDIR/

EXPOSE 8080/tcp 8081/tcp

USER $USER
ENTRYPOINT ["docker-entrypoint.sh"]
