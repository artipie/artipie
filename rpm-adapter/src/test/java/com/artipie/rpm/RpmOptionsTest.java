/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm;

import org.apache.commons.cli.Option;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.llorllale.cactoos.matchers.Satisfies;

/**
 * Tests for {@link RpmOptions}.
 *
 * @since 0.11
 * @checkstyle LeftCurlyCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class RpmOptionsTest {

    @Test
    void createsCorrectDigestOption() {
        MatcherAssert.assertThat(
            RpmOptions.DIGEST.option(),
            new AllOf<>(
                new ListOf<Matcher<? super Option>>(
                    //@checkstyle LineLengthCheck (5 lines)
                    new Satisfies<>(opt -> "dgst".equals(opt.getArgName())),
                    new Satisfies<>(opt -> "d".equals(opt.getOpt())),
                    new Satisfies<>(opt -> "(optional, default sha256) configures Digest instance for Rpm: sha256 or sha1".equals(opt.getDescription())),
                    new Satisfies<>(opt -> "digest".equals(opt.getLongOpt()))
                )
            )
        );
    }

    @Test
    void createsCorrectNamingOption() {
        MatcherAssert.assertThat(
            RpmOptions.NAMING_POLICY.option(),
            new AllOf<>(
                new ListOf<Matcher<? super Option>>(
                    //@checkstyle LineLengthCheck (5 lines)
                    new Satisfies<>(opt -> "np".equals(opt.getArgName())),
                    new Satisfies<>(opt -> "n".equals(opt.getOpt())),
                    new Satisfies<>(opt -> "(optional, default plain) configures NamingPolicy for Rpm: plain, sha256 or sha1".equals(opt.getDescription())),
                    new Satisfies<>(opt -> "naming-policy".equals(opt.getLongOpt()))
                )
            )
        );
    }

    @Test
    void createsCorrectFilelistsOption() {
        MatcherAssert.assertThat(
            RpmOptions.FILELISTS.option(),
            new AllOf<>(
                new ListOf<Matcher<? super Option>>(
                    //@checkstyle LineLengthCheck (5 lines)
                    new Satisfies<>(opt -> "fl".equals(opt.getArgName())),
                    new Satisfies<>(opt -> "f".equals(opt.getOpt())),
                    new Satisfies<>(opt -> "(optional, default true) includes File Lists for Rpm: true or false".equals(opt.getDescription())),
                    new Satisfies<>(opt -> "filelists".equals(opt.getLongOpt()))
                )
            )
        );
    }

    @Test
    void returnsCorrectDigestName() {
        MatcherAssert.assertThat(
            RpmOptions.DIGEST.optionName(),
            new IsEqual<>("digest")
        );
    }

    @Test
    void returnsCorrectNamingPolicyName() {
        MatcherAssert.assertThat(
            RpmOptions.NAMING_POLICY.optionName(),
            new IsEqual<>("naming-policy")
        );
    }

    @Test
    void returnsCorrectFilistsName() {
        MatcherAssert.assertThat(
            RpmOptions.FILELISTS.optionName(),
            new IsEqual<>("filelists")
        );
    }
}
