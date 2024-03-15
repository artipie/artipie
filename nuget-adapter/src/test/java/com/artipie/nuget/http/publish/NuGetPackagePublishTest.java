/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http.publish;

import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.nuget.AstoRepository;
import com.artipie.nuget.http.NuGet;
import com.artipie.nuget.http.TestAuthentication;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.policy.PolicyByUsername;
import com.google.common.io.Resources;
import io.reactivex.Flowable;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NuGet}.
 * Package publish resource.
 *
 * @since 0.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class NuGetPackagePublishTest {

    /**
     * Tested NuGet slice.
     */
    private NuGet nuget;

    /**
     * Events queue.
     */
    private Queue<ArtifactEvent> events;

    @BeforeEach
    void init() throws Exception {
        this.events = new ConcurrentLinkedQueue<>();
        this.nuget = new NuGet(
            URI.create("http://localhost").toURL(),
            new AstoRepository(new InMemoryStorage()),
            new PolicyByUsername(TestAuthentication.USERNAME),
            new TestAuthentication(),
            "test",
            Optional.of(this.events)
        );
    }

    @Test
    void shouldPutPackagePublish() throws Exception {
        final Response response = this.putPackage(nupkg());
        MatcherAssert.assertThat(
            response,
            new RsHasStatus(RsStatus.CREATED)
        );
        MatcherAssert.assertThat("Events queue has one event", this.events.size() == 1);
    }

    @Test
    void shouldFailPutPackage() throws Exception {
        MatcherAssert.assertThat(
            "Should fail to add package which is not a ZIP archive",
            this.putPackage("not a zip".getBytes()),
            new RsHasStatus(RsStatus.BAD_REQUEST)
        );
        MatcherAssert.assertThat("Events queue is empty", this.events.isEmpty());
    }

    @Test
    void shouldFailPutSamePackage() throws Exception {
        this.putPackage(nupkg()).send(
            (status, headers, body) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Should fail to add same package when it is already present in the repository",
            this.putPackage(nupkg()),
            new RsHasStatus(RsStatus.CONFLICT)
        );
        MatcherAssert.assertThat("Events queue is contains one item", this.events.size() == 1);
    }

    @Test
    void shouldFailGetPackagePublish() {
        final Response response = this.nuget.response(
            new RequestLine(RqMethod.GET, "/package"),
            new TestAuthentication.Headers(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(response, new RsHasStatus(RsStatus.METHOD_NOT_ALLOWED));
        MatcherAssert.assertThat("Events queue is empty", this.events.isEmpty());
    }

    @Test
    void shouldUnauthorizedPutPackageForAnonymousUser() {
        MatcherAssert.assertThat(
            this.nuget.response(
                new RequestLine(RqMethod.PUT, "/package"),
                Headers.EMPTY,
                Flowable.fromArray(ByteBuffer.wrap("data".getBytes()))
            ),
            new ResponseMatcher(
                RsStatus.UNAUTHORIZED, Headers.EMPTY
            )
        );
        MatcherAssert.assertThat("Events queue is empty", this.events.isEmpty());
    }

    private Response putPackage(final byte[] pack) throws Exception {
        final HttpEntity entity = MultipartEntityBuilder.create()
            .addBinaryBody("package.nupkg", pack)
            .build();
        final ByteArrayOutputStream sink = new ByteArrayOutputStream();
        entity.writeTo(sink);
        return this.nuget.response(
            new RequestLine(RqMethod.PUT, "/package"),
            new Headers.From(
                new TestAuthentication.Header(),
                new Header("Content-Type", entity.getContentType().getValue())
            ),
            Flowable.fromArray(ByteBuffer.wrap(sink.toByteArray()))
        );
    }

    private static byte[] nupkg() throws Exception {
        final URL resource = Thread.currentThread().getContextClassLoader()
            .getResource("newtonsoft.json/12.0.3/newtonsoft.json.12.0.3.nupkg");
        return Resources.toByteArray(resource);
    }

}
