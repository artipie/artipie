/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.Settings;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rt.RtRule;
import java.util.Map;

/**
 * Dashboard routing rule.
 * <p>
 * Checks if settings has {@code org} layout and URI is for dashboard.
 * </p>
 * @since 0.9
 */
final class RtIsDashboard implements RtRule {

    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * New routing rule.
     * @param settings Settings
     */
    RtIsDashboard(final Settings settings) {
        this.settings = settings;
    }

    @Override
    public boolean apply(final String line, final Iterable<Map.Entry<String, String>> headers) {
        return this.settings.layout().hasDashboard()
            && isDashboardPath(new RequestLineFrom(line).uri().getPath());
    }

    /**
     * Check if request path is for dashboard: it starts with /dashboard and has maximum 3 parts.
     * @param path Request path
     * @return True if dashboard
     * @checkstyle MagicNumberCheck (10 lines)
     */
    private static boolean isDashboardPath(final String path) {
        return path.startsWith("/dashboard") && path.replaceAll("^/+", "").split("/").length <= 3;
    }
}
