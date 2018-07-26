package com.jetbrains.edu.coursecreator.actions.stepik;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;
import org.jsoup.helper.StringUtil;

import java.util.ArrayList;
import java.util.Collections;

public class CCShowChangedFiles extends DumbAwareAction {

  public CCShowChangedFiles() {
    super("Compare with Course on Stepik", "Show changed files comparing to the course on Stepik", null);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    Project project = event.getProject();
    if (project == null) {
      return;
    }

    Course course = StudyTaskManager.getInstance(project).getCourse();
    assert course != null;

    String message = buildChangeMessage(course);
    Messages.showInfoMessage(message, course.getName() + " Comparing to Stepik");
  }

  @VisibleForTesting
  @NotNull
  public static String buildChangeMessage(@NotNull Course course) {
    StringBuilder builder = new StringBuilder();
    if (course.getStepikChangeStatus() != StepikChangeStatus.UP_TO_DATE) {
      appendChangeLine(course, builder);
    }

    for (StudyItem item : course.getItems()) {
      if (item.getStepikChangeStatus() != StepikChangeStatus.UP_TO_DATE) {
        appendChangeLine(item, builder);
      }

      if (item instanceof Section) {
        for (Lesson lesson : ((Section)item).getLessons()) {
          if (lesson.getStepikChangeStatus() != StepikChangeStatus.UP_TO_DATE) {
            appendChangeLine(lesson, builder);
          }
          for (Task task : lesson.taskList) {
            if (task.getStepikChangeStatus() != StepikChangeStatus.UP_TO_DATE) {
              appendChangeLine(task, builder);
            }
          }
        }
      }

      if (item instanceof Lesson) {
        for (Task task : ((Lesson)item).taskList) {
          if (task.getStepikChangeStatus() != StepikChangeStatus.UP_TO_DATE) {
            appendChangeLine(task, builder);
          }
        }
      }
    }
    String message = builder.toString();
    if (message.isEmpty()) {
      return "No changes";
    }
    return message;
  }

  private static void appendChangeLine(@NotNull StudyItem item, @NotNull StringBuilder stringBuilder) {
    stringBuilder
      .append(getPath(item))
      .append(" ")
      .append(item.getStepikChangeStatus())
      .append("\n");
  }

  @Override
  public void update(AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(false);
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course instanceof RemoteCourse && !course.isStudy()) {
      presentation.setEnabledAndVisible(true);
    }
  }

  private static String getPath(@NotNull StudyItem item) {
    ArrayList<String> parents = new ArrayList<>();
    StudyItem parent = item.getParent();
    while (!(parent instanceof Course)) {
      parents.add(parent.getName());
      parent = parent.getParent();
    }
    Collections.reverse(parents);

    String parentsLine = StringUtil.join(parents, "/");
    return parentsLine + "/" + item.getName();
  }
}
