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
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.Section;
import com.jetbrains.edu.learning.stepik.*;
import com.jetbrains.edu.learning.stepik.api.StepikNewConnector;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.RestService;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jetbrains.edu.learning.stepik.builtInServer.EduBuiltInServerUtils.*;

public class StepikRestService extends OAuthRestService {
  private static final Logger LOG = Logger.getInstance(StepikRestService.class.getName());
  private static final Pattern OPEN_COURSE_PATTERN = Pattern.compile("/" + StepikNames.EDU_STEPIK_SERVICE_NAME + "\\?link=.+");
  private static final Pattern COURSE_PATTERN = Pattern.compile("https://stepik\\.org/lesson(?:/[a-zA-Z\\-]*-|/)(\\d+)/step/(\\d+)");
  private static final Pattern
    OAUTH_CODE_PATTERN = Pattern.compile("/" + RestService.PREFIX + "/" + StepikNames.EDU_STEPIK_SERVICE_NAME + "/oauth" + "\\?code=(\\w+)");

  public StepikRestService() {
    super(StepikNames.STEPIK);
  }

  @NotNull
  private static String log(@NotNull String message) {
    LOG.info(message);
    return message;
  }

  @NotNull
  @Override
  protected String getServiceName() {
    return StepikNames.EDU_STEPIK_SERVICE_NAME;
  }

  @Override
  protected boolean isPrefixlessAllowed() {
    return true;
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
  public String execute(@NotNull QueryStringDecoder urlDecoder, @NotNull FullHttpRequest request, @NotNull ChannelHandlerContext context)
    throws IOException {
    String uri = urlDecoder.uri();
    LOG.info("Request: " + uri);

    Matcher matcher = OPEN_COURSE_PATTERN.matcher(uri);
    if (matcher.matches()) {
      int courseId;
      int stepId;
      String link = getStringParameter(StepikNames.LINK, urlDecoder);

      if (link == null) {
        return log("The link parameter was not found");
      }

      LOG.info("Try to open a course: " + link);

      QueryStringDecoder linkDecoder = new QueryStringDecoder(link);

      matcher = COURSE_PATTERN.matcher(linkDecoder.path());

      if (!matcher.matches()) {
        return log("Unrecognized the link parameter");
      }

      int lessonId;
      int stepIndex;
      try {
        lessonId = Integer.parseInt(matcher.group(1));
        stepIndex = Integer.parseInt(matcher.group(2));
      } catch (NumberFormatException e) {
        return log("Unrecognized the link");
      }

      int unitId = getIntParameter("unit", linkDecoder);

      if (unitId == -1) {
        return log("Unrecognized the Unit id");
      }

      StepikWrappers.Unit unit = StepikConnector.getUnit(unitId);
      if (unit.getId() == 0) {
        return log("Unrecognized the Unit id");
      }

      Section section = StepikNewConnector.INSTANCE.getSection(unit.getSection());
      if (section == null) {
        return log("No section found with id " + unit.getSection());
      }
      courseId = section.getCourseId();
      if (courseId == 0) {
        return log("Unrecognized the course id");
      }
      Lesson lesson = StepikNewConnector.INSTANCE.getLesson(lessonId);
      if (lesson == null) {
        return log("No lesson found with id " + lessonId);
      }
      List<Integer> stepIds = lesson.steps;

      if (stepIds.isEmpty()) {
        return log("Unrecognized the step id");
      }
      stepId = stepIds.get(stepIndex - 1);

      LOG.info(String.format("Try to open a course: courseId=%s, stepId=%s", courseId, stepId));

      if (focusOpenEduProject(courseId, stepId) || openRecentEduCourse(courseId, stepId) || createEduCourse(courseId, stepId)) {
        RestService.sendOk(request, context);
        LOG.info("Course opened: " + courseId);
        return null;
      }

      RestService.sendStatus(HttpResponseStatus.NOT_FOUND, false, context.channel());
      String message = "A project didn't found or created";
      LOG.info(message);
      return message;
    }

    Matcher codeMatcher = OAUTH_CODE_PATTERN.matcher(uri);
    if (codeMatcher.matches()) {
      String code = getStringParameter("code", urlDecoder);
      if (code != null) {
        final boolean success = StepikAuthorizedClient.login(code, StepikConnector.getOAuthRedirectUrl());
        final StepikUser user = EduSettings.getInstance().getUser();
        if (success && user != null) {
          EduSettings.getInstance().setUser(user);
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

  private static void focusOnApplicationWindow() {
    JFrame frame = WindowManager.getInstance().findVisibleFrame();
    ApplicationManager.getApplication().invokeLater(() -> {
      AppIcon.getInstance().requestFocus((IdeFrame)frame);
      frame.toFront();
    });
  }

  private static void showStepikNotification(@NotNull NotificationType notificationType, @NotNull String text) {
    Notification notification = new Notification("Stepik", "Stepik", text, notificationType);
    notification.notify(null);
  }
}
