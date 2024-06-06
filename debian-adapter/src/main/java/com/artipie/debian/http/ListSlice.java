/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian.http;

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Slice;
import com.artipie.http.headers.ContentFileName;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.slice.ListingFormat;
import com.artipie.http.slice.SliceListing;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.concurrent.CompletableFuture;

public final class ListSlice implements Slice {
    private final Storage asto;

    public ListSlice(final Storage asto) {
        this.asto = asto;
    }

    @Override
    public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
        return new SliceListing(this.asto, "application/json", ListingFormat.Standard.JSON)
            .response(new RequestLine(line.method().value(), "", line.version()), headers, body)
            .thenApply(response -> {
                JSONArray array = null;
                try {
                    array = new JSONArray(new String(response.body().asBytes()));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                JSONArray filtered = new JSONArray();
                for (int i = 0; i < array.length(); i++) {
                    String element = null;
                    try {
                        element = array.getString(i);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    if (element.endsWith(".deb")) {
                        filtered.put(element);
                    }
                }
                return ResponseBuilder.ok()
                    .header(new ContentFileName(line.uri()))
                    .jsonBody(filtered.toString())
                    .build();
            });
    }
}