/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Spring boot application.
 * @since 0.26
 * @checkstyle AbbreviationAsWordInNameCheck (500 lines)
 */
@SpringBootApplication
public class SBApplication {

    /**
     * Application config.
     */
    @Autowired
    private AppConfig config;

    /**
     * Main start method.
     * @param args Program arguments
     */
    public static void main(final String... args) {
        SpringApplication.run(SBApplication.class, args);
    }

    /**
     * Start Artipie Vert.x server.
     * @throws IOException On IO error
     */
    @Bean
    void startVertx() throws IOException {
        final int port = this.config.vertxPort();
        new VertxMain(
            this.config.httpClient(), this.config.setting(),
            Vertx.vertx(), port
        ).start();
    }

}
