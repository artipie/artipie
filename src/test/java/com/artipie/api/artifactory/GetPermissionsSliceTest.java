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
package com.artipie.api.artifactory;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.Credentials;
import com.artipie.Settings;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AnyOf;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link GetPermissionsSlice}.
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class GetPermissionsSliceTest {
    /**
     * Artipie base url.
     */
    private static final String BASE = "http://artipie.com/";

    /**
     * Artipie yaml meta section.
     */
    private static final YamlMapping META = Yaml.createYamlMappingBuilder()
        .add("base_url", GetPermissionsSliceTest.BASE)
        .build();

    @Test
    void shouldReturnsPermissionsList() {
        final String uri = String.format(
            "%sapi/security/permissions/", GetPermissionsSliceTest.BASE
        );
        final String jsonobj = "{\"name\":\"%s\",\"uri\":\"%s%s\"}";
        final String read = "readSourceArtifacts";
        final String cache = "populateCaches";
        final String readobj = String.format(jsonobj, read, uri, read);
        final String cacheobj = String.format(jsonobj, cache, uri, cache);
        final Storage storage = new InMemoryStorage();
        storage.save(new Key.From(this.nameYaml(read)), Content.EMPTY).join();
        storage.save(new Key.From(this.nameYaml(cache)), Content.EMPTY).join();
        MatcherAssert.assertThat(
            new GetPermissionsSlice(
                new Settings.Fake(storage, new Credentials.FromEnv(), GetPermissionsSliceTest.META)
            ),
            new SliceHasResponse(
                new AnyOf<>(
                    Arrays.asList(
                        new RsHasBody(
                            String.format("[%s,%s]", readobj, cacheobj), StandardCharsets.UTF_8
                        ),
                        new RsHasBody(
                            String.format("[%s,%s]", cacheobj, readobj), StandardCharsets.UTF_8
                        )
                    )
                ),
                new RequestLine(RqMethod.GET, "/")
            )
        );
    }

    private String nameYaml(final String name) {
        return String.format("%s.yaml", name);
    }
}
