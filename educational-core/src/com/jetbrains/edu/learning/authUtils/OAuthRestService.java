package com.jetbrains.edu.learning.authUtils;

import com.intellij.ide.util.PropertiesComponent;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.RestService;
import org.jetbrains.io.Responses;

import java.io.IOException;

import static com.jetbrains.edu.learning.authUtils.RestServiceUtilsKt.createResponse;

// Should be implemented to handle oauth redirect to localhost:<port>
// and get the authorization code for different oauth providers
public abstract class OAuthRestService extends RestService {

  private static final String IS_REST_SERVICES_ENABLED = "Edu.Stepik.RestServicesEnabled";

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

  @Override
  public boolean isSupported(@NotNull FullHttpRequest request) {
    return isRestServicesEnabled() && super.isSupported(request);
  }

  @Override
  protected boolean isMethodSupported(@NotNull HttpMethod method) {
    return method == HttpMethod.GET;
  }

  public static boolean isRestServicesEnabled() {
    return PropertiesComponent.getInstance().getBoolean(IS_REST_SERVICES_ENABLED, true);
  }

  public static void setRestServicesEnabled(boolean enabled) {
    PropertiesComponent.getInstance().setValue(IS_REST_SERVICES_ENABLED, enabled, true);
  }
}
