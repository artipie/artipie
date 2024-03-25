/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.hex.http;

import com.artipie.ArtipieException;
import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.OneTimePublisher;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.hex.http.headers.HexContentType;
import com.artipie.hex.proto.generated.PackageOuterClass;
import com.artipie.hex.proto.generated.SignedOuterClass;
import com.artipie.hex.tarball.MetadataConfig;
import com.artipie.hex.tarball.TarReader;
import com.artipie.hex.utils.Gzip;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Login;
import com.artipie.http.rq.RequestLine;
import com.artipie.scheduling.ArtifactEvent;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.reactivestreams.Publisher;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This slice creates package meta-info from request body(tar-archive) and saves this tar-archive.
 */
@SuppressWarnings("PMD.ExcessiveMethodLength")
public final class UploadSlice implements Slice {
    /**
     * Path to publish.
     */
    static final Pattern PUBLISH = Pattern.compile("(/repos/)?(?<org>.+)?/publish");

    /**
     * Query to publish.
     */
    static final Pattern QUERY = Pattern.compile("replace=(?<replace>true|false)");

    /**
     * Repository type.
     */
    private static final String REPO_TYPE = "hexpm";

    /**
     * Repository storage.
     */
    private final Storage storage;

    /**
     * Artifact events queue.
     */
    private final Optional<Queue<ArtifactEvent>> events;

    /**
     * Repository name.
     */
    private final String repoName;

    /**
     * @param storage Repository storage.
     * @param events Artifact events
     * @param repoName Repository name
     */
    public UploadSlice(Storage storage,
                       Optional<Queue<ArtifactEvent>> events,
                       String repoName) {
        this.storage = storage;
        this.events = events;
        this.repoName = repoName;
    }

    @Override
    public Response response(
        final RequestLine line,
        final Headers headers,
        final Content body
    ) {
        final URI uri = line.uri();
        final String path = Objects.nonNull(uri.getPath()) ? uri.getPath() : "";
        final Matcher pathmatcher = UploadSlice.PUBLISH.matcher(path);
        final String query = Objects.nonNull(uri.getQuery()) ? uri.getQuery() : "";
        final Matcher querymatcher = UploadSlice.QUERY.matcher(query);
        final Response res;
        if (pathmatcher.matches() && querymatcher.matches()) {
            final boolean replace = Boolean.parseBoolean(querymatcher.group("replace"));
            final AtomicReference<String> name = new AtomicReference<>();
            final AtomicReference<String> version = new AtomicReference<>();
            final AtomicReference<String> innerchcksum = new AtomicReference<>();
            final AtomicReference<String> outerchcksum = new AtomicReference<>();
            final AtomicReference<byte[]> tarcontent = new AtomicReference<>();
            final AtomicReference<List<PackageOuterClass.Release>> releases =
                new AtomicReference<>();
            final AtomicReference<Key> packagekey = new AtomicReference<>();
            res = new AsyncResponse(UploadSlice.asBytes(body)
                .thenAccept(
                    bytes -> UploadSlice.readVarsFromTar(
                        bytes,
                        name,
                        version,
                        innerchcksum,
                        outerchcksum,
                        tarcontent,
                        packagekey
                    )
                ).thenCompose(
                    nothing -> this.storage.exists(packagekey.get())
                ).thenCompose(
                    packageExists -> this.readReleasesListFromStorage(
                        packageExists,
                        releases,
                        packagekey
                    ).thenAccept(
                        nothing -> UploadSlice.handleReleases(releases, replace, version)
                    ).thenApply(
                        nothing -> UploadSlice.constructSignedPackage(
                            name,
                            version,
                            innerchcksum,
                            outerchcksum,
                            releases
                        )
                    ).thenCompose(
                        signedPackage -> this.saveSignedPackageToStorage(
                            packagekey,
                            signedPackage
                        )
                    ).thenCompose(
                        nothing -> this.saveTarContentToStorage(
                            name,
                            version,
                            tarcontent
                        )
                    )
                ).handle(
                    (content, throwable) -> {
                        final Response result;
                        if (throwable == null) {
                            result = ResponseBuilder.created()
                                .headers(new HexContentType(headers).fill())
                                .build();
                            this.events.ifPresent(
                                queue -> queue.add(
                                    new ArtifactEvent(
                                        UploadSlice.REPO_TYPE, this.repoName,
                                        new Login(headers).getValue(),
                                        name.get(), version.get(), tarcontent.get().length
                                    )
                                )
                            );
                        } else {
                            result = ResponseBuilder.internalError()
                                .body(throwable.getMessage().getBytes())
                                .build();
                        }
                        return result;
                    }
                )
            );
        } else {
            res = ResponseBuilder.badRequest().build();
        }
        return res;
    }

    /**
     * Handle releases by finding version.
     *
     * @param releases List of releases from storage
     * @param replace Need replace for release
     * @param version Version for searching
     * @throws ArtipieException if realise exist in releases and don't need to replace.
     */
    private static void handleReleases(
        final AtomicReference<List<PackageOuterClass.Release>> releases,
        final boolean replace,
        final AtomicReference<String> version
    ) {
        final List<PackageOuterClass.Release> releaseslist = releases.get();
        if (releaseslist.isEmpty()) {
            return;
        }
        boolean versionexist = false;
        final List<PackageOuterClass.Release> filtered = new ArrayList<>(releaseslist.size());
        for (final PackageOuterClass.Release release : releaseslist) {
            if (release.getVersion().equals(version.get())) {
                versionexist = true;
            } else {
                filtered.add(release);
            }
        }
        if (versionexist && !replace) {
            throw new ArtipieException(String.format("Version %s already exists.", version.get()));
        }
        if (replace) {
            releases.set(filtered);
        }
    }

