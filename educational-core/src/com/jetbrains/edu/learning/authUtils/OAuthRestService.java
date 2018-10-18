package com.jetbrains.edu.learning.authUtils;

import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.intellij.openapi.util.io.StreamUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.RestService;
import org.jetbrains.io.Responses;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

// Should be implemented to handle oauth redirect to localhost:<port>
// and get the authorization code for different oauth providers
public abstract class OAuthRestService extends RestService {
  protected final String myPlatformName;

  protected OAuthRestService(@NotNull String platformName) {
    myPlatformName = platformName;
  }

  @NotNull
  protected String sendErrorResponse(
    @NotNull HttpRequest request,
    @NotNull ChannelHandlerContext context,
    @NotNull String errorMessage
  ) throws IOException {
    LOG.warn(myPlatformName + ": " + errorMessage);
    showErrorPage(request, context, errorMessage);
    return errorMessage;
  }

  @Nullable
  protected String sendOkResponse(@NotNull HttpRequest request, @NotNull ChannelHandlerContext context) throws IOException {
    LOG.info(myPlatformName + ": Successful authorization");
    showOkPage(request, context);
    return null;
  }

  protected void showOkPage(
    @NotNull HttpRequest request,
    @NotNull ChannelHandlerContext context
  ) throws IOException {
    final String pageContent = OAuthUtils.getOkPageContent(myPlatformName);
    Responses.send(createResponse(pageContent), context.channel(), request);
  }

  protected void showErrorPage(
    @NotNull HttpRequest request,
    @NotNull ChannelHandlerContext context,
    @NotNull String errorMessage
  ) throws IOException {
    final String pageContent = OAuthUtils.getErrorPageContent(myPlatformName, errorMessage);
    Responses.send(createResponse(pageContent), context.channel(), request);
  }

  @NotNull
  public static HttpResponse createResponse(@NotNull String template) throws IOException {
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

  @Override
  protected boolean isMethodSupported(@NotNull HttpMethod method) {
    return method == HttpMethod.GET;
  }
}
