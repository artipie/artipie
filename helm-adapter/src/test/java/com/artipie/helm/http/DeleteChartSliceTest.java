/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.helm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.helm.metadata.IndexYamlMapping;
import com.artipie.helm.test.ContentOfIndex;
import com.artipie.http.Headers;
import com.artipie.http.hm.ResponseAssert;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.scheduling.ArtifactEvent;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

/**
 * Test for {@link DeleteChartSlice}.
 */
final class DeleteChartSliceTest {

    /**
     * Test repo name.
     */
    private static final String RNAME = "test-helm-repo";

    /**
     * Storage.
     */
    private Storage storage;

    /**
     * Artifact events.
     */
    private Queue<ArtifactEvent> events;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
        this.events = new ConcurrentLinkedQueue<>();
    }

    @ParameterizedTest
    @ValueSource(
        strings = {"", "/charts", "/charts/", "/charts/name/1.3.2/extra", "/wrong/name/0.1.1"}
        )
    void returnBadRequest(final String rqline) {
        ResponseAssert.check(
            new DeleteChartSlice(this.storage, Optional.of(this.events), DeleteChartSliceTest.RNAME)
                .response(new RequestLine(RqMethod.DELETE, rqline), Headers.EMPTY, Content.EMPTY)
                .join(),
            RsStatus.BAD_REQUEST
        );
        MatcherAssert.assertThat(
            "None items were added into events queue", this.events.isEmpty()
        );
    }

    @Test
    void deleteAllVersionsByName() {
        final String arkone = "ark-1.0.1.tgz";
        final String arktwo = "ark-1.2.0.tgz";
        Stream.of("index.yaml", "ark-1.0.1.tgz", "ark-1.2.0.tgz", "tomcat-0.4.1.tgz")
            .forEach(source -> new TestResource(source).saveTo(this.storage));
        ResponseAssert.check(
            new DeleteChartSlice(this.storage, Optional.of(this.events), DeleteChartSliceTest.RNAME)
                .response(new RequestLine(RqMethod.DELETE, "/charts/ark"), Headers.EMPTY, Content.EMPTY)
                .join(),
            RsStatus.OK
        );
        Assertions.assertTrue(
            new ContentOfIndex(this.storage).index()
                .byChart("ark").isEmpty(),
            "Deleted chart is present in index"
        );
        Assertions.assertFalse(
            this.storage.exists(new Key.From(arkone)).join(),
            "Archive of deleted chart remains"
        );
        Assertions.assertFalse(
            this.storage.exists(new Key.From(arktwo)).join(),
            "Archive of deleted chart remains"
        );
        MatcherAssert.assertThat(
            "One item was added into events queue", this.events.size() == 1
        );
    }

    @Test
    void deleteByNameAndVersion() {
        Stream.of("index.yaml", "ark-1.0.1.tgz", "ark-1.2.0.tgz", "tomcat-0.4.1.tgz")
            .forEach(source -> new TestResource(source).saveTo(this.storage));
        ResponseAssert.check(
            new DeleteChartSlice(this.storage, Optional.of(this.events), DeleteChartSliceTest.RNAME)
                .response(new RequestLine(RqMethod.DELETE, "/charts/ark/1.0.1"), Headers.EMPTY, Content.EMPTY)
                .join(),
            RsStatus.OK
        );
        final IndexYamlMapping index = new ContentOfIndex(this.storage).index();
        MatcherAssert.assertThat(
            "Deleted chart is present in index",
            index.byChartAndVersion("ark", "1.0.1").isPresent(),
            new IsEqual<>(false)
        );
        Assertions.assertTrue(
            index.byChartAndVersion("ark", "1.2.0").isPresent(),
            "Second chart was also deleted"
        );
        Assertions.assertFalse(
            this.storage.exists(new Key.From("ark-1.0.1.tgz")).join(),
            "Archive of deleted chart remains"
        );
        MatcherAssert.assertThat(
            "One item was added into events queue", this.events.size() == 1
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"/charts/not-exist", "/charts/ark/0.0.0"})
    void failsToDeleteByNotExisted(final String rqline) {
        Stream.of("index.yaml", "ark-1.0.1.tgz", "ark-1.2.0.tgz", "tomcat-0.4.1.tgz")
            .forEach(source -> new TestResource(source).saveTo(this.storage));
        ResponseAssert.check(
            new DeleteChartSlice(this.storage, Optional.ofNullable(this.events), DeleteChartSliceTest.RNAME)
                .response(new RequestLine(RqMethod.DELETE, rqline), Headers.EMPTY, Content.EMPTY)
                .join(),
            RsStatus.NOT_FOUND
        );
        MatcherAssert.assertThat(
            "None items were added into events queue", this.events.isEmpty()
        );
    }
}
