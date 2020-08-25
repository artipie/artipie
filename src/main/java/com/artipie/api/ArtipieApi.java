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
package com.artipie.api;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.Settings;
import com.artipie.YamlPermissions;
import com.artipie.api.artifactory.AddUpdateUserSlice;
import com.artipie.api.artifactory.CreateRepoSlice;
import com.artipie.api.artifactory.DeleteUserSlice;
import com.artipie.api.artifactory.GetPermissionsSlice;
import com.artipie.api.artifactory.GetUserSlice;
import com.artipie.api.artifactory.GetUsersSlice;
import com.artipie.asto.Concatenation;
import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncSlice;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.SliceAuth;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Single;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

/**
 * Artipie API endpoints.
 * @since 0.6
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.ExcessiveMethodLength"})
public final class ArtipieApi extends Slice.Wrap {

    /**
     * New Artipie API.
     * @param settings Artipie settings
     * @todo #444:30min Constructor decomposition
     *  This constructor is very huge, difficult to read and understand: extract some methods,
     *  wrappers, classes, etc from it to make it more elegant.
     */
    public ArtipieApi(final Settings settings) {
        // @checkstyle LineLengthCheck (500 lines)
        super(
            new AsyncSlice(
                Single.zip(
                    Single.fromCallable(settings::auth).flatMap(SingleInterop::fromFuture),
                    Single.fromCallable(settings::storage).map(RxStorageWrapper::new)
                        .flatMap(storage -> storage.value(new Key.From("_permissions.yaml")).flatMap(data -> new Concatenation(data).single()))
                        .map(buf -> new Remaining(buf).bytes())
                        .map(bytes -> Yaml.createYamlInput(new String(bytes, StandardCharsets.UTF_8)).readYamlMapping())
                        .map(YamlPermissions::new),
                    (auth, perm) -> new SliceAuth(
                        new SliceRoute(
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(Pattern.compile("/api/repos/(?:[^/.]+)")),
                                    new ByMethodsRule(RqMethod.GET),
                                    (line, headers) -> URLEncodedUtils.parse(
                                        new RequestLineFrom(line).uri(),
                                        StandardCharsets.UTF_8.displayName()
                                    ).stream().anyMatch(pair -> "repo".equals(pair.getName()))
                                ),
                                (line, headers, body) -> {
                                    final Matcher matcher = Pattern.compile("/api/repos/(?<user>[^/.]+)")
                                        .matcher(new RequestLineFrom(line).uri().getPath());
                                    if (!matcher.matches()) {
                                        throw new IllegalStateException("Should match");
                                    }
                                    return new RsWithHeaders(
                                        new RsWithStatus(RsStatus.FOUND),
                                        new Header(
                                            "Location",
                                            String.format(
                                                "/dashboard/%s/%s?type=%s",
                                                matcher.group("user"),
                                                URLEncodedUtils.parse(
                                                    new RequestLineFrom(line).uri(),
                                                    StandardCharsets.UTF_8.displayName()
                                                ).stream()
                                                    .filter(pair -> "repo".equals(pair.getName()))
                                                    .findFirst().orElseThrow().getValue(),
                                                URLEncodedUtils.parse(
                                                    new RequestLineFrom(line).uri(),
                                                    StandardCharsets.UTF_8.displayName()
                                                ).stream()
                                                    .filter(pair -> "type".equals(pair.getName()))
                                                    .findFirst().map(NameValuePair::getValue)
                                                    .orElse("maven")
                                            )
                                        )
                                    );
                                }
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(Pattern.compile("/api/repos/(?:[^/.]+)")),
                                    new ByMethodsRule(RqMethod.GET)
                                ),
                                new ApiRepoListSlice(settings)
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(Pattern.compile("/api/repos/(?:[^/.]+)/(?:[^/.]+)")),
                                    new ByMethodsRule(RqMethod.GET)
                                ),
                                new ApiRepoGetSlice(settings)
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(Pattern.compile("/api/repos/(?:[^/.]+)")),
                                    new ByMethodsRule(RqMethod.POST)
                                ),
                                new ApiRepoUpdateSlice(settings)
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(Pattern.compile("/api/users/(?:[^/.]+)/password")),
                                    new ByMethodsRule(RqMethod.POST)
                                ),
                                new ApiChangeUserPassword(settings)
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(Pattern.compile("/api/repositories/.*")),
                                    new ByMethodsRule(RqMethod.PUT)
                                ),
                                new CreateRepoSlice(settings)
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(GetUserSlice.PTRN),
                                    new ByMethodsRule(RqMethod.GET)
                                ),
                                new GetUserSlice(settings)
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(GetUsersSlice.PATH),
                                    new ByMethodsRule(RqMethod.GET)
                                ),
                                new GetUsersSlice(settings)
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(GetUserSlice.PTRN),
                                    new ByMethodsRule(RqMethod.DELETE)
                                ),
                                new DeleteUserSlice(settings)
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(GetUserSlice.PTRN),
                                    new RtRule.Any(
                                        new ByMethodsRule(RqMethod.PUT),
                                        new ByMethodsRule(RqMethod.POST)
                                    )
                                ),
                                new AddUpdateUserSlice(settings)
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(GetPermissionsSlice.PATH),
                                    new ByMethodsRule(RqMethod.GET)
                                ),
                                new GetPermissionsSlice(settings)
                            )
                        ),
                        new Permission.ByName("api", perm),
                        new AuthApi(auth)
                    )
                ).to(SingleInterop.get())
            )
        );
    }
}
