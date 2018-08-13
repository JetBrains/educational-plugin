package com.jetbrains.edu.learning.authUtils;

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.intellij.openapi.util.io.StreamUtil;
import com.jetbrains.edu.learning.EduUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.io.Responses;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class OAuthUtils {
  private static final String OAUTH_OK_PAGE = "/oauthResponsePages/okPage.html";
  private static final String OAUTH_ERROR_PAGE = "/oauthResponsePages/errorPage.html";

  private static final String IDE_NAME_TEMPLATE = "%IDE_NAME";
  private static final String PLATFORM_NAME_TEMPLATE = "%PLATFORM_NAME";
  private static final String ERROR_MESSAGE_TEMPLATE = "%ERROR_MESSAGE";

  private OAuthUtils() {}

  // used from rest services
  public static void showOkPage(
    @NotNull HttpRequest request,
    @NotNull ChannelHandlerContext context,
    @NotNull String platformName
  ) throws IOException {
    final String pageContent = getOkPageContent(platformName);
    Responses.send(createResponse(pageContent), context.channel(), request);
  }

  // used from rest services
  public static void showErrorPage(
    @NotNull HttpRequest request,
    @NotNull ChannelHandlerContext context,
    @NotNull String platformName,
    @NotNull String errorMessage
  ) throws IOException {
    final String pageContent = getErrorPageContent(platformName, errorMessage);
    Responses.send(createResponse(pageContent), context.channel(), request);
  }

  public static String getOkPageContent(@NotNull String platformName) throws IOException {
    return getPageTemplate(OAUTH_OK_PAGE)
      .replaceAll(IDE_NAME_TEMPLATE, ApplicationNamesInfo.getInstance().getFullProductName())
      .replaceAll(PLATFORM_NAME_TEMPLATE, platformName);
  }

  public static String getErrorPageContent(@NotNull String platformName, @NotNull String errorMessage) throws IOException {
    return getPageTemplate(OAUTH_ERROR_PAGE)
      .replaceAll(ERROR_MESSAGE_TEMPLATE, errorMessage)
      .replaceAll(PLATFORM_NAME_TEMPLATE, platformName);
  }

  @NotNull
  private static HttpResponse createResponse(@NotNull String template) throws IOException {
    try (
      BufferExposingByteArrayOutputStream byteOut = new BufferExposingByteArrayOutputStream()
    ) {
      byteOut.write(StreamUtil.loadFromStream(new ByteArrayInputStream(template.getBytes(Charset.forName("UTF-8")))));
      HttpResponse response = Responses.response("text/html", Unpooled.wrappedBuffer(byteOut.getInternalBuffer(), 0, byteOut.size()));
      Responses.addNoCache(response);
      response.headers().set("X-Frame-Options", "Deny");
      return response;
    }
  }

  @NotNull
  private static String getPageTemplate(@NotNull String pagePath) throws IOException {
    try (InputStream pageTemplateStream = EduUtils.class.getResourceAsStream(pagePath)) {
      return StreamUtil.readText(pageTemplateStream, Charset.forName("UTF-8"));
    }
  }


}
