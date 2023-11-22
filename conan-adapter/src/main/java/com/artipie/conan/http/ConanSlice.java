/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package  com.artipie.conan.http;

import com.artipie.asto.Storage;
import com.artipie.conan.ItemTokenizer;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BearerAuthzSlice;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.auth.TokenAuthentication;
import com.artipie.http.auth.Tokens;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceDownload;
import com.artipie.http.slice.SliceSimple;
import com.artipie.security.perms.Action;
import com.artipie.security.perms.AdapterBasicPermission;
import com.artipie.security.policy.Policy;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Artipie {@link Slice} for Conan repository HTTP API.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ClassFanOutComplexityCheck (500 lines)
 */
public final class ConanSlice extends Slice.Wrap {

    /**
     * Test context name for AuthUser.
     */
    private static final String TEST_CONTEXT = "test";

    /**
     * Anonymous tokens.
     */
    private static final Tokens ANONYMOUS = new Tokens() {

        @Override
        public TokenAuthentication auth() {
            return token ->
                CompletableFuture.completedFuture(
                    Optional.of(new AuthUser("anonymous", ConanSlice.TEST_CONTEXT))
                );
        }

        @Override
        public String generate(final AuthUser user) {
            return "123qwe";
        }
    };

    /**
     * Fake implementation of {@link Tokens} for the single user.
     * @since 0.5
     */
    public static final class FakeAuthTokens implements Tokens {

        /**
         * Token value for the user.
         */
        private final String token;

        /**
         * Username value for the user.
         */
        private final String username;

        /**
         * Ctor.
         * @param token Token value for the user.
         * @param username Username value for the user.
         */
        public FakeAuthTokens(final String token, final String username) {
            this.token = token;
            this.username = username;
        }

        @Override
        public TokenAuthentication auth() {
            return tkn -> {
                Optional<AuthUser> res = Optional.empty();
                if (this.token.equals(tkn)) {
                    res = Optional.of(new AuthUser(this.username, ConanSlice.TEST_CONTEXT));
                }
                return CompletableFuture.completedFuture(res);
            };
        }

        @Override
        public String generate(final AuthUser user) {
            if (user.name().equals(this.username)) {
                return this.token;
            }
            throw new IllegalStateException(String.join("Unexpected user: ", user.name()));
        }
    }

    /**
     * Ctor.
     * @param storage Storage object.
     * @param tokenizer Tokenizer for repository items.
     */
    public ConanSlice(final Storage storage, final ItemTokenizer tokenizer) {
        this(storage, Policy.FREE, Authentication.ANONYMOUS, ConanSlice.ANONYMOUS, tokenizer, "*");
    }

