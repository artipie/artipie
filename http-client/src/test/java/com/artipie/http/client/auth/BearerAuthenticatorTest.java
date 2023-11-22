/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client.auth;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.client.FakeClientSlices;
import com.artipie.http.headers.Header;
import com.artipie.http.headers.WwwAuthenticate;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.StandardRs;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BearerAuthenticator}.
 *
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
class BearerAuthenticatorTest {

    @Test
    void shouldRequestTokenFromRealm() {
        final AtomicReference<String> pathcapture = new AtomicReference<>();
        final AtomicReference<String> querycapture = new AtomicReference<>();
        final FakeClientSlices fake = new FakeClientSlices(
            (rsline, rqheaders, rqbody) -> {
                final URI uri = new RequestLineFrom(rsline).uri();
                pathcapture.set(uri.getRawPath());
                querycapture.set(uri.getRawQuery());
                return StandardRs.OK;
            }
        );
        final String host = "artipie.com";
        final int port = 321;
        final String path = "/get_token";
        new BearerAuthenticator(
            fake,
            bytes -> "token",
            Authenticator.ANONYMOUS
        ).authenticate(
            new Headers.From(
                new WwwAuthenticate(
                    String.format(
                        "Bearer realm=\"https://%s:%d%s\",param1=\"1\",param2=\"abc\"",
                        host, port, path
                    )
                )
            )
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Scheme is correct",
            fake.capturedSecure(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Host is correct",
            fake.capturedHost(),
            new IsEqual<>(host)
        );
        MatcherAssert.assertThat(
            "Port is correct",
            fake.capturedPort(),
            new IsEqual<>(port)
        );
        MatcherAssert.assertThat(
            "Path is correct",
            pathcapture.get(),
            new IsEqual<>(path)
        );
        MatcherAssert.assertThat(
            "Query is correct",
            querycapture.get(),
            new IsEqual<>("param1=1&param2=abc")
        );
    }

    @Test
    void shouldRequestTokenUsingAuthenticator() {
        final AtomicReference<Iterable<java.util.Map.Entry<String, String>>> capture;
        capture = new AtomicReference<>();
        final Header auth = new Header("X-Header", "Value");
        final FakeClientSlices fake = new FakeClientSlices(
            (rsline, rqheaders, rqbody) -> {
                capture.set(rqheaders);
                return StandardRs.OK;
            }
        );
        new BearerAuthenticator(
            fake,
            bytes -> "something",
            ignored -> CompletableFuture.completedFuture(new Headers.From(auth))
        ).authenticate(
            new Headers.From(
                new WwwAuthenticate("Bearer realm=\"https://whatever\"")
            )
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            capture.get(),
            Matchers.containsInAnyOrder(auth)
        );
    }

    @Test
    void shouldProduceBearerHeaderUsingTokenFormat() {
        final String token = "mF_9.B5f-4.1JqM";
        final byte[] response = String.format("{\"access_token\":\"%s\"}", token).getBytes();
        final AtomicReference<byte[]> captured = new AtomicReference<>();
        final Headers headers = new BearerAuthenticator(
            new FakeClientSlices(
                (rqline, rqheaders, rqbody) -> new RsWithBody(new Content.From(response))
            ),
            bytes -> {
                captured.set(bytes);
                return token;
            },
            Authenticator.ANONYMOUS
        ).authenticate(
            new Headers.From(new WwwAuthenticate("Bearer realm=\"http://localhost\""))
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Token response sent to token format",
            captured.get(),
            new IsEqual<>(response)
        );
        MatcherAssert.assertThat(
            "Result headers contains authorization",
            StreamSupport.stream(
                headers.spliterator(),
                false
            ).map(Header::new).collect(Collectors.toList()),
            Matchers.contains(new Header("Authorization", String.format("Bearer %s", token)))
        );
    }
}
