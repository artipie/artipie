/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian.http;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.debian.AstoGzArchive;
import com.artipie.debian.Config;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.RsStatus;
import com.artipie.scheduling.ArtifactEvent;
import java.io.IOException;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link UpdateSlice}.
 * @since 0.1
 */
@SuppressWarnings({"PMD.AssignmentInOperand", "PMD.AvoidDuplicateLiterals"})
class UpdateSliceTest {

    /**
     * Repository settings.
     */
    private static final YamlMapping SETTINGS = Yaml.createYamlMappingBuilder()
        .add("Architectures", "amd64")
        .add("Components", "main").build();

    /**
     * Test storage.
     */
    private Storage asto;

    /**
     * Artifact events queue.
     */
    private Queue<ArtifactEvent> events;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
        this.events = new ConcurrentLinkedQueue<>();
    }

    @Test
    void uploadsAndCreatesIndex() {
        final Key release = new Key.From("dists/my_repo/Release");
        final Key inrelease = new Key.From("dists/my_repo/InRelease");
        this.asto.save(release, Content.EMPTY).join();
        this.asto.save(inrelease, Content.EMPTY).join();
        MatcherAssert.assertThat(
            "Response is OK",
            new UpdateSlice(
                this.asto,
                new Config.FromYaml("my_repo", UpdateSliceTest.SETTINGS, new InMemoryStorage()),
                Optional.of(this.events)
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.PUT, "/main/aglfn_1.7-3_amd64.deb"),
                Headers.EMPTY,
                new Content.From(new TestResource("aglfn_1.7-3_amd64.deb").asBytes())
            )
        );
        MatcherAssert.assertThat(
            "Packages index added",
            this.asto.exists(new Key.From("dists/my_repo/main/binary-amd64/Packages.gz")).join(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Debian package added",
            this.asto.exists(new Key.From("main/aglfn_1.7-3_amd64.deb")).join(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Release index updated",
            this.asto.value(release).join().size().get(),
            new IsNot<>(new IsEqual<>(0L))
        );
        MatcherAssert.assertThat(
            "InRelease index updated",
            this.asto.value(inrelease).join().size().get(),
            new IsNot<>(new IsEqual<>(0L))
        );
        MatcherAssert.assertThat("Artifact event added to queue", this.events.size() == 1);
    }

    @Test
    void uploadsAndUpdatesIndex() throws IOException {
        final Key release = new Key.From("dists/deb_repo/Release");
        final Key inrelease = new Key.From("dists/deb_repo/InRelease");
        this.asto.save(release, Content.EMPTY).join();
        this.asto.save(inrelease, Content.EMPTY).join();
        final Key key = new Key.From("dists/deb_repo/main/binary-amd64/Packages.gz");
        new TestResource("Packages.gz").saveTo(this.asto, key);
        MatcherAssert.assertThat(
            "Response is OK",
            new UpdateSlice(
                this.asto,
                new Config.FromYaml(
                    "deb_repo",
                    UpdateSliceTest.SETTINGS,
                    new InMemoryStorage()
                ),
                Optional.of(this.events)
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.PUT, "/main/libobus-ocaml_1.2.3-1+b3_amd64.deb"),
                Headers.EMPTY,
                new Content.From(new TestResource("libobus-ocaml_1.2.3-1+b3_amd64.deb").asBytes())
            )
        );
        MatcherAssert.assertThat(
            "Debian package added",
            this.asto.exists(new Key.From("main/libobus-ocaml_1.2.3-1+b3_amd64.deb")).join(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            new AstoGzArchive(this.asto).unpack(key),
            new StringContainsInOrder(
                new ListOf<String>(
                    "Package: aglfn",
                    "Package: pspp",
                    "Package: libobus-ocaml"
                )
            )
        );
        MatcherAssert.assertThat(
            "Release index updated",
            this.asto.value(release).join().size().get(),
            new IsNot<>(new IsEqual<>(0L))
        );
        MatcherAssert.assertThat(
            "InRelease index updated",
            this.asto.value(inrelease).join().size().get(),
            new IsNot<>(new IsEqual<>(0L))
        );
        MatcherAssert.assertThat("Artifact event added to queue", this.events.size() == 1);
    }

    @Test
    void returnsBadRequestAndRemovesItem() {
        MatcherAssert.assertThat(
            "Response is bad request",
            new UpdateSlice(
                this.asto,
                new Config.FromYaml(
                    "my_repo",
                    UpdateSliceTest.SETTINGS,
                    new InMemoryStorage()
                ),
                Optional.of(this.events)
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.PUT, "/main/aglfn_1.7-3_all.deb"),
                Headers.EMPTY,
                new Content.From(new TestResource("aglfn_1.7-3_all.deb").asBytes())
            )
        );
        MatcherAssert.assertThat(
            "Debian package was not added",
            this.asto.exists(new Key.From("main/aglfn_1.7-3_all.deb")).join(),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat("Artifact event was not added to queue", this.events.isEmpty());
    }

    @Test
    void returnsErrorAndRemovesItem() {
        MatcherAssert.assertThat(
            "Response is internal error",
            new UpdateSlice(
                this.asto,
                new Config.FromYaml(
                    "my_repo",
                    UpdateSliceTest.SETTINGS,
                    new InMemoryStorage()
                ),
                Optional.of(this.events)
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.INTERNAL_ERROR),
                new RequestLine(RqMethod.PUT, "/main/corrupted.deb"),
                Headers.EMPTY,
                new Content.From("abc123".getBytes())
            )
        );
        MatcherAssert.assertThat(
            "Debian package was not added",
            this.asto.exists(new Key.From("main/corrupted.deb")).join(),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat("Artifact event was not added to queue", this.events.isEmpty());
    }

}
