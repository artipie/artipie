/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.etcd;

import com.artipie.asto.Meta;
import io.etcd.jetcd.KeyValue;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Metadata from Etcd key value.
 * @since 0.1
 */
final class EtcdMeta implements Meta {

    /**
     * Key value.
     */
    private final KeyValue kvs;

    /**
     * New metadata.
     * @param kvs Key value
     */
    EtcdMeta(final KeyValue kvs) {
        this.kvs = kvs;
    }

    @Override
    public <T> T read(final ReadOperator<T> opr) {
        final Map<String, String> raw = new HashMap<>();
        Meta.OP_SIZE.put(raw, Long.valueOf(this.kvs.getValue().size()));
        Meta.OP_CREATED_AT.put(raw, Instant.ofEpochMilli(this.kvs.getCreateRevision()));
        Meta.OP_UPDATED_AT.put(raw, Instant.ofEpochMilli(this.kvs.getModRevision()));
        return opr.take(raw);
    }
}
