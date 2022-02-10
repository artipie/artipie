/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.http.headers.Accept;
import com.artipie.http.headers.ContentType;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.metrics.Metrics;
import com.artipie.metrics.memory.InMemoryMetrics;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.AnyOf;
import org.hamcrest.core.IsAnything;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.MatchesPattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test case for {@link PromuSlice}.
 *
 * @see <a href="https://sysdig.com/blog/prometheus-metrics/"/>
 * @since 0.23
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ParameterNumberCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle MethodNameCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class PromuSliceTest {

    /**
     * Openmetrics text mime type.
     */
    private static final String OPENMETRICS_TEXT = "application/openmetrics-text";

    /**
     * Plain text mime type.
     */
    private static final String PLAIN_TEXT = "text/plain";

    @ParameterizedTest
    @CsvSource(
        {
            "http.response.length,500,counter,text/plain",
            "http.response.length,500,counter,application/openmetrics-text",
            "app.used.memory,200,gauge,text/plain",
            "app.used.memory,200,gauge,application/openmetrics-text"
        }
    )
    void producesMetrics(
        final String name, final long value,
        final String type, final String mimetype
    ) {
        final Metrics metrics = new InMemoryMetrics();
        collect(metrics, name, value, type);
        MatcherAssert.assertThat(
            new PromuSlice(metrics),
            new SliceHasResponse(
                new AllOf<>(
                    Arrays.asList(
                        new RsHasStatus(RsStatus.OK),
                        new RsHasHeaders(
                            new IsEqual<>(new Header(ContentType.NAME, mimetype)),
                            new IsAnything<>()
                        ),
                        new RsHasBody(
                            new MatchesPattern(
                                Pattern.compile(
                                    metricFormatted(name, value, type, mimetype)
                                )
                            ),
                            StandardCharsets.UTF_8
                        )
                    )
                ),
                new RequestLine(RqMethod.GET, "/prometheus/metrics"),
                new Headers.From(Accept.NAME, mimetype),
                Content.EMPTY
            )
        );
    }

    @Test
    void returnsMetricsThatMatchNames() {
        final Metrics metrics = new InMemoryMetrics();
        metrics.counter("http.response.count").inc();
        metrics.gauge("http.response.content").set(5000L);
        metrics.gauge("app.workload").set(300L);
        MatcherAssert.assertThat(
            new PromuSlice(metrics),
            new SliceHasResponse(
                new RsHasBody(
                    new AnyOf<>(
                        new IsEqual<>(
                            String.join(
                                "\n",
                                "# HELP http_response_content Http response content",
                                "# TYPE http_response_content gauge",
                                "http_response_content 5000.0",
                                "# HELP app_workload App workload",
                                "# TYPE app_workload gauge",
                                "app_workload 300.0",
                                ""
                            )
                        ),
                        new IsEqual<>(
                            String.join(
                                "\n",
                                "# HELP app_workload App workload",
                                "# TYPE app_workload gauge",
                                "app_workload 300.0",
                                "# HELP http_response_content Http response content",
                                "# TYPE http_response_content gauge",
                                "http_response_content 5000.0",
                                ""
                            )
                        )
                    ),
                    StandardCharsets.UTF_8
                ),
                new RequestLine(
                    RqMethod.GET,
                    "/prometheus/metrics?name=http_response_content&name=app_workload"
                ),
                new Headers.From(
                    new Header(Accept.NAME, PromuSliceTest.PLAIN_TEXT)
                ),
                Content.EMPTY
            )
        );
    }

    /**
     * Collect locally a metric.
     * @param metrics All metrics
     * @param name Metric name
     * @param value Metric value
     * @param type Metric type
     */
    private static void collect(
        final Metrics metrics,
        final String name,
        final long value,
        final String type
    ) {
        switch (type) {
            case "counter":
                metrics.counter(name).add(value);
                break;
            case "gauge":
                metrics.gauge(name).set(value);
                break;
            default:
                throw new IllegalArgumentException("Unknown metric type");
        }
    }

    /**
     * Formats metric in Prometheus text/plain.
     * @param name Metric name
     * @param value Metric value
     * @param type Metric type
     * @return Text formatted
     */
    private static String metricInPlainText(
        final String name,
        final long value,
        final String type
    ) {
        final double dvalue = value;
        final String nname = normalize(name);
        final String help = help(name);
        final String text;
        switch (type) {
            case "counter":
                text = String.format(
                    Locale.ENGLISH,
                    String.join(
                        "\n",
                        "# HELP %s_total %s",
                        "# TYPE %s_total counter",
                        "%s_total %.1f",
                        "# HELP %s_created %s",
                        "# TYPE %s_created gauge",
                        "%s_created [0-9]*\\.?[0-9]+(E[0-9]+)?",
                        ""
                    ),
                    nname, help, nname, nname, dvalue, nname, help, nname, nname
                );
                break;
            case "gauge":
                text = String.format(
                    Locale.ENGLISH,
                    String.join(
                        "\n",
                        "# HELP %s %s",
                        "# TYPE %s gauge",
                        "%s %.1f",
                        ""
                    ),
                    nname, help, nname, nname, dvalue
                );
                break;
            default:
                throw new IllegalArgumentException("Unknown metric type");
        }
        return text;
    }

    /**
     * Formats metric in Prometheus application/openmetrics-text.
     * @param name Metric name
     * @param value Metric value
     * @param type Metric type
     * @return Text formatted
     */
    private static String metricInOpenmetricsText(
        final String name,
        final long value,
        final String type
    ) {
        final double dvalue = value;
        final String nname = normalize(name);
        final String help = help(name);
        final String text;
        switch (type) {
            case "counter":
                text = String.format(
                    Locale.ENGLISH,
                    String.join(
                        "\n",
                        "# TYPE %s counter",
                        "# HELP %s %s",
                        "%s_total %.1f",
                        "%s_created [0-9]*\\.?[0-9]+(E[0-9]+)?",
                        "# EOF",
                        ""
                    ),
                    nname, nname, help, nname, dvalue, nname
                );
                break;
            case "gauge":
                text = String.format(
                    Locale.ENGLISH,
                    String.join(
                        "\n",
                        "# TYPE %s gauge",
                        "# HELP %s %s",
                        "%s %.1f",
                        "# EOF",
                        ""
                    ),
                    nname, nname, help, nname, dvalue
                );
                break;
            default:
                throw new IllegalArgumentException("Unknown metric type");
        }
        return text;
    }

    /**
     * Metric formatted according to mime type.
     * @param name Name
     * @param value Value
     * @param type Metric type
     * @param mimetype Mime type
     * @return Metric formatted
     */
    private static String metricFormatted(
        final String name,
        final long value,
        final String type,
        final String mimetype
    ) {
        final String body;
        switch (mimetype) {
            case PromuSliceTest.PLAIN_TEXT:
                body = metricInPlainText(name, value, type);
                break;
            case PromuSliceTest.OPENMETRICS_TEXT:
                body = metricInOpenmetricsText(name, value, type);
                break;
            default:
                throw new IllegalArgumentException("Unknown mime type");
        }
        return body;
    }

    /**
     * Normalizes metrics name.
     * @param name Name
     * @return Normalized name
     */
    private static String normalize(final String name) {
        return name.replaceAll("\\.", "_");
    }

    /**
     * Builds help from name.
     * @param name Name
     * @return Help
     */
    private static String help(final String name) {
        return StringUtils.capitalize(
            name.replaceAll("\\.", " ")
        );
    }
}
