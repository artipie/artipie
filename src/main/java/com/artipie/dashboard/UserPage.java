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
package com.artipie.dashboard;

import com.artipie.Settings;
import com.artipie.asto.Key;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.http.rq.RequestLineFrom;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.TemplateLoader;
import io.reactivex.Single;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;

/**
 * User page.
 * @since 0.10
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class UserPage implements Page {

    /**
     * URI path pattern.
     */
    private static final Pattern PTN = Pattern.compile("/dashboard/(?<user>[^/.]+)/?");

    /**
     * Template engine.
     */
    private final Handlebars handlebars;

    /**
     * Settings.
     */
    private final Settings settings;

    /**
     * New page.
     * @param tpl Template loader
     * @param settings Settings
     */
    UserPage(final TemplateLoader tpl, final Settings settings) {
        this.handlebars = new Handlebars(tpl);
        this.settings = settings;
    }

    @Override
    public Single<String> render(final String line,
        final Iterable<Map.Entry<String, String>> headers) {
        final Matcher matcher = PTN.matcher(new RequestLineFrom(line).uri().getPath());
        if (!matcher.matches()) {
            throw new IllegalStateException("Should match");
        }
        final String user = matcher.group("user");
        return Single.fromCallable(this.settings::storage)
            .map(RxStorageWrapper::new)
            .flatMap(str -> str.list(new Key.From(user)))
            .map(
                repos -> this.handlebars.compile("user").apply(
                    new MapOf<>(
                        new MapEntry<>("title", user),
                        new MapEntry<>("user", user),
                        new MapEntry<>(
                            "repos",
                            repos.stream()
                                .map(key -> key.string().replace(".yaml", ""))
                                .collect(Collectors.toList())
                        )
                    )
                )
            );
    }
}
