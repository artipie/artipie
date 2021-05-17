/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
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
package com.artipie.metrics.prometheus;

import com.artipie.metrics.Metrics;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import javax.xml.bind.DatatypeConverter;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

/**
 * {@link PushGateway} implementation storing data in memory.
 *
 * @since 0.9
 */
public class PushGateway {
    private static final int MILLISECONDS_PER_SECOND = 1000;
    private static final String urf8 = "UTF-8";
    private static final String responseCodeFrom = "Response code from ";
    private static final String postReq = "POST";
    private static final String putReq = "PUT";
    private static final String deleteReq = "DELETE";
    private static final String was = " was ";

    // Visible for testing.
    protected final String gatewayBaseURL;

    private HttpConnectionFactory connectionFactory = new DefaultHttpConnectionFactory();

    /**
     * Construct a Pushgateway, with the given address.
     * <p>
     * @param address  host:port or ip:port of the Pushgateway.
     */
    public PushGateway(final String address) {
        this(createURLSneakily("http://" + address));
    }

    /**
     * Construct a Pushgateway, with the given URL.
     * <p>
     * @param serverBaseURL the base URL and optional context path of the Pushgateway server.
     */
    public PushGateway(final URL serverBaseURL) {
        this.gatewayBaseURL = URI.create(serverBaseURL.toString() + "/metrics/")
            .normalize()
            .toString();
    }

