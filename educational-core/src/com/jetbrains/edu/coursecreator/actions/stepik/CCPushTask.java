package com.jetbrains.edu.coursecreator.actions.stepik;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Modal;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.courseFormat.StepikChangeStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.stepik.StepikNames;
import org.jetbrains.annotations.NotNull;

public class CCPushTask extends DumbAwareAction {
  public CCPushTask() {
    super("Update Task on Stepik", "Update Task on Stepik", null);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(false);
    final IdeView view = e.getData(LangDataKeys.IDE_VIEW);
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (view == null || project == null) {
      return;
    }
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (!(course instanceof RemoteCourse)) {
      return;
    }
    if (!course.getCourseMode().equals(CCUtils.COURSE_MODE)) return;
    final PsiDirectory[] directories = view.getDirectories();
    if (directories.length == 0 || directories.length > 1) {
      return;
    }
    final PsiDirectory taskDir = directories[0];
    if (taskDir == null) {
      return;
    }
    final PsiDirectory lessonDir = taskDir.getParentDirectory();
    if (lessonDir == null) {
      return;
    }
    Lesson lesson = CCUtils.lessonFromDir(course, lessonDir, project);
    if (lesson != null && lesson.getId() > 0 && course.getId() > 0) {
      e.getPresentation().setEnabledAndVisible(true);
      final Task task = lesson.getTask(taskDir.getName());
      if (task != null && task.getStepId() <= 0) {
        e.getPresentation().setText("Upload Task to Stepik");
      }
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final IdeView view = e.getData(LangDataKeys.IDE_VIEW);
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (view == null || project == null) {
      return;
    }
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }
    final PsiDirectory[] directories = view.getDirectories();
    if (directories.length == 0 || directories.length > 1) {
      return;
    }

    final PsiDirectory taskDir = directories[0];

    if (taskDir == null) {
      return;
    }

    final PsiDirectory lessonDir = taskDir.getParentDirectory();
    if (lessonDir == null) return;

    Lesson lesson = CCUtils.lessonFromDir(course, lessonDir, project);
    if (lesson == null) return;

    final Task task = lesson.getTask(taskDir.getName());
    if (task == null) return;

    ProgressManager.getInstance().run(new Modal(project, "Uploading Task", true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setText("Uploading task to " + StepikNames.STEPIK_URL);
        if (task.getStepId() <= 0) {
          boolean isPosted = CCStepikConnector.postTask(project, task, lesson.getId());
          if (isPosted) {
            CCStepikConnector.showNotification(project, "Task " + task.getName() + " uploaded",
                                               CCStepikConnector
                                                 .openOnStepikAction("/lesson/" + task.getLesson().getId() + "/step/" + task.getIndex())
            );
          }
          else {
            CCStepikConnector.showErrorNotification(project, "Error uploading task", "Task " + task.getName() + "wasn't uploaded");
          }
        }
        else {
          boolean isPosted = CCStepikConnector.updateTask(project, task);
          task.setStepikChangeStatus(StepikChangeStatus.UP_TO_DATE);
          if (isPosted) {
            int lessonId = task.getLesson().getId();
            CCStepikConnector.showNotification(project, "Task " + task.getName() + " updated",
                                               CCStepikConnector.openOnStepikAction("/lesson/" + lessonId + "/step/" + task.getIndex())
            );
          }
          else {
            CCStepikConnector.showErrorNotification(project, "Error updating task", "Task " + task.getName() + "wasn't updated");
          }
        }
      }
    });
  }
}