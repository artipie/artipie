/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import io.vertx.ext.web.RoutingContext;
import java.util.Set;

/**
 * Repository name.
 *
 * @since 0.26
 */
@SuppressWarnings("PMD.DataClass")
public class RepositoryName {
    /**
     * Username path parameter name.
     */
    public static final String UNAME = "uname";

    /**
     * Repository path parameter name.
     */
    public static final String RNAME = "rname";

    /**
     * Repository path parameter name.
     */
    public static final String FLAT = "flat";

    /**
     * Words that should not be present inside repository name.
     */
    public static final Set<String> RESTRICTED_REPOS = Set.of(
        "_storages", "_permissions", "_credentials"
    );

    /**
     * Layout.
     */
    private final String layout;

    /**
     * Repository name.
     */
    private final String rname;

    /**
     * User name.
     */
    private final String uname;

    /**
     * Ctor.
     * @param rname Repository name
     * @param uname User name
     * @param layout Layout
     */
    public RepositoryName(final String rname, final String uname, final String layout) {
        this.layout = layout;
        this.rname = rname;
        this.uname = uname;
    }

    /**
     * Creates {@link RepositoryName} instance.
     * @param ctx RoutingContext
     * @param layout Layout
     * @return RepositoryName
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static RepositoryName create(final RoutingContext ctx, final String layout) {
        return new RepositoryName(
            ctx.pathParam(RepositoryName.RNAME),
            ctx.pathParam(RepositoryName.UNAME),
            layout
        );
    }

    /**
     * Creates {@link RepositoryName} instance for 'flat' layout.
     * @param rname Repository name
     * @return RepositoryName
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static RepositoryName create(final String rname) {
        return new RepositoryName(rname, null, RepositoryName.FLAT);
    }

    /**
     * Check if the repository name is allowed.
     * @param rname Repository name.
     * @return True if repository name is allowed
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static boolean allowedReponame(final RepositoryName rname) {
        return allowedReponame(rname.toString());
    }

    /**
     * Check if the repository name is allowed.
     * @param name Repository name.
     * @return True if repository name is allowed
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static boolean allowedReponame(final String name) {
        return RESTRICTED_REPOS.stream().filter(name::contains).findAny().isEmpty();
    }

    /**
     * Provides string representation of repository name in toString() method.
     * <ul>
     *     <li>'reponame' for flat layout</li>
     *     <li>'username/reponame' for org layout</li>
     * </ul>
     * @checkstyle NoJavadocForOverriddenMethodsCheck (10 lines)
     */
    @Override
    public String toString() {
        final String reponame;
        if (RepositoryName.FLAT.equals(this.layout)) {
            reponame = this.rname;
        } else {
            reponame = String.format("%s/%s", this.uname, this.rname);
        }
        return reponame;
    }
}
