/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie;

import com.artipie.http.Slice;
import com.artipie.http.auth.BasicAuthzSlice;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.misc.RandomFreePort;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.common.RsJson;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceSimple;
import com.artipie.security.perms.Action;
import com.artipie.security.perms.AdapterBasicPermission;
import com.artipie.security.policy.Policy;
import com.artipie.vertx.VertxSliceServer;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import javax.json.Json;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

/**
 * Slices integration tests.
 */
@EnabledForJreRange(min = JRE.JAVA_11, disabledReason = "HTTP client is not supported prior JRE_11")
public final class SliceITCase {

    /**
     * Test target slice.
     */
    private static final Slice TARGET = new SliceRoute(
        new RtRulePath(
            new ByMethodsRule(RqMethod.GET),
            new BasicAuthzSlice(
                new SliceSimple(
                    new RsJson(
                        () -> Json.createObjectBuilder().add("any", "any").build()
                    )
                ),
                (username, password) -> Optional.empty(),
                new OperationControl(Policy.FREE, new AdapterBasicPermission("test", Action.ALL))
            )
        )
    );

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Application port.
     */
    private int port;

    @BeforeEach
    void init() throws Exception {
        this.port = new RandomFreePort().get();
        this.server = new VertxSliceServer(SliceITCase.TARGET, this.port);
        this.server.start();
    }

    @Test
    @Timeout(10)
    void singleRequestWorks() throws Exception {
        this.getRequest();
    }

    @Test
    @Timeout(10)
    void doubleRequestWorks() throws Exception {
        this.getRequest();
        this.getRequest();
    }

    @AfterEach
    void stop() {
        this.server.stop();
        this.server.close();
    }

    private void getRequest() throws Exception {
        final HttpResponse<String> rsp = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(
                URI.create(String.format("http://localhost:%d/any", this.port))
            ).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        );
        MatcherAssert.assertThat("status", rsp.statusCode(), Matchers.equalTo(200));
        MatcherAssert.assertThat("body", rsp.body(), new StringContains("{\"any\":\"any\"}"));
    }
}
