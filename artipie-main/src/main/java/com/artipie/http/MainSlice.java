/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.RepositorySlices;
import com.artipie.http.rq.RqMethod;
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
 */
public final class MainSlice extends Slice.Wrap {

    /**
     * Route path returns {@code NO_CONTENT} status if path is empty.
     */
    private static final RtPath EMPTY_PATH = (line, headers, body) -> {
        final String path = line.uri().getPath();
        if (path.equals("*") || path.equals("/")
            || path.replaceAll("^/+", "").split("/").length == 0) {
            return Optional.of(BaseResponse.noContent());
        }
        return Optional.empty();
    };

    /**
     * Artipie entry point.
     *
     * @param settings Artipie settings.
     */
    public MainSlice(
        final Settings settings,
        final RepositorySlices slices
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
                    new DockerRoutingSlice(
                        settings, new SliceByPath(slices)
                    )
                )
            )
        );
    }
}
