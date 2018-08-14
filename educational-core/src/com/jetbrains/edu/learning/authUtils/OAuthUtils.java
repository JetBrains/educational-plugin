package com.jetbrains.edu.learning.authUtils;

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.util.io.StreamUtil;
import com.jetbrains.edu.learning.EduUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public final class OAuthUtils {
  private static final String OAUTH_OK_PAGE = "/oauthResponsePages/okPage.html";
  private static final String OAUTH_ERROR_PAGE = "/oauthResponsePages/errorPage.html";

  private static final String IDE_NAME_TEMPLATE = "%IDE_NAME";
  private static final String PLATFORM_NAME_TEMPLATE = "%PLATFORM_NAME";
  private static final String ERROR_MESSAGE_TEMPLATE = "%ERROR_MESSAGE";

  private OAuthUtils() {}

  @NotNull
  public static String getOkPageContent(@NotNull String platformName) throws IOException {
    return getPageTemplate(OAUTH_OK_PAGE)
      .replaceAll(IDE_NAME_TEMPLATE, ApplicationNamesInfo.getInstance().getFullProductName())
      .replaceAll(PLATFORM_NAME_TEMPLATE, platformName);
  }

  @NotNull
  public static String getErrorPageContent(@NotNull String platformName, @NotNull String errorMessage) throws IOException {
    return getPageTemplate(OAUTH_ERROR_PAGE)
      .replaceAll(ERROR_MESSAGE_TEMPLATE, errorMessage)
      .replaceAll(PLATFORM_NAME_TEMPLATE, platformName);
  }

  @NotNull
  private static String getPageTemplate(@NotNull String pagePath) throws IOException {
    try (InputStream pageTemplateStream = EduUtils.class.getResourceAsStream(pagePath)) {
      return StreamUtil.readText(pageTemplateStream, Charset.forName("UTF-8"));
    }
  }
}
