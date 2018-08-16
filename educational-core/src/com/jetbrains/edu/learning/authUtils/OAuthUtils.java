package com.jetbrains.edu.learning.authUtils;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.util.io.StreamUtil;
import com.jetbrains.edu.learning.EduUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

public final class OAuthUtils {
  public static class GRANT_TYPE {
    public static final String AUTHORIZATION_CODE = "authorization_code";
    public static final String REFRESH_TOKEN = "refresh_token";
  }

  private static final String OAUTH_OK_PAGE = "/oauthResponsePages/okPage.html";
  private static final String OAUTH_ERROR_PAGE = "/oauthResponsePages/errorPage.html";

  private static final String IDE_NAME = "%IDE_NAME";
  private static final String PLATFORM_NAME = "%PLATFORM_NAME";
  private static final String ERROR_MESSAGE = "%ERROR_MESSAGE";

  private OAuthUtils() {}

  @NotNull
  public static String getOkPageContent(@NotNull String platformName) throws IOException {
    return getPageContent(OAUTH_OK_PAGE, ImmutableMap.of(
      IDE_NAME, ApplicationNamesInfo.getInstance().getFullProductName(),
      PLATFORM_NAME, platformName
    ));
  }

  @NotNull
  public static String getErrorPageContent(@NotNull String platformName, @NotNull String errorMessage) throws IOException {
    return getPageContent(OAUTH_ERROR_PAGE, ImmutableMap.of(
      ERROR_MESSAGE, errorMessage,
      PLATFORM_NAME, platformName
    ));
  }

  @NotNull
  private static String getPageContent(@NotNull String pagePath, @NotNull Map<String, String> replacements) throws IOException {
    String pageTemplate = getPageTemplate(pagePath);
    for (Map.Entry<String, String> replacement : replacements.entrySet()) {
      pageTemplate = pageTemplate.replaceAll(replacement.getKey(), replacement.getValue());
    }
    return pageTemplate;
  }

  @NotNull
  private static String getPageTemplate(@NotNull String pagePath) throws IOException {
    try (InputStream pageTemplateStream = EduUtils.class.getResourceAsStream(pagePath)) {
      return StreamUtil.readText(pageTemplateStream, Charset.forName("UTF-8"));
    }
  }
}