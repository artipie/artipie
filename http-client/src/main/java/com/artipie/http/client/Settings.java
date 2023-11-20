/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Client slices settings.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.ExcessivePublicCount")
public interface Settings {

    /**
     * Read HTTP proxy settings if enabled.
     *
     * @return Proxy settings if enabled, empty if no proxy should be used.
     */
    Optional<Proxy> proxy();

    /**
     * Determine if it is required to trust all SSL certificates.
     *
     * @return If no SSL certificate checks required <code>true</code> is returned,
     *  <code>false</code> - otherwise.
     */
    boolean trustAll();

    /**
     * Determine if redirects should be followed.
     *
     * @return If redirects should be followed <code>true</code> is returned,
     *  <code>false</code> - otherwise.
     */
    boolean followRedirects();

    /**
     * Max time, in milliseconds, a connection can take to connect to destination.
     * Zero means infinite wait time.
     *
     * @return Connect timeout in milliseconds.
     */
    long connectTimeout();

    /**
     * The max time, in milliseconds, a connection can be idle (no incoming or outgoing traffic).
     * Zero means infinite wait time.
     *
     * @return Idle timeout in milliseconds.
     */
    long idleTimeout();

    /**
     * Use http3 transport.
     * @return Should http3 protocol be used?
     * @checkstyle LocalFinalVariableNameCheck (10 lines)
     * @checkstyle MethodNameCheck (10 lines)
     */
    boolean http3();

    /**
     * Proxy settings.
     *
     * @since 0.1
     */
    interface Proxy {

        /**
         * Read if proxy is secure.
         *
         * @return If proxy should be accessed via HTTPS protocol <code>true</code> is returned,
         *  <code>false</code> - for unsecure HTTP proxies.
         */
        boolean secure();

        /**
         * Read proxy host name.
         *
         * @return Proxy host.
         */
        String host();

        /**
         * Read proxy port.
         *
         * @return Proxy port.
         */
        int port();

        /**
         * Simple proxy settings.
         *
         * @since 0.1
         */
        final class Simple implements Proxy {

            /**
             * Secure flag.
             */
            private final boolean secure;

            /**
             * Proxy host.
             */
            private final String host;

            /**
             * Proxy port.
             */
            private final int port;

            /**
             * Ctor.
             *
             * @param secure Secure flag.
             * @param host Proxy host.
             * @param port Proxy port.
             */
            public Simple(final boolean secure, final String host, final int port) {
                this.secure = secure;
                this.host = host;
                this.port = port;
            }

            @Override
            public boolean secure() {
                return this.secure;
            }

            @Override
            public String host() {
                return this.host;
            }

            @Override
            public int port() {
                return this.port;
            }
        }
    }

    /**
     * Default {@link Settings}.
     *
     * @since 0.1
     */
    final class Default implements Settings {

        @Override
        public Optional<Proxy> proxy() {
            return Optional.empty();
        }

        @Override
        public boolean trustAll() {
            return false;
        }

        @Override
        public boolean followRedirects() {
            return false;
        }

        @Override
        public long connectTimeout() {
            // @checkstyle MagicNumberCheck (1 line)
            return 15_000L;
        }

        @Override
        public long idleTimeout() {
            return 0L;
        }

        @Override
        public boolean http3() {
            return false;
        }
    }

    /**
     * Settings that add proxy to origin {@link Settings}.
     *
     * @since 0.1
     */
    final class WithProxy implements Settings {

        /**
         * Origin settings.
         */
        private final Settings origin;

        /**
         * Proxy.
         */
        private final Proxy prx;

        /**
         * Ctor.
         *
         * @param prx Proxy.
         */
        public WithProxy(final Proxy prx) {
            this(new Settings.Default(), prx);
        }

        /**
         * Ctor.
         *
         * @param origin Origin settings.
         * @param prx Proxy.
         */
        public WithProxy(final Settings origin, final Proxy prx) {
            this.origin = origin;
            this.prx = prx;
        }

        @Override
        public Optional<Proxy> proxy() {
            return Optional.of(this.prx);
        }

        @Override
        public boolean trustAll() {
            return this.origin.trustAll();
        }

        @Override
        public boolean followRedirects() {
            return this.origin.followRedirects();
        }

        @Override
        public long connectTimeout() {
            return this.origin.connectTimeout();
        }

        @Override
        public long idleTimeout() {
            return this.origin.idleTimeout();
        }

        @Override
        public boolean http3() {
            return false;
        }
    }

    /**
     * Settings that add trust all setting to origin {@link Settings}.
     *
     * @since 0.1
     */
    final class WithTrustAll implements Settings {

        /**
         * Origin settings.
         */
        private final Settings origin;

        /**
         * Trust all setting.
         */
        private final boolean trust;

        /**
         * Ctor.
         *
         * @param trust Trust all setting.
         */
        public WithTrustAll(final boolean trust) {
            this(new Settings.Default(), trust);
        }

        /**
         * Ctor.
         *
         * @param origin Origin settings.
         * @param trust Trust all setting.
         */
        public WithTrustAll(final Settings origin, final boolean trust) {
            this.origin = origin;
            this.trust = trust;
        }

        @Override
        public Optional<Proxy> proxy() {
            return this.origin.proxy();
        }

        @Override
        public boolean trustAll() {
            return this.trust;
        }

        @Override
        public boolean followRedirects() {
            return this.origin.followRedirects();
        }

        @Override
        public long connectTimeout() {
            return this.origin.connectTimeout();
        }

