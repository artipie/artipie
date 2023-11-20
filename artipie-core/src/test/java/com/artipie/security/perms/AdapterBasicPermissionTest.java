/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.security.perms;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link AdapterBasicPermission}.
 * @since 1.2
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class AdapterBasicPermissionTest {

    @ParameterizedTest
    @ValueSource(strings = {"read", "write", "delete", "*", "read,write,delete"})
    void permWithAllActionImpliesAnyAction(final String actions) {
        MatcherAssert.assertThat(
            new AdapterBasicPermission("some-name", Action.ALL).implies(
                new AdapterBasicPermission("some-name", actions)
            ),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "read,write", "write,read", "delete,read", "read,delete", "read",
        "read,write,delete", "write,read,delete", "delete,write,read"
    })
    void permWithReadImpliesPermRead(final String actions) {
        MatcherAssert.assertThat(
            new AdapterBasicPermission("some-name", actions).implies(
                new AdapterBasicPermission("some-name",  Action.Standard.READ)
            ),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "read,write", "write,read", "delete,write", "write,delete", "write",
        "read,delete,write", "write,read,delete", "delete,write,read"
    })
    void permWithWriteImpliesPermWrite(final String actions) {
        MatcherAssert.assertThat(
            new AdapterBasicPermission("some-name", actions).implies(
                new AdapterBasicPermission("some-name",  Action.Standard.WRITE)
            ),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "delete,write", "write,delete", "delete,read", "read,delete", "delete",
        "read,delete,write", "write,read,delete", "delete,write,read"
    })
    void permWithDeleteImpliesPermDelete(final String actions) {
        MatcherAssert.assertThat(
            new AdapterBasicPermission("some-name", actions).implies(
                new AdapterBasicPermission("some-name",  Action.Standard.DELETE)
            ),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "write", "delete", "delete,read", "read,delete", "read,write", "read,write",
        "read,write,delete", "write,read,delete", "delete,write,read"
    })
    void permReadNotImpliesOtherPerms(final String actions) {
        MatcherAssert.assertThat(
            new AdapterBasicPermission("some-name", Action.Standard.READ).implies(
                new AdapterBasicPermission("some-name",  actions)
            ),
            new IsEqual<>(false)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "read", "delete", "write,read", "read,write", "delete,write", "delete,write",
        "read,write,delete", "write,read,delete", "delete,write,read"
    })
    void permWriteNotImpliesOtherPerms(final String actions) {
        MatcherAssert.assertThat(
            new AdapterBasicPermission("some-name", Action.Standard.WRITE).implies(
                new AdapterBasicPermission("some-name",  actions)
            ),
            new IsEqual<>(false)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "write", "read", "delete,read", "delete,write", "write,delete", "read,delete",
        "read,write,delete", "write,read,delete", "delete,write,read"
    })
    void permDeleteNotImpliesOtherPerms(final String actions) {
        MatcherAssert.assertThat(
            new AdapterBasicPermission("some-name", Action.Standard.DELETE).implies(
                new AdapterBasicPermission("some-name",  actions)
            ),
            new IsEqual<>(false)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"write", "delete", "read"})
    void notImpliesWhenNamesAreDifferent(final String action) {
        MatcherAssert.assertThat(
            new AdapterBasicPermission("some-name", action).implies(
                new AdapterBasicPermission("another-name",  action)
            ),
            new IsEqual<>(false)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"write", "delete", "read"})
    void impliesWhenNameIsWildcard(final String action) {
        MatcherAssert.assertThat(
            new AdapterBasicPermission("*", action).implies(
                new AdapterBasicPermission("another-name",  action)
            ),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"write", "delete", "read"})
    void notImpliesPermWithWildcard(final String action) {
        MatcherAssert.assertThat(
            new AdapterBasicPermission("some-name", action).implies(
                new AdapterBasicPermission("*",  action)
            ),
            new IsEqual<>(false)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"write", "delete", "read", "*"})
    void noActionsAreAllowedIfActionsStringIsEmpty(final String action) {
        MatcherAssert.assertThat(
            new AdapterBasicPermission("some-name", "").implies(
                new AdapterBasicPermission("some-name",  action)
            ),
            new IsEqual<>(false)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"write", "delete", "read", "*"})
    void noneActionIsImplied(final String action) {
        MatcherAssert.assertThat(
            new AdapterBasicPermission("some-name", action).implies(
                new AdapterBasicPermission("some-name", Action.NONE)
            ),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"write", "delete", "read", "*"})
    void noneActionDoesNotImplyAnyAction(final String action) {
        MatcherAssert.assertThat(
            new AdapterBasicPermission("some-name", Action.NONE).implies(
                new AdapterBasicPermission("some-name", action)
            ),
            new IsEqual<>(false)
        );
    }

}
