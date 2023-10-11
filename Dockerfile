FROM --platform=linux/amd64 ubuntu:latest

ENV HOME="/root" \
    LC_ALL="en_US.UTF-8" \
    IDE_DIST="/idea"

ENV JAVA_HOME="$IDE_DIST/jbr"

RUN apt update && \
    apt install -y unzip git telnet curl nano iputils-ping

COPY "JetBrainsAcademy.zip" "/tmp/JetBrainsAcademy.zip"

RUN set -ex && \
    IDE_NAME="ideaIC-2023.2.2" && \
    IDE_URL="https://download.jetbrains.com/idea/$IDE_NAME.tar.gz" && \
    curl -fsSL "$IDE_URL" -o "/tmp/$IDE_NAME.tar.gz" \
               "$IDE_URL.sha256" -o "/tmp/$IDE_NAME.tar.gz.sha256" && \
    (cd /tmp && sha256sum --check --status "$IDE_NAME.tar.gz.sha256") && \
    mkdir -p $IDE_DIST && \
    tar -xzf "/tmp/$IDE_NAME.tar.gz" --directory $IDE_DIST --strip-components=1 && \
    unzip -d "$IDE_DIST/plugins" /tmp/JetBrainsAcademy.zip && \
    chmod +x "$IDE_DIST"/bin/*.sh && \
    rm -rf /tmp/*

RUN mkdir /project
WORKDIR /project
