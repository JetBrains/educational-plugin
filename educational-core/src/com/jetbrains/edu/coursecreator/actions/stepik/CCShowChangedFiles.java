package com.jetbrains.edu.coursecreator.actions.stepik;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CCShowChangedFiles extends DumbAwareAction {

  public CCShowChangedFiles() {
    super("Show Changed Files", "Show changed files", null);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    Project project = event.getProject();
    if (project == null) {
      return;
    }

    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (!(course instanceof RemoteCourse)) {
      return;
    }

    StringBuilder message = new StringBuilder();
    if (course.getStepikChangeStatus() != StepikChangeStatus.UP_TO_DATE) {
      message
        .append("course : ")
        .append(course.getStepikChangeStatus())
        .append("\n");
    }

    message.append("\t");
    for (StudyItem item : course.getItems()) {
      if (item.getStepikChangeStatus() != StepikChangeStatus.UP_TO_DATE) {
        message.append(item.getName());
        message.append(" ");
        message.append(item.getStepikChangeStatus());
        message.append("\n");
      }

      if (item instanceof Section) {
        for (Lesson lesson : ((Section)item).getLessons()) {
          if (lesson.getStepikChangeStatus() != StepikChangeStatus.UP_TO_DATE) {
            appendLessonLine(item.getName(), lesson, message);
          }
          for (Task task : lesson.taskList) {
            if (task.getStepikChangeStatus() != StepikChangeStatus.UP_TO_DATE) {
              appendTaskLine(message, (Section)item, lesson, task);
            }
          }
        }
      }

      if (item instanceof Lesson) {
        for (Task task : ((Lesson)item).taskList) {
          message.append(task.getName());
          if (task.getStepikChangeStatus() != StepikChangeStatus.UP_TO_DATE) {
            appendTaskLine(message, null, (Lesson)item, task);
          }
        }
      }
    }

    Messages.showInfoMessage(message.toString(), "Course Changes");
  }

  private void appendTaskLine(@NotNull StringBuilder message, @Nullable Section section, @NotNull Lesson lesson, @NotNull Task task) {
    if (section != null) {
      message.append(section.getName());
      message.append("/");
    }
    message.append(lesson.getName());
    message.append("/");
    message.append(task.getName());
    message.append(" ");
    message.append(task.getStepikChangeStatus());
    message.append("\n");
  }

  private static void appendLessonLine(@NotNull String parentName,
                                       @NotNull Lesson lesson, @NotNull StringBuilder message) {
    message.append(parentName);
    message.append("/");
    message.append(lesson.getName());
    message.append(" ");
    message.append(lesson.getStepikChangeStatus());
    message.append("\n");
  }

  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(false);
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course instanceof RemoteCourse && !course.isStudy()) {
      e.getPresentation().setEnabledAndVisible(true);
    }
    else {
      e.getPresentation().setEnabledAndVisible(false);
    }
  }
}