        @Override
        public long idleTimeout() {
            return this.origin.idleTimeout();
        }

        @Override
        public boolean http3() {
            return false;
        }
    }

    /**
     * Settings that add follow redirect setting to origin {@link Settings}.
     *
     * @since 0.1
     */
    final class WithFollowRedirects implements Settings {

        /**
         * Origin settings.
         */
        private final Settings origin;

        /**
         * Follow redirect setting.
         */
        private final boolean redirect;

        /**
         * Ctor.
         *
         * @param redirect Follow redirect setting.
         */
        public WithFollowRedirects(final boolean redirect) {
            this(new Settings.Default(), redirect);
        }

        /**
         * Ctor.
         *
         * @param origin Origin settings.
         * @param redirect Follow redirect setting.
         */
        public WithFollowRedirects(final Settings origin, final boolean redirect) {
            this.origin = origin;
            this.redirect = redirect;
        }

        @Override
        public Optional<Proxy> proxy() {
            return this.origin.proxy();
        }

        @Override
        public boolean trustAll() {
            return this.origin.trustAll();
        }

        @Override
        public boolean followRedirects() {
            return this.redirect;
        }

        @Override
        public long connectTimeout() {
            return this.origin.connectTimeout();
        }

        @Override
        public long idleTimeout() {
            return this.origin.idleTimeout();
        }

        @Override
        public boolean http3() {
            return false;
        }
    }

    /**
     * Settings that add connect timeout setting to origin {@link Settings}.
     *
     * @since 0.2
     */
    final class WithConnectTimeout implements Settings {

        /**
         * Origin settings.
         */
        private final Settings origin;

        /**
         * Connect timeout setting.
         */
        private final long millis;

        /**
         * Ctor.
         *
         * @param timeout Connect timeout.
         * @param unit The time unit of the timeout argument.
         */
        public WithConnectTimeout(final long timeout, final TimeUnit unit) {
            this(unit.toMillis(timeout));
        }

        /**
         * Ctor.
         *
         * @param origin Origin settings.
         * @param timeout Connect timeout.
         * @param unit The time unit of the timeout argument.
         */
        public WithConnectTimeout(final Settings origin, final long timeout, final TimeUnit unit) {
            this(origin, unit.toMillis(timeout));
        }

        /**
         * Ctor.
         *
         * @param millis Connect timeout in milliseconds.
         */
        public WithConnectTimeout(final long millis) {
            this(new Settings.Default(), millis);
        }

        /**
         * Ctor.
         *
         * @param origin Origin settings.
         * @param millis Connect timeout setting.
         */
        public WithConnectTimeout(final Settings origin, final long millis) {
            this.origin = origin;
            this.millis = millis;
        }

        @Override
        public Optional<Proxy> proxy() {
            return this.origin.proxy();
        }

        @Override
        public boolean trustAll() {
            return this.origin.trustAll();
        }

        @Override
        public boolean followRedirects() {
            return this.origin.followRedirects();
        }

        @Override
        public long connectTimeout() {
            return this.millis;
        }

        @Override
        public long idleTimeout() {
            return this.origin.idleTimeout();
        }

        @Override
        public boolean http3() {
            return false;
        }
    }

    /**
     * Settings that add idle timeout setting to origin {@link Settings}.
     *
     * @since 0.2
     */
    final class WithIdleTimeout implements Settings {

        /**
         * Origin settings.
         */
        private final Settings origin;

        /**
         * Idle timeout setting.
         */
        private final long millis;

        /**
         * Ctor.
         *
         * @param timeout Idle timeout.
         * @param unit The time unit of the timeout argument.
         */
        public WithIdleTimeout(final long timeout, final TimeUnit unit) {
            this(unit.toMillis(timeout));
        }

        /**
         * Ctor.
         *
         * @param origin Origin settings.
         * @param timeout Idle timeout.
         * @param unit The time unit of the timeout argument.
         */
        public WithIdleTimeout(final Settings origin, final long timeout, final TimeUnit unit) {
            this(origin, unit.toMillis(timeout));
        }

        /**
         * Ctor.
         *
         * @param millis Idle timeout in milliseconds.
         */
        public WithIdleTimeout(final long millis) {
            this(new Settings.Default(), millis);
        }

        /**
         * Ctor.
         *
         * @param origin Origin settings.
         * @param millis Idle timeout setting.
         */
        public WithIdleTimeout(final Settings origin, final long millis) {
            this.origin = origin;
            this.millis = millis;
        }

        @Override
        public Optional<Proxy> proxy() {
            return this.origin.proxy();
        }

        @Override
        public boolean trustAll() {
            return this.origin.trustAll();
        }

        @Override
        public boolean followRedirects() {
            return this.origin.followRedirects();
        }

        @Override
        public long connectTimeout() {
            return this.origin.connectTimeout();
        }

        @Override
        public long idleTimeout() {
            return this.millis;
        }

        @Override
        public boolean http3() {
            return false;
        }
    }

    /**
     * Settings with http3 and trust all.
     * @since 0.3
     */
    final class Http3WithTrustAll implements Settings {

        @Override
        public Optional<Proxy> proxy() {
            return Optional.empty();
        }

        @Override
        public boolean trustAll() {
            return true;
        }

        @Override
        public boolean followRedirects() {
            return false;
        }

        @Override
        public long connectTimeout() {
            // @checkstyle MagicNumberCheck (5 lines)
            return 15_000L;
        }

        @Override
        public long idleTimeout() {
            return 0;
        }

        @Override
        public boolean http3() {
            return true;
        }
    }
}
