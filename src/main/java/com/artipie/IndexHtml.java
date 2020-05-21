package com.artipie;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rs.RsStatus;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Index HTML page.
 * @since 0.4
 */
public final class IndexHtml implements Slice {

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return connection -> connection.accept(
            RsStatus.OK,
            new Headers.From(
                new Headers.From("content-type", "text/html")
            ),
            new Content.From(
                String.join(
                    "\n",
                    "<html>",
                    "<head>",
                    "<title>Artipie central</title>",
                    "</head>",
                    "<body>",
                    "To setup Artipie central repository follow",
                    // @checkstyle LineLengthCheck (1 line)
                    "<a href=\"https://github.com/artipie/artipie/blob/master/README.md#artipie-central\">this instructions</a>",
                    "</body>",
                    "</html>"
                ).getBytes(StandardCharsets.UTF_8)
            )
        );
    }
}
