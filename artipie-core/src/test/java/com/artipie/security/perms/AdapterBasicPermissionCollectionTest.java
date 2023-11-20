/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.security.perms;

import java.security.AllPermission;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AdapterBasicPermission}.
 * @since 1.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class AdapterBasicPermissionCollectionTest {

    @Test
    void impliesWhenPermissionIsPresent() {
        final AdapterBasicPermission.AdapterBasicPermissionCollection collection =
            new AdapterBasicPermission.AdapterBasicPermissionCollection();
        collection.add(new AdapterBasicPermission("read", Action.Standard.READ));
        collection.add(new AdapterBasicPermission("all", Action.ALL));
        collection.add(new AdapterBasicPermission("write", Action.Standard.WRITE));
        MatcherAssert.assertThat(
            collection.implies(new AdapterBasicPermission("all", Action.Standard.DELETE)),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            collection.implies(new AdapterBasicPermission("write", Action.Standard.WRITE)),
            new IsEqual<>(true)
        );
    }

    @Test
    void throwsErrorIfWrongTypeAdded() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new AdapterBasicPermission.AdapterBasicPermissionCollection()
                .add(new AllPermission())
        );
    }

    @Test
    void doesNotImplyPermissionOfWrongType() {
        final AdapterBasicPermission.AdapterBasicPermissionCollection collection =
            new AdapterBasicPermission.AdapterBasicPermissionCollection();
        collection.add(new AdapterBasicPermission("some", Action.Standard.READ));
        MatcherAssert.assertThat(
            collection.implies(new AllPermission()),
            new IsEqual<>(false)
        );
    }

    @Test
    void notImpliesWhenPermissionIsNotPresent() {
        final AdapterBasicPermission.AdapterBasicPermissionCollection collection =
            new AdapterBasicPermission.AdapterBasicPermissionCollection();
        collection.add(new AdapterBasicPermission("one", Action.Standard.READ));
        collection.add(new AdapterBasicPermission("two", Action.ALL));
        collection.add(new AdapterBasicPermission("three", Action.Standard.WRITE));
        MatcherAssert.assertThat(
            collection.implies(new AdapterBasicPermission("some", Action.Standard.DELETE)),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            collection.implies(new AdapterBasicPermission("one", Action.Standard.WRITE)),
            new IsEqual<>(false)
        );
    }

    @Test
    void impliesAnyWhenAllPermissionIsPresent() {
        final AdapterBasicPermission.AdapterBasicPermissionCollection collection =
            new AdapterBasicPermission.AdapterBasicPermissionCollection();
        collection.add(new AdapterBasicPermission("*", Action.ALL));
        MatcherAssert.assertThat(
            collection.implies(new AdapterBasicPermission("theName", Action.Standard.WRITE)),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            collection.implies(new AdapterBasicPermission("anotherName", Action.Standard.DELETE)),
            new IsEqual<>(true)
        );
    }

}
