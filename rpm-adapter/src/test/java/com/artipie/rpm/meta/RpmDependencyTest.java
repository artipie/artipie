/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.rpm.pkg.HeaderTags;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link RpmDependency}.
 * @since 0.1
 * @checkstyle ParameterNumberCheck (500 lines)
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.UseObjectForClearerAPI"})
class RpmDependencyTest {

    @ParameterizedTest
    @CsvSource({
        "test,0.1,test,0.1",
        "test,1.5-snapshot,test,1.5-snapshot",
        "test,1.5.6,test,1.5.6-snapshot",
        "test,2.0.4-snapshot,test,2.0.4",
        "test,'',test,0.2",
        "test,0.3,test,''"
    })
    void returnsTrueWhenSatisfied(final String name, final String vers, final String aname,
        final String avers) {
        MatcherAssert.assertThat(
            new RpmDependency(name, new HeaderTags.Version(vers), Optional.of("EQ"))
                .isSatisfiedBy(aname, new HeaderTags.Version(avers)),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "0.1.1,LE,0.1,true",
        "0.1.1,GE,0.1,false",
        "0.1,GE,0.1.1,true",
        "0.1,LE,0.1.1,false"
    })
    void checksIfSatisfiedWithFlag(final String vers, final String flag, final String avers,
        final boolean res) {
        MatcherAssert.assertThat(
            new RpmDependency("any", new HeaderTags.Version(vers), Optional.of(flag))
                .isSatisfiedBy("any", new HeaderTags.Version(avers)),
            new IsEqual<>(res)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "test,0.1,any,0.1",
        "test,1.4,test,0.1"
    })
    void returnsFalseWhenNotSatisfied(final String name, final String vers, final String aname,
        final String avers) {
        MatcherAssert.assertThat(
            new RpmDependency(name, new HeaderTags.Version(vers), Optional.empty())
                .isSatisfiedBy(aname, new HeaderTags.Version(avers)),
            new IsEqual<>(false)
        );
    }

}
