/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;

/**
 * Storage event for the {@code deleteAll} operation.
 *
 * @since 0.28.0
 * @checkstyle JavadocVariableCheck (500 lines)
 * @checkstyle VisibilityModifierCheck (500 lines)
 */
@Name("artipie.StorageDeleteAll")
@Label("Storage Delete All")
@Category({"Artipie", "Storage"})
@Description("Delete all values with key prefix from a storage")
public final class StorageDeleteAllEvent extends AbstractStorageEvent {

}
