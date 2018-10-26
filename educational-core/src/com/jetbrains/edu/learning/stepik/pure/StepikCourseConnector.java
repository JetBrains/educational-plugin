package com.jetbrains.edu.learning.stepik.pure;

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.stepik.StepikLanguages;
import com.jetbrains.edu.learning.stepik.StepikUser;
import com.jetbrains.edu.learning.stepik.StepikWrappers;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.jetbrains.edu.learning.stepik.StepikConnector.*;

public class StepikCourseConnector {
  private static final Logger LOG = Logger.getInstance(StepikCourseConnector.class.getName());

  public static int getCourseIdFromLink(@NotNull String link) {
    try {
      URL url = new URL(link);
      String[] pathParts = url.getPath().split("/");
      for (int i = 0; i < pathParts.length; i++) {
        String part = pathParts[i];
        if (part.equals("course") && i + 1 < pathParts.length) {
          return Integer.parseInt(pathParts[i + 1]);
        }
      }
    }
    catch (MalformedURLException | NumberFormatException e) {
      LOG.warn(e.getMessage());
    }
    return -1;
  }

  public static StepikCourse getCourseInfoByLink(@NotNull StepikUser user, @NotNull String link) {
    int courseId;
    try {
      courseId = Integer.parseInt(link);
    }
    catch (NumberFormatException e) {
      courseId = getCourseIdFromLink(link);
    }
    if (courseId != -1) {
      RemoteCourse info = getCourseInfo(user, courseId, false);
      return StepikCourse.fromRemote(info);
    }
    return null;
  }

  public static List<Language> getSupportedLanguages(StepikCourse remoteCourse) {
    List<Language> languages = new ArrayList<>();
    try {
      Map<String, String> codeTemplates = getFirstCodeTemplates(remoteCourse);
      for (String languageName : codeTemplates.keySet()) {
        String id = StepikLanguages.langOfName(languageName).getId();
        Language language = Language.findLanguageByID(id);
        if (language != null) {
          languages.add(language);
        }
      }
    }
    catch (IOException e) {
      LOG.warn(e.getMessage());
    }

    return languages;
  }

  @NotNull
  private static Map<String, String> getFirstCodeTemplates(@NotNull StepikCourse remoteCourse) throws IOException {
    String[] unitsIds = getUnitsIds(remoteCourse);
    List<Lesson> lessons = getLessons(unitsIds);
    for (Lesson lesson : lessons) {
      String[] stepIds = lesson.steps.stream().map(stepId -> String.valueOf(stepId)).toArray(String[]::new);
      List<StepikWrappers.StepSource> allStepSources = getStepSources(stepIds, remoteCourse.getLanguageID());

      for (StepikWrappers.StepSource stepSource : allStepSources) {
        StepikWrappers.Step step = stepSource.block;
        if (step != null && step.name.equals("code") && step.options != null) {
          Map<String, String> codeTemplates = step.options.codeTemplates;
          if (codeTemplates != null) {
            return codeTemplates;
          }
        }
      }
    }

    return Collections.emptyMap();
  }

}