    /**
     * Reads variables from tar-content.
     * @param tar Tar archive with Elixir package in byte format
     * @param name Ref for package name
     * @param version Ref for package version
     * @param innerchecksum Ref for package innerChecksum
     * @param outerchecksum Ref for package outerChecksum
     * @param tarcontent Ref for tar archive in byte format
     * @param packagekey Ref for key for store
     */
    private static void readVarsFromTar(
        final byte[] tar,
        final AtomicReference<String> name,
        final AtomicReference<String> version,
        final AtomicReference<String> innerchecksum,
        final AtomicReference<String> outerchecksum,
        final AtomicReference<byte[]> tarcontent,
        final AtomicReference<Key> packagekey
    ) {
        tarcontent.set(tar);
        outerchecksum.set(DigestUtils.sha256Hex(tar));
        final TarReader reader = new TarReader(tar);
        reader
            .readEntryContent(TarReader.METADATA)
            .map(MetadataConfig::new)
            .map(
                metadataConfig -> {
                    final String app = metadataConfig.app();
                    name.set(app);
                    packagekey.set(new Key.From(DownloadSlice.PACKAGES, app));
                    version.set(metadataConfig.version());
                    return metadataConfig;
                }
            ).orElseThrow();
        reader.readEntryContent(TarReader.CHECKSUM)
            .map(
                checksumBytes -> {
                    innerchecksum.set(new String(checksumBytes));
                    return checksumBytes;
                }
            ).orElseThrow();
    }

    /**
     * Reads releasesList from storage.
     * @param packageexist Ref on package exist
     * @param releases Ref for list of releases
     * @param packagekey Ref on key for searching package
     * @return Empty CompletableFuture
     */
    private CompletableFuture<Void> readReleasesListFromStorage(
        final Boolean packageexist,
        final AtomicReference<List<PackageOuterClass.Release>> releases,
        final AtomicReference<Key> packagekey
    ) {
        final CompletableFuture<Void> future;
        if (packageexist) {
            future = this.storage.value(packagekey.get())
                .thenCompose(UploadSlice::asBytes)
                .thenAccept(
                    gzippedBytes -> {
                        final byte[] bytes = new Gzip(gzippedBytes).decompress();
                        try {
                            final SignedOuterClass.Signed signed =
                                SignedOuterClass.Signed.parseFrom(bytes);
                            final PackageOuterClass.Package pkg =
                                PackageOuterClass.Package.parseFrom(signed.getPayload());
                            releases.set(pkg.getReleasesList());
                        } catch (final InvalidProtocolBufferException ipbex) {
                            throw new ArtipieException("Cannot parse package", ipbex);
                        }
                    }
                );
        } else {
            releases.set(Collections.emptyList());
            future = CompletableFuture.completedFuture(null);
        }
        return future;
    }

    /**
     * Constructs new signed package.
     * @param name Ref on package name
     * @param version Ref on package version
     * @param innerchecksum Ref on package innerChecksum
     * @param outerchecksum Ref on package outerChecksum
     * @param releases Ref on list of releases
     * @return Package wrapped in Signed
     */
    private static SignedOuterClass.Signed constructSignedPackage(
        final AtomicReference<String> name,
        final AtomicReference<String> version,
        final AtomicReference<String> innerchecksum,
        final AtomicReference<String> outerchecksum,
        final AtomicReference<List<PackageOuterClass.Release>> releases
    ) {
        final PackageOuterClass.Release release;
        try {
            release = PackageOuterClass.Release.newBuilder()
                .setVersion(version.get())
                .setInnerChecksum(ByteString.copyFrom(Hex.decodeHex(innerchecksum.get())))
                .setOuterChecksum(ByteString.copyFrom(Hex.decodeHex(outerchecksum.get())))
                .build();
        } catch (final DecoderException dex) {
            throw new ArtipieException("Cannot decode hexed checksum", dex);
        }
        final PackageOuterClass.Package pckg = PackageOuterClass.Package.newBuilder()
            .setName(name.get())
            .setRepository("artipie")
            .addAllReleases(releases.get())
            .addReleases(release)
            .build();
        return SignedOuterClass.Signed.newBuilder()
            .setPayload(ByteString.copyFrom(pckg.toByteArray()))
            .setSignature(ByteString.EMPTY)
            .build();
    }

    /**
     * Save signed package to storage.
     * @param packagekey Ref on key for saving package
     * @param signed Package wrapped in Signed
     * @return Empty CompletableFuture
     */
    private CompletableFuture<Void> saveSignedPackageToStorage(
        final AtomicReference<Key> packagekey,
        final SignedOuterClass.Signed signed
    ) {
        return this.storage.save(
            packagekey.get(),
            new Content.From(new Gzip(signed.toByteArray()).compress())
        );
    }

    /**
     * Save tar-content to storage.
     * @param name Ref on package name
     * @param version Ref on package version
     * @param tarcontent Ref on tar archive in byte format
     * @return Empty CompletableFuture
     */
    private CompletableFuture<Void> saveTarContentToStorage(
        final AtomicReference<String> name,
        final AtomicReference<String> version,
        final AtomicReference<byte[]> tarcontent
    ) {
        return this.storage.save(
            new Key.From(DownloadSlice.TARBALLS, String.format("%s-%s.tar", name, version)),
            new Content.From(tarcontent.get())
        );
    }

    /**
     * Reads ByteBuffer-contents of Publisher into single byte array.
     * @param body Request body
     * @return CompletionStage with bytes from request body
     */
    private static CompletionStage<byte[]> asBytes(final Publisher<ByteBuffer> body) {
        return new Concatenation(new OneTimePublisher<>(body)).single()
            .to(SingleInterop.get())
            .thenApply(Remaining::new)
            .thenApply(Remaining::bytes);
    }
}