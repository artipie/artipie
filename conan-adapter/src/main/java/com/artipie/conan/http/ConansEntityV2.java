/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package  com.artipie.conan.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.http.rq.RequestLineFrom;
import io.vavr.Tuple2;
import java.io.StringReader;
import java.net.URLConnection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;

/**
 * Conan /v2/conans/* REST APIs.
 * Conan recognizes two types of packages: package binary and package recipe (sources).
 * Package recipe ("source code") could be built to multiple package binaries with different
 * configuration (conaninfo.txt).
 * Artipie-conan storage structure for now corresponds to standard conan_server.
 * @since 0.1
 */
public final class ConansEntityV2 {

    /**
     * File with binary package information on corresponding build configuration.
     */
    private static final String CONAN_INFO = "conaninfo.txt";

    /**
     * Manifest file stores list of package files with their hashes.
     */
    private static final String CONAN_MANIFEST = "conanmanifest.txt";

    /**
     * Main files of package binary.
     */
    private static final String[] PKG_BIN_LIST = {
        ConansEntityV2.CONAN_MANIFEST, ConansEntityV2.CONAN_INFO, "conan_package.tgz",
    };

    /**
     * Main files of package recipe.
     */
    private static final String[] PKG_SRC_LIST = {
        ConansEntityV2.CONAN_MANIFEST, "conan_export.tgz", "conanfile.py", "conan_sources.tgz",
    };

    /**
     * Field for file name in the request path.
     */
    private static final String FILE = "file";

    /**
     * Field for revision number in the request path.
     */
    private static final String REV = "rev";

    /**
     * Field for package name/path in the request path.
     */
    private static final String PATH = "path";

    /**
     * Field for 2nd revision number in the request path.
     */
    private static final String REV_2 = "rev2";

    /**
     * Field for package binary hash in the request path.
     */
    private static final String HASH = "hash";

    /**
     * Field for package binary path template.
     */
    private static final String PKG_BIN_PATH = "%1$s/%2$s/package/%3$s/%4$s/%5$s";

    /**
     * Field for package recipe (source) path template.
     */
    private static final String PKG_SRC_PATH = "%1$s/%2$s/export/%3$s";

    /**
     * This class is not instantiated.
     */
    private ConansEntityV2() {
    }

    /**
     * Get HTTP Content-Type value for given file name.
     * @param filename File name as String.
     * @return Content-type value as String.
     */
    private static String getContentType(final String filename) {
        String type = URLConnection.guessContentTypeFromName(filename);
        if (type == null) {
            final int index = filename.lastIndexOf('.');
            final String ext = filename.substring(index);
            if (ext.equals(".py")) {
                type = "text/x-python";
            } else if (ext.equals(".tgz")) {
                type = "x-gzip";
            } else {
                type = "application/octet-stream";
            }
        }
        return type;
    }

    /**
     * Wraps json string inside "files" object.
     * @param builder Json object builder filled with values.
     * @return String with json object.
     */
    private static String asFilesJson(final JsonObjectBuilder builder) {
        return Json.createObjectBuilder().add("files", builder).build().toString();
    }

    /**
     * Gets latest revision record from Conan revisions.txt json file.
     * @param key Artipie storage key for revisions.txt file.
     * @param storage Artipie storage instance.
     * @return Request result Future with last revision record as String.
     */
    private static CompletableFuture<BaseConanSlice.RequestResult> getLatestRevisionJson(
        final Key key, final Storage storage) {
        return storage.exists(key).thenCompose(
            exist -> {
                final CompletableFuture<BaseConanSlice.RequestResult> result;
                if (exist) {
                    result = storage.value(key).thenCompose(
                        content -> new PublisherAs(content).asciiString().thenApply(
                            string -> {
                                final JsonParser parser = Json.createParser(
                                    new StringReader(string)
                                );
                                parser.next();
                                final JsonArray revisions = parser.getObject()
                                    .getJsonArray("revisions");
                                final Optional<JsonValue> max = revisions.stream().max(
                                    (v1, v2) -> {
                                        final String revision = "revision";
                                        final String revx = v1.asJsonObject().getString(revision);
                                        final String revy = v2.asJsonObject().getString(revision);
                                        return Integer.parseInt(revx) - Integer.parseInt(revy);
                                    });
                                final String maxrev = max.get().toString();
                                return new BaseConanSlice.RequestResult(maxrev);
                            }
                        )
                    );
                } else {
                    result = CompletableFuture.completedFuture(new BaseConanSlice.RequestResult());
                }
                return result;
            }
        );
    }

    /**
     * Provides latest revision for package binary.
     * @since 0.1
     */
    public static final class PkgBinLatest extends BaseConanSlice {

        /**
         * Ctor.
         * @param storage Current Artipie storage instance.
         */
        public PkgBinLatest(final Storage storage) {
            super(storage, new PathWrap.PkgBinLatest());
        }

        @Override
        public CompletableFuture<RequestResult> getResult(
            final RequestLineFrom request, final String hostname, final Matcher matcher
        ) {
            final Key key = new Key.From(
                String.format(
                    "%1$s/%2$s/package/%3$s/revisions.txt", matcher.group(ConansEntityV2.PATH),
                    matcher.group(ConansEntityV2.REV), matcher.group(ConansEntityV2.HASH)
                ));
            return ConansEntityV2.getLatestRevisionJson(key, getStorage());
        }
    }

