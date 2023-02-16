/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.http.auth.Tokens;
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
import com.artipie.misc.ArtipieProperties;
import com.artipie.settings.Settings;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Slice Artipie serves on it's main port.
 * The slice handles `/.health`, `/.version` and repositories requests
 * extracting repository name from URI path.
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
     *
     * @param http HTTP client.
     * @param settings Artipie settings.
     * @param tokens Tokens: authentication and generation
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    public MainSlice(
        final ClientSlices http,
        final Settings settings,
        final Tokens tokens
    ) {
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
                        new RtRule.ByPath("/.version")
                    ),
                    new VersionSlice(new ArtipieProperties())
                ),
                new RtRulePath(
                    RtRule.FALLBACK,
                    new DockerRoutingSlice(settings, new SliceByPath(http, settings, tokens))
                )
            )
        );
    }
}
