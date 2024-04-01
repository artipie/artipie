/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;

import javax.json.Json;
import java.io.StringReader;
import java.util.concurrent.CompletableFuture;

/**
 * Slice to handle `POST /release/{owner_login}/{package_name}/{version}` and
 * `POST /package/{owner_login}/{package_name}`.
 * @todo #32:30min Implement this slice properly, it should handle post requests to create package
 *  and release. For now link for full documentation is not found, check swagger
 *  https://api.anaconda.org/docs#/ and github issue for any updates.
 *  https://github.com/Anaconda-Platform/anaconda-client/issues/580
 */
public final class PostPackageReleaseSlice implements Slice {

    @Override
    public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
        return ResponseBuilder.ok()
            .jsonBody(Json.createReader(
                new StringReader(
                    String.join(
                        "\n",
                        "{",
                        "  \"app_entry\": {}, ",
                        "  \"app_summary\": {}, ",
                        "  \"app_type\": {}, ",
                        "  \"builds\": [], ",
                        "  \"conda_platforms\": [], ",
                        "  \"description\": \"\", ",
                        "  \"dev_url\": null, ",
                        "  \"doc_url\": null, ",
                        "  \"full_name\": \"any/example-project\", ",
                        "  \"home\": \"None\", ",
                        "  \"html_url\": \"http://host.testcontainers.internal/any/example-project\", ",
                        "  \"id\": \"610d24984e06fc71454caec7\", ",
                        "  \"latest_version\": \"\", ",
                        "  \"license\": null, ",
                        "  \"license_url\": null, ",
                        "  \"name\": \"example-project\", ",
                        "  \"owner\": \"any\", ",
                        "  \"package_types\": [",
                        "    \"conda\"",
                        "  ], ",
                        "  \"public\": true, ",
                        "  \"revision\": 0, ",
                        "  \"source_git_tag\": null, ",
                        "  \"source_git_url\": null, ",
                        "  \"summary\": \"An example xyz package\", ",
                        "  \"url\": \"http://host.testcontainers.internal/packages/any/example-project\", ",
                        "  \"versions\": []",
                        "}"
                    )
                )
            ).read()
        ).completedFuture();
    }
}
