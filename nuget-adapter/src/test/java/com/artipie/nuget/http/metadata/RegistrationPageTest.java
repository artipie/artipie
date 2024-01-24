/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http.metadata;

import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.nuget.AstoRepository;
import com.artipie.nuget.PackageIdentity;
import com.artipie.nuget.Repository;
import com.artipie.nuget.metadata.NuspecField;
import com.artipie.nuget.metadata.PackageId;
import com.artipie.nuget.metadata.Version;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.JsonObject;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

/**
 * Tests for {@link RegistrationPage}.
 *
 * @since 0.1
 */
class RegistrationPageTest {

    @Test
    void shouldGenerateJson() {
        final Storage storage = new InMemoryStorage();
        final Repository repository = new AstoRepository(storage);
        final PackageId id = new PackageId("My.Lib");
        final String lower = "0.1";
        final String upper = "0.2";
        final List<NuspecField> versions = Stream.of(lower, "0.1.2", upper)
            .map(Version::new)
            .collect(Collectors.toList());
        for (final NuspecField version : versions) {
            new BlockingStorage(storage).save(
                new PackageIdentity(id, version).nuspecKey(),
                String.join(
                    "",
                    "<?xml version=\"1.0\"?>",
                    "<package xmlns=\"http://schemas.microsoft.com/packaging/2013/05/nuspec.xsd\">",
                    "<metadata>",
                    String.format("<id>%s</id>", id.normalized()),
                    String.format("<version>%s</version>", version.normalized()),
                    "</metadata>",
                    "</package>"
                ).getBytes()
            );
        }
        MatcherAssert.assertThat(
            new RegistrationPage(repository, RegistrationPageTest::contentUrl, id, versions).json()
                .toCompletableFuture().join(),
            new AllOf<>(
                Arrays.asList(
                    new JsonHas("lower", new JsonValueIs(lower)),
                    new JsonHas("upper", new JsonValueIs(upper)),
                    new JsonHas("count", new JsonValueIs(versions.size())),
                    new JsonHas(
                        "items",
                        new JsonContains(
                            versions.stream()
                                .map(version -> entryMatcher(id, version))
                                .collect(Collectors.toList())
                        )
                    )
                )
            )
        );
    }

    @Test
    void shouldFailToGenerateJsonWhenEmpty() {
        final String id = "Some.Lib";
        final Throwable throwable = Assertions.assertThrows(
            IllegalStateException.class,
            () -> new RegistrationPage(
                new AstoRepository(new InMemoryStorage()),
                RegistrationPageTest::contentUrl,
                new PackageId(id),
                Collections.emptyList()
            ).json()
        );
        MatcherAssert.assertThat(
            throwable.getMessage(),
            new AllOf<>(
                Arrays.asList(
                    new StringContains(true, "Registration page contains no versions"),
                    new StringContains(false, id)
                )
            )
        );
    }

    private static Matcher<JsonObject> entryMatcher(
        final NuspecField id, final NuspecField version
    ) {
        return new AllOf<>(
            Arrays.asList(
                new JsonHas(
                    "catalogEntry",
                    new AllOf<>(
                        Arrays.asList(
                            new JsonHas("id", new JsonValueIs(id.normalized())),
                            new JsonHas("version", new JsonValueIs(version.normalized()))
                        )
                    )
                ),
                new JsonHas(
                    "packageContent",
                    new JsonValueIs(
                        RegistrationPageTest.contentUrl(new PackageIdentity(id, version)).toString()
                    )
                )
            )
        );
    }

    private static URL contentUrl(final PackageIdentity identity) {
        try {
            return URI.create(
                String.format("http://localhost:8080/content/%s", identity.nupkgKey().string())
            ).toURL();
        } catch (final MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
