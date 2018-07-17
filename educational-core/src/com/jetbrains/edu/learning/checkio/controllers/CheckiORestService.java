package com.jetbrains.edu.learning.checkio.controllers;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.intellij.openapi.util.io.StreamUtil;
import com.jetbrains.edu.learning.checkio.CheckiONames;
import com.jetbrains.edu.learning.checkio.api.CheckiOApiController;
import com.jetbrains.edu.learning.checkio.model.CheckiOUser;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.RestService;
import org.jetbrains.io.Responses;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
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
    String uri = decoder.uri();
    Matcher codeMatcher = OAUTH_CODE_PATTERN.matcher(uri);
    if (codeMatcher.matches()) {
      String code = getStringParameter("code", decoder);
      if (code != null) {
        CheckiOUser newUser = CheckiOApiController.getInstance().getUser(code);
        if (newUser != null) {
          ApplicationManager.getApplication().getMessageBus().syncPublisher(CheckiOAuthorizationController.LOGGED_IN).userLoggedIn(newUser);
          sendHtmlResponse(request, context, CheckiONames.CHECKIO_OAUTH_SUCCEED_PAGE);
          return null;
        }
      }
      sendHtmlResponse(request, context, CheckiONames.CHECKIO_OAUTH_FAILED_PAGE);
      return "Couldn't find code parameter for CheckiO OAuth";
    }

    RestService.sendStatus(HttpResponseStatus.BAD_REQUEST, false, context.channel());
    return "Unknown command: " + uri;
  }

  private void sendHtmlResponse(@NotNull HttpRequest request, @NotNull ChannelHandlerContext context, String pagePath) throws IOException {
    try(
      BufferExposingByteArrayOutputStream byteOut = new BufferExposingByteArrayOutputStream();
      InputStream pageTemplateStream = getClass().getResourceAsStream(pagePath)
    ) {
      String pageTemplate = StreamUtil.readText(pageTemplateStream, Charset.forName("UTF-8"));
      String pageWithProductName = pageTemplate.replaceAll("%IDE_NAME", ApplicationNamesInfo.getInstance().getFullProductName());
      byteOut.write(StreamUtil.loadFromStream(new ByteArrayInputStream(pageWithProductName.getBytes(Charset.forName("UTF-8")))));
      HttpResponse response = Responses.response("text/html", Unpooled.wrappedBuffer(byteOut.getInternalBuffer(), 0, byteOut.size()));
      Responses.addNoCache(response);
      response.headers().set("X-Frame-Options", "Deny");
      Responses.send(response, context.channel(), request);
    }
  }
}
