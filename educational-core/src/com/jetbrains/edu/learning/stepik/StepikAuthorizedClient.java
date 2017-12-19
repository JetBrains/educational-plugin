package com.jetbrains.edu.learning.stepik;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.edu.learning.EduSettings;
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

public class StepikAuthorizedClient {
  private static final Logger LOG = Logger.getInstance(StepikAuthorizedClient.class.getName());

  private static CloseableHttpClient ourClient;

  private StepikAuthorizedClient() {
  }

  @Nullable
  public static CloseableHttpClient getHttpClient() {
    if (ourClient != null) {
      return ourClient;
    }

    EduSettings eduSettings = EduSettings.getInstance();

    assert eduSettings.getUser() != null: "User must not be null";

    StepicUser user = eduSettings.getUser();
    assert user != null;

    if (!StepikClient.isTokenUpToDate(user.getAccessToken())) {
      StepikWrappers.TokenInfo tokens = getUpdatedTokens(user.getRefreshToken());
      if (tokens != null) {
        user.setTokenInfo(tokens);
      }
      else {
        return null;
      }
    }

    ourClient = createInitializedClient(user.getAccessToken());

    return ourClient;
  }

  @Nullable
  public static <T> T getFromStepik(@NotNull String link, @NotNull final Class<T> container) throws IOException {
    final CloseableHttpClient client = getHttpClient();
    return client == null ? null : StepikClient.getFromStepik(link, container, client);
  }

  /*
   * This method should be used only in project generation while project is not available.
   * Make sure you saved stepik user in task manager after using this method.
   */
  @NotNull
  public static CloseableHttpClient getHttpClient(@NotNull final StepicUser user) {
    if (ourClient != null) {
      return ourClient;
    }

    if (!StepikClient.isTokenUpToDate(user.getAccessToken())) {
      StepikWrappers.TokenInfo tokenInfo = getUpdatedTokens(user.getRefreshToken());
      if (tokenInfo != null) {
        user.setTokenInfo(tokenInfo);
      }
      else {
        return StepikClient.getHttpClient();
      }
    }

    ourClient = createInitializedClient(user.getAccessToken());

    return ourClient;
  }

   /*
   * This method should be used only in project generation while project is not available.
   * Make sure you saved stepik user in task manager after using this method.
   */
  public static <T> T getFromStepik(String link, final Class<T> container, @NotNull final StepicUser stepicUser) throws IOException {
    return StepikClient.getFromStepik(link, container, getHttpClient(stepicUser));
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

  @Nullable
  public static StepicUser login(@NotNull final String code, String redirectUrl) {
    final List<NameValuePair> parameters = new ArrayList<>();
    parameters.add(new BasicNameValuePair("grant_type", "authorization_code"));
    parameters.add(new BasicNameValuePair("code", code));
    parameters.add(new BasicNameValuePair("redirect_uri", redirectUrl));
    parameters.add(new BasicNameValuePair("client_id", StepikNames.CLIENT_ID));

    StepikWrappers.TokenInfo tokenInfo = getTokens(parameters);
    if (tokenInfo == null) {
      return null;
    }
    return login(tokenInfo);
  }


  public static StepicUser login(@NotNull StepikWrappers.TokenInfo tokenInfo) {
    final StepicUser user = new StepicUser(tokenInfo);
    ourClient = createInitializedClient(user.getAccessToken());

    final StepicUser currentUser = getCurrentUser();
    if (currentUser != null) {
      user.setId(currentUser.getId());
      user.setFirstName(currentUser.getFirstName());
      user.setLastName(currentUser.getLastName());
    }
    return user;
  }

  public static void invalidateClient() {
    ourClient = null;
  }

  @Nullable
  private static StepikWrappers.TokenInfo getUpdatedTokens(@NotNull final String refreshToken) {
    final List<NameValuePair> parameters = new ArrayList<>();
    parameters.add(new BasicNameValuePair("client_id", StepikNames.CLIENT_ID));
    parameters.add(new BasicNameValuePair("content-type", "application/json"));
    parameters.add(new BasicNameValuePair("grant_type", "refresh_token"));
    parameters.add(new BasicNameValuePair("refresh_token", refreshToken));

    return getTokens(parameters);
  }

  @Nullable
  public static StepicUser getCurrentUser() {
    CloseableHttpClient client = getHttpClient();
    if (client != null) {
      try {
        final StepikWrappers.AuthorWrapper wrapper = StepikClient.getFromStepik(StepikNames.CURRENT_USER,
                                                                                   StepikWrappers.AuthorWrapper.class,
                                                                                   client);
        if (wrapper != null && !wrapper.users.isEmpty()) {
          return wrapper.users.get(0);
        }
      }
      catch (IOException e) {
        LOG.warn("Couldn't get a current user");
      }
    }
    return null;
  }

  @Nullable
  public static StepikWrappers.TokenInfo getTokens(@NotNull final List<NameValuePair> parameters, @Nullable String credentials) {
    final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

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
        return gson.fromJson(responseString, StepikWrappers.TokenInfo.class);
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
  public static StepikWrappers.TokenInfo getTokens(@NotNull final List<NameValuePair> parameters) {
    return getTokens(parameters, null);
  }
}
