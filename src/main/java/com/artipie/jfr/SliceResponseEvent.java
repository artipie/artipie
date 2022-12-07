/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.jfr;

import jdk.jfr.Category;
import jdk.jfr.DataAmount;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * Slice response event triggered when the slice's {@code response} method is called.
 *
 * @since 0.28
 * @checkstyle JavadocVariableCheck (500 lines)
 * @checkstyle MemberNameCheck (500 lines)
 * @checkstyle VisibilityModifierCheck (500 lines)
 */
@Name("artipie.SliceResponse")
@Label("Slice Response")
@Category({"Artipie", "Slice"})
@Description("HTTP request")
@StackTrace(false)
public class SliceResponseEvent extends Event {

    @Label("Request Method")
    public volatile String method;

    @Label("Request Path")
    public volatile String path;

    @Label("Headers")
    public String headers;

    @Label("Request Body Chunks Count")
    public volatile int requestChunks;

    @Label("Request Body Value Size")
    @DataAmount
    public volatile long requestSize;

    @Label("Response Body Chunks Count")
    public volatile int responseChunks;

    @Label("Response Body Value Size")
    @DataAmount
    public volatile long responseSize;
}
