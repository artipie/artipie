/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;

/**
 * Storage event for the {@code exclusively} operation.
 *
 * @since 0.28.0
 * @checkstyle JavadocVariableCheck (500 lines)
 * @checkstyle VisibilityModifierCheck (500 lines)
 */
@Name("artipie.StorageExclusively")
@Label("Storage Exclusively")
@Category({"Artipie", "Storage"})
@Description("Runs operation exclusively for specified key")
public final class StorageExclusivelyEvent extends AbstractStorageEvent {

}
