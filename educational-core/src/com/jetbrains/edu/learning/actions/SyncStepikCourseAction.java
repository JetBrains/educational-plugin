package com.jetbrains.edu.learning.actions;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.EducationalCoreIcons;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.EduCourse;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector;
import com.jetbrains.edu.learning.stepik.CourseUpdateInfo;
import com.jetbrains.edu.learning.stepik.StepikCourseUpdater;
import com.jetbrains.edu.learning.stepik.StepikSolutionsLoader;
import com.jetbrains.edu.learning.stepik.StepikUpdateDateExt;
import com.jetbrains.edu.learning.stepik.hyperskill.StepikUpdateChecker;
import com.jetbrains.edu.learning.stepik.submissions.SubmissionsManager;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.coursecreator.CCNotificationUtils.showNotification;

@SuppressWarnings("ComponentNotRegistered")
public class SyncStepikCourseAction extends SyncCourseAction {

  @NonNls
  public static final String ACTION_ID = "Educational.Stepik.UpdateCourse";

  public SyncStepikCourseAction() {
    super(EduCoreBundle.lazyMessage("action.synchronize.course"),
          EduCoreBundle.lazyMessage("action.synchronize.course"),
          EducationalCoreIcons.StepikRefresh);
  }

  @Override
  public void synchronizeCourse(@NotNull Project project) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    assert course instanceof EduCourse;
    ProgressManager.getInstance().run(new Task.Backgroundable(project, EduCoreBundle.message("stepik.updating.course"), true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        doSynchronizeCourse(project, (EduCourse)course, indicator);
      }
    });

    EduCounterUsageCollector.synchronizeCourse(course, EduCounterUsageCollector.SynchronizeCoursePlace.WIDGET);
  }

  private void doSynchronizeCourse(@NotNull Project project, @NotNull EduCourse course, @NotNull ProgressIndicator indicator) {
    updateCourseStructure(project, course);
    StepikUpdateChecker.getInstance(project).queueNextCheck();
    SubmissionsManager submissionsManager = SubmissionsManager.getInstance(project);
    if (submissionsManager.submissionsSupported()) {
      submissionsManager.getSubmissions(StreamEx.of(CourseExt.getAllTasks(course)).map(task -> task.getId()).toSet());
      StepikSolutionsLoader.getInstance(project).loadSolutions(course, indicator);
    }
  }

  public void updateCourseStructure(@NotNull Project project, @NotNull EduCourse course) {
    CourseUpdateInfo info = StepikUpdateDateExt.checkIsStepikUpToDate(course);
    boolean isUpToDate = info.isUpToDate();
    if (!isUpToDate) {
      new StepikCourseUpdater(project, course).updateCourse(info.getRemoteCourseInfo());
    }
    else {
      showNotification(project, EduCoreBundle.message("notification.course.up.to.date"), null);
    }
  }

  @Override
  public boolean isAvailable(@NotNull Project project) {
    if (!EduUtils.isEduProject(project)) {
      return false;
    }

    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course instanceof EduCourse && ((EduCourse)course).isStepikRemote()) {
      return true;
    }
    return false;
  }
}
