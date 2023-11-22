/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.json.JsonObject;

/**
 * PHP Composer package.
 *
 * @since 0.1
 */
public interface Package {
    /**
     * Extract name from package.
     *
     * @return Package name.
     */
    CompletionStage<Name> name();

    /**
     * Extract version from package. Returns passed as a parameter value if present
     * in case of absence version.
     *
     * @param value Value in case of absence of version. This value can be empty.
     * @return Package version.
     */
    CompletionStage<Optional<String>> version(Optional<String> value);

    /**
     * Reads package content as JSON object.
     *
     * @return Package JSON object.
     */
    CompletionStage<JsonObject> json();
}
