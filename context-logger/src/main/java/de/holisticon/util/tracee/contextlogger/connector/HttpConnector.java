package de.holisticon.util.tracee.contextlogger.connector;

/*
 * import de.holisticon.util.tracee.Tracee;
 * import de.holisticon.util.tracee.TraceeLogger;
 * import org.apache.http.HttpResponse;
 * import org.apache.http.entity.ContentType;
 * import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
 * import org.apache.http.impl.nio.client.HttpAsyncClients;
 * import org.apache.http.nio.IOControl;
 * import org.apache.http.nio.client.methods.AsyncCharConsumer;
 * import org.apache.http.nio.client.methods.HttpAsyncMethods;
 * import org.apache.http.protocol.HttpContext;
 * import java.io.IOException;
 * import java.nio.CharBuffer;
 * import java.util.HashMap;
 * import java.util.Map;
 * import java.util.concurrent.Future;
 */

import java.io.IOException;
import java.util.Map;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpClientConfig.Builder;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Realm;
import com.ning.http.client.Realm.AuthScheme;
import com.ning.http.client.Response;

import de.holisticon.util.tracee.Tracee;
import de.holisticon.util.tracee.TraceeLogger;

/**
 * A Connector to send error reports via http.
 * Created by Tobias Gindler, holisticon AG on 07.02.14.
 */
public class HttpConnector implements Connector {

    public static final String PROPERTY_URL = "url";
    public static final String PROPERTY_BASIC_AUTH_USER = "basicAuth.user";
    public static final String PROPERTY_BASIC_AUTH_PASSWORD = "basicAuth.password";
    public static final String PROPERTY_PROXY_HOST = "proxy.host";
    public static final String PROPERTY_PROXY_PORT = "proxy.port";
    public static final String PROPERTY_PROXY_USER = "proxy.user";
    public static final String PROPERTY_PROXY_PASSWORD = "proxy.password";
    public static final String PROPERTY_REQUEST_TIMEOUT = "request.timoutInMs";

    public static final int DEFAULT_REQUEST_TIMEOUT = 10000;
    public static final int DEFAULT_REQUEST_IDLE_TIMEOUT = 500;

    private static final TraceeLogger LOGGER = Tracee.getBackend().getLoggerFactory().getLogger(HttpConnector.class);

    private String url;

    private Builder builder;

    @Override
    public void init(final Map<String, String> properties) {
        // map properties
        url = properties.get(PROPERTY_URL);
        final String basicAuthUser = properties.get(PROPERTY_BASIC_AUTH_USER);
        final String basicAuthPassword = properties.get(PROPERTY_BASIC_AUTH_PASSWORD);
        final String proxyHost = properties.get(PROPERTY_PROXY_HOST);
        final String proxyUser = properties.get(PROPERTY_PROXY_USER);
        final String proxyPassword = properties.get(PROPERTY_PROXY_PASSWORD);

        final Integer proxyPort = convertStringToInt(properties.get(PROPERTY_PROXY_PORT), null);

        final Integer requestTimeoutInMs = convertStringToInt(properties.get(PROPERTY_REQUEST_TIMEOUT), DEFAULT_REQUEST_TIMEOUT);

        builder = new AsyncHttpClientConfig.Builder();

        if (basicAuthUser != null && basicAuthPassword != null) {
            final Realm realm = new Realm.RealmBuilder().setPrincipal(basicAuthUser).setPassword(basicAuthPassword).setUsePreemptiveAuth(true)
                    .setScheme(AuthScheme.BASIC).build();

            builder.setRealm(realm);
        }

        // Add Proxy Support - use authentication if user and password properties are set
        if (proxyHost != null && proxyPort != null) {
            final ProxyServer proxy;
            if (proxyUser != null && proxyPassword != null) {
                proxy = new ProxyServer(proxyHost, proxyPort, proxyUser, proxyPassword);
            } else {
                proxy = new ProxyServer(proxyHost, proxyPort);
            }

            builder.setProxyServer(proxy);
        }

        builder.setRequestTimeoutInMs(requestTimeoutInMs);
        builder.setIdleConnectionTimeoutInMs(DEFAULT_REQUEST_IDLE_TIMEOUT);

    }

    @Override
    public void sendErrorReport(final String json) {

        final String tmpUrl = this.url;

        try {
            @SuppressWarnings("resource")
            final AsyncHttpClient asyncHttpClient = new AsyncHttpClient(builder.build());
            asyncHttpClient.preparePost(this.url).setBody(json).setHeader("Content-type", "text/json;charset=utf-8").setBodyEncoding("UTF-8")
                    .execute(new AsyncCompletionHandler<Response>() {

                        @Override
                        public Response onCompleted(final Response response) throws Exception {
                            LOGGER.info("Error report send via HTTP to '" + tmpUrl + "'");
                            return response;
                        }

                        @Override
                        public void onThrowable(final Throwable t) {
                            // Something wrong happened.
                            LOGGER.error("An error occurred while sending the error report via HTTP to '" + tmpUrl + "'", t);
                        }
                    });

        }
        catch (final IOException e) {
            LOGGER.error("An error occurred while sending the error report via HTTP to '" + this.url + "'", e);
        }

    }

    private Integer convertStringToInt(final String value, final Integer defaultValue) {
        try {
            if (value != null) {
                return Integer.valueOf(value);
            }
        } catch (final NumberFormatException e) {
            // ignore
        }
        return defaultValue;
    }
}