    /**
     * Provides latest revision for package source (recipe).
     * @since 0.1
     */
    public static final class PkgSrcLatest extends BaseConanSlice {

        /**
         * Ctor.
         * @param storage Current Artipie storage instance.
         */
        public PkgSrcLatest(final Storage storage) {
            super(storage, new PathWrap.PkgSrcLatest());
        }

        @Override
        public CompletableFuture<RequestResult> getResult(
            final RequestLineFrom request, final String hostname, final Matcher matcher
        ) {
            final Key key = new Key.From(
                String.format(
                    "%1$s/revisions.txt", matcher.group(ConansEntityV2.PATH)
                ));
            return ConansEntityV2.getLatestRevisionJson(key, getStorage());
        }
    }

    /**
     * Provides requested file for package binary.
     * @since 0.1
     */
    public static final class PkgBinFile extends BaseConanSlice {

        /**
         * Ctor.
         * @param storage Current Artipie storage instance.
         */
        public PkgBinFile(final Storage storage) {
            super(storage, new PathWrap.PkgBinFile());
        }

        @Override
        public CompletableFuture<RequestResult> getResult(
            final RequestLineFrom request, final String hostname, final Matcher matcher
        ) {
            final Key key = new Key.From(
                String.format(
                    ConansEntityV2.PKG_BIN_PATH, matcher.group(ConansEntityV2.PATH),
                    matcher.group(ConansEntityV2.REV), matcher.group(ConansEntityV2.HASH),
                    matcher.group(ConansEntityV2.REV_2), matcher.group(ConansEntityV2.FILE)
                ));
            return getStorage().exists(key).thenCompose(
                exist -> {
                    final CompletableFuture<RequestResult> result;
                    if (exist) {
                        result = getStorage().value(key).thenCompose(
                            content -> new PublisherAs(content).bytes().thenApply(
                                bytes -> new RequestResult(
                                    bytes, ConansEntityV2.getContentType(key.string())
                                ))
                        );
                    } else {
                        result = CompletableFuture.completedFuture(new RequestResult());
                    }
                    return result;
                }
            );
        }
    }

    /**
     * Provides list of files for package binary.
     * @since 0.1
     */
    public static final class PkgBinFiles extends BaseConanSlice {

        /**
         * Ctor.
         * @param storage Current Artipie storage instance.
         */
        public PkgBinFiles(final Storage storage) {
            super(storage, new PathWrap.PkgBinFiles());
        }

        @Override
        public CompletableFuture<RequestResult> getResult(
            final RequestLineFrom request, final String hostname, final Matcher matcher
        ) {
            return BaseConanSlice.generateJson(
                ConansEntityV2.PKG_BIN_LIST, file -> {
                    final Key key = new Key.From(
                        String.format(
                            ConansEntityV2.PKG_BIN_PATH, matcher.group(ConansEntityV2.PATH),
                            matcher.group(ConansEntityV2.REV), matcher.group(ConansEntityV2.HASH),
                            matcher.group(ConansEntityV2.REV_2), file
                        ));
                    return new Tuple2<>(key, getStorage().exists(key));
                }, tuple -> Optional.of("").filter(t -> tuple._2()), ConansEntityV2::asFilesJson
            );
        }
    }

    /**
     * Provides requested file for package source (recipe).
     * @since 0.1
     */
    public static final class PkgSrcFile extends BaseConanSlice {

        /**
         * Ctor.
         * @param storage Current Artipie storage instance.
         */
        public PkgSrcFile(final Storage storage) {
            super(storage, new PathWrap.PkgSrcFile());
        }

        @Override
        public CompletableFuture<RequestResult> getResult(
            final RequestLineFrom request, final String hostname, final Matcher matcher
        ) {
            final Key key = new Key.From(
                String.format(
                    ConansEntityV2.PKG_SRC_PATH, matcher.group(ConansEntityV2.PATH),
                    matcher.group(ConansEntityV2.REV), matcher.group(ConansEntityV2.FILE)
                ));
            return getStorage().exists(key).thenCompose(
                exist -> {
                    final CompletableFuture<RequestResult> result;
                    if (exist) {
                        result = getStorage().value(key).thenCompose(
                            content -> new PublisherAs(content).bytes().thenApply(
                                bytes -> new RequestResult(
                                    bytes, ConansEntityV2.getContentType(key.string())
                                ))
                        );
                    } else {
                        result = CompletableFuture.completedFuture(new RequestResult());
                    }
                    return result;
                }
            );
        }
    }

    /**
     * Provides list of files for package source (recipe).
     * @since 0.1
     */
    public static final class PkgSrcFiles extends BaseConanSlice {

        /**
         * Ctor.
         * @param storage Current Artipie storage instance.
         */
        public PkgSrcFiles(final Storage storage) {
            super(storage, new PathWrap.PkgSrcFiles());
        }

        @Override
        public CompletableFuture<RequestResult> getResult(
            final RequestLineFrom request, final String hostname, final Matcher matcher
        ) {
            return BaseConanSlice.generateJson(
                ConansEntityV2.PKG_SRC_LIST, file -> {
                    final Key key = new Key.From(
                        String.format(
                            ConansEntityV2.PKG_SRC_PATH, matcher.group(ConansEntityV2.PATH),
                            matcher.group(ConansEntityV2.REV), file
                        ));
                    return new Tuple2<>(key, getStorage().exists(key));
                }, tuple -> Optional.of("").filter(t -> tuple._2()), ConansEntityV2::asFilesJson
            );
        }
    }
}
