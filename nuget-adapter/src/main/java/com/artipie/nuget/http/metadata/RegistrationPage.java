/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http.metadata;

import com.artipie.nuget.PackageIdentity;
import com.artipie.nuget.Repository;
import com.artipie.nuget.metadata.NuspecField;
import java.util.List;
import java.util.concurrent.CompletionStage;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

/**
 * Registration page.
 * See <a href="https://docs.microsoft.com/en-us/nuget/api/registration-base-url-resource#registration-page-object">Registration page</a>
 *
 * @since 0.1
 */
final class RegistrationPage {

    /**
     * Repository.
     */
    private final Repository repository;

    /**
     * Package content location.
     */
    private final ContentLocation content;

    /**
     * Package identifier.
     */
    private final NuspecField id;

    /**
     * Ordered list of versions on this page from lowest to highest.
     */
    private final List<NuspecField> versions;

    /**
     * Ctor.
     *
     * @param repository Repository.
     * @param content Package content location.
     * @param id Package identifier.
     * @param versions Ordered list of versions on this page from lowest to highest.
     * @todo #87:60min Refactor RegistrationPage class, reduce number of fields.
     *  Probably it is needed to extract some abstraction for creating leaf objects,
     *  that will join `repository` and `content` fields and produce leaf JSON for package identity.
     * @checkstyle ParameterNumberCheck (2 line)
     */
    RegistrationPage(
        final Repository repository,
        final ContentLocation content,
        final NuspecField id,
        final List<NuspecField> versions
    ) {
        this.repository = repository;
        this.content = content;
        this.id = id;
        this.versions = versions;
    }

    /**
     * Generates page in JSON.
     *
     * @return Page JSON.
     */
    public CompletionStage<JsonObject> json() {
        if (this.versions.isEmpty()) {
            throw new IllegalStateException(
                String.format("Registration page contains no versions: '%s'", this.id)
            );
        }
        final NuspecField lower = this.versions.get(0);
        final NuspecField upper = this.versions.get(this.versions.size() - 1);
        return new CompletionStages<>(
            this.versions.stream().map(
                version -> this.leaf(new PackageIdentity(this.id, version))
            )
        ).all().thenApply(
            leafs -> {
                final JsonArrayBuilder items = Json.createArrayBuilder();
                for (final JsonObject leaf : leafs) {
                    items.add(leaf);
                }
                return Json.createObjectBuilder()
                    .add("lower", lower.normalized())
                    .add("upper", upper.normalized())
                    .add("count", this.versions.size())
                    .add("items", items)
                    .build();
            }
        );
    }

    /**
     * Builds registration leaf.
     * See <a href="https://docs.microsoft.com/en-us/nuget/api/registration-base-url-resource#registration-leaf-object-in-a-page"></a>
     *
     * @param identity Package identity.
     * @return JSON representing registration leaf.
     */
    private CompletionStage<JsonObject> leaf(final PackageIdentity identity) {
        return this.repository.nuspec(identity).thenApply(
            nuspec -> Json.createObjectBuilder()
                .add(
                    "catalogEntry",
                    Json.createObjectBuilder()
                        .add("id", nuspec.id().raw())
                        .add("version", nuspec.version().normalized())
                )
                .add("packageContent", this.content.url(identity).toString())
                .build()
        );
    }
}
