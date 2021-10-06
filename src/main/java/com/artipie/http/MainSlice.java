/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.ArtipieProperties;
import com.artipie.Settings;
import com.artipie.YamlStorage;
import com.artipie.api.ArtipieApi;
import com.artipie.api.DashboardSlice;
import com.artipie.asto.Key;
import com.artipie.asto.SubStorage;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtPath;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceOptional;
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
     * @param http HTTP client
     * @param settings Artipie settings
     */
    public MainSlice(final ClientSlices http, final Settings settings) {
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
                    new SliceOptional<>(
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
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.GET),
                        new RtRule.ByPath("/.version")
                    ),
                    new VersionSlice(new ArtipieProperties())
                ),
                new RtRulePath(
                    new RtRule.ByPath(Pattern.compile("/api/?.*")),
                    new ArtipieApi(http, settings)
                ),
                new RtRulePath(
                    new RtIsDashboard(settings),
                    new DashboardSlice(settings)
                ),
                new RtRulePath(
                    RtRule.FALLBACK,
                    new AllRepositoriesSlice(http, settings)
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
