/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.AppConfig;
import com.artipie.api.docs.Repository;
import com.artipie.settings.Repositories;
import com.artipie.settings.RepositoriesFromStorage;
import com.artipie.settings.repo.RepoConfig;
import javax.json.JsonStructure;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Repository management endpoint.
 * @since 0.26
 */
@RestController
@RequestMapping("api/repository")
public final class RepositoryController implements Repository {

    /**
     * Repositories config.
     */
    private final Repositories repos;

    /**
     * Artipie layout.
     */
    private final String layout;

    /**
     * Ctor.
     * @param config Application config
     */
    public RepositoryController(final AppConfig config) {
        this.repos = new RepositoriesFromStorage(
            config.httpClient(), config.setting().repoConfigsStorage()
        );
        this.layout = config.setting().layout().toString();
    }

    @GetMapping("/{uname}/{rname}")
    @Override
    public ResponseEntity<String> get(
        @PathVariable(required = false) final String uname,
        @PathVariable final String rname
    ) {
        final JsonStructure json = new Yaml2Json().apply(
            this.repos.config(this.repoName(rname, uname)).thenApply(
                RepoConfig::toString
            ).toCompletableFuture().join()
        );
        json.asJsonObject().remove("permissions");
        final HttpHeaders hdrs = new HttpHeaders();
        hdrs.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<String>(json.toString(), hdrs, HttpStatus.OK);
    }

    /**
     * Get repository name depending on layout.
     * @param rname Repository name
     * @param uname User name
     * @return Repository name depending on layout
     */
    private String repoName(final String rname, final String uname) {
        String res = rname;
        if (this.layout.equals("org")) {
            res = String.format("%s/%s", uname, rname);
        }
        return res;
    }

}
