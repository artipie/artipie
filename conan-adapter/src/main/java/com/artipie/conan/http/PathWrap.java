/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021-2023 Artipie
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
package  com.artipie.conan.http;

import java.util.regex.Pattern;

/**
 * Wrapper for Conan protocol request paths.
 * @since 0.1
 */
public abstract class PathWrap {
    /**
     * Path pattern for specific request type.
     */
    private final String path;

    /**
     * Pattern object instance for path.
     */
    private final Pattern pattern;

    /**
     * Ctor.
     * @param path Path pattern for specific request type.
     */
    protected PathWrap(final String path) {
        this.path = path;
        this.pattern = Pattern.compile(path);
    }

    /**
     * Returns path.
     * @return Path as String.
     */
    final String getPath() {
        return this.path;
    }

    /**
     * Returns Pattern for provided path.
     * @return Pattern instance.
     */
    final Pattern getPattern() {
        return this.pattern;
    }

    /**
     * Request path for /download_urls request (package sources).
     * @since 0.1
     */
    public static final class DownloadSrc extends PathWrap {
        /**
         * Ctor.
         */
        protected DownloadSrc() {
            super("^/v1/conans/(?<path>.*)/download_urls$");
        }
    }

    /**
     * Request path for /download_urls request (package binary).
     * @since 0.1
     */
    public static final class DownloadBin extends PathWrap {
        /**
         * Ctor.
         */
        protected DownloadBin() {
            super("^/v1/conans/(?<path>.*)/packages/(?<hash>[0-9,a-f]*)/download_urls$");
        }
    }

    /**
     * Request path for /search reqest (for package binaries).
     * @since 0.1
     */
    public static final class SearchBinPkg extends PathWrap {
        /**
         * Ctor.
         */
        protected SearchBinPkg() {
            super("^/v1/conans/(?<path>.*)/search$");
        }
    }

    /**
     * Request path for package binary info by its hash.
     * @since 0.1
     */
    public static final class PkgBinInfo extends PathWrap {
        /**
         * Ctor.
         */
        protected PkgBinInfo() {
            super("^/v1/conans/(?<path>.*)/packages/(?<hash>[0-9,a-f]*)$");
        }
    }

    /**
     * Request path for /search reqest (for package recipes).
     * @since 0.1
     */
    public static class SearchSrcPkg extends PathWrap {
        /**
         * Ctor.
         */
        protected SearchSrcPkg() {
            super("^/v1/conans/search$");
        }
    }

    /**
     * Request path for Conan V2 latest revision info request (package sources).
     * @since 0.1
     */
    public static final class PkgSrcLatest extends PathWrap {
        /**
         * Ctor.
         */
        protected PkgSrcLatest() {
            super("^/v2/conans/(?<path>.*)/latest$");
        }
    }

    /**
     * Request path for V2 package binary latest revision info by its hash.
     * @since 0.1
     */
    public static final class PkgBinLatest extends PathWrap {
        /**
         * Ctor.
         * @checkstyle LineLengthCheck (5 lines)
         */
        protected PkgBinLatest() {
            super("^/v2/conans/(?<path>.*)/revisions/(?<rev>[0-9]*)/packages/(?<hash>[0-9,a-f]*)/latest$");
        }
    }

    /**
     * Request path for V2 /files request for recipe files list (package sources).
     * @since 0.1
     */
    public static final class PkgSrcFiles extends PathWrap {
        /**
         * Ctor.
         */
        protected PkgSrcFiles() {
            super("^/v2/conans/(?<path>.*)/revisions/(?<rev>[0-9]*)/files$");
        }
    }

    /**
     * Request path for V2 files/>file< request for package recipe.
     * @since 0.1
     */
    public static final class PkgSrcFile extends PathWrap {
        /**
         * Ctor.
         */
        protected PkgSrcFile() {
            super("^/v2/conans/(?<path>.*)/revisions/(?<rev>[0-9]*)/files/(?<file>.*)$");
        }
    }

    /**
     * Request path for package binary files list by its hash.
     * @since 0.1
     */
    public static final class PkgBinFiles extends PathWrap {
        /**
         * Ctor.
         * @checkstyle LineLengthCheck (5 lines)
         */
        protected PkgBinFiles() {
            super("^/v2/conans/(?<path>.*)/revisions/(?<rev>[0-9]*)/packages/(?<hash>[0-9,a-f]*)/revisions/(?<rev2>[0-9]*)/files$");
        }
    }

    /**
     * Request path for downloading Conan package binary files.
     * @since 0.1
     */
    public static final class PkgBinFile extends PathWrap {
        /**
         * Ctor.
         * @checkstyle LineLengthCheck (5 lines)
         */
        @SuppressWarnings("LineLengthCheck")
        protected PkgBinFile() {
            super("^/v2/conans/(?<path>.*)/revisions/(?<rev>[0-9]*)/packages/(?<hash>[0-9,a-f]*)/revisions/(?<rev2>[0-9]*)/files/(?<file>.*)$");
        }
    }

    /**
     * Request for user auth.
     * @since 0.1
     */
    public static class UserAuth extends PathWrap {

        /**
         * Ctor.
         */
        protected UserAuth() {
            super("^/v1/users/authenticate$");
        }
    }

    /**
     * Request user credentials check.
     * @since 0.1
     */
    public static class CredsCheck extends PathWrap {

        /**
         * Ctor.
         */
        protected CredsCheck() {
            super("^/v1/users/check_credentials$");
        }
    }

    /**
     * Request to check package recipe presense and retrieving manifest.
     * @since 0.1
     */
    public static class DigestForPkgSrc extends PathWrap {
        /**
         * Ctor.
         */
        protected DigestForPkgSrc() {
            super("^/v1/conans/(?<path>.*)/digest");
        }
    }

    /**
     * Request to check package binary presense and retrieving manifest.
     * @since 0.1
     */
    public static class DigestForPkgBin extends PathWrap {
        /**
         * Ctor.
         */
        protected DigestForPkgBin() {
            super("^/v1/conans/(?<path>.*)/packages/(?<hash>[0-9,a-f]*)/digest");
        }
    }

    /**
     * Request for package sources info by hash.
     * @since 0.1
     */
    public static class PkgSrcInfo extends PathWrap {
        /**
         * Ctor.
         */
        protected PkgSrcInfo() {
            super("^/v1/conans/(?<path>.*)$");
        }
    }

    /**
     * Request path for /upload_urls request.
     * @since 0.1
     */
    public static final class UploadSrc extends PathWrap {

        /**
         * Ctor.
         */
        protected UploadSrc() {
            super("^/v1/conans/(?<path>.*)/upload_urls$");
        }
    }
}
