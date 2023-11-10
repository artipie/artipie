/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.nuget;

import com.artipie.nuget.metadata.Nuspec;
import java.io.ByteArrayInputStream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Nupkg}.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
class NupkgTest {

    /**
     * Resource `newtonsoft.json.12.0.3.nupkg` name.
     */
    private String name;

    @BeforeEach
    void init() {
        this.name = "newtonsoft.json.12.0.3.nupkg";
    }

    @Test
    void shouldExtractNuspec() {
        final Nuspec nuspec = new Nupkg(
            new ByteArrayInputStream(new NewtonJsonResource(this.name).bytes())
        ).nuspec();
        MatcherAssert.assertThat(
            nuspec.id().normalized(),
            Matchers.is("newtonsoft.json")
        );
    }
}
