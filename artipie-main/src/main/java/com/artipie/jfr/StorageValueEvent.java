/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.jfr;

import jdk.jfr.Category;
import jdk.jfr.DataAmount;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;

/**
 * Storage event for the {@code value} operation.
 *
 * @since 0.28.0
 * @checkstyle JavadocVariableCheck (500 lines)
 * @checkstyle VisibilityModifierCheck (500 lines)
 * @checkstyle MemberNameCheck (500 lines)
 */
@Name("artipie.StorageValue")
@Label("Storage Get")
@Category({"Artipie", "Storage"})
@Description("Get value from a storage")
public final class StorageValueEvent extends AbstractStorageEvent {

    @Label("Chunks Count")
    public volatile int chunks;

    @Label("Value Size")
    @DataAmount
    public volatile long size;
}
