package com.jetbrains.edu.learning.stepik;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.net.ssl.CertificateManager;
import com.intellij.util.net.ssl.ConfirmingTrustManager;
import org.apache.http.HttpHost;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;

public class StepikClient {
  private static final Logger LOG = Logger.getInstance(StepikClient.class.getName());

  private StepikClient() {
  }

  @NotNull
  public static HttpClientBuilder getBuilder() {
    final HttpClientBuilder builder = HttpClients.custom().setSSLContext(CertificateManager.getInstance().getSslContext()).
      setMaxConnPerRoute(100000).setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE);

    final HttpConfigurable proxyConfigurable = HttpConfigurable.getInstance();
    final List<Proxy> proxies = proxyConfigurable.getOnlyBySettingsSelector().select(URI.create(StepikNames.STEPIK_URL));
    final InetSocketAddress address = proxies.size() > 0 ? (InetSocketAddress)proxies.get(0).address() : null;
    if (address != null) {
      builder.setProxy(new HttpHost(address.getHostName(), address.getPort()));
    }
    final ConfirmingTrustManager trustManager = CertificateManager.getInstance().getTrustManager();
    try {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
      builder.setSSLContext(sslContext);
    }
    catch (NoSuchAlgorithmException | KeyManagementException e) {
      LOG.error(e.getMessage());
    }
    return builder;
  }

}
