FROM maven:3.6.3-jdk-13 as build_jar
RUN yum install git -y
WORKDIR /jar
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src/ /jar/src/
RUN mvn package

FROM oracle/graalvm-ce:20.0.0-java11
WORKDIR /native
COPY --from=build_jar /jar/target/artipie-jar-with-dependencies.jar /native
RUN gu install native-image
RUN native-image --verbose -H:+TraceClassInitialization --initialize-at-run-time=org.slf4j,org.apache.log4j \
  -jar /native/artipie-jar-with-dependencies.jar

#Error: Classes that should be initialized at run time got initialized during image building:
# org.apache.log4j.Priority was unintentionally initialized at build time. To see why org.apache.log4j.Priority got initialized use -H:+TraceClassInitialization
#org.slf4j.LoggerFactory was unintentionally initialized at build time. To see why org.slf4j.LoggerFactory got initialized use -H:+TraceClassInitialization
#org.apache.log4j.helpers.AppenderAttachableImpl was unintentionally initialized at build time. To see why org.apache.log4j.helpers.AppenderAttachableImpl got initialized use -H:+TraceClassInitialization
#org.slf4j.impl.StaticLoggerBinder was unintentionally initialized at build time. To see why org.slf4j.impl.StaticLoggerBinder got initialized use -H:+TraceClassInitialization
#org.apache.log4j.Category was unintentionally initialized at build time. To see why org.apache.log4j.Category got initialized use -H:+TraceClassInitialization
#org.slf4j.log4j12.Log4jLoggerAdapter was unintentionally initialized at build time. To see why org.slf4j.log4j12.Log4jLoggerAdapter got initialized use -H:+TraceClassInitialization
#org.slf4j.helpers.SubstituteLoggerFactory was unintentionally initialized at build time. To see why org.slf4j.helpers.SubstituteLoggerFactory got initialized use -H:+TraceClassInitialization
#org.apache.log4j.LogManager was unintentionally initialized at build time. To see why org.apache.log4j.LogManager got initialized use -H:+TraceClassInitialization
#org.slf4j.helpers.NamedLoggerBase was unintentionally initialized at build time. To see why org.slf4j.helpers.NamedLoggerBase got initialized use -H:+TraceClassInitialization
#org.apache.log4j.Level was unintentionally initialized at build time. To see why org.apache.log4j.Level got initialized use -H:+TraceClassInitialization
#org.slf4j.helpers.MessageFormatter was unintentionally initialized at build time. To see why org.slf4j.helpers.MessageFormatter got initialized use -H:+TraceClassInitialization
#org.apache.log4j.helpers.LogLog was unintentionally initialized at build time. To see why org.apache.log4j.helpers.LogLog got initialized use -H:+TraceClassInitialization
#org.slf4j.helpers.FormattingTuple was unintentionally initialized at build time. To see why org.slf4j.helpers.FormattingTuple got initialized use -H:+TraceClassInitialization
#org.apache.log4j.spi.DefaultRepositorySelector was unintentionally initialized at build time. To see why org.apache.log4j.spi.DefaultRepositorySelector got initialized use -H:+TraceClassInitialization
#org.slf4j.helpers.MarkerIgnoringBase was unintentionally initialized at build time. To see why org.slf4j.helpers.MarkerIgnoringBase got initialized use -H:+TraceClassInitialization
#org.apache.log4j.Logger was unintentionally initialized at build time. To see why org.apache.log4j.Logger got initialized use -H:+TraceClassInitialization
#org.slf4j.log4j12.Log4jLoggerFactory was unintentionally initialized at build time. To see why org.slf4j.log4j12.Log4jLoggerFactory got initialized use -H:+TraceClassInitialization
#org.slf4j.helpers.NOPLoggerFactory was unintentionally initialized at build time. To see why org.slf4j.helpers.NOPLoggerFactory got initialized use -H:+TraceClassInitialization
#org.apache.log4j.Priority,org.slf4j.LoggerFactory,org.apache.log4j.helpers.AppenderAttachableImpl,org.slf4j.impl.StaticLoggerBinder,org.apache.log4j.Category,org.slf4j.log4j12.Log4jLoggerAdapter,org.slf4j.helpers.SubstituteLoggerFactory,org.apache.log4j.LogManager,org.slf4j.helpers.NamedLoggerBase,org.apache.log4j.Level,org.slf4j.helpers.MessageFormatter,org.apache.log4j.helpers.LogLog,org.slf4j.helpers.FormattingTuple,org.apache.log4j.spi.DefaultRepositorySelector,org.slf4j.helpers.MarkerIgnoringBase,org.apache.log4j.Logger,org.slf4j.log4j12.Log4jLoggerFactory,org.slf4j.helpers.NOPLoggerFactory