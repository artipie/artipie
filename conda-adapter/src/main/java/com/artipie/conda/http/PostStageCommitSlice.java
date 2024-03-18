/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda.http;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.common.RsJson;
import org.reactivestreams.Publisher;

import javax.json.Json;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Slice to handle `POST /stage/{owner_login}/{package_name}/{version}/{basename}` and
 * `POST /commit/{owner_login}/{package_name}/{version}/{basename}` requests.
 * @since 0.4
 * @todo #32:30min Implement this slice properly, it should handle post requests to create stage
 *  and commit package. For now link for full documentation is not found, check swagger
 *  https://api.anaconda.org/docs#/ and github issue for any updates.
 *  https://github.com/Anaconda-Platform/anaconda-client/issues/580
 */
public final class PostStageCommitSlice implements Slice {

    /**
     * Regex to obtain uploaded package architecture and name from request line.
     */
    private static final Pattern PKG = Pattern.compile(".*/(.*/.*(\\.tar\\.bz2|\\.conda))$");

    /**
     * Url to upload.
     */
    private final String url;

    /**
     * Ctor.
     * @param url Url to upload
     */
    public PostStageCommitSlice(final String url) {
        this.url = url;
    }

    @Override
    public Response response(
        final RequestLine line,
        final Headers headers,
        final Publisher<ByteBuffer> body) {
        final Response res;
        final Matcher matcher = PostStageCommitSlice.PKG.matcher(
            line.uri().getPath()
        );
        if (matcher.matches()) {
            final String name = matcher.group(1);
            res = new RsJson(
                () -> Json.createReader(
                    new StringReader(
                        String.join(
                            "\n",
                            "{",
                            String.format("  \"basename\": \"%s\", ", name),
                            "  \"dist_id\": \"610d3949955e84a9b0dada33\", ",
                            "  \"form_data\": {",
                            "    \"Content-Type\": \"application/octet-stream\", ",
                            "    \"acl\": \"private\", ",
                            "    \"key\": \"610d055a4e06fc7145474a3a/610d3949955e84a9b0dada33\", ",
                            "    \"policy\": \"eyJjb25kaXRpb25zIjogW3siYWNsIjogInByaXZhdGUifSwgeyJzdWNjZXNzX2FjdGlvbl9zdGF0dXMiOiAiMjAxIn0sIFsic3RhcnRzLXdpdGgiLCAiJENvbnRlbnQtVHlwZSIsICIiXSwgWyJzdGFydHMtd2l0aCIsICIkQ29udGVudC1NRDUiLCAiIl0sIFsic3RhcnRzLXdpdGgiLCAiJENvbnRlbnQtTGVuZ3RoIiwgIiJdLCB7IngtYW16LXN0b3JhZ2UtY2xhc3MiOiAiU1RBTkRBUkQifSwgeyJidWNrZXQiOiAiYmluc3Rhci1jaW8tcGFja2FnZXMtcHJvZCJ9LCB7ImtleSI6ICI2MTBkMDU1YTRlMDZmYzcxNDU0NzRhM2EvNjEwZDM5NDk5NTVlODRhOWIwZGFkYTMzIn0sIHsieC1hbXotYWxnb3JpdGhtIjogIkFXUzQtSE1BQy1TSEEyNTYifSwgeyJ4LWFtei1jcmVkZW50aWFsIjogIkFTSUFXVUk0NkRaRkpVT1dKM0pXLzIwMjEwODA2L3VzLWVhc3QtMS9zMy9hd3M0X3JlcXVlc3QifSwgeyJ4LWFtei1kYXRlIjogIjIwMjEwODA2VDEzMjk0NVoifSwgeyJ4LWFtei1zZWN1cml0eS10b2tlbiI6ICJJUW9KYjNKcFoybHVYMlZqRUVFYUNYVnpMV1ZoYzNRdE1TSkdNRVFDSUh2eDRsVzNBTG5oT1BYK0RUU0xINUNzMDIwZng0bFBOQkZpMGFuN3VZWEtBaUIvNHVBR1g1WG5tQU9pT1BoQzhUKy8yVjFGUW5kaHJRSVZQdmZVUlZ3OHl5cjZBd2haRUFBYUREUTFOVGcyTkRBNU9ETTNPQ0lNMDR5MEFDZGxsbTg1TjRhNUt0Y0Q0aFRTSVJKNkhQNXY0UnF1dTFaR1JLSWR4R043RmxaT3JYTW9DVFZmOFc5UkVtYnd6UkJwWmhBQXJnTlZZRjZJVHl4c0pGdGRDT0RRc0ZJeHVDVU4wd0hPWmRHVU9BQnUvZjQzdDR4cXErNWZvckxJNUMvSDRJaHFBdGVvOStFK2Vva0FQSDBpazdaOGo4L1pyQjZhMXY4NG0rZW9qc1pLRzMvRVpNc2NKVVdBOHFuSXJhSGJNV0ZTNG84OE1nVWdhT0R3cytMZjFEQXlyVkhpYm1CTFBlYkpkTzdEUDNQdTRqVFhZZnRXMDZXLzFjV21iSkQvZU5XWEI0ZFdSR0lFcnFkSjBTSWcxSXBpQkNnWWtFV0lBRllvVm4rMnNxTFdhc2ZqTlArVjNRV0Zrb2twUjVESndPWjZDL0ZIWEVPK1paKzdaVkVZbHZuMWtBUk1lTWNIaENqNGVjMS93SllZRXNtVFQydGNlKzBzYThKaVlzR29rSWI1K200SlNQU2VQM2l6QUY1V05zVGVvUmZsdW9LWjZ4NzBlL01PTzZMakFkTTRGSU9WSnAwNWhmV1dBSXVvOVVCRTRXbzdTdWw0bFIwZjJsdlhsWEFobjRuL21TZjVlRTdQMWZ2TUFMSkZ1Y1RQY2VQdjNiWUhHajh3QjhOK2dmVURvYjI3bktxYTRrdnpuNHhpTFEyVUg2VDJYRDlqWUZsYTdheThIbVYrR0E0YU1rdkxHQ2RTTEZiOFAvR2Z5OXVtN01JOWRhbEZUNHpqMWplNUdRWkZoTUpFZzRyVEVqZ0tsWTBQd2F5bWpoWk5wUWdwTGVSeWovU0dsNHl1TU5QYXM0Z0dPcVlCOWJkdGJzYk1EWVJNa01HVUhDV2drQnd5dkVjclJ3MFhpSnFwT0VITGNEbkhvWklGQ0YrMXRhcHArZ1lBaWIrajROajloamtQYXkxcFFsazhKZGJXVjUycEFwMDJ6Um5EakFqN0VSRGJwZ3hCNlI4TWlXTnlrYmZsM0ZkcmFWLzBCcHMzQjBwbnFscmxKNVRQVU5zenRQUkZtd1JsY2h1TnJzYlpZV1ZlbTRpK3prUUJXNER3ZlpEdllSRms2aE16WU9KUUhncUx5V1lla3BqS2U3QzBkTTQ0VWhNZ2hBPT0ifV0sICJleHBpcmF0aW9uIjogIjIwMjEtMDgtMDZUMTQ6Mjk6NDVaIn0=\", ",
                            "    \"success_action_status\": \"201\", ",
                            "    \"x-amz-algorithm\": \"AWS4-HMAC-SHA256\", ",
                            "    \"x-amz-credential\": \"ASIAWUI46DZFJUOWJ3JW/20210806/us-east-1/s3/aws4_request\", ",
                            "    \"x-amz-date\": \"20210806T132945Z\", ",
                            "    \"x-amz-security-token\": \"IQoJb3JpZ2luX2VjEEEaCXVzLWVhc3QtMSJGMEQCIHvx4lW3ALnhOPX+DTSLH5Cs020fx4lPNBFi0an7uYXKAiB/4uAGX5XnmAOiOPhC8T+/2V1FQndhrQIVPvfURVw8yyr6AwhZEAAaDDQ1NTg2NDA5ODM3OCIM04y0ACdllm85N4a5KtcD4hTSIRJ6HP5v4Rquu1ZGRKIdxGN7FlZOrXMoCTVf8W9REmbwzRBpZhAArgNVYF6ITyxsJFtdCODQsFIxuCUN0wHOZdGUOABu/f43t4xqq+5forLI5C/H4IhqAteo9+E+eokAPH0ik7Z8j8/ZrB6a1v84m+eojsZKG3/EZMscJUWA8qnIraHbMWFS4o88MgUgaODws+Lf1DAyrVHibmBLPebJdO7DP3Pu4jTXYftW06W/1cWmbJD/eNWXB4dWRGIErqdJ0SIg1IpiBCgYkEWIAFYoVn+2sqLWasfjNP+V3QWFkokpR5DJwOZ6C/FHXEO+ZZ+7ZVEYlvn1kARMeMcHhCj4ec1/wJYYEsmTT2tce+0sa8JiYsGokIb5+m4JSPSeP3izAF5WNsTeoRfluoKZ6x70e/MOO6LjAdM4FIOVJp05hfWWAIuo9UBE4Wo7Sul4lR0f2lvXlXAhn4n/mSf5eE7P1fvMALJFucTPcePv3bYHGj8wB8N+gfUDob27nKqa4kvzn4xiLQ2UH6T2XD9jYFla7ay8HmV+GA4aMkvLGCdSLFb8P/Gfy9um7MI9dalFT4zj1je5GQZFhMJEg4rTEjgKlY0PwaymjhZNpQgpLeRyj/SGl4yuMNPas4gGOqYB9bdtbsbMDYRMkMGUHCWgkBwyvEcrRw0XiJqpOEHLcDnHoZIFCF+1tapp+gYAib+j4Nj9hjkPay1pQlk8JdbWV52pAp02zRnDjAj7ERDbpgxB6R8MiWNykbfl3FdraV/0Bps3B0pnqlrlJ5TPUNsztPRFmwRlchuNrsbZYWVem4i+zkQBW4DwfZDvYRFk6hMzYOJQHgqLyWYekpjKe7C0dM44UhMghA==\", ",
                            "    \"x-amz-signature\": \"2b07418c21d98eb5febcc11116bf56106a4447aa8c935aa52d7ae11b5364cf3f\", ",
                            "    \"x-amz-storage-class\": \"STANDARD\"",
                            "  }, ",
                            "  \"package_id\": \"610d055a4e06fc7145474a3a\", ",
                            String.format("  \"post_url\": \"%s/%s\", ", this.url, name),
                            String.format("  \"s3_url\": \"%s/%s\", ", this.url, name),
                            "  \"s3form_data\": {",
                            "    \"Content-Type\": \"application/octet-stream\", ",
                            "    \"acl\": \"private\", ",
                            "    \"key\": \"610d055a4e06fc7145474a3a/610d3949955e84a9b0dada33\", ",
                            "    \"policy\": \"eyJjb25kaXRpb25zIjogW3siYWNsIjogInByaXZhdGUifSwgeyJzdWNjZXNzX2FjdGlvbl9zdGF0dXMiOiAiMjAxIn0sIFsic3RhcnRzLXdpdGgiLCAiJENvbnRlbnQtVHlwZSIsICIiXSwgWyJzdGFydHMtd2l0aCIsICIkQ29udGVudC1NRDUiLCAiIl0sIFsic3RhcnRzLXdpdGgiLCAiJENvbnRlbnQtTGVuZ3RoIiwgIiJdLCB7IngtYW16LXN0b3JhZ2UtY2xhc3MiOiAiU1RBTkRBUkQifSwgeyJidWNrZXQiOiAiYmluc3Rhci1jaW8tcGFja2FnZXMtcHJvZCJ9LCB7ImtleSI6ICI2MTBkMDU1YTRlMDZmYzcxNDU0NzRhM2EvNjEwZDM5NDk5NTVlODRhOWIwZGFkYTMzIn0sIHsieC1hbXotYWxnb3JpdGhtIjogIkFXUzQtSE1BQy1TSEEyNTYifSwgeyJ4LWFtei1jcmVkZW50aWFsIjogIkFTSUFXVUk0NkRaRkpVT1dKM0pXLzIwMjEwODA2L3VzLWVhc3QtMS9zMy9hd3M0X3JlcXVlc3QifSwgeyJ4LWFtei1kYXRlIjogIjIwMjEwODA2VDEzMjk0NVoifSwgeyJ4LWFtei1zZWN1cml0eS10b2tlbiI6ICJJUW9KYjNKcFoybHVYMlZqRUVFYUNYVnpMV1ZoYzNRdE1TSkdNRVFDSUh2eDRsVzNBTG5oT1BYK0RUU0xINUNzMDIwZng0bFBOQkZpMGFuN3VZWEtBaUIvNHVBR1g1WG5tQU9pT1BoQzhUKy8yVjFGUW5kaHJRSVZQdmZVUlZ3OHl5cjZBd2haRUFBYUREUTFOVGcyTkRBNU9ETTNPQ0lNMDR5MEFDZGxsbTg1TjRhNUt0Y0Q0aFRTSVJKNkhQNXY0UnF1dTFaR1JLSWR4R043RmxaT3JYTW9DVFZmOFc5UkVtYnd6UkJwWmhBQXJnTlZZRjZJVHl4c0pGdGRDT0RRc0ZJeHVDVU4wd0hPWmRHVU9BQnUvZjQzdDR4cXErNWZvckxJNUMvSDRJaHFBdGVvOStFK2Vva0FQSDBpazdaOGo4L1pyQjZhMXY4NG0rZW9qc1pLRzMvRVpNc2NKVVdBOHFuSXJhSGJNV0ZTNG84OE1nVWdhT0R3cytMZjFEQXlyVkhpYm1CTFBlYkpkTzdEUDNQdTRqVFhZZnRXMDZXLzFjV21iSkQvZU5XWEI0ZFdSR0lFcnFkSjBTSWcxSXBpQkNnWWtFV0lBRllvVm4rMnNxTFdhc2ZqTlArVjNRV0Zrb2twUjVESndPWjZDL0ZIWEVPK1paKzdaVkVZbHZuMWtBUk1lTWNIaENqNGVjMS93SllZRXNtVFQydGNlKzBzYThKaVlzR29rSWI1K200SlNQU2VQM2l6QUY1V05zVGVvUmZsdW9LWjZ4NzBlL01PTzZMakFkTTRGSU9WSnAwNWhmV1dBSXVvOVVCRTRXbzdTdWw0bFIwZjJsdlhsWEFobjRuL21TZjVlRTdQMWZ2TUFMSkZ1Y1RQY2VQdjNiWUhHajh3QjhOK2dmVURvYjI3bktxYTRrdnpuNHhpTFEyVUg2VDJYRDlqWUZsYTdheThIbVYrR0E0YU1rdkxHQ2RTTEZiOFAvR2Z5OXVtN01JOWRhbEZUNHpqMWplNUdRWkZoTUpFZzRyVEVqZ0tsWTBQd2F5bWpoWk5wUWdwTGVSeWovU0dsNHl1TU5QYXM0Z0dPcVlCOWJkdGJzYk1EWVJNa01HVUhDV2drQnd5dkVjclJ3MFhpSnFwT0VITGNEbkhvWklGQ0YrMXRhcHArZ1lBaWIrajROajloamtQYXkxcFFsazhKZGJXVjUycEFwMDJ6Um5EakFqN0VSRGJwZ3hCNlI4TWlXTnlrYmZsM0ZkcmFWLzBCcHMzQjBwbnFscmxKNVRQVU5zenRQUkZtd1JsY2h1TnJzYlpZV1ZlbTRpK3prUUJXNER3ZlpEdllSRms2aE16WU9KUUhncUx5V1lla3BqS2U3QzBkTTQ0VWhNZ2hBPT0ifV0sICJleHBpcmF0aW9uIjogIjIwMjEtMDgtMDZUMTQ6Mjk6NDVaIn0=\", ",
                            "    \"success_action_status\": \"201\", ",
                            "    \"x-amz-algorithm\": \"AWS4-HMAC-SHA256\", ",
                            "    \"x-amz-credential\": \"ASIAWUI46DZFJUOWJ3JW/20210806/us-east-1/s3/aws4_request\", ",
                            "    \"x-amz-date\": \"20210806T132945Z\", ",
                            "    \"x-amz-security-token\": \"IQoJb3JpZ2luX2VjEEEaCXVzLWVhc3QtMSJGMEQCIHvx4lW3ALnhOPX+DTSLH5Cs020fx4lPNBFi0an7uYXKAiB/4uAGX5XnmAOiOPhC8T+/2V1FQndhrQIVPvfURVw8yyr6AwhZEAAaDDQ1NTg2NDA5ODM3OCIM04y0ACdllm85N4a5KtcD4hTSIRJ6HP5v4Rquu1ZGRKIdxGN7FlZOrXMoCTVf8W9REmbwzRBpZhAArgNVYF6ITyxsJFtdCODQsFIxuCUN0wHOZdGUOABu/f43t4xqq+5forLI5C/H4IhqAteo9+E+eokAPH0ik7Z8j8/ZrB6a1v84m+eojsZKG3/EZMscJUWA8qnIraHbMWFS4o88MgUgaODws+Lf1DAyrVHibmBLPebJdO7DP3Pu4jTXYftW06W/1cWmbJD/eNWXB4dWRGIErqdJ0SIg1IpiBCgYkEWIAFYoVn+2sqLWasfjNP+V3QWFkokpR5DJwOZ6C/FHXEO+ZZ+7ZVEYlvn1kARMeMcHhCj4ec1/wJYYEsmTT2tce+0sa8JiYsGokIb5+m4JSPSeP3izAF5WNsTeoRfluoKZ6x70e/MOO6LjAdM4FIOVJp05hfWWAIuo9UBE4Wo7Sul4lR0f2lvXlXAhn4n/mSf5eE7P1fvMALJFucTPcePv3bYHGj8wB8N+gfUDob27nKqa4kvzn4xiLQ2UH6T2XD9jYFla7ay8HmV+GA4aMkvLGCdSLFb8P/Gfy9um7MI9dalFT4zj1je5GQZFhMJEg4rTEjgKlY0PwaymjhZNpQgpLeRyj/SGl4yuMNPas4gGOqYB9bdtbsbMDYRMkMGUHCWgkBwyvEcrRw0XiJqpOEHLcDnHoZIFCF+1tapp+gYAib+j4Nj9hjkPay1pQlk8JdbWV52pAp02zRnDjAj7ERDbpgxB6R8MiWNykbfl3FdraV/0Bps3B0pnqlrlJ5TPUNsztPRFmwRlchuNrsbZYWVem4i+zkQBW4DwfZDvYRFk6hMzYOJQHgqLyWYekpjKe7C0dM44UhMghA==\", ",
                            "    \"x-amz-signature\": \"2b07418c21d98eb5febcc11116bf56106a4447aa8c935aa52d7ae11b5364cf3f\", ",
                            "    \"x-amz-storage-class\": \"STANDARD\"",
                            "  }",
                            "}"
                        )
                    )
                ).read(), StandardCharsets.UTF_8
            );
        } else {
            res = new RsWithStatus(RsStatus.BAD_REQUEST);
        }
        return res;
    }
}
