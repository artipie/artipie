/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.jfr;

import jdk.jfr.Category;
import jdk.jfr.DataAmount;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;

/**
 * Storage event for the {@code save} operation.
 *
 * @since 0.28.0
 */
@Name("artipie.StorageSave")
@Label("Storage Save")
@Category({"Artipie", "Storage"})
@Description("Save value to a storage")
public final class StorageSaveEvent extends AbstractStorageEvent {

    @Label("Chunks Count")
    public volatile int chunks;

    @Label("Value Size")
    @DataAmount
    public volatile long size;
}
