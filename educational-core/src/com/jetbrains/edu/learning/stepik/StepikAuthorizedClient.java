package com.jetbrains.edu.learning.stepik;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.authUtils.TokenInfo;
import com.jetbrains.edu.learning.serialization.SerializationUtils;
import com.jetbrains.edu.learning.stepik.api.StepikConnector;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.*;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StepikAuthorizedClient {
  private static final Logger LOG = Logger.getInstance(StepikAuthorizedClient.class.getName());

  private static CloseableHttpClient ourClient;

  private StepikAuthorizedClient() {
  }

  @Nullable
  public static CloseableHttpClient getHttpClient() {
    StepikUser user = EduSettings.getInstance().getUser();

    final boolean isUpToDate = user == null || user.getTokenInfo().isUpToDate();
    if (ourClient != null && isUpToDate) {
      return ourClient;
    }

    if (user == null) {
       return null;
    }

    if (!isUpToDate && !updateTokens(user)) {
      return null;
    }

    ourClient = createInitializedClient(user.getAccessToken());

    return ourClient;
  }

  @Nullable
  public static <T> T getFromStepik(@NotNull String link,
                                    @NotNull final Class<T> container) throws IOException {
    return getFromStepik(link, container, null);
  }

  @Nullable
  public static <T> T getFromStepik(@NotNull String link,
                                    @NotNull final Class<T> container,
                                    @Nullable Map<Key, Object> params) throws IOException {
    final CloseableHttpClient client = getHttpClient();
    return client == null ? null : StepikClient.getFromStepik(link, container, client, params);
  }

  private static boolean updateTokens(@NotNull StepikUser user) {
    TokenInfo tokenInfo = getUpdatedTokens(user.getRefreshToken());
    if (tokenInfo != null) {
      user.setTokenInfo(tokenInfo);
      return true;
    }
    return false;
  }

  @NotNull
  private static CloseableHttpClient createInitializedClient(@NotNull String accessToken) {
    final List<BasicHeader> headers = new ArrayList<>();
    headers.add(getAuthorizationHeader(accessToken));
    headers.add(new BasicHeader("Content-type", StepikNames.CONTENT_TYPE_APP_JSON));
    return StepikClient.getBuilder().setDefaultHeaders(headers).build();
  }

  @NotNull
  public static BasicHeader getAuthorizationHeader(@NotNull String accessToken) {
    return new BasicHeader("Authorization", "Bearer " + accessToken);
  }

  public static boolean login(@NotNull final String code, String redirectUrl) {
    final boolean success = StepikConnector.INSTANCE.login(code, redirectUrl);
    if (!success) return false;
    final StepikUser user = EduSettings.getInstance().getUser();
    if (user == null) {
      return false;
    }
    ourClient = createInitializedClient(user.getAccessToken());
    return true;
  }

  // TO BE REMOVED!
  public static StepikUser login(@NotNull TokenInfo tokenInfo) {
    final StepikUser user = new StepikUser(tokenInfo);
    ourClient = createInitializedClient(user.getAccessToken());

    final StepikUserInfo currentUser = StepikConnector.INSTANCE.getCurrentUserInfo(user);
    if (currentUser != null) {
      user.setUserInfo(currentUser);
    }
    return user;
  }

  public static void invalidateClient() {
    ourClient = null;
  }

  @Nullable
  private static TokenInfo getUpdatedTokens(@NotNull final String refreshToken) {
    final List<NameValuePair> parameters = new ArrayList<>();
    parameters.add(new BasicNameValuePair("client_id", StepikNames.CLIENT_ID));
    parameters.add(new BasicNameValuePair("content-type", "application/json"));
    parameters.add(new BasicNameValuePair("grant_type", "refresh_token"));
    parameters.add(new BasicNameValuePair("refresh_token", refreshToken));

    return getTokens(parameters);
  }

  @Nullable
  public static TokenInfo getTokens(@NotNull final List<NameValuePair> parameters, @Nullable String credentials) {
    final Gson gson = new GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .registerTypeAdapter(TokenInfo.class, new SerializationUtils.TokenInfoDeserializer())
      .create();

    final HttpPost request = new HttpPost(StepikNames.TOKEN_URL);

    if (credentials != null) {
      request.addHeader("Authorization", "Basic " + Base64.encodeBase64String(credentials.getBytes(Consts.UTF_8)));
    }
    request.setEntity(new UrlEncodedFormEntity(parameters, Consts.UTF_8));

    try {
      final CloseableHttpClient client = StepikClient.getHttpClient();
      final CloseableHttpResponse response = client.execute(request);
      final StatusLine statusLine = response.getStatusLine();
      final HttpEntity responseEntity = response.getEntity();
      final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
      EntityUtils.consume(responseEntity);
      if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
        return gson.fromJson(responseString, TokenInfo.class);
      }
      else {
        LOG.warn("Failed to get tokens: " + statusLine.getStatusCode() + statusLine.getReasonPhrase());
      }
    }
    catch (IOException e) {
      LOG.warn(e.getMessage());
    }
    return null;
  }

  @Nullable
  public static TokenInfo getTokens(@NotNull final List<NameValuePair> parameters) {
    return getTokens(parameters, null);
  }
}
