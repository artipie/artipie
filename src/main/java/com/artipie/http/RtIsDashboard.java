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
package com.artipie.http;

import com.artipie.Settings;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rt.RtRule;
import java.io.IOException;
import java.io.UncheckedIOException;
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
        return isOrg(this.settings)
            && isDashboardPath(new RequestLineFrom(line).uri().getPath());
    }

    /**
     * Check if layout is org.
     * @param settings Artipie settings
     * @return True if org
     */
    private static boolean isOrg(final Settings settings) {
        try {
            return settings.layout().equals("org");
        } catch (final IOException err) {
            throw new UncheckedIOException("Failed to read layout settings", err);
        }
    }

    /**
     * Check if request path is for dashboard.
     * @param path Request path
     * @return True if dashboard
     */
    private static boolean isDashboardPath(final String path) {
        return path.replaceAll("^/+", "").split("/").length <= 2;
    }
}
