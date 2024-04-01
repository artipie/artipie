/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http;

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.composer.AstoRepository;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.RsStatus;
import com.artipie.scheduling.ArtifactEvent;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.regex.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link AddArchiveSlice}.
 *
 * @since 0.4
 */
final class AddArchiveSliceTest {
    /**
     * Test storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
    }

    @ParameterizedTest
    @CsvSource({
        "/log-1.1.3.zip,log,1.1.3",
        "/log-bad.1.3.zip,,",
        "/path/name-2.1.3.zip,,",
        "/name-prefix-0.10.321.zip,name-prefix,0.10.321",
        "/name.suffix-1.2.2-patch.zip,name.suffix,1.2.2-patch",
        "/name-2.3.1-beta1.zip,name,2.3.1-beta1"
    })
    void patternExtractsNameAndVersionCorrectly(
        final String url, final String name, final String vers
    ) {
        final Matcher matcher = AddArchiveSlice.PATH.matcher(url);
        final String cname;
        final String cvers;
        if (matcher.matches()) {
            cname = matcher.group("name");
            cvers = matcher.group("version");
        } else {
            cname = null;
            cvers = null;
        }
        MatcherAssert.assertThat(
            "Name is correct",
            cname,
            new IsEqual<>(name)
        );
        MatcherAssert.assertThat(
            "Version is correct",
            cvers,
            new IsEqual<>(vers)
        );
    }

    @Test
    void returnsBadRequest() {
        MatcherAssert.assertThat(
            new AddArchiveSlice(new AstoRepository(this.storage), "my-php"),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.PUT, "/bad/request")
            )
        );
    }

    @Test
    void returnsCreateStatus() {
        final String archive = "log-1.1.3.zip";
        final AstoRepository asto = new AstoRepository(
            this.storage, Optional.of("http://artipie:8080/")
        );
        final Queue<ArtifactEvent> queue = new LinkedList<>();
        MatcherAssert.assertThat(
            new AddArchiveSlice(asto, Optional.of(queue), "my-test-php"),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine(RqMethod.PUT, String.format("/%s", archive)),
                Headers.EMPTY,
                new Content.From(new TestResource(archive).asBytes())
            )
        );
        MatcherAssert.assertThat("Queue has one item", queue.size() == 1);
    }
}
