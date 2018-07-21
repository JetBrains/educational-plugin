package com.jetbrains.edu.learning.authUtils;

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.intellij.openapi.util.io.StreamUtil;
import com.jetbrains.edu.learning.EduNames;
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

public class BuiltinServerUtils {
  private BuiltinServerUtils() {}

  public static void showOkPage(@NotNull HttpRequest request, @NotNull ChannelHandlerContext context, @NotNull String platformName)
    throws IOException {

    final String pageContent = getPageTemplate(EduNames.OAUTH_OK_PAGE)
      .replaceAll("%IDE_NAME", ApplicationNamesInfo.getInstance().getFullProductName())
      .replaceAll("%PLATFORM_NAME", platformName);

    Responses.send(createResponse(pageContent), context.channel(), request);
  }

  public static void showErrorPage(@NotNull HttpRequest request,
                                   @NotNull ChannelHandlerContext context,
                                   @NotNull String platformName,
                                   @NotNull String errorMessage) throws IOException {

    final String pageContent = getPageTemplate(EduNames.OAUTH_ERROR_PAGE)
      .replaceAll("%ERROR_MESSAGE", errorMessage)
      .replaceAll("%PLATFORM_NAME", platformName);

    Responses.send(createResponse(pageContent), context.channel(), request);
  }

  public static String getOkPageContent(@NotNull String platformName) throws IOException {
    return getPageTemplate(EduNames.OAUTH_OK_PAGE)
      .replaceAll("%IDE_NAME", ApplicationNamesInfo.getInstance().getFullProductName())
      .replaceAll("%PLATFORM_NAME", platformName);
  }

  public static String getErrorPageContent(@NotNull String platformName, @NotNull String errorMessage) throws IOException {
    return getPageTemplate(EduNames.OAUTH_ERROR_PAGE)
      .replaceAll("%ERROR_MESSAGE", errorMessage)
      .replaceAll("%PLATFORM_NAME", platformName);
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
