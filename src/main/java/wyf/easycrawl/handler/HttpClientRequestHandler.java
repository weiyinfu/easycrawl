package wyf.easycrawl.handler;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wyf.easycrawl.Request;
import wyf.easycrawl.RequestHandler;
import wyf.easycrawl.Response;
import wyf.easycrawl.util.SslUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 一切requestHandler都是单例模式
 * 使用apache httpclient的http客户端
 */
public class HttpClientRequestHandler implements RequestHandler {
Logger logger = LoggerFactory.getLogger(HttpClientRequestHandler.class);
CloseableHttpClient client = null;


public HttpClientRequestHandler(boolean ignoreSslError) {
    logger.info("creating client");
    var builder = HttpClientBuilder.create()
            .setConnectionTimeToLive(100, TimeUnit.SECONDS)
            .disableAutomaticRetries()
            .setDefaultHeaders(Collections.singletonList(
                    new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.108 Safari/537.36")
            ))
            .setDefaultConnectionConfig(ConnectionConfig.custom()
                    .setBufferSize(2048)
                    .build())
            .setDefaultSocketConfig(SocketConfig.custom()
                    .build())
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setMaxRedirects(3)
                    .setCircularRedirectsAllowed(false)
                    .setConnectionRequestTimeout(1000 * 3)
                    .setSocketTimeout(1000 * 3)
                    .setConnectionRequestTimeout(5 * 1000)
                    .build());
    if (ignoreSslError) {
        try {
            SslUtils.ignoreSsl();
            builder.setConnectionManager(new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .register("https", new SSLConnectionSocketFactory(SSLContext.getInstance("SSL")))
                    .build()));
        } catch (Exception e) {
            logger.error("ignore ssl error", e);
            System.exit(-1);
        }
    }
    client = builder.build();
}

@Override
public void handle(Request req) {
    try {
        HttpGet get = new HttpGet(req.getUrl());
        HttpResponse resp = client.execute(get);
        Response response = new Response();
        response.setData(EntityUtils.toByteArray(resp.getEntity()));
        req.getCallback().handle(req, response);
    } catch (IOException e) {
        e.printStackTrace();
    }
}
}
