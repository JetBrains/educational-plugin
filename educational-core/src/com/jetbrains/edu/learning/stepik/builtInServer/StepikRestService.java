/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.edu.learning.stepik.builtInServer;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.AppIcon;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.authUtils.OAuthRestService;
import com.jetbrains.edu.learning.stepik.StepikNames;
import com.jetbrains.edu.learning.stepik.StepikUser;
import com.jetbrains.edu.learning.stepik.api.StepikConnector;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.RestService;
import org.jetbrains.io.Responses;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jetbrains.edu.learning.authUtils.RestServiceUtilsKt.createResponse;
import static com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText;

public class StepikRestService extends OAuthRestService {
  private static final Logger LOG = Logger.getInstance(StepikRestService.class.getName());
  private static final Pattern OAUTH_CODE_PATTERN = StepikConnector.getInstance().getOAuthPattern();
  private static final Pattern OAUTH_ERROR_CODE_PATTERN = StepikConnector.getInstance().getOAuthPattern("\\?error=(\\w+)");

  public StepikRestService() {
    super(StepikNames.STEPIK);
  }

  @Override
  protected boolean isPrefixlessAllowed() {
    return true;
  }

  @Override
  protected boolean isHostTrusted(@NotNull FullHttpRequest request,
                                  @NotNull QueryStringDecoder urlDecoder) throws InterruptedException, InvocationTargetException {
    String uri = request.uri();
    Matcher codeMatcher = OAUTH_CODE_PATTERN.matcher(uri);
    Matcher errorMatcher = OAUTH_ERROR_CODE_PATTERN.matcher(uri);
    if (request.method() == HttpMethod.GET && (codeMatcher.matches() || errorMatcher.matches())) {
      return true;
    }
    return super.isHostTrusted(request, urlDecoder);
  }

  @Nullable
  @Override
  public String execute(@NotNull QueryStringDecoder urlDecoder, @NotNull FullHttpRequest request, @NotNull ChannelHandlerContext context)
    throws IOException {
    String uri = urlDecoder.uri();
    LOG.info("Request: " + uri);

    Matcher errorCodeMatcher = OAUTH_ERROR_CODE_PATTERN.matcher(uri);
    if (errorCodeMatcher.matches()) {
      String pageContent = getInternalTemplateText("stepik.redirectPage.html");
      Responses.send(createResponse(pageContent), context.channel(), request);
      return null;
    }

    Matcher codeMatcher = OAUTH_CODE_PATTERN.matcher(uri);
    if (codeMatcher.matches()) {
      String code = getStringParameter(CODE_ARGUMENT, urlDecoder);
      if (code != null) {
        final boolean success = StepikConnector.getInstance().login(code);
        final StepikUser user = EduSettings.getInstance().getUser();
        if (success && user != null) {
          showOkPage(request, context);
          showStepikNotification(NotificationType.INFORMATION,
                                 "Logged in as " + user.getFirstName() + " " + user.getLastName());
          focusOnApplicationWindow();
          return null;
        }
      }

      showStepikNotification(NotificationType.ERROR, "Failed to log in");
      return sendErrorResponse(request, context, "Couldn't find code parameter for Stepik OAuth");
    }

    RestService.sendStatus(HttpResponseStatus.BAD_REQUEST, false, context.channel());
    String message = "Unknown command: " + uri;
    LOG.info(message);
    return message;
  }

  @Override
  protected @NotNull String getServiceName() {
    return StepikConnector.getInstance().getServiceName();
  }

  private static void focusOnApplicationWindow() {
    JFrame frame = WindowManager.getInstance().findVisibleFrame();
    if (frame == null) return;
    ApplicationManager.getApplication().invokeLater(() -> {
      AppIcon.getInstance().requestFocus((IdeFrame)frame);
      frame.toFront();
    });
  }

  private static void showStepikNotification(@NotNull NotificationType notificationType, @NotNull String text) {
    Notification notification = new Notification("EduTools", "Stepik", text, notificationType);
    notification.notify(null);
  }
}
