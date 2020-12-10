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
import com.artipie.Settings;
import com.artipie.YamlStorage;
import com.artipie.api.ArtipieApi;
import com.artipie.api.DashboardSlice;
import com.artipie.asto.Key;
import com.artipie.asto.SubStorage;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtPath;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.metrics.MetricSlice;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Slice Artipie serves on it's main port.
 * The slice handles `/.health`, `/.metrics`, `/.meta/metrics`, `/api`, `/dashboard`
 * and repository requests extracting repository name from URI path.
 *
 * @since 0.11
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class MainSlice extends Slice.Wrap {

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
    public MainSlice(final Settings settings) {
        super(
            new SliceRoute(
                MainSlice.EMPTY_PATH,
                new RtRulePath(
                    new RtRule.ByPath(Pattern.compile("/\\.health")),
                    new HealthSlice(settings)
                ),
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.GET),
                        new RtRule.ByPath("/.metrics")
                    ),
                    new OptionalSlice<>(
                        metricsStorage(settings),
                        Optional::isPresent,
                        yaml -> new MetricSlice(
                            new SubStorage(
                                new Key.From(".meta", "metrics"),
                                new YamlStorage(yaml.orElseThrow()).storage()
                            )
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.ByPath(Pattern.compile("/api/?.*")),
                    new ArtipieApi(settings)
                ),
                new RtRulePath(
                    new RtIsDashboard(settings),
                    new DashboardSlice(settings)
                ),
                new RtRulePath(
                    RtRule.FALLBACK,
                    new AllRepositoriesSlice(settings)
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
        return Optional.ofNullable(settings.meta().yamlMapping("metrics"))
            .flatMap(metrics -> Optional.ofNullable(metrics.yamlMapping("storage")));
    }
}
