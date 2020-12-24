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

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.RepoPermissionsFromStorage;
import com.artipie.Settings;
import com.artipie.YamlPermissions;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncSlice;
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
import com.artipie.management.api.ApiAuthSlice;
import com.artipie.management.api.ApiChangeUserPassword;
import com.artipie.management.api.ApiRepoGetSlice;
import com.artipie.management.api.ApiRepoListSlice;
import com.artipie.management.api.ApiRepoUpdateSlice;
import com.artipie.management.api.ContentAsYaml;
import com.artipie.management.api.CookiesAuthScheme;
import com.artipie.management.api.artifactory.AddUpdatePermissionSlice;
import com.artipie.management.api.artifactory.AddUpdateUserSlice;
import com.artipie.management.api.artifactory.CreateRepoSlice;
import com.artipie.management.api.artifactory.DeletePermissionSlice;
import com.artipie.management.api.artifactory.DeleteUserSlice;
import com.artipie.management.api.artifactory.FromRqLine;
import com.artipie.management.api.artifactory.GetPermissionSlice;
import com.artipie.management.api.artifactory.GetPermissionsSlice;
import com.artipie.management.api.artifactory.GetStorageSlice;
import com.artipie.management.api.artifactory.GetUserSlice;
import com.artipie.management.api.artifactory.GetUsersSlice;
import com.artipie.repo.ArtipieStorages;
import com.artipie.repo.ConfigFile;
import com.artipie.repo.ConfigFileApi;
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
 * @checkstyle ClassFanOutComplexityCheck (500 lines)
 * @checkstyle MethodLengthCheck (500 lines)
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
                    Single.fromCallable(settings::credentials).flatMap(SingleInterop::fromFuture),
                    Single.fromCallable(settings::storage).<YamlMapping>flatMap(
                        storage -> SingleInterop.fromFuture(
                            new ConfigFile("_permissions.yaml").valueFrom(storage)
                        ).to(new ContentAsYaml())
                    ).map(yaml -> yaml.yamlMapping("permissions"))
                    .map(YamlPermissions::new),
                    (auth, creds, perm) -> new ApiAuthSlice(
                        auth, perm,
                        new SliceRoute(
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(Pattern.compile("/api/repos/(?:[^/.]+)")),
                                    new ByMethodsRule(RqMethod.GET),
                                    (line, headers) -> URLEncodedUtils.parse(
                                        new RequestLineFrom(line).uri(),
                                        StandardCharsets.UTF_8
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
                                                    StandardCharsets.UTF_8
                                                ).stream()
                                                    .filter(pair -> "repo".equals(pair.getName()))
                                                    .findFirst().orElseThrow().getValue(),
                                                URLEncodedUtils.parse(
                                                    new RequestLineFrom(line).uri(),
                                                    StandardCharsets.UTF_8
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
                                new ApiRepoListSlice(
                                    settings.storage(),
                                    new ConfigFileApi(settings.storage())
                                )
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(Pattern.compile("/api/repos/(?:[^/.]+)/(?:[^/.]+)")),
                                    new ByMethodsRule(RqMethod.GET)
                                ),
                                new ApiRepoGetSlice(new ConfigFileApi(settings.storage()))
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(Pattern.compile("/api/repos/(?:[^/.]+)")),
                                    new ByMethodsRule(RqMethod.POST)
                                ),
                                new ApiRepoUpdateSlice(new ConfigFileApi(settings.storage()))
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(Pattern.compile("/api/users/(?:[^/.]+)/password")),
                                    new ByMethodsRule(RqMethod.POST)
                                ),
                                new ApiChangeUserPassword(creds)
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(FromRqLine.RqPattern.CREATE_REPO.pattern()),
                                    new ByMethodsRule(RqMethod.PUT)
                                ),
                                new CreateRepoSlice(
                                    settings.storage(),
                                    new ConfigFileApi(settings.storage())
                                )
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(FromRqLine.RqPattern.USER.pattern()),
                                    new ByMethodsRule(RqMethod.GET)
                                ),
                                new GetUserSlice(creds)
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(GetUsersSlice.PATH),
                                    new ByMethodsRule(RqMethod.GET)
                                ),
                                new GetUsersSlice(creds, settings.meta())
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(FromRqLine.RqPattern.USER.pattern()),
                                    new ByMethodsRule(RqMethod.DELETE)
                                ),
                                new DeleteUserSlice(creds)
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(FromRqLine.RqPattern.USER.pattern()),
                                    new RtRule.Any(
                                        new ByMethodsRule(RqMethod.PUT),
                                        new ByMethodsRule(RqMethod.POST)
                                    )
                                ),
                                new AddUpdateUserSlice(creds)
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(FromRqLine.RqPattern.REPOS.pattern()),
                                    new ByMethodsRule(RqMethod.GET)
                                ),
                                new GetPermissionsSlice(
                                    new RepoPermissionsFromStorage(settings.storage()),
                                    settings.meta()
                                )
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(FromRqLine.RqPattern.REPO.pattern()),
                                    new ByMethodsRule(RqMethod.PUT)
                                ),
                                new AddUpdatePermissionSlice(
                                    new RepoPermissionsFromStorage(settings.storage())
                                )
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(FromRqLine.RqPattern.REPO.pattern()),
                                    new ByMethodsRule(RqMethod.DELETE)
                                ),
                                new DeletePermissionSlice(
                                    new RepoPermissionsFromStorage(settings.storage()),
                                    new ConfigFileApi(settings.storage())
                                )
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(FromRqLine.RqPattern.REPO.pattern()),
                                    new ByMethodsRule(RqMethod.GET)
                                ),
                                new GetPermissionSlice(
                                    new RepoPermissionsFromStorage(settings.storage()),
                                    new ConfigFileApi(settings.storage())
                                )
                            ),
                            new RtRulePath(
                                new RtRule.All(
                                    new RtRule.ByPath(GetStorageSlice.Request.PATH),
                                    new ByMethodsRule(RqMethod.GET)
                                ),
                                new GetStorageSlice(
                                    new ArtipieStorages(settings.storage()),
                                    settings.layout().pattern()
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
