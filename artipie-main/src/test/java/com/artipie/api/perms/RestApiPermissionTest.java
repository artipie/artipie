/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.api.perms;

import java.util.Set;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Test for {@link RestApiPermission}.
 * @since 0.30
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.CompareObjectsWithEquals"})
class RestApiPermissionTest {

    @ParameterizedTest
    @EnumSource(ApiRepositoryPermission.RepositoryAction.class)
    void repositoryPermissionWorksCorrect(final ApiRepositoryPermission.RepositoryAction action) {
        MatcherAssert.assertThat(
            "All implies any other action",
            new ApiRepositoryPermission(ApiRepositoryPermission.RepositoryAction.ALL).implies(
                new ApiRepositoryPermission(action)
            ),
            new IsEqual<>(true)
        );
        if (action != ApiRepositoryPermission.RepositoryAction.ALL) {
            MatcherAssert.assertThat(
                "Any other action does not imply all",
                new ApiRepositoryPermission(action).implies(
                    new ApiRepositoryPermission(ApiRepositoryPermission.RepositoryAction.ALL)
                ),
                new IsEqual<>(false)
            );
            for (final ApiRepositoryPermission.RepositoryAction item
                : ApiRepositoryPermission.RepositoryAction.values()) {
                if (item != action) {
                    MatcherAssert.assertThat(
                        "Action not implies other action",
                        new ApiRepositoryPermission(action)
                            .implies(new ApiRepositoryPermission(item)),
                        new IsEqual<>(false)
                    );
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(ApiAliasPermission.AliasAction.class)
    void aliasPermissionWorksCorrect(final ApiAliasPermission.AliasAction action) {
        MatcherAssert.assertThat(
            "All implies any other action",
            new ApiAliasPermission(ApiAliasPermission.AliasAction.ALL).implies(
                new ApiAliasPermission(action)
            ),
            new IsEqual<>(true)
        );
        if (action != ApiAliasPermission.AliasAction.ALL) {
            MatcherAssert.assertThat(
                "Any other action does not imply all",
                new ApiAliasPermission(action).implies(
                    new ApiAliasPermission(ApiAliasPermission.AliasAction.ALL)
                ),
                new IsEqual<>(false)
            );
            for (final ApiAliasPermission.AliasAction item
                : ApiAliasPermission.AliasAction.values()) {
                if (item != action) {
                    MatcherAssert.assertThat(
                        "Action not implies other action",
                        new ApiAliasPermission(action).implies(new ApiAliasPermission(item)),
                        new IsEqual<>(false)
                    );
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(ApiRolePermission.RoleAction.class)
    void rolePermissionWorksCorrect(final ApiRolePermission.RoleAction action) {
        MatcherAssert.assertThat(
            "All implies any other action",
            new ApiRolePermission(ApiRolePermission.RoleAction.ALL).implies(
                new ApiRolePermission(action)
            ),
            new IsEqual<>(true)
        );
        if (action != ApiRolePermission.RoleAction.ALL) {
            MatcherAssert.assertThat(
                "Any other action does not imply all",
                new ApiRolePermission(action).implies(
                    new ApiRolePermission(ApiRolePermission.RoleAction.ALL)
                ),
                new IsEqual<>(false)
            );
            for (final ApiRolePermission.RoleAction item : ApiRolePermission.RoleAction.values()) {
                if (item != action) {
                    MatcherAssert.assertThat(
                        "Action not implies other action",
                        new ApiRolePermission(action).implies(new ApiRolePermission(item)),
                        new IsEqual<>(false)
                    );
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(ApiUserPermission.UserAction.class)
    void userPermissionWorksCorrect(final ApiUserPermission.UserAction action) {
        MatcherAssert.assertThat(
            "All implies any other action",
            new ApiUserPermission(ApiUserPermission.UserAction.ALL).implies(
                new ApiUserPermission(action)
            ),
            new IsEqual<>(true)
        );
        if (action != ApiUserPermission.UserAction.ALL) {
            MatcherAssert.assertThat(
                "Any other action does not imply all",
                new ApiUserPermission(action).implies(
                    new ApiUserPermission(ApiUserPermission.UserAction.ALL)
                ),
                new IsEqual<>(false)
            );
            for (final ApiUserPermission.UserAction item : ApiUserPermission.UserAction.values()) {
                if (item != action) {
                    MatcherAssert.assertThat(
                        "Action not implies other action",
                        new ApiUserPermission(action).implies(new ApiUserPermission(item)),
                        new IsEqual<>(false)
                    );
                }
            }
        }
    }

    @Test
    void permissionsWithSeveralActionsWorksCorrect() {
        final ApiAliasPermission alias = new ApiAliasPermission(Set.of("read", "create"));
        MatcherAssert.assertThat(
            "Implies read",
            alias.implies(new ApiAliasPermission(ApiAliasPermission.AliasAction.READ)),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Not implies delete",
            alias.implies(new ApiAliasPermission(ApiAliasPermission.AliasAction.DELETE)),
            new IsEqual<>(false)
        );
        final ApiRepositoryPermission repo = new ApiRepositoryPermission(Set.of("read", "delete"));
        MatcherAssert.assertThat(
            "Not implies create",
            repo.implies(
                new ApiRepositoryPermission(ApiRepositoryPermission.RepositoryAction.CREATE)
            ),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Implies delete",
            repo.implies(
                new ApiRepositoryPermission(ApiRepositoryPermission.RepositoryAction.DELETE)
            ),
            new IsEqual<>(true)
        );
        final ApiUserPermission user = new ApiUserPermission(Set.of("*"));
        MatcherAssert.assertThat(
            "Implies create",
            user.implies(
                new ApiUserPermission(ApiUserPermission.UserAction.CREATE)
            ),
            new IsEqual<>(true)
        );
    }

    @Test
    void notImpliesOtherClassPermission() {
        MatcherAssert.assertThat(
            new ApiAliasPermission(ApiAliasPermission.AliasAction.READ)
                .implies(new ApiUserPermission(ApiUserPermission.UserAction.READ)),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            new ApiUserPermission(ApiUserPermission.UserAction.CHANGE_PASSWORD)
                .implies(new ApiRolePermission(ApiRolePermission.RoleAction.UPDATE)),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            new ApiRepositoryPermission(ApiRepositoryPermission.RepositoryAction.ALL)
                .implies(new ApiUserPermission(ApiUserPermission.UserAction.ALL)),
            new IsEqual<>(false)
        );
    }

}
