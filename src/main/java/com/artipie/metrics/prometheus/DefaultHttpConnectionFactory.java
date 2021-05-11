package com.artipie.metrics.prometheus;

import com.artipie.metrics.prometheus.HttpConnectionFactory;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class DefaultHttpConnectionFactory implements HttpConnectionFactory {
    @Override
    public HttpURLConnection create(String url) throws IOException {
        return (HttpURLConnection) new URL(url).openConnection();
    }
}
