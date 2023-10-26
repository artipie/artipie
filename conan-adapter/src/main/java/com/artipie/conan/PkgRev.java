/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021-2023 Artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package  com.artipie.conan;

import java.time.Instant;
import javax.json.Json;
import javax.json.JsonObject;

/**
 * Conan package revision data.
 * @since 0.1
 */
public class PkgRev {

    /**
     * Revision json field.
     */
    private static final String REVISION = "revision";

    /**
     * Timestamp json field. Uses ISO 8601 format.
     */
    private static final String TIMESTAMP = "time";

    /**
     * Revision number of the package.
     */
    private final int revision;

    /**
     * Timestamp for revision info.
     */
    private final Instant instant;

    /**
     * Initializes new instance.
     * @param revision Revision number of the package.
     */
    PkgRev(final int revision) {
        this.revision = revision;
        this.instant = Instant.now();
    }

    /**
     * Creates new revision object for json index file. Time is in ISO 8601 format.
     * @return JsonObject with revision info.
     */
    public JsonObject toJson() {
        return Json.createObjectBuilder()
            .add(PkgRev.REVISION, Integer.toString(this.revision))
            .add(PkgRev.TIMESTAMP, this.instant.toString()).build();
    }
}