    public void setConnectionFactory(final HttpConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Creates a URL instance from a String representation of a URL without throwing a checked exception.
     * Required because you can't wrap a call to another constructor in a try statement.
     *
     * @param urlString the String representation of the URL.
     * @return The URL instance.
     */
    private static URL createURLSneakily(final String urlString) {
        try {
            return new URL(urlString);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Pushes all metrics in a registry, replacing all those with the same job and no grouping key.
     * <p>
     * This uses the PUT HTTP method.
     */
    public void push(final CollectorRegistry registry, final String job) throws IOException {
        doRequest(registry, job, null, PushGateway.putReq);
    }

    /**
     * Pushes all metrics in a Collector, replacing all those with the same job and no grouping key.
     * <p>
     * This is useful for pushing a single Gauge.
     * <p>
     * This uses the PUT HTTP method.
     */
    public void push(final Collector collector, final String job) throws IOException {
        final CollectorRegistry registry = new CollectorRegistry();
        collector.register(registry);
        push(registry, job);
    }

    /**
     * Pushes all metrics in a registry, replacing all those with the same job and grouping key.
     * <p>
     * This uses the PUT HTTP method.
     */
    public void push(final CollectorRegistry registry, final String job,
        final Map<String, String> groupingKey) throws IOException {
        doRequest(registry, job, groupingKey, PushGateway.putReq);
    }

    /**
     * Pushes all metrics in a Collector, replacing all those with the same job and grouping key.
     * <p>
     * This is useful for pushing a single Gauge.
     * <p>
     * This uses the PUT HTTP method.
     */
    public void push(final Collector collector, final String job,
        final Map<String, String> groupingKey) throws IOException {
        final CollectorRegistry registry = new CollectorRegistry();
        collector.register(registry);
        push(registry, job, groupingKey);
    }

    /**
     * Pushes all metrics in a registry, replacing only previously pushed metrics of the same name and job and no grouping key.
     * <p>
     * This uses the POST HTTP method.
     */
    public void pushAdd(final CollectorRegistry registry, final String job) throws IOException {
        doRequest(registry, job, null, PushGateway.postReq);
    }

    /**
     * Pushes all metrics in a Collector, replacing only previously pushed metrics of the same name and job and no grouping key.
     * <p>
     * This is useful for pushing a single Gauge.
     * <p>
     * This uses the POST HTTP method.
     */
    public void pushAdd(final Collector collector, final String job) throws IOException {
        final CollectorRegistry registry = new CollectorRegistry();
        collector.register(registry);
        pushAdd(registry, job);
    }

    /**
     * Pushes all metrics in a registry, replacing only previously pushed metrics of the same name, job and grouping key.
     * <p>
     * This uses the POST HTTP method.
     */
    public void pushAdd(final CollectorRegistry registry, final String job,
        final Map<String, String> groupingKey) throws IOException {
        doRequest(registry, job, groupingKey, PushGateway.postReq);
    }

    /**
     * Pushes all metrics in a Collector, replacing only previously pushed metrics of the same name, job and grouping key.
     * <p>
     * This is useful for pushing a single Gauge.
     * <p>
     * This uses the POST HTTP method.
     */
    public void pushAdd(final Collector collector, final String job,
        final Map<String, String> groupingKey) throws IOException {
        final CollectorRegistry registry = new CollectorRegistry();
        collector.register(registry);
        pushAdd(registry, job, groupingKey);
    }


    /**
     * Deletes metrics from the Pushgateway.
     * <p>
     * Deletes metrics with no grouping key and the provided job.
     * This uses the DELETE HTTP method.
     */
    public void delete(final String job) throws IOException {
        doRequest(null, job, null, PushGateway.deleteReq);
    }

    /**
     * Deletes metrics from the Pushgateway.
     * <p>
     * Deletes metrics with the provided job and grouping key.
     * This uses the DELETE HTTP method.
     */
    public void delete(final String job, final Map<String, String> groupingKey)
        throws IOException {
        doRequest(null, job, groupingKey, PushGateway.deleteReq);
    }

    String getUrl(final String job) throws UnsupportedEncodingException {
        String url = gatewayBaseURL;
        if (job.contains("/")) {
            url = url.concat("job@base64/").concat(base64url(job));
        } else {
            url = url.concat("job/").concat(URLEncoder.encode(job, PushGateway.urf8));
        }
        return url;
    }

    String enhanceUrl(String url, final Map<String, String> groupingKey)
        throws UnsupportedEncodingException {
        String newUrl = url;
        if (groupingKey != null) {
            for (final Map.Entry<String, String> entry: groupingKey.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    newUrl = newUrl.concat("/").concat(entry.getKey()).concat("@base64/=");
                } else if (entry.getValue().contains("/")) {
                    newUrl = newUrl.concat("/").concat(entry.getKey()).concat("@base64/")
                        .concat(base64url(entry.getValue()));
                } else {
                    newUrl = newUrl.concat("/").concat(entry.getKey()).concat("/")
                        .concat(URLEncoder.encode(entry.getValue(), PushGateway.urf8));
                }
            }
        }
        return newUrl;
    }

    void doRequest(final CollectorRegistry registry, final String job,
        final Map<String, String> groupingKey, final String method) throws IOException {
        String url = getUrl(job);
        url = enhanceUrl(url, groupingKey);
        final HttpURLConnection connection = connectionFactory.create(url);
        connection.setRequestProperty("Content-Type", TextFormat.CONTENT_TYPE_004);
        if (!method.equals(deleteReq)) {
            connection.setDoOutput(true);
        }
        connection.setRequestMethod(method);

        connection.setConnectTimeout(10 * MILLISECONDS_PER_SECOND);
        connection.setReadTimeout(10 * MILLISECONDS_PER_SECOND);
        connection.connect();

        try {
            if (!method.equals(deleteReq)) {
                final BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(connection.getOutputStream(), PushGateway.urf8)
                );
                TextFormat.write004(writer, registry.metricFamilySamples());
                writer.flush();
                writer.close();
            }

            final int response = connection.getResponseCode();
            if (response/100 != 2) {
                String errorMessage;
                final InputStream errorStream = connection.getErrorStream();
                if (errorStream == null) {
                    errorMessage = PushGateway.responseCodeFrom.concat(url)
                        .concat(was).concat(String.valueOf(response));
                } else {
                    final String errBody = readFromStream(errorStream);
                    errorMessage = PushGateway.responseCodeFrom.concat(url).concat(was)
                        .concat(String.valueOf(response)).concat(", response body: ")
                        .concat(errBody);
                }
                throw new IOException(errorMessage);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String base64url(final String v) {
        // Per RFC4648 table 2. We support Java 6, and java.util.Base64 was only added in Java 8,
        try {
            return DatatypeConverter.printBase64Binary(v.getBytes(PushGateway.urf8))
                .replace("+", "-").replace("/", "_");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);  // Unreachable.
        }
    }

    /**
     * Current counter value.
     *
     * @param is is OK
     * @throws IOException is OK
     */
    private static String readFromStream(final InputStream is) throws IOException {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        int length = is.read(buffer);
        while (length != -1) {
            result.write(buffer, 0, length);
            length = is.read(buffer);
        }
        return result.toString(PushGateway.urf8);
    }
}
