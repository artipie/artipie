/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;

/**
 * Storage event for the {@code delete} operation.
 *
 * @since 0.28.0
 * @checkstyle JavadocVariableCheck (500 lines)
 * @checkstyle VisibilityModifierCheck (500 lines)
 */
@Name("artipie.StorageDelete")
@Label("Storage Delete")
@Category({"Artipie", "Storage"})
@Description("Delete value from a storage")
public final class StorageDeleteEvent extends AbstractStorageEvent {

}
