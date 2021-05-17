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

import com.jcabi.log.Logger;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
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

/**
 * {@link PushGateway} implementation storing data in memory.
 *
 * @since 0.9
 */
public class PushGateway {

    /**
     * Simple Variable.
     */
    private static final int THOUSAND2 = 1024;

    /**
     * Simple Variable.
     */
    private static final int THOUSAND = 1000;

    /**
     * Simple Variable.
     */
    private static final int HUNDRED = 100;

    /**
     * Simple Variable.
     */
    private static final int TEN = 10;

    /**
     * Simple Variable.
     */
    private static final String UTF8 = "UTF-8";

    /**
     * Simple Variable.
     */
    private static final String RESCODE = "Response code from ";

    /**
     * Simple Variable.
     */
    private static final String POSTREQ = "POST";

    /**
     * Simple Variable.
     */
    private static final String WAS = " was ";

    /**
     * Simple Variable.
     */
    private final String gatebaseurl;

    /**
     * Simple Variable.
     */
    private final HttpConnectionFactory connfactory;

    /**
     * Construct a Pushgateway, with the given address.
     * <p>
     * @param address Is host:port or ip:port of the Pushgateway.
     */
    public PushGateway(final String address) {
        this(createSneakily("http://".concat(address)));
    }

    /**
     * Construct a Pushgateway, with the given URL.
     * <p>
     * @param serverburl Is the base URL and optional context path of the Pushgateway server.
     */
    public PushGateway(final URL serverburl) {
        this.gatebaseurl = URI.create(serverburl.toString().concat("/metrics/"))
            .normalize()
            .toString();
        this.connfactory = new DefaultHttpConnectionFactory();
    }

    /**
     * Current counter value.
     *
     * @param registry Is OK
     * @param job Is OK
     * @throws IOException Is OK
     */
    public void pushAdd(final CollectorRegistry registry, final String job) throws IOException {
        this.doRequest(registry, job, null);
    }

    /**
     * Current counter value.
     *
     * @param job Is OK
     * @return String Is OK
     * @throws UnsupportedEncodingException Is OK
     */
    String getUrl(final String job) throws UnsupportedEncodingException {
        String url = this.gatebaseurl;
        if (job.contains("/")) {
            url = url.concat("job@base64/").concat(baseurl(job));
        } else {
            url = url.concat("job/").concat(URLEncoder.encode(job, PushGateway.UTF8));
        }
        return url;
    }

    /**
     * Current counter value.
     *
     * @param url Is OK
     * @param groupingkey Is OK
     * @return String Is OK
     * @throws UnsupportedEncodingException Is OK
     */
    static String enhanceUrl(final String url, final Map<String, String> groupingkey)
        throws UnsupportedEncodingException {
        String newurl = url;
        if (groupingkey != null) {
            for (final Map.Entry<String, String> entry: groupingkey.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    newurl = newurl.concat("/").concat(entry.getKey()).concat("@base64/=");
                } else if (entry.getValue().contains("/")) {
                    newurl = newurl.concat("/").concat(entry.getKey()).concat("@base64/")
                        .concat(baseurl(entry.getValue()));
                } else {
                    newurl = newurl.concat("/").concat(entry.getKey()).concat("/")
                        .concat(URLEncoder.encode(entry.getValue(), PushGateway.UTF8));
                }
            }
        }
        return newurl;
    }

    /**
     * Current counter value.
     *
     * @param response Is OK
     * @param connection Is OK
     * @param url Is OK
     * @throws IOException Is OK
     */
    static void checkError(final int response, final HttpURLConnection connection,
        final String url) throws IOException {
        if (response / PushGateway.HUNDRED != 2) {
            final String errormessage;
            final InputStream errorstream = connection.getErrorStream();
            if (errorstream == null) {
                errormessage = PushGateway.RESCODE.concat(url)
                    .concat(PushGateway.WAS).concat(String.valueOf(response));
            } else {
                final String errbody = readFromStream(errorstream);
                errormessage = PushGateway.RESCODE.concat(url)
                    .concat(PushGateway.WAS)
                    .concat(String.valueOf(response)).concat(", response body: ")
                    .concat(errbody);
                Logger.error(PushGateway.class, errormessage);
            }
        }
    }

    /**
     * Current counter value.
     *
     * @param registry Is OK
     * @param job Is OK
     * @param groupingkey Is OK
     * @throws IOException Is OK
     */
    void doRequest(final CollectorRegistry registry, final String job,
        final Map<String, String> groupingkey) throws IOException {
        String url = this.getUrl(job);
        url = PushGateway.enhanceUrl(url, groupingkey);
        final HttpURLConnection connection = this.connfactory.create(url);
        connection.setRequestProperty("Content-Type", TextFormat.CONTENT_TYPE_004);
        connection.setDoOutput(true);
        connection.setRequestMethod(PushGateway.POSTREQ);
        connection.setConnectTimeout(PushGateway.TEN * PushGateway.THOUSAND);
        connection.setReadTimeout(PushGateway.TEN * PushGateway.THOUSAND);
        connection.connect();
        try {
            final BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(connection.getOutputStream(), PushGateway.UTF8)
            );
            TextFormat.write004(writer, registry.metricFamilySamples());
            writer.flush();
            writer.close();
            final int response = connection.getResponseCode();
            PushGateway.checkError(response, connection, url);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Current counter value.
     *
     * @param vvar Is OK
     * @return String Is OK
     */
    private static String baseurl(final String vvar) {
        String res = "";
        try {
            res = DatatypeConverter.printBase64Binary(vvar.getBytes(PushGateway.UTF8))
                .replace("+", "-").replace("/", "_");
        } catch (final UnsupportedEncodingException exc) {
            Logger.error(PushGateway.class, exc.getMessage());
        }
        return res;
    }

    /**
     * Current counter value.
     *
     * @param instream Instream OK
     * @return String Is OK
     * @throws IOException instream OK
     */
    private static String readFromStream(final InputStream instream) throws IOException {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        final byte[] buffer = new byte[PushGateway.THOUSAND2];
        int length = instream.read(buffer);
        while (length != -1) {
            result.write(buffer, 0, length);
            length = instream.read(buffer);
        }
        return result.toString(PushGateway.UTF8);
    }

    /**
     * Creates a URL instance from a String representation of a URL without throwing a
     * checked exception.
     * Required because you can't wrap a call to another constructor in a try statement.
     *
     * @param urlstring The String representation of the URL.
     * @return The URL instance.
     */
    private static URL createSneakily(final String urlstring) {
        URL res = null;
        try {
            res = new URL(urlstring);
        } catch (final MalformedURLException exc) {
            Logger.error(PushGateway.class, exc.getMessage());
        }
        return res;
    }
}
