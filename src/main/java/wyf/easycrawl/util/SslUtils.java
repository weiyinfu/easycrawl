package wyf.easycrawl.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * java 信任SSL证书
 */
public class SslUtils {
static Logger logger = LoggerFactory.getLogger(SslUtils.class);
static X509TrustManager x509TrustMangager = new X509TrustManager() {
    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
};

/**
 * 忽略HTTPS请求的SSL证书，必须在openConnection之前调用
 */
public static void ignoreSsl() {
    try {
        logger.info("ignoring ssl");
        //把SSL和SSLv3两个协议的trustManager改成自定义的trustmanager
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, new TrustManager[]{x509TrustMangager}, null);
        SSLContext.getInstance("SSLv3").init(null, new TrustManager[]{x509TrustMangager}, null);
    } catch (Exception ex) {
        logger.error("ignore ssl error", ex);
        System.exit(-1);
    }
}
}