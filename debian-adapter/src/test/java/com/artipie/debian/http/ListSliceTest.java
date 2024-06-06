/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian.http;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.RsStatus;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;


public class ListSliceTest {

    /**
     * Repository settings.
     */
    private static final YamlMapping SETTINGS = Yaml.createYamlMappingBuilder()
        .add("Architectures", "amd64")
        .add("Components", "main").build();

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void testEmpty() {
        Response response = new ListSlice(this.asto).response(
            new RequestLine(RqMethod.GET, "/list-all"),
            Headers.EMPTY,
            Content.EMPTY
        ).join();
        byte[] bytes = response.body().asBytes();
        String responseBody = new String(bytes, StandardCharsets.UTF_8);
        MatcherAssert.assertThat(
            "Response status is not OK",
            response,
            new RsHasStatus(RsStatus.OK)
        );
        MatcherAssert.assertThat(
            "Response body does not match",
            responseBody,
            new IsEqual<>("[]") // replace with the expected response body
        );
    }

    @Test
    void testList() {
        final Key release = new Key.From("dists/my_repo/Release");
        final Key inrelease = new Key.From("dists/my_repo/InRelease");
        final Key deb1 = new Key.From("main/artifact1.deb");
        final Key deb2 = new Key.From("main/artifact2.deb");
        final Key packages = new Key.From("dists/my_repo/main/binary-amd64/Packages.gz");

        this.asto.save(release, Content.EMPTY).join();
        this.asto.save(inrelease, Content.EMPTY).join();
        this.asto.save(deb1, Content.EMPTY).join();
        this.asto.save(deb2, Content.EMPTY).join();
        this.asto.save(packages, Content.EMPTY).join();

        Response response = new ListSlice(this.asto).response(
            new RequestLine(RqMethod.GET, "/list-all"),
            Headers.EMPTY,
            Content.EMPTY
        ).join();
        byte[] bytes = response.body().asBytes();
        String responseBody = new String(bytes, StandardCharsets.UTF_8);
        MatcherAssert.assertThat(
            "Response status OK",
            response,
            new RsHasStatus(RsStatus.OK)
        );
        MatcherAssert.assertThat(
            "Response body match",
            responseBody,
            new IsEqual<>("[\"main/artifact1.deb\",\"main/artifact2.deb\"]") // replace with the expected response body
        );
    }
}
