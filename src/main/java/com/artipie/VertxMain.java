/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
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

package com.artipie;

import com.artipie.asto.fs.FileStorage;
import com.artipie.http.Slice;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import java.nio.file.Path;

/**
 * Vertx server entry point.
 * @since 1.0
 */
public final class VertxMain implements Runnable {

    /**
     * Slice to serve.
     */
    private final Slice slice;

    /**
     * Server port.
     */
    private final int port;

    /**
     * Ctor.
     * @param slice To server
     * @param port HTTP port
     */
    private VertxMain(final Slice slice, final int port) {
        this.slice = slice;
        this.port = port;
    }

    @Override
    public void run() {
        new VertxSliceServer(this.slice, this.port).start();
    }

    /**
     * Entry point.
     * @param args CLI args
     */
    public static void main(final String... args) {
        final String storage = System.getProperty("artipie.storage");
        final int port = Integer.getInteger("artipie.port");
        final Thread thread = new Thread(
            new VertxMain(
                new Pie(new FileStorage(Path.of(storage))),
                port
            )
        );
        thread.setName(VertxMain.class.getSimpleName());
        thread.checkAccess();
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
        try {
            thread.join();
        } catch (final InterruptedException iox) {
            Logger.info(VertxMain.class, "interrupted: %s", iox);
        }
    }
}
