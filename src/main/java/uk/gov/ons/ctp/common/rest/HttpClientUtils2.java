package uk.gov.ons.ctp.common.rest;

import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class HttpClientUtils2 {

  private static final PoolingHttpClientConnectionManager connectionManager =
      createConnectionManager();

  private static PoolingHttpClientConnectionManager createConnectionManager() {
    try {
      SSLConnectionSocketFactory socketFactory =
          new SSLConnectionSocketFactory(
              SSLContext.getDefault(),
              new String[] {"TLSv1", "TLSv1.1", "TLSv1.2"},
              null,
              SSLConnectionSocketFactory.getDefaultHostnameVerifier());
      Registry<ConnectionSocketFactory> registry =
          RegistryBuilder.<ConnectionSocketFactory>create()
              .register("http", PlainConnectionSocketFactory.INSTANCE)
              .register("https", socketFactory)
              .build();

      PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
      cm.setMaxTotal(200);
      cm.setDefaultMaxPerRoute(20);

      return cm;
    } catch (NoSuchAlgorithmException | RuntimeException ex) {
      // Logger.getLogger(HttpClientUtils.class.getName()).log(Level.SEVERE, null, ex);
      return null;
    }
  }

  public static PoolingHttpClientConnectionManager getConnectionManager() {
    return connectionManager;
  }
}
