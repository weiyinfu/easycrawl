package wyf.easycrawl.handler;

import wyf.easycrawl.Request;
import wyf.easycrawl.RequestHandler;
import wyf.easycrawl.Response;
import wyf.easycrawl.util.SslUtils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;

/**
 * 直接基于Java原生URLConnection的Http客户端
 */
public class UrlConnectionRequestHandler implements RequestHandler {
public UrlConnectionRequestHandler(boolean ignoreSslError) {
    if (ignoreSslError) {
        SslUtils.ignoreSsl();
        try {
            var sc = SSLContext.getInstance("SSL");
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((s, sslSession) -> true);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}

@Override
public void handle(Request req) {
    try {
        URLConnection conn = new URL(req.getUrl()).openConnection();
        conn.addRequestProperty("Accept", "*/*");
        conn.setRequestProperty("User-Agent", "User-Agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.108 Safari/537.36");
        conn.setDoInput(true);
        conn.connect();
        InputStream cin = conn.getInputStream();
        ByteArrayOutputStream cout = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        while (true) {
            int sz = cin.read(buf);
            if (sz <= 0) break;
            cout.write(buf, 0, sz);
        }
        Response resp = new Response();
        resp.setData(cout.toByteArray());
        req.getCallback().handle(req, resp);
    } catch (IOException e) {
        e.printStackTrace();
    }
}

}
