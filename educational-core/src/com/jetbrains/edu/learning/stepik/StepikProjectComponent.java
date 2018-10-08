package com.jetbrains.edu.learning.stepik;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.remote.RemoteInfo;
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse;
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourseRemoteInfo;
import com.jetbrains.edu.learning.stepik.courseFormat.ext.StepikCourseExt;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.learning.EduUtils.getVisibleWidget;
import static com.jetbrains.edu.learning.EduUtils.isStudyProject;
import static com.jetbrains.edu.learning.stepik.StepikNames.STEP_ID;

public class StepikProjectComponent implements ProjectComponent {
  private static final Logger LOG = Logger.getInstance(StepikProjectComponent.class.getName());
  private final Project myProject;

  private StepikProjectComponent(@NotNull final Project project) {
    myProject = project;
  }

  @Override
  public void projectOpened() {
    if (myProject.isDisposed() || !isStudyProject(myProject)) {
      return;
    }
    StartupManager.getInstance(myProject).runWhenProjectIsInitialized(
      () -> {
        Course course = StudyTaskManager.getInstance(myProject).getCourse();
        if (course instanceof StepikCourse) {
          if (!StepikCourseExt.isAdaptive((StepikCourse)course)) {
            StepikConnector.updateCourseIfNeeded(myProject, (StepikCourse)course);
          }

          final StepicUser currentUser = EduSettings.getInstance().getUser();
          if (currentUser != null && !course.getAuthors().contains(currentUser) && !CCUtils.isCourseCreator(myProject)) {
            loadSolutionsFromStepik(course);
          }
          selectStep((StepikCourse)course);
        }
      }
    );
    addStepikWidget();
  }

  private void loadSolutionsFromStepik(@NotNull Course course) {
    final RemoteInfo remoteInfo = course.getRemoteInfo();
    if (!(course instanceof StepikCourse) || remoteInfo instanceof StepikCourseRemoteInfo && !((StepikCourseRemoteInfo)remoteInfo).getLoadSolutions()) {
      return;
    }
    if (PropertiesComponent.getInstance(myProject).getBoolean(StepikNames.ARE_SOLUTIONS_UPDATED_PROPERTY)) {
      PropertiesComponent.getInstance(myProject).setValue(StepikNames.ARE_SOLUTIONS_UPDATED_PROPERTY, false);
      return;
    }
    try {
      StepikSolutionsLoader.getInstance(myProject).loadSolutionsInBackground();
    }
    catch (Exception e) {
      LOG.warn(e.getMessage());
    }
  }

  private void addStepikWidget() {
    StepikUserWidget widget = getVisibleWidget(myProject);
    StatusBar statusBar = WindowManager.getInstance().getStatusBar(myProject);
    if (widget != null) {
      statusBar.removeWidget(StepikUserWidget.ID);
    }
    statusBar.addWidget(new StepikUserWidget(myProject), "before Position");
  }

  private void selectStep(@NotNull StepikCourse course) {
    int stepId = PropertiesComponent.getInstance().getInt(STEP_ID, 0);
    if (stepId != 0) {
      StepikUtils.navigateToStep(myProject, course, stepId);
    }
  }

  @NotNull
  @Override
  public String getComponentName() {
    return "StepikProjectComponent";
  }

}
