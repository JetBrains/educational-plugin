package com.jetbrains.edu.learning.stepik;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
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
import com.intellij.ui.content.Content;
import com.intellij.util.messages.MessageBusConnection;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduLogInListener;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.EduCourse;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask;
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector;
import com.jetbrains.edu.learning.taskDescription.ui.AdditionalTabPanel;
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionToolWindowFactory;
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import static com.jetbrains.edu.learning.EduUtils.isEduProject;
import static com.jetbrains.edu.learning.EduUtils.navigateToStep;
import static com.jetbrains.edu.learning.configuration.EduConfiguratorWithSubmissions.SUBMISSIONS_TAB_NAME;

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
          if (EduSettings.getInstance().getUser() != null) {
            prepareSubmissionsContent(course);
          }
          else {
            MessageBusConnection busConnection = myProject.getMessageBus().connect(myProject);
            busConnection.subscribe(EduSettings.SETTINGS_CHANGED, new EduLogInListener() {
              @Override
              public void userLoggedIn() {
                if (EduSettings.getInstance().getUser() == null) {
                  return;
                }
                prepareSubmissionsContent(course);
              }

              @Override
              public void userLoggedOut() { }
            });
          }
          StepikUtils.updateCourseIfNeeded(myProject, (EduCourse)course);

          final StepikUser currentUser = EduSettings.getInstance().getUser();
          if (currentUser == null) {
            showBalloon();
          }
          if (currentUser != null && !course.getAuthors().contains(currentUser.userInfo) && !CCUtils.isCourseCreator(myProject)) {
            loadSolutionsFromStepik(course);
            loadSubmissionsFromStepik(course);
          }
          selectStep(course);
        }
      }
    );
  }

  private void prepareSubmissionsContent(@NotNull Course course) {
    ToolWindow window = ToolWindowManager.getInstance(myProject).getToolWindow(
      TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW);
    if (window != null) {
      Content submissionsContent = window.getContentManager().findContent(SUBMISSIONS_TAB_NAME);
      if (submissionsContent != null) {
        JComponent submissionsPanel = submissionsContent.getComponent();
        if (submissionsPanel instanceof AdditionalTabPanel) {
          ApplicationManager.getApplication().invokeLater(() -> ((AdditionalTabPanel)submissionsPanel).addLoadingPanel());
        }
      }
    }
    loadSubmissionsFromStepik(course);
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

  private void loadSubmissionsFromStepik(@NotNull Course course) {
    if (course instanceof EduCourse && ((EduCourse)course).isRemote() && EduSettings.isLoggedIn()) {
      ApplicationManager.getApplication().executeOnPooledThread(() -> {
        List<Task> allTasks = CourseExt.getAllTasks(course);
        for (Task task : allTasks) {
          if (task instanceof CodeTask || task instanceof ChoiceTask || task instanceof EduTask) {
            SubmissionsManager.getAllSubmissions(task.getId());
          }
        }
        ApplicationManager.getApplication().invokeLater(() -> TaskDescriptionView.getInstance(myProject).updateAdditionalTaskTab());
      });
    }
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
