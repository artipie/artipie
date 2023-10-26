/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
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
