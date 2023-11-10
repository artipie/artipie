/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.nuget.http;

import com.artipie.http.auth.Authentication;
import com.artipie.http.headers.Authorization;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Single user basic authentication for usage in tests.
 *
 * @since 0.2
 */
public final class TestAuthentication extends Authentication.Wrap {

    /**
     * User name.
     */
    public static final String USERNAME = "Aladdin";

    /**
     * Password.
     */
    public static final String PASSWORD = "OpenSesame";

    /**
     * Ctor.
     */
    public TestAuthentication() {
        super(new Single(TestAuthentication.USERNAME, TestAuthentication.PASSWORD));
    }

    /**
     * Basic authentication header.
     *
     * @since 0.2
     */
    public static final class Header extends com.artipie.http.headers.Header.Wrap {

        /**
         * Ctor.
         */
        public Header() {
            super(
                new Authorization.Basic(
                    TestAuthentication.USERNAME,
                    TestAuthentication.PASSWORD
                )
            );
        }
    }

    /**
     * Basic authentication headers.
     *
     * @since 0.2
     */
    public static final class Headers implements com.artipie.http.Headers {

        /**
         * Origin headers.
         */
        private final com.artipie.http.Headers origin;

        /**
         * Ctor.
         */
        public Headers() {
            this.origin = new From(new Header());
        }

        @Override
        public Iterator<Map.Entry<String, String>> iterator() {
            return this.origin.iterator();
        }

        @Override
        public void forEach(final Consumer<? super Map.Entry<String, String>> action) {
            this.origin.forEach(action);
        }

        @Override
        public Spliterator<Map.Entry<String, String>> spliterator() {
            return this.origin.spliterator();
        }
    }
}
