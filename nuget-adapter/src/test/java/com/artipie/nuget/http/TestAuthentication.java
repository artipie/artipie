/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http;

import com.artipie.http.Headers;
import com.artipie.http.auth.Authentication;
import com.artipie.http.headers.Authorization;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Single user basic authentication for usage in tests.
 *
 * @since 0.2
 */
public final class TestAuthentication extends Authentication.Wrap {


    public static final String USERNAME = "Aladdin";
    public static final String PASSWORD = "OpenSesame";

    public static final com.artipie.http.headers.Header HEADER = new Authorization.Basic(TestAuthentication.USERNAME, TestAuthentication.PASSWORD);
    public static final com.artipie.http.Headers HEADERS = com.artipie.http.Headers.from(HEADER);

    public TestAuthentication() {
        super(new Single(TestAuthentication.USERNAME, TestAuthentication.PASSWORD));
    }
}
