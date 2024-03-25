/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http;

import com.artipie.asto.Content;
import com.artipie.asto.Meta;
import com.artipie.composer.Repository;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Login;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.ResponseBuilder;
import com.artipie.scheduling.ArtifactEvent;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Slice for adding a package to the repository in ZIP format.
 * See <a href="https://getcomposer.org/doc/05-repositories.md#artifact">Artifact repository</a>.
 */
@SuppressWarnings({"PMD.SingularField", "PMD.UnusedPrivateField"})
final class AddArchiveSlice implements Slice {
    /**
     * Composer HTTP for entry point.
     * See <a href="https://getcomposer.org/doc/04-schema.md#version">docs</a>.
     */
    public static final Pattern PATH = Pattern.compile(
        "^/(?<full>(?<name>[a-z0-9_.\\-]*)-(?<version>v?\\d+.\\d+.\\d+[-\\w]*).zip)$"
    );

    /**
     * Repository type.
     */
    public static final String REPO_TYPE = "php";

    /**
     * Repository.
     */
    private final Repository repository;

    /**
     * Artifact events.
     */
    private final Optional<Queue<ArtifactEvent>> events;

    /**
     * Repository name.
     */
    private final String rname;

    /**
     * Ctor.
     * @param repository Repository.
     * @param rname Repository name
     */
    AddArchiveSlice(final Repository repository, final String rname) {
        this(repository, Optional.empty(), rname);
    }

    /**
     * Ctor.
     * @param repository Repository
     * @param events Artifact events
     * @param rname Repository name
     */
    AddArchiveSlice(
        final Repository repository, final Optional<Queue<ArtifactEvent>> events,
        final String rname
    ) {
        this.repository = repository;
        this.events = events;
        this.rname = rname;
    }

    @Override
    public Response response(RequestLine line, Headers headers, Content body) {
        final String uri = line.uri().getPath();
        final Matcher matcher = AddArchiveSlice.PATH.matcher(uri);
        if (matcher.matches()) {
            final Archive.Zip archive =
                new Archive.Zip(new Archive.Name(matcher.group("full"), matcher.group("version")));
            CompletableFuture<Void> res =
                this.repository.addArchive(archive, new Content.From(body));
            if (this.events.isPresent()) {
                res = res.thenCompose(
                    nothing -> this.repository.storage().metadata(archive.name().artifact())
                        .thenApply(meta -> meta.read(Meta.OP_SIZE).orElseThrow())
                ).thenAccept(
                    size -> this.events.get().add(
                        new ArtifactEvent(
                            AddArchiveSlice.REPO_TYPE, this.rname,
                            new Login(headers).getValue(), archive.name().full(),
                            archive.name().version(), size
                        )
                    )
                );
            }
            return new AsyncResponse(res.thenApply(nothing -> ResponseBuilder.created().build()));
        }
        return ResponseBuilder.badRequest().build();
    }
}
