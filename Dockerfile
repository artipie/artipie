FROM maven:3.6.3-jdk-13 as build_jar
RUN yum install git -y
WORKDIR /jar
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src/ /jar/src/
RUN mvn package

FROM oracle/graalvm-ce:20.0.0-java11 as graal_build
WORKDIR /native
COPY --from=build_jar /jar/target/artipie-jar-with-dependencies.jar /native
RUN gu install native-image
RUN native-image \
    -H:NumberOfThreads=8 \
    --no-fallback \
    --initialize-at-build-time=org.slf4j,org.apache.log4j,io.netty \
    --initialize-at-run-time=io.netty.channel.DefaultChannelId \
    --initialize-at-run-time=io.netty.util.NetUtil \
    --initialize-at-run-time=io.netty.channel.socket.InternetProtocolFamily \
    --initialize-at-run-time=io.netty.resolver.HostsFileEntriesResolver \
    --initialize-at-run-time=io.netty.resolver.dns.DnsNameResolver, \
    --initialize-at-run-time=io.netty.resolver.dns.DnsServerAddressStreamProviders \
    --initialize-at-run-time=io.netty.resolver.dns.PreferredAddressTypeComparator\$1, \
    --initialize-at-run-time=io.netty.resolver.dns.DefaultDnsServerAddressStreamProvider \
    --initialize-at-run-time=io.netty.buffer.AbstractReferenceCountedByteBuf \
    --initialize-at-run-time=io.netty.handler.codec.http.websocketx.extensions.compression.DeflateEncoder \
    --initialize-at-run-time=io.netty.handler.codec.http.websocketx.extensions.compression.DeflateDecoder \
    --initialize-at-run-time=io.netty.handler.codec.http.HttpObjectEncoder \
    --initialize-at-run-time=io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder \
    --initialize-at-run-time=io.netty.handler.codec.http2.Http2CodecUtil \
    --initialize-at-run-time=io.netty.handler.codec.http2.Http2ConnectionHandler \
    --initialize-at-run-time=io.netty.handler.codec.http2.DefaultHttp2FrameWriter \
    --initialize-at-run-time=io.netty.util.internal.logging.Log4JLogger \
    --initialize-at-run-time=io.netty.handler.ssl.ReferenceCountedOpenSslServerContext \
    --initialize-at-run-time=io.netty.handler.ssl.JdkNpnApplicationProtocolNegotiator \
    --initialize-at-run-time=io.netty.handler.ssl.ReferenceCountedOpenSslEngine \
    --initialize-at-run-time=io.netty.handler.ssl.ConscryptAlpnSslEngine \
    --initialize-at-run-time=io.netty.handler.ssl.JettyNpnSslEngine \
    --initialize-at-run-time=io.netty.handler.ssl.ReferenceCountedOpenSslContext \
    --initialize-at-run-time=io.netty.handler.ssl.ReferenceCountedOpenSslClientContext \
    --report-unsupported-elements-at-runtime \
    --allow-incomplete-classpath \
    -jar /native/artipie-jar-with-dependencies.jar
RUN ls -la


FROM adoptopenjdk/openjdk13:alpine-jre
WORKDIR /app
COPY --from=graal_build /native/artipie-jar-with-dependencies /app