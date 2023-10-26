/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.perms;

import com.artipie.security.perms.ArtipiePermissionFactory;
import com.artipie.security.perms.PermissionConfig;
import com.artipie.security.perms.PermissionFactory;

/**
 * Docker permissions factory. Docker permission format in yaml:
 * <pre>{@code
 * docker_permissions:
 *   artipie-docker-repo-name:
 *     my-alpine: # resource (image) name
 *       - pull
 *     ubuntu-slim:
 *       - pull
 *       - push
 * }</pre>
 * @since 0.18
 */
@ArtipiePermissionFactory("docker_repository_permissions")
public final class DockerRepositoryPermissionFactory implements
    PermissionFactory<DockerRepositoryPermission.DockerRepositoryPermissionCollection> {

    @Override
    public DockerRepositoryPermission.DockerRepositoryPermissionCollection newPermissions(
        final PermissionConfig config
    ) {
        final DockerRepositoryPermission.DockerRepositoryPermissionCollection res =
            new DockerRepositoryPermission.DockerRepositoryPermissionCollection();
        for (final String repo : config.keys()) {
            final PermissionConfig subconfig = (PermissionConfig) config.config(repo);
            for (final String resource : subconfig.keys()) {
                res.add(
                    new DockerRepositoryPermission(repo, resource, subconfig.sequence(resource))
                );
            }
        }
        return res;
    }
}
