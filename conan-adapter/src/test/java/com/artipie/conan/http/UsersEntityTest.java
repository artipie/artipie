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
package  com.artipie.conan.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.auth.Authentication;
import com.artipie.http.headers.Authorization;
import com.artipie.http.hm.IsJson;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import javax.json.Json;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.Test;

/**
 * Test for {@link UsersEntity}.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (999 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class UsersEntityTest {

    @Test
    public void userAuthTest() {
        final String login = ConanSliceITCase.SRV_USERNAME;
        final String password = ConanSliceITCase.SRV_PASSWORD;
        MatcherAssert.assertThat(
            "Slice response must match",
            new UsersEntity.UserAuth(
                new Authentication.Single(
                    ConanSliceITCase.SRV_USERNAME, ConanSliceITCase.SRV_PASSWORD
                ),
                new ConanSlice.FakeAuthTokens(ConanSliceITCase.TOKEN, ConanSliceITCase.SRV_USERNAME)
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(String.format("%s", ConanSliceITCase.TOKEN).getBytes())
                ),
                new RequestLine(RqMethod.GET, "/v1/users/authenticate"),
                new Headers.From(new Authorization.Basic(login, password)),
                Content.EMPTY
            )
        );
    }

    @Test
    public void credsCheckTest() {
        MatcherAssert.assertThat(
            "Response must match",
            new UsersEntity.CredsCheck().response(
                new RequestLine(RqMethod.GET, "/v1/users/check_credentials").toString(),
                new Headers.From("Host", "localhost"), Content.EMPTY
            ), Matchers.allOf(
                new RsHasBody(
                    new IsJson(new IsEqual<>(Json.createObjectBuilder().build()))
                ),
                new RsHasStatus(RsStatus.OK)
            )
        );
    }
}
