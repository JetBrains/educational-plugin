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
import com.jetbrains.edu.learning.stepik.courseFormat.StepikChangeStatus;
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse;
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

      if (isNew(item)) {
        appendChangeLine(item, builder, "New");
      }

      if (item instanceof Section) {
        for (Lesson lesson : ((Section)item).getLessons()) {
          if (lesson.getStepikChangeStatus() != StepikChangeStatus.UP_TO_DATE) {
            appendChangeLine(lesson, builder);
          }

          // all tasks of new lesson are new
          if (isNew(lesson)) {
            appendChangeLine(lesson, builder, "New");
            continue;
          }

          for (Task task : lesson.taskList) {
            if (task.getStepikChangeStatus() != StepikChangeStatus.UP_TO_DATE) {
              appendChangeLine(task, builder);
            }
            if (isNew(task)) {
              appendChangeLine(task, builder, "New");
            }
          }
        }
      }

      if (item instanceof Lesson) {
        // all tasks of new lesson are new
        if (isNew(item)) {
          continue;
        }

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

  /**
   * Check if current item is recently added and isn't on Stepik. We have to do it as
   * we don't have "New" StepikChangeStatus
   */
  private static boolean isNew(@NotNull StudyItem item) {
    return item.getId() == 0;
  }

  private static void appendChangeLine(@NotNull StudyItem item, @NotNull StringBuilder stringBuilder) {
    appendChangeLine(item, stringBuilder, item.getStepikChangeStatus().toString());
  }

  private static void appendChangeLine(@NotNull StudyItem item, @NotNull StringBuilder stringBuilder, @NotNull String status) {
    stringBuilder
      .append(getPath(item))
      .append(" ")
      .append(status)
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
    if (course instanceof StepikCourse && !course.isStudy()) {
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
    return parentsLine + (parentsLine.isEmpty() ? "" : "/") + item.getName();
  }
}
