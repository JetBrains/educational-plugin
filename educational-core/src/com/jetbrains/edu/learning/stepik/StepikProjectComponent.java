package com.jetbrains.edu.learning.stepik;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.*;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.EduCourse;
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.jetbrains.edu.learning.EduUtils.isEduProject;
import static com.jetbrains.edu.learning.EduUtils.navigateToStep;

@SuppressWarnings("ComponentNotRegistered") // educational-core.xml
public class StepikProjectComponent implements ProjectComponent {
  private static final Logger LOG = Logger.getInstance(StepikProjectComponent.class.getName());

  public static final Key<Integer> STEP_ID = Key.create("STEP_ID");
  private final Project myProject;

  private StepikProjectComponent(@NotNull final Project project) {
    myProject = project;
  }

  @Override
  public void projectOpened() {
    if (myProject.isDisposed() || !isEduProject(myProject)) {
      return;
    }

    StartupManager.getInstance(myProject).runWhenProjectIsInitialized(
      () -> {
        Course course = StudyTaskManager.getInstance(myProject).getCourse();
        if (course instanceof EduCourse && ((EduCourse)course).isRemote()) {
          StepikUtils.updateCourseIfNeeded(myProject, (EduCourse)course);

          final StepikUser currentUser = EduSettings.getInstance().getUser();
          if (currentUser == null) {
            showBalloon();
          }
          if (currentUser != null && !course.getAuthors().contains(currentUser.userInfo) && !CCUtils.isCourseCreator(myProject)) {
            loadSolutionsFromStepik(course);
          }
          selectStep(course);
        }
      }
    );
  }

  private void showBalloon() {
    IdeFrame frame = WindowManager.getInstance().getIdeFrame(myProject);
    if (frame == null) return;
    StatusBar statusBar = frame.getStatusBar();
    if (statusBar == null) return;
    StatusBarWidget widget = statusBar.getWidget(StepikWidget.ID);
    if (!(widget instanceof CustomStatusBarWidget)) return;
    CustomStatusBarWidget customWidget = (CustomStatusBarWidget)widget;
    JComponent widgetComponent = customWidget.getComponent();
    if (widgetComponent == null) return;
    String redirectUrl = StepikAuthorizer.getOAuthRedirectUrl();
    String authLnk = StepikAuthorizer.createOAuthLink(redirectUrl);
    BalloonBuilder builder =
      JBPopupFactory.getInstance()
        .createHtmlTextBalloonBuilder("<a href=\"\">Log in</a> to synchronize your study progress", MessageType.INFO, null);
    builder.setClickHandler(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        BrowserUtil.browse(authLnk);
      }
    }, true);
    builder.setHideOnClickOutside(true);
    builder.setCloseButtonEnabled(true);
    builder.setHideOnCloseClick(true);
    Balloon balloon = builder.createBalloon();
    balloon.showInCenterOf(widgetComponent);
  }

  private void loadSolutionsFromStepik(@NotNull Course course) {
    if (course instanceof EduCourse && ((EduCourse)course).isRemote()) {
      if (PropertiesComponent.getInstance(myProject).getBoolean(StepikNames.ARE_SOLUTIONS_UPDATED_PROPERTY)) {
        PropertiesComponent.getInstance(myProject).setValue(StepikNames.ARE_SOLUTIONS_UPDATED_PROPERTY, false);
        return;
      }
      try {
        StepikSolutionsLoader.getInstance(myProject).loadSolutionsInBackground();
        EduCounterUsageCollector.synchronizeCourse(EduCounterUsageCollector.SynchronizeCoursePlace.PROJECT_REOPEN);
      }
      catch (Exception e) {
        LOG.warn(e.getMessage());
      }
    }
  }

  private void selectStep(@NotNull Course course) {
    Integer stepId = course.getUserData(STEP_ID);
    if (stepId != null) {
      navigateToStep(myProject, course, stepId);
    }
  }

  @NotNull
  @Override
  public String getComponentName() {
    return "StepikProjectComponent";
  }
}
