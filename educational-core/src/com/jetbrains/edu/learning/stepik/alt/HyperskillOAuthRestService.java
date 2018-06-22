package com.jetbrains.edu.learning.stepik.alt;

import com.jetbrains.edu.learning.authUtils.OAuthRestService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.RestService;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HyperskillOAuthRestService extends OAuthRestService {
  public static final String EDU_HYPERSKILL_SERVICE_NAME = "edu/hyperskill/oauth";
  private static final Pattern
    OAUTH_CODE_PATTERN = Pattern.compile("/api/" + EDU_HYPERSKILL_SERVICE_NAME + "\\?code=(\\w+)");

  protected HyperskillOAuthRestService() {
    super("Hyperskill");
  }

  @NotNull
  @Override
  protected String getServiceName() {
    return EDU_HYPERSKILL_SERVICE_NAME;
  }

  @Override
  protected boolean isMethodSupported(@NotNull HttpMethod method) {
    return method == HttpMethod.GET;
  }

  @Override
  protected boolean isHostTrusted(@NotNull FullHttpRequest request) throws InterruptedException, InvocationTargetException {
    final String uri = request.uri();
    final Matcher codeMatcher = OAUTH_CODE_PATTERN.matcher(uri);
    if (request.method() == HttpMethod.GET && codeMatcher.matches()) {
      return true;
    }
    return super.isHostTrusted(request);
  }

  @Nullable
  @Override
  public String execute(@NotNull QueryStringDecoder decoder,
                        @NotNull FullHttpRequest request,
                        @NotNull ChannelHandlerContext context) throws IOException {
    final String uri = decoder.uri();

    if (OAUTH_CODE_PATTERN.matcher(uri).matches()) {
      final String code = getStringParameter("code", decoder);
      assert code != null; // cannot be null because of pattern

      boolean success = HyperskillConnector.INSTANCE.login(code);
      if (success) {
        LOG.info(myPlatformName + ": OAuth code is handled");
        return sendOkResponse(request, context);
      }
      return sendErrorResponse(request, context, "Failed to login using provided code");
    }

    RestService.sendStatus(HttpResponseStatus.BAD_REQUEST, false, context.channel());
    return "Unknown command: " + uri;
  }
}
