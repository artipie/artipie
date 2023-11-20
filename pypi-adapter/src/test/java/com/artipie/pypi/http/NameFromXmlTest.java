/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi.http;

import com.artipie.asto.Content;
import java.util.concurrent.CompletionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link SearchSlice.NameFromXml}.
 * @since 0.7
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class NameFromXmlTest {

    @Test
    void getsProjectName() {
        MatcherAssert.assertThat(
            new SearchSlice.NameFromXml(new Content.From(this.xml().getBytes()))
                .get().toCompletableFuture().join(),
            new IsEqual<>("my_project")
        );
    }

    @Test
    void failsOnInvalidXml() {
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                CompletionException.class,
                () -> new SearchSlice.NameFromXml(
                    new Content.From("<?xml version='1.0'?>\n<a>1</a>".getBytes())
                ).get().toCompletableFuture().join()
            ).getCause(),
            new IsInstanceOf(IllegalArgumentException.class)
        );
    }

    private String xml() {
        return String.join(
            "\n", "<?xml version='1.0'?>",
            "<methodCall>",
            "<methodName>search</methodName>",
            "<params>",
            "<param>",
            "<value><struct>",
            "<member>",
            "<name>name</name>",
            "<value><array><data>",
            "<value><string>my_project</string></value>",
            "</data></array></value>",
            "</member>",
            "<member>",
            "<name>summary</name>",
            "<value><array><data>",
            "<value><string>abcdef</string></value>",
            "</data></array></value>",
            "</member>",
            "</struct></value>",
            "</param>",
            "<param>",
            "<value><string>or</string></value>",
            "</param>",
            "</params>",
            "</methodCall>"
        );
    }

}
