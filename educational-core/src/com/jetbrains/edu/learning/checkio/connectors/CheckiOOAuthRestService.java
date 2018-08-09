package com.jetbrains.edu.learning.checkio.connectors;

import com.jetbrains.edu.learning.authUtils.BuiltinServerUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.RestService;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class CheckiOOAuthRestService extends RestService {
  private final Pattern myOauthCodePattern;
  private final CheckiOOAuthConnector myOAuthConnector;
  private final String myPlatformName;

  protected CheckiOOAuthRestService(@NotNull String platformName, @NotNull String restServicePath, @NotNull CheckiOOAuthConnector oAuthConnector) {
    myPlatformName = platformName;
    myOauthCodePattern = Pattern.compile(restServicePath + "\\?code=(\\w+)");
    myOAuthConnector = oAuthConnector;
  }

  @Override
  protected boolean isMethodSupported(@NotNull HttpMethod method) {
    return method == HttpMethod.GET;
  }

  @Override
  protected boolean isHostTrusted(@NotNull FullHttpRequest request) throws InterruptedException, InvocationTargetException {
    final String uri = request.uri();
    final Matcher codeMatcher = myOauthCodePattern.matcher(uri);
    if (request.method() == HttpMethod.GET && codeMatcher.matches()) {
      return true;
    }
    return super.isHostTrusted(request);
  }

  @Nullable
  @Override
  public String execute(@NotNull QueryStringDecoder decoder, @NotNull FullHttpRequest request, @NotNull ChannelHandlerContext context)
    throws IOException {

    final String uri = decoder.uri();

    if (myOauthCodePattern.matcher(uri).matches()) {
      final String code = getStringParameter("code", decoder);
      assert code != null; // cannot be null because of pattern

      LOG.info("OAuth code is handled");
      final String errorMessage = myOAuthConnector.afterCodeReceived(code);

      if (errorMessage == null) {
        return sendOkResponse(request, context);
      } else {
        return sendErrorResponse(request, context, "Couldn't get user info");
      }
    }

    RestService.sendStatus(HttpResponseStatus.BAD_REQUEST, false, context.channel());
    return "Unknown command: " + uri;
  }

  @NotNull
  protected String sendErrorResponse(
    @NotNull HttpRequest request,
    @NotNull ChannelHandlerContext context,
    @NotNull String errorMessage
  ) throws IOException {
    LOG.error(errorMessage);
    BuiltinServerUtils.showErrorPage(request, context, myPlatformName, errorMessage);
    return errorMessage;
  }

  @Nullable
  protected String sendOkResponse(@NotNull HttpRequest request, @NotNull ChannelHandlerContext context) throws IOException {
    LOG.info("Successful CheckiO authorization");
    BuiltinServerUtils.showOkPage(request, context, myPlatformName);
    return null;
  }
}
