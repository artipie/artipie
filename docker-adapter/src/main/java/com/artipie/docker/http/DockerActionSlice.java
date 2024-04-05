/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.Docker;

public abstract class DockerActionSlice implements ScopeSlice {

    protected final Docker docker;

    public DockerActionSlice(Docker docker) {
        this.docker = docker;
    }
}
