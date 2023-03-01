FROM artipie/artipie:latest

LABEL description="Artipie binary repository management tool that includes all kinds of Artipie storages"
LABEL maintainer="andpopov@gmail.com"

USER 0:0

RUN mkdir -p /tmp/deps/
COPY ./AstoVersion.java /tmp/deps

RUN cd /tmp/deps && \
    jar -xvf /usr/lib/artipie/artipie.jar && \
    version=$(javac ./AstoVersion.java && java -cp . AstoVersion ./META-INF/maven/com.artipie/artipie/pom.xml) && \
    mkdir -p /tmp/deps/lib && \
    \
    curl -sL "https://raw.githubusercontent.com/artipie/asto/${version}/pom.xml" -o pom.xml && \
    \
    curl -sL https://dlcdn.apache.org/maven/maven-3/3.9.0/binaries/apache-maven-3.9.0-bin.tar.gz  | tar zx && \
    PATH=/tmp/deps/apache-maven-3.9.0/bin:$PATH && \
    mkdir -p /tmp/deps/.m2 && \
    \
    curl -sL "https://github.com/artipie/asto/archive/refs/tags/${version}.tar.gz" | tar zx && \
    \
    cd "asto-${version:1}" && \
    mvn install -DskipTests -Dmaven.repo.local=/tmp/deps/.m2 && \
    mvn dependency:copy-dependencies -Dmaven.repo.local=/tmp/deps/.m2 -DoutputDirectory=/tmp/deps/lib && \
    \
    rm -rf /tmp/deps/lib/asto*.jar && \
    rm -rf /tmp/deps/lib/*artipie*.jar && \
    \
    cd /tmp/deps/lib && \
    curl -sL "https://repo1.maven.org/maven2/com/artipie/asto-s3/${version}/asto-s3-${version}.jar" -o "asto-s3-${version}.jar" && \
    curl -sL "https://repo1.maven.org/maven2/com/artipie/asto-etcd/${version}/asto-etcd-${version}.jar" -o "asto-etcd-${version}.jar" && \
    curl -sL "https://repo1.maven.org/maven2/com/artipie/asto-redis/${version}/asto-redis-${version}.jar" -o "asto-redis-${version}.jar" && \
    curl -sL "https://repo1.maven.org/maven2/com/artipie/asto-vertx-file/${version}/asto-vertx-file-${version}.jar" -o "asto-vertx-file-${version}.jar" && \
    \
    cp -r -n ./* /usr/lib/artipie/lib && \
    \
    cd /tmp && \
    rm -rf /tmp/deps
USER 2021:2020