    /**
     * Ctor.
     * @param storage Storage object.
     * @param policy Access policy.
     * @param auth Authentication parameters.
     * @param tokens User auth token generator.
     * @param tokenizer Tokens provider for repository items.
     * @param name Repository name.
     * @checkstyle MethodLengthCheck (200 lines)
     * @checkstyle ParameterNumberCheck (20 lines)
     */
    @SuppressWarnings("PMD.ExcessiveMethodLength")
    public ConanSlice(
        final Storage storage,
        final Policy<?> policy,
        final Authentication auth,
        final Tokens tokens,
        final ItemTokenizer tokenizer,
        final String name
    ) {
        super(
            new SliceRoute(
                new RtRulePath(
                    new RtRule.ByPath("^/v1/ping$"),
                    new SliceSimple(
                        new RsWithHeaders(
                            new RsWithStatus(RsStatus.ACCEPTED),
                            new Headers.From(
                                "X-Conan-Server-Capabilities",
                                "complex_search,revisions,revisions"
                            )
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(new PathWrap.CredsCheck().getPath()),
                        ByMethodsRule.Standard.GET
                    ),
                    new BearerAuthzSlice(
                        new UsersEntity.CredsCheck(),
                        tokens.auth(),
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.ByPath(new PathWrap.UserAuth().getPath()),
                    new UsersEntity.UserAuth(auth, tokens)
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(new PathWrap.DigestForPkgBin().getPath()),
                        ByMethodsRule.Standard.GET
                    ),
                    new BearerAuthzSlice(
                        new ConansEntity.DigestForPkgBin(storage),
                        tokens.auth(),
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(new PathWrap.DigestForPkgSrc().getPath()),
                        ByMethodsRule.Standard.GET
                    ),
                    new BearerAuthzSlice(
                        new ConansEntity.DigestForPkgSrc(storage),
                        tokens.auth(),
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.ByPath(new PathWrap.SearchSrcPkg().getPath()),
                    new BearerAuthzSlice(
                        new ConansEntity.GetSearchSrcPkg(storage),
                        tokens.auth(),
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(new PathWrap.DownloadBin().getPath()),
                        ByMethodsRule.Standard.GET
                    ),
                    new BearerAuthzSlice(
                        new ConansEntity.DownloadBin(storage),
                        tokens.auth(),
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(new PathWrap.DownloadSrc().getPath()),
                        ByMethodsRule.Standard.GET
                    ),
                    new BearerAuthzSlice(
                        new ConansEntity.DownloadSrc(storage),
                        tokens.auth(),
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(new PathWrap.SearchBinPkg().getPath()),
                        ByMethodsRule.Standard.GET
                    ),
                    new BearerAuthzSlice(
                        new ConansEntity.GetSearchBinPkg(storage),
                        tokens.auth(),
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(new PathWrap.PkgBinInfo().getPath()),
                        ByMethodsRule.Standard.GET
                    ),
                    new BearerAuthzSlice(
                        new ConansEntity.GetPkgInfo(storage),
                        tokens.auth(),
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(new PathWrap.PkgBinLatest().getPath()),
                        ByMethodsRule.Standard.GET
                    ),
                    new BearerAuthzSlice(
                        new ConansEntityV2.PkgBinLatest(storage),
                        tokens.auth(),
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(new PathWrap.PkgSrcLatest().getPath()),
                        ByMethodsRule.Standard.GET
                    ),
                    new BearerAuthzSlice(
                        new ConansEntityV2.PkgSrcLatest(storage),
                        tokens.auth(),
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(new PathWrap.PkgBinFile().getPath()),
                        ByMethodsRule.Standard.GET
                    ),
                    new BearerAuthzSlice(
                        new ConansEntityV2.PkgBinFile(storage),
                        tokens.auth(),
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(new PathWrap.PkgBinFiles().getPath()),
                        ByMethodsRule.Standard.GET
                    ),
                    new BearerAuthzSlice(
                        new ConansEntityV2.PkgBinFiles(storage),
                        tokens.auth(),
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(new PathWrap.PkgSrcFile().getPath()),
                        ByMethodsRule.Standard.GET
                    ),
                    new BearerAuthzSlice(
                        new ConansEntityV2.PkgSrcFile(storage),
                        tokens.auth(),
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(new PathWrap.PkgSrcFiles().getPath()),
                        ByMethodsRule.Standard.GET
                    ),
                    new BearerAuthzSlice(
                        new ConansEntityV2.PkgSrcFiles(storage),
                        tokens.auth(),
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new ByMethodsRule(RqMethod.GET),
                    new BearerAuthzSlice(
                        new SliceDownload(storage),
                        tokens.auth(),
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(ConanUpload.UPLOAD_SRC_PATH.getPath()),
                        ByMethodsRule.Standard.POST
                    ),
                    new BearerAuthzSlice(
                        new ConanUpload.UploadUrls(storage, tokenizer),
                        tokens.auth(),
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.WRITE)
                        )
                    )
                ),
                new RtRulePath(
                    new ByMethodsRule(RqMethod.PUT),
                    new ConanUpload.PutFile(storage, tokenizer)
                )
            )
        );
    }
}
