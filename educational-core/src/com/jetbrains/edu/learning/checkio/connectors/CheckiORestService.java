package com.jetbrains.edu.learning.checkio.connectors;

import com.intellij.openapi.application.ApplicationManager;
import com.jetbrains.edu.learning.authUtils.BuiltinServerUtils;
import com.jetbrains.edu.learning.checkio.CheckiONames;
import com.jetbrains.edu.learning.checkio.model.CheckiOUser;
import com.jetbrains.edu.learning.checkio.model.Tokens;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.RestService;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckiORestService extends RestService {
  private CheckiORestService() {}

  private static final Pattern OAUTH_CODE_PATTERN = Pattern.compile(CheckiONames.EDU_CHECKIO_OAUTH_SERVICE + "\\?code=(\\w+)");

  @NotNull
  @Override
  protected String getServiceName() {
    return CheckiONames.EDU_CHECKIO_SERVICE_NAME;
  }

  @Override
  protected boolean isMethodSupported(@NotNull HttpMethod method) {
    return method == HttpMethod.GET;
  }

  @Override
  protected boolean isHostTrusted(@NotNull FullHttpRequest request) throws InterruptedException, InvocationTargetException {
    String uri = request.uri();
    Matcher codeMatcher = OAUTH_CODE_PATTERN.matcher(uri);
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

    if (OAUTH_CODE_PATTERN.matcher(uri).matches()) {
      LOG.info("OAuth code is handled");

      final String code = getStringParameter("code", decoder);

      if (code == null) {
        return sendErrorResponse(request, context, "CheckiO OAuth code is null");
      }

      final Tokens newTokens = CheckiOConnector.getTokens(code);
      final CheckiOUser newUser = newTokens == null ? null : CheckiOConnector.getUser(newTokens.getAccessToken());

      if (newUser != null) {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(CheckiOConnector.LOGGED_IN).loggedIn(newTokens, newUser);
        return sendOkResponse(request, context);
      }

      return sendErrorResponse(request, context, "Couldn't get user info");
    }

    RestService.sendStatus(HttpResponseStatus.BAD_REQUEST, false, context.channel());
    return "Unknown command: " + uri;
  }

  @NotNull
  private static String sendErrorResponse(@NotNull HttpRequest request,
                                          @NotNull ChannelHandlerContext context,
                                          @NotNull String errorMessage) throws IOException {
    LOG.error(errorMessage);
    BuiltinServerUtils.showErrorPage(request, context, CheckiONames.CHECKIO, errorMessage);
    return errorMessage;
  }

  @Nullable
  private static String sendOkResponse(@NotNull HttpRequest request, @NotNull ChannelHandlerContext context) throws IOException {
    LOG.info("Successful CheckiO authorization");
    BuiltinServerUtils.showOkPage(request, context, CheckiONames.CHECKIO);
    return null;
  }
}
