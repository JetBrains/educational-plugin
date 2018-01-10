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
package com.jetbrains.edu.learning.stepik;

import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.courseFormat.tasks.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StepikUtils {

  public static String wrapStepikTasks(Task task, @NotNull String text, boolean adaptive) {
    String finalText = text;
    if (task instanceof TheoryTask) {
      finalText += "<br/><br/><b>Note</b>: This theory task aims to help you solve difficult tasks. ";
    }
    else if (task instanceof CodeTask) {
      finalText += "<br/><br/><b>Note</b>: Use standard input to obtain input for the task.";
    }
    if (!(task instanceof EduTask) && !(task instanceof OutputTask)) {
      finalText += getFooterWithLink(task, adaptive);
    }

    return finalText;
  }

  @NotNull
  private static String getFooterWithLink(Task task, boolean adaptive) {
    final String link = adaptive ? getAdaptiveLink(task) : getLink(task, task.getStepikPosition());
    return "<div class=\"footer\">" + "<a href=" + link + ">Open on Stepik</a>" + "</div>";
  }

  @Nullable
  public static String getLink(@Nullable Task task, int stepNumber) {
    if (task == null) {
      return null;
    }
    Lesson lesson = task.getLesson();
    if (lesson == null || !(lesson.getCourse() instanceof RemoteCourse)) {
      return null;
    }

    return String.format("%s/lesson/%d/step/%d", StepikNames.STEPIK_URL, lesson.getId(), stepNumber);
  }

  @Nullable
  public static String getAdaptiveLink(@Nullable Task task) {
    String link = getLink(task, 1);
    return link == null ? null : link + "?adaptive=true";
  }

  public static void setCourseLanguage(RemoteCourse info) {
    String courseType = info.getType();
    final int separator = courseType.indexOf(" ");
    assert separator != -1;
    final String language = courseType.substring(separator + 1);
    info.setLanguage(language);
  }
}
