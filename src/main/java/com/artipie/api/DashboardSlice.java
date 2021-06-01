/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.Settings;
import com.artipie.YamlPermissions;
import com.artipie.asto.Concatenation;
import com.artipie.asto.Remaining;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncSlice;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.management.api.ApiAuthSlice;
import com.artipie.management.api.CookiesAuthScheme;
import com.artipie.management.dashboard.PageSlice;
import com.artipie.management.dashboard.RepoPage;
import com.artipie.management.dashboard.UserPage;
import com.artipie.repo.ConfigFile;
import com.artipie.repo.ConfigFileApi;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Single;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Slice with dashboard.
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class DashboardSlice extends Slice.Wrap {

    /**
     * New dashboard slice.
     * @param settings Settings
     */
    public DashboardSlice(final Settings settings) {
        this(settings, new ClassPathTemplateLoader("/dashboard", ".hbs"));
    }

    /**
     * Primary ctor.
     * @param settings Settings
     * @param tpl Template loader for pages
     * @todo #797:30min When `ContentAs` is used here instead of `Concatenation` and
     *  `Remaining` `ArtipieApiITCase` get stuck on github actions (this does not happen
     *  locally on windows os), figure out why, make necessary corrections and
     *  use `ContentAs` here. Probably this problem is similar to artipie/artipie#790.
     */
    private DashboardSlice(final Settings settings, final TemplateLoader tpl) {
        // @checkstyle LineLengthCheck (100 lines)
        super(
            new AsyncSlice(
                Single.zip(
                    Single.fromCallable(settings::auth).flatMap(SingleInterop::fromFuture),
                    Single.fromCallable(settings::storage).flatMap(
                        storage -> SingleInterop.fromFuture(
                            new ConfigFile("_permissions.yaml").valueFrom(storage)
                        ).flatMap(data -> new Concatenation(data).single())
                    ).map(buf -> new Remaining(buf).bytes())
                    .map(bytes -> Yaml.createYamlInput(new String(bytes, StandardCharsets.UTF_8)).readYamlMapping())
                    .map(yaml -> yaml.yamlMapping("permissions"))
                    .map(YamlPermissions::new),
                    (auth, perm) -> new ApiAuthSlice(
                        auth, perm,
                        new SliceRoute(
                            new RtRulePath(
                                new RtRule.ByPath(Pattern.compile("/dashboard/(?:[^/.]+)/?")),
                                new PageSlice(
                                    new UserPage(
                                        tpl,
                                        settings.storage(),
                                        new ConfigFileApi(settings.storage())
                                    )
                                )
                            ),
                            new RtRulePath(
                                new RtRule.ByPath(Pattern.compile("/dashboard/(?:[^/.]+)/(?:[^/.]+)/?")),
                                new PageSlice(
                                    new RepoPage(
                                        tpl, new ConfigFileApi(settings.storage())
                                    )
                                )
                            )
                        ),
                        new CookiesAuthScheme()
                    )
                ).to(SingleInterop.get())
            )
        );
    }
}
