package com.jetbrains.edu.coursecreator.actions.stepik;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.actions.CCChangeCourseInfo;
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector;
import com.jetbrains.edu.coursecreator.ui.CCEditCourseInfoDialog;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import com.jetbrains.edu.learning.stepik.UserSettingObserver;
import com.jetbrains.edu.learning.stepik.StepikConnector;
import org.jetbrains.annotations.NotNull;

public class CCPushCourse extends DumbAwareAction {
  public CCPushCourse() {
    super("&Upload Course to Stepik", "Upload Course to Stepik", null);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    Project project = e.getProject();
    presentation.setEnabledAndVisible(project != null && CCUtils.isCourseCreator(project));
    if (project != null) {
      final Course course = StudyTaskManager.getInstance(project).getCourse();
      if (course instanceof RemoteCourse) {
        presentation.setText("Update Course on Stepik");
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
    if (course instanceof RemoteCourse) {
      updateCourseWithProgress(project, (RemoteCourse)course);
    }
    else {
      boolean isLoggedIn = EduSettings.getInstance().getUser() != null;
      AnAction action = ActionManager.getInstance().getAction(CCChangeCourseInfo.ACTION_ID);
      ApplicationManager.getApplication().invokeAndWait(() -> {
        CCEditCourseInfoDialog dialog = ((CCChangeCourseInfo)action).createDialog(project, course, "Course");
        dialog.showAuthor(false);
        String okButtonText = isLoggedIn ? "Upload" : "Log In and Upload";
        dialog.setOkButtonText(okButtonText);
        if (!dialog.showAndApply()) {
          return;
        }
        if (!isLoggedIn) {
          new UserSettingObserver(() -> CCStepikConnector.postCourseWithProgress(project, course)).observe();
          StepikConnector.doAuthorize(EduUtils::showOAuthDialog);
        }
        else {
          CCStepikConnector.postCourseWithProgress(project, course);
        }
      });
    }
    EduUsagesCollector.courseUploaded();
  }

  private static void updateCourseWithProgress(Project project, RemoteCourse course) {
    ProgressManager.getInstance().run(new Task.Modal(project, "Updating Course", true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(false);
        CCStepikConnector.updateCourse(project, course);
        CCStepikConnector.showNotification(project, "Course updated");
      }
    });
  }
}