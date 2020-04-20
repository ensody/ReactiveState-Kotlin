FROM ubuntu:latest

ENV DEBIAN_FRONTEND=noninteractive

ENV PATH=$PATH:~/bin

WORKDIR /workspace

COPY .devcontainer/ /dockerbuild/app/.devcontainer/
COPY scripts/ /dockerbuild/app/scripts/
RUN /dockerbuild/app/.devcontainer/build-dev.sh
