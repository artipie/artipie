/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.artipie.http;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.MeasuredSlice;
import com.artipie.Settings;
import com.artipie.YamlStorage;
import com.artipie.api.ArtipieApi;
import com.artipie.asto.Key;
import com.artipie.asto.SubStorage;
import com.artipie.dashboard.DashboardSlice;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtPath;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.metrics.MetricSlice;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Pie of slices.
 * @since 0.1
 * @checkstyle ReturnCountCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class Pie extends Slice.Wrap {

    /**
     * Route path returns {@code NO_CONTENT} status if path is empty.
     */
    private static final RtPath EMPTY_PATH = (line, headers, body) -> {
        final String path = new RequestLineFrom(line).uri().getPath();
        final Optional<Response> res;
        if (path.equals("*") || path.equals("/")
            || path.replaceAll("^/+", "").split("/").length == 0) {
            res = Optional.of(new RsWithStatus(RsStatus.NO_CONTENT));
        } else {
            res = Optional.empty();
        }
        return res;
    };

    /**
     * Artipie entry point.
     * @param settings Artipie settings
     */
    public Pie(final Settings settings) {
        super(
            new SafeSlice(
                new TimeoutSlice(
                    new MeasuredSlice(
                        new DockerRoutingSlice(
                            new LoggingSlice(
                                Level.INFO,
                                new SliceRoute(
                                    Pie.EMPTY_PATH,
                                    new RtRulePath(
                                        new RtRule.ByPath(Pattern.compile("/\\.health")),
                                        new HealthSlice(settings)
                                    ),
                                    new RtRulePath(
                                        new RtRule.ByPath(Pattern.compile("/api/?.*")),
                                        new ArtipieApi(settings)
                                    ),
                                    new RtRulePath(
                                        new RtIsDashboard(settings), new DashboardSlice(settings)
                                    ),
                                    new RtRulePath(
                                        new RtRule.All(
                                            new ByMethodsRule(RqMethod.GET),
                                            new RtRule.ByPath("/.metrics")
                                        ),
                                        new OptionalSlice<>(
                                            metricsStorage(settings), Optional::isPresent,
                                            yaml -> new MetricSlice(
                                                new SubStorage(
                                                    new Key.From(".meta", "metrics"),
                                                    new YamlStorage(yaml.orElseThrow())
                                                        .storage()
                                                )
                                            )
                                        )
                                    ),
                                    new RtRulePath(
                                        RtRule.FALLBACK, new SliceByPath(settings)
                                    )
                                )
                            )
                        )
                    ),
                    Duration.ofMinutes(1)
                )
            )
        );
    }

    /**
     * Metrics storage Yaml node.
     * @param settings Artipie settings
     * @return Yaml node, could be null
     */
    private static Optional<YamlMapping> metricsStorage(final Settings settings) {
        try {
            return Optional.ofNullable(settings.meta().yamlMapping("metrics"))
                .flatMap(metrics -> Optional.ofNullable(metrics.yamlMapping("storage")));
        } catch (final IOException err) {
            throw new IllegalStateException("Invalid artipie config", err);
        }
    }
}
