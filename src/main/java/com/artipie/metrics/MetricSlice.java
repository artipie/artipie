/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentAs;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.common.RsJson;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Observable;
import java.nio.ByteBuffer;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import org.reactivestreams.Publisher;

/**
 * Slice with metrics JSON.
 * @since 0.10
 */
public final class MetricSlice implements Slice {

    /**
     * Storage with metrics.
     */
    private final Storage storage;

    /**
     * New slice with metrics.
     * @param storage Storage with metrics
     */
    public MetricSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final RxStorageWrapper rxsto = new RxStorageWrapper(this.storage);
        return new AsyncResponse(
            rxsto.list(Key.ROOT)
                .flatMapObservable(Observable::fromIterable)
                .flatMapSingle(
                    key -> rxsto.value(key).to(ContentAs.LONG).map(
                        val -> Json.createObjectBuilder()
                            .add("key", key.string())
                            .add("value", val)
                    )
                ).reduce(Json.createArrayBuilder(), JsonArrayBuilder::add)
                .map(json -> new RsJson(json.build()))
                .to(SingleInterop.get())
        );
    }
}
