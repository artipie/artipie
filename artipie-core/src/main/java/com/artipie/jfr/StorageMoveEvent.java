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
 * Storage event for the {@code move} operation.
 *
 * @since 0.28.0
 */
@Name("artipie.StorageMove")
@Label("Storage Move")
@Category({"Artipie", "Storage"})
@Description("Move value from one location to another")
public final class StorageMoveEvent extends AbstractStorageEvent {
    @Label("Target Key")
    public volatile String target;
}
