/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api.docs;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

/**
 * Repository managements API.
 * @since 0.26
 */
@OpenAPIDefinition(
    info = @Info(
        title = "Repositories Rest API",
        description = "This controller is meant for repositories management, it provides methods to get repository info, delete, update and create repositories"
    )
)
public interface Repository {

    /**
     * Returns repository called {name} settings as json object.
     * @param uname Username
     * @param rname The name of the repository.
     * @return Http response
     */
    @Operation(
        summary = "Get repository settings by name",
        description = """
           Provides repository configuration in json format, repository permissions are not
           included
        """,
        responses = {
            @ApiResponse(
                description = "Returns repository setting json",
                responseCode = "200",
                content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(responseCode = "404", description = "Repository not found")
        }
    )
    ResponseEntity<String> get(
        @Parameter(description = "User name", required = false) final String uname,
        @Parameter(description = "Repository name", required = true) final String rname
    );
}
