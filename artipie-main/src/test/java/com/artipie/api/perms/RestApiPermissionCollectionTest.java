/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api.perms;

import java.util.Set;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Test for {@link com.artipie.api.perms.RestApiPermission.RestApiPermissionCollection}.
 * @since 0.30
 * @checkstyle DesignForExtensionCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class RestApiPermissionCollectionTest {

    @ParameterizedTest
    @EnumSource(ApiRepositoryPermission.RepositoryAction.class)
    void collectionWithAllPermissionImpliesAnyOtherPermission(
        final ApiRepositoryPermission.RepositoryAction action
    ) {
        final ApiRepositoryPermission.RestApiPermissionCollection collection =
            new ApiRepositoryPermission.ApiRepositoryPermissionCollection();
        collection.add(new ApiRepositoryPermission(ApiRepositoryPermission.RepositoryAction.ALL));
        MatcherAssert.assertThat(
            collection.implies(new ApiRepositoryPermission(action)),
            new IsEqual<>(true)
        );
    }

    @Test
    void addsAndImplies() {
        final RestApiPermission.RestApiPermissionCollection collection =
            new ApiUserPermission.ApiUserPermissionCollection();
        collection.add(new ApiUserPermission(ApiUserPermission.UserAction.CREATE));
        collection.add(new ApiUserPermission(Set.of("update", "enable")));
        MatcherAssert.assertThat(
            "Implies permission with added action",
            collection.implies(new ApiUserPermission(ApiUserPermission.UserAction.CREATE)),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Implies permission with added action",
            collection.implies(new ApiUserPermission(ApiUserPermission.UserAction.UPDATE)),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Implies permission with added action",
            collection.implies(new ApiUserPermission(ApiUserPermission.UserAction.ENABLE)),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Not implies permission with not added action",
            collection.implies(new ApiUserPermission(ApiUserPermission.UserAction.DELETE)),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Not implies permission of different class",
            collection.implies(
                new ApiRepositoryPermission(ApiRepositoryPermission.RepositoryAction.CREATE)
            ),
            new IsEqual<>(false)
        );
    }

    @Test
    void throwsErrorIfAnotherClassAdded() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new ApiAliasPermission.ApiAliasPermissionCollection()
                .add(new ApiRepositoryPermission(ApiRepositoryPermission.RepositoryAction.UPDATE))
        );
    }

}
