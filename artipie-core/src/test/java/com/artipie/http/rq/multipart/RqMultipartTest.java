/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rq.multipart;

import com.artipie.asto.Content;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.test.TestResource;
import com.artipie.http.headers.ContentDisposition;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rq.RqHeaders;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.reactivestreams.Publisher;

/**
 * Test case for multipart request parser.
 * @since 1.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class RqMultipartTest {

    @Test
    @Timeout(1)
    void processesFullMultipartRequest() throws Exception {
        final String first = String.join(
            "\n",
            "1) This is implicitly typed plain ASCII text.",
            "It does NOT end with a linebreak."
        );
        final String second = String.join(
            "\n",
            "2) This is explicitly typed plain ASCII text.",
            "It DOES end with a linebreak.", ""
        );
        final String simple = String.join(
            "\r\n",
            String.join(
                "\n",
                "This is the preamble.  It is to be ignored, though it",
                "is a handy place for mail composers to include an",
                "explanatory note to non-MIME compliant readers."
            ),
            "--simple boundary",
            "",
            first,
            "--simple boundary",
            "Content-type: text/plain; charset=us-ascii",
            "",
            second,
            "--simple boundary--",
            "This is the epilogue.  It is also to be ignored."
        );
        final List<String> parsed = Flowable.fromPublisher(
            new RqMultipart(
                new ContentType("multipart/mixed; boundary=\"simple boundary\""),
                new Content.From(simple.getBytes(StandardCharsets.US_ASCII))
            ).parts()
        ).<String>flatMapSingle(
            part -> Single.fromFuture(
                new PublisherAs(part).string(StandardCharsets.US_ASCII).toCompletableFuture()
            )
        ).toList().blockingGet();
        MatcherAssert.assertThat(
            parsed,
            Matchers.contains(first, second)
        );
    }

    @Test
    // @Timeout(1)
    void multipartWithEmptyBodies() throws Exception {
        // @checkstyle LineLengthCheck (100 lines)
        final String payload = String.join(
            "\r\n",
            "--92fd51d48f874720a066238b824c0146",
            "Content-Disposition: form-data; name=\"maintainer\"",
            "",
            "",
            "--92fd51d48f874720a066238b824c0146",
            "Content-Disposition: form-data; name=\"maintainer_email\"",
            "",
            "",
            "--92fd51d48f874720a066238b824c0146",
            "Content-Disposition: form-data; name=\"license\"",
            "",
            "MIT",
            "--92fd51d48f874720a066238b824c0146--"
        );
        final List<String> parsed = Flowable.fromPublisher(
            new RqMultipart(
                new ContentType("multipart/mixed; boundary=\"92fd51d48f874720a066238b824c0146\""),
                new Content.From(payload.getBytes(StandardCharsets.US_ASCII))
            ).parts()
        ).<String>flatMapSingle(
            part -> Single.fromFuture(
                new PublisherAs(part).string(StandardCharsets.US_ASCII).toCompletableFuture()
            ).map(body -> String.format("%s: %s", new ContentDisposition(part.headers()).fieldName(), body))
        ).toList().blockingGet();
        MatcherAssert.assertThat(
            parsed,
            Matchers.containsInAnyOrder("maintainer: ", "maintainer_email: ", "license: MIT")
        );
    }

    @Test
    void dontSkipNonPreambleFirstEmptyPart() {
        final String payload = String.join(
            "\r\n",
            "--123",
            "Foo: bar",
            "",
            "",
            "--123--"
        );
        MatcherAssert.assertThat(
            Flowable.fromPublisher(
                new RqMultipart(
                    new ContentType("multipart/mixed; boundary=\"123\""),
                    new Content.From(payload.getBytes(StandardCharsets.US_ASCII))
                ).parts()
            ).flatMap(Flowable::fromPublisher).toList().blockingGet(),
            Matchers.not(Matchers.empty())
        );
    }

    @Test
    @SuppressWarnings("deprecation")
    void readOnePartOfRequest() {
        // @checkstyle LineLengthCheck (100 lines)
        final String payload = String.join(
            "\r\n",
            "--4f0974f4a401fd757d35fe31a4737ac2",
            "Content-Disposition: form-data; name=\"Content-Type\"",
            "",
            "application/octet-stream",
            "--4f0974f4a401fd757d35fe31a4737ac2",
            "Content-Disposition: form-data; name=\"acl\"",
            "",
            "private",
            "--4f0974f4a401fd757d35fe31a4737ac2",
            "Content-Disposition: form-data; name=\"key\"",
            "",
            "000000000000000000000000000000000000000000000000000000",
            "--4f0974f4a401fd757d35fe31a4737ac2",
            "Content-Disposition: form-data; name=\"policy\"",
            "",
            "00000000000000000000000000000000000000000000000000",
            "--4f0974f4a401fd757d35fe31a4737ac2",
            "Content-Disposition: form-data; name=\"success_action_status\"",
            "",
            "201",
            "--4f0974f4a401fd757d35fe31a4737ac2",
            "Content-Disposition: form-data; name=\":action\"",
            "",
            "file_upload",
            "--4f0974f4a401fd757d35fe31a4737ac2",
            "Content-Disposition: form-data; name=\"x-amz-algorithm\"",
            "",
            "AWS4-HMAC-SHA256",
            "--4f0974f4a401fd757d35fe31a4737ac2",
            "Content-Disposition: form-data; name=\"x-amz-credential\"",
            "",
            "0000000000000000000000000000000000/0000000000000/us-east-1/s3/aws4_request",
            "--4f0974f4a401fd757d35fe31a4737ac2",
            "Content-Disposition: form-data; name=\"x-amz-date\"",
            "",
            "0000000000000000000",
            "--4f0974f4a401fd757d35fe31a4737ac2",
            "Content-Disposition: form-data; name=\"x-amz-security-token\"",
            "",
            "000000000000000000000000000000000000000000000000000000000000000000000",
            "--4f0974f4a401fd757d35fe31a4737ac2",
            "Content-Disposition: form-data; name=\"x-amz-signature\"",
            "",
            "0000000000000000000000000000000000000000",
            "--4f0974f4a401fd757d35fe31a4737ac2",
            "Content-Disposition: form-data; name=\"x-amz-storage-class\"",
            "",
            "STANDARD",
            "--4f0974f4a401fd757d35fe31a4737ac2",
            "Content-Disposition: form-data; name=\"Content-Length\"",
            "",
            "2123",
            "--4f0974f4a401fd757d35fe31a4737ac2",
            "Content-Disposition: form-data; name=\"Content-MD5\"",
            "",
            "hynZmK8eQ13TplxL8eLNng==",
            "--4f0974f4a401fd757d35fe31a4737ac2",
            "Content-Disposition: form-data; name=\"file\"; filename=\"linux-64/example-package-0.0.1-0.tar.bz2\"",
            "Content-Type: application/x-tar",
            "",
            "AAAAAAAAAAAAAAAAAA",
            "",
            "--4f0974f4a401fd757d35fe31a4737ac2--"
        );
        final Publisher<ByteBuffer> body = Flowable.fromPublisher(
            new RqMultipart(
                new ContentType("multipart/mixed; boundary=\"4f0974f4a401fd757d35fe31a4737ac2\""),
                new Content.From(payload.getBytes(StandardCharsets.US_ASCII))
            ).filter(headers -> new ContentDisposition(headers).fieldName().equals("x-amz-signature"))
        ).flatMap(part -> part);
        final byte[] target = new PublisherAs(body).bytes().toCompletableFuture().join();
        MatcherAssert.assertThat(target, Matchers.equalTo("0000000000000000000000000000000000000000".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void inspectParts() {
        final String payload = String.join(
            "\r\n",
            "--bnd123",
            "Test: 1",
            "",
            "data-1",
            "--bnd123",
            "Test: 2",
            "",
            "data-2",
            "--bnd123",
            "Test: 3",
            "",
            "data-3",
            "",
            "--bnd123--"
        );
        final List<String> parts = Flowable.fromPublisher(
            new RqMultipart(
                new ContentType("multipart/mixed; boundary=\"bnd123\""),
                new Content.From(payload.getBytes(StandardCharsets.US_ASCII))
            ).inspect(
                (part, inspector) -> {
                    if (new RqHeaders(part.headers(), "Test").get(0).equals("2")) {
                        inspector.accept(part);
                    } else {
                        inspector.ignore(part);
                    }
                    final CompletableFuture<Void> res = new CompletableFuture<>();
                    res.complete(null);
                    return res;
                }
            )
        ).flatMapSingle(
            part -> Single.fromFuture(new PublisherAs(part).asciiString().toCompletableFuture())
        ).toList().blockingGet();
        MatcherAssert.assertThat("parts must have one element", parts, Matchers.hasSize(1));
        MatcherAssert.assertThat(
            "part element must have correct body",
            parts.get(0),
            Matchers.equalTo("data-2")
        );
    }

    @Test
    void parseCondaPayload() throws Exception {
        final byte[] payload = new TestResource("multipart").asBytes();
        final int size = Flowable.fromPublisher(
            new RqMultipart(
                new ContentType("multipart/mixed; boundary=\"92fd51d48f874720a066238b824c0146\""),
                new Content.From(payload)
            ).parts()
        ).flatMap(Flowable::fromPublisher)
            .reduce(0, (acc, chunk) -> acc + chunk.remaining()).blockingGet();
        // @checkstyle MagicNumberCheck (1 line)
        MatcherAssert.assertThat(size, Matchers.equalTo(4163));
    }
}
