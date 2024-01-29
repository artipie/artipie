/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * Event triggered when storage is created.
 *
 * @since 0.28.0
 */
@Name("artipie.StorageCreate")
@Label("Storage Create")
@Category({"Artipie", "Storage"})
@Description("Event triggered when storage is created")
@StackTrace(false)
public class StorageCreateEvent extends Event {

    @Label("Storage Identifier")
    public volatile String storage;

}
