/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.proxy;

import com.artipie.asto.Content;
import com.artipie.npm.proxy.model.NpmAsset;
import com.artipie.npm.proxy.model.NpmPackage;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsSame;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

/**
 * Test NPM Proxy works.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class NpmProxyTest {

    /**
     * Last modified date for both package and asset.
     */
    private static final String LAST_MODIFIED = "Tue, 24 Mar 2020 12:15:16 GMT";

    /**
     * Asset Content-Type.
     */
    private static final String DEF_CONTENT_TYPE = "application/octet-stream";

    /**
     * Assert content.
     */
    private static final String DEF_CONTENT = "foobar";

    /**
     * NPM Proxy instance.
     */
    private NpmProxy npm;

    /**
     * Mocked NPM Proxy storage instance.
     */
    @Mock
    private NpmProxyStorage storage;

    /**
     * Mocked NPM Proxy remote client instance.
     */
    @Mock
    private NpmRemote remote;

    @Test
    public void getsPackage() throws IOException {
        final String name = "asdas";
        final NpmPackage expected = defaultPackage(OffsetDateTime.now());
        Mockito.when(this.storage.getPackage(name)).thenReturn(Maybe.empty());
        Mockito.doReturn(Maybe.just(expected)).when(this.remote).loadPackage(name);
        Mockito.when(this.storage.save(expected)).thenReturn(Completable.complete());
        MatcherAssert.assertThat(
            this.npm.getPackage(name).blockingGet(),
            new IsSame<>(expected)
        );
        Mockito.verify(this.storage).getPackage(name);
        Mockito.verify(this.remote).loadPackage(name);
        Mockito.verify(this.storage).save(expected);
    }

    @Test
    public void getsAsset() {
        final String path = "asdas/-/asdas-1.0.0.tgz";
        final NpmAsset loaded = defaultAsset();
        final NpmAsset expected = defaultAsset();
        Mockito.when(this.storage.getAsset(path)).thenAnswer(
            new Answer<Maybe<NpmAsset>>() {
                private boolean first = true;

                @Override
                public Maybe<NpmAsset> answer(final InvocationOnMock invocation) {
                    final Maybe<NpmAsset> result;
                    if (this.first) {
                        this.first = false;
                        result = Maybe.empty();
                    } else {
                        result = Maybe.just(expected);
                    }
                    return result;
                }
            }
        );
        Mockito.when(
            this.remote.loadAsset(Mockito.eq(path), Mockito.any())
        ).thenReturn(Maybe.just(loaded));
        Mockito.when(this.storage.save(loaded)).thenReturn(Completable.complete());
        MatcherAssert.assertThat(
            this.npm.getAsset(path).blockingGet(),
            new IsSame<>(expected)
        );
        Mockito.verify(this.storage, Mockito.times(2)).getAsset(path);
        Mockito.verify(this.remote).loadAsset(Mockito.eq(path), Mockito.any());
        Mockito.verify(this.storage).save(loaded);
    }

    @Test
    public void getsPackageFromCache() throws IOException {
        final String name = "asdas";
        final NpmPackage expected = defaultPackage(OffsetDateTime.now());
        Mockito.doReturn(Maybe.just(expected)).when(this.storage).getPackage(name);
        MatcherAssert.assertThat(
            this.npm.getPackage(name).blockingGet(),
            new IsSame<>(expected)
        );
        Mockito.verify(this.storage).getPackage(name);
    }

    @Test
    public void getsAssetFromCache() {
        final String path = "asdas/-/asdas-1.0.0.tgz";
        final NpmAsset expected = defaultAsset();
        Mockito.when(this.storage.getAsset(path)).thenReturn(Maybe.just(expected));
        MatcherAssert.assertThat(
            this.npm.getAsset(path).blockingGet(),
            new IsSame<>(expected)
        );
        Mockito.verify(this.storage).getAsset(path);
    }

    @Test
    public void doesNotFindPackage() {
        final String name = "asdas";
        Mockito.when(this.storage.getPackage(name)).thenReturn(Maybe.empty());
        Mockito.when(this.remote.loadPackage(name)).thenReturn(Maybe.empty());
        MatcherAssert.assertThat(
            "Unexpected package found",
            this.npm.getPackage(name).isEmpty().blockingGet()
        );
        Mockito.verify(this.storage).getPackage(name);
        Mockito.verify(this.remote).loadPackage(name);
    }

    @Test
    public void doesNotFindAsset() {
        final String path = "asdas/-/asdas-1.0.0.tgz";
        Mockito.when(this.storage.getAsset(path)).thenReturn(Maybe.empty());
        Mockito.when(
            this.remote.loadAsset(Mockito.eq(path), Mockito.any())
        ).thenReturn(Maybe.empty());
        MatcherAssert.assertThat(
            "Unexpected asset found",
            this.npm.getAsset(path).isEmpty().blockingGet()
        );
        Mockito.verify(this.storage).getAsset(path);
    }

    @BeforeEach
    void setUp() throws IOException {
        this.npm = new NpmProxy(this.storage, this.remote);
        Mockito.doNothing().when(this.remote).close();
    }

    @AfterEach
    void tearDown() throws IOException {
        this.npm.close();
        Mockito.verify(this.remote).close();
    }

    private static NpmPackage defaultPackage(final OffsetDateTime refreshed) throws IOException {
        return new NpmPackage(
            "asdas",
            IOUtils.resourceToString(
                "/json/cached.json",
                StandardCharsets.UTF_8
            ),
            NpmProxyTest.LAST_MODIFIED,
            refreshed
        );
    }

    private static NpmAsset defaultAsset() {
        return new NpmAsset(
            "asdas/-/asdas-1.0.0.tgz",
            new Content.From(NpmProxyTest.DEF_CONTENT.getBytes()),
            NpmProxyTest.LAST_MODIFIED,
            NpmProxyTest.DEF_CONTENT_TYPE
        );
    }

    /**
     * Tests with metadata TTL exceeded.
     * @since 0.2
     */
    @Nested
    class MetadataTtlExceeded {
        @Test
        public void getsPackage() throws IOException {
            final String name = "asdas";
            final NpmPackage original = NpmProxyTest.defaultPackage(
                OffsetDateTime.now().minus(2, ChronoUnit.HOURS)
            );
            final NpmPackage refreshed = defaultPackage(OffsetDateTime.now());
            Mockito.doReturn(Maybe.just(original))
                .when(NpmProxyTest.this.storage).getPackage(name);
            Mockito.doReturn(Maybe.just(refreshed))
                .when(NpmProxyTest.this.remote).loadPackage(name);
            Mockito.when(
                NpmProxyTest.this.storage.save(refreshed)
            ).thenReturn(Completable.complete());
            MatcherAssert.assertThat(
                NpmProxyTest.this.npm.getPackage(name).blockingGet(),
                new IsSame<>(refreshed)
            );
            Mockito.verify(NpmProxyTest.this.storage).getPackage(name);
            Mockito.verify(NpmProxyTest.this.remote).loadPackage(name);
            Mockito.verify(NpmProxyTest.this.storage).save(refreshed);
        }

        @Test
        public void getsPackageFromCache() throws IOException {
            final String name = "asdas";
            final NpmPackage original = NpmProxyTest.defaultPackage(
                OffsetDateTime.now().minus(2, ChronoUnit.HOURS)
            );
            Mockito.doReturn(Maybe.just(original))
                .when(NpmProxyTest.this.storage).getPackage(name);
            Mockito.when(
                NpmProxyTest.this.remote.loadPackage(name)
            ).thenReturn(Maybe.empty());
            MatcherAssert.assertThat(
                NpmProxyTest.this.npm.getPackage(name).blockingGet(),
                new IsSame<>(original)
            );
            Mockito.verify(NpmProxyTest.this.storage).getPackage(name);
            Mockito.verify(NpmProxyTest.this.remote).loadPackage(name);
        }
    }
}
