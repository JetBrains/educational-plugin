package com.jetbrains.edu.learning.stepik;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.util.PlatformUtils;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.net.ssl.CertificateManager;
import com.intellij.util.net.ssl.ConfirmingTrustManager;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.PluginUtils;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.stepik.api.Reply;
import com.jetbrains.edu.learning.stepik.serialization.StepikLessonAdapter;
import com.jetbrains.edu.learning.stepik.serialization.StepikReplyAdapter;
import com.jetbrains.edu.learning.stepik.serialization.StepikStepOptionsAdapter;
import org.apache.http.HttpHost;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

public class StepikClient {
  private static final Logger LOG = Logger.getInstance(StepikClient.class.getName());

  private StepikClient() {
  }

  public static Gson createGson(@Nullable Map<Key, Object> params) {
    String language = StepikAuthorizer.COURSE_LANGUAGE.get(params);
    return new GsonBuilder()
        .registerTypeAdapter(StepOptions.class, new StepikStepOptionsAdapter(language))
        .registerTypeAdapter(Lesson.class, new StepikLessonAdapter(language))
        .registerTypeAdapter(Reply.class, new StepikReplyAdapter(language))
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
  }

  @NotNull
  public static HttpClientBuilder getBuilder() {
    final HttpClientBuilder builder = HttpClients.custom().setSSLContext(CertificateManager.getInstance().getSslContext()).
      setMaxConnPerRoute(100000).setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE).setUserAgent(getUserAgent());

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

  @NotNull
  private static String getUserAgent() {
    String pluginVersion = PluginUtils.pluginVersion(EduNames.PLUGIN_ID);
    String version = pluginVersion == null ? "unknown" : pluginVersion;

    return String.format("%s/version(%s)/%s/%s", StepikNames.PLUGIN_NAME, version, System.getProperty("os.name"),
                         PlatformUtils.getPlatformPrefix());
  }
}
