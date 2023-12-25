/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.s3;

import com.artipie.asto.Meta;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

/**
 * Metadata from S3 object.
 * @since 0.1
 */
final class S3HeadMeta implements Meta {

    /**
     * S3 head object response.
     */
    private final HeadObjectResponse rsp;

    /**
     * New metadata.
     * @param rsp Head response
     */
    S3HeadMeta(final HeadObjectResponse rsp) {
        this.rsp = rsp;
    }

    @Override
    public <T> T read(final ReadOperator<T> opr) {
        final Map<String, String> raw = new HashMap<>();
        Meta.OP_SIZE.put(raw, this.rsp.contentLength());
        // @checkstyle MethodBodyCommentsCheck (1 line)
        // ETag is a quoted MD5 of blob content according to S3 docs
        Meta.OP_MD5.put(raw, this.rsp.eTag().replaceAll("\"", ""));
        return opr.take(raw);
    }
}
