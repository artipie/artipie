# Dockerfile for testing conan operations
FROM ubuntu:22.04
ENV REV 1
ENV CONAN_TRACE_FILE "/tmp/conan_trace.log"
ENV DEBIAN_FRONTEND "noninteractive"
ENV CONAN_VERBOSE_TRACEBACK 1
ENV CONAN_NON_INTERACTIVE 1
ENV no_proxy "host.docker.internal,host.testcontainers.internal,localhost,127.0.0.1"
WORKDIR "/home"
RUN apt clean -y && apt update -y -o APT::Update::Error-Mode=any
RUN apt install --no-install-recommends -y python3-pip curl g++ git make cmake gzip xz-utils
RUN pip3 install -U pip setuptools
RUN pip3 install -U conan==1.60.2
RUN conan profile new --detect default
RUN conan profile update settings.compiler.libcxx=libstdc++11 default
RUN conan remote add conancenter https://center.conan.io False --force
RUN conan remote add conan-center https://conan.bintray.com False --force
RUN conan remote add conan-test http://artipie.artipie:9300 False
RUN conan remote disable conan-center

COPY ./run.sh /root/
WORKDIR /root
CMD "/root/run.sh"
