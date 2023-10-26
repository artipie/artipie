/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021-2023 Artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package  com.artipie.conan;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.conan.http.ConanSlice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.security.policy.PolicyByUsername;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main class.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (200 lines)
 */
public final class Cli {

    /**
     * Artipie conan username for basic auth.
     */
    public static final String USERNAME = "demo_login";

    /**
     * Artipie conan password for basic auth.
     */
    public static final String PASSWORD = "demo_password";

    /**
     * Fake demo auth token.
     */
    public static final String DEMO_TOKEN = "fake_demo_token";

    /**
     * TCP Port for Conan server. Default is 9300.
     */
    private static final int CONAN_PORT = 9300;

    /**
     * Private constructor for main class.
     */
    private Cli() {
    }

    /**
     * Entry point.
     * @param args Command line arguments.
     */
    public static void main(final String... args) {
        final Path path = Paths.get("/home/user/.conan_server/data");
        final Storage storage = new FileStorage(path);
        final ConanRepo repo = new ConanRepo(storage);
        repo.batchUpdateIncrementally(Key.ROOT);
        final Vertx vertx = Vertx.vertx();
        final ItemTokenizer tokenizer = new ItemTokenizer(vertx.getDelegate());
        final VertxSliceServer server = new VertxSliceServer(
            vertx,
            new LoggingSlice(
                new ConanSlice(
                    storage,
                    new PolicyByUsername(Cli.USERNAME),
                    new Authentication.Single(
                        Cli.USERNAME, Cli.PASSWORD
                    ),
                    new ConanSlice.FakeAuthTokens(Cli.DEMO_TOKEN, Cli.USERNAME),
                    tokenizer,
                    "*"
            )),
            Cli.CONAN_PORT
        );
        server.start();
    }
}
