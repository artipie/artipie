/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Yegor Bugayenko
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

import com.jcabi.log.Logger;

/**
 * The main entrance.
 *
 * @since 0.1
 */
public final class Main {

    /**
     * The args.
     */
    private final String[] args;

    /**
     * The ctor.
     * @param cmd Command line arguments
     */
    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    private Main(final String... cmd) {
        this.args = cmd;
    }

    /**
     * The main entry point.
     *
     * @param cmd Command line arguments
     * @throws Exception If fails
     */
    public static void main(final String... cmd) throws Exception {
        new Main(cmd).exec();
    }

    /**
     * Run it.
     *
     * @throws Exception If fails
     */
    public void exec() throws Exception {
        assert this.args != null;
        Logger.info(this, "Works just fine!");
    }

}
