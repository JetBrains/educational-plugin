package com.jetbrains.edu.learning.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.EduCourse;
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector;
import com.jetbrains.edu.learning.stepik.StepikCourseUpdater;
import com.jetbrains.edu.learning.stepik.StepikSolutionsLoader;
import com.jetbrains.edu.learning.stepik.StepikUpdateDateExt;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.jetbrains.edu.learning.EduUtils.showNotification;

public class SyncStepikCourseAction extends SyncCourseAction {

  public SyncStepikCourseAction() {
    super("Synchronize Course", "Synchronize Course", EducationalCoreIcons.StepikRefresh);
  }

  @Override
  public void synchronizeCourse(@NotNull Project project) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    assert course != null;
    if (course instanceof EduCourse && ((EduCourse)course).isRemote()) {
      ProgressManager.getInstance().run(new Task.Backgroundable(project, "Updating Course", true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);

          if (StepikUpdateDateExt.isUpToDate((EduCourse)course)) {
            ApplicationManager.getApplication().invokeLater(() -> showNotification(project, "Course is up to date", null));
          }
          else {
            new StepikCourseUpdater((EduCourse)course, project).updateCourse();
          }
        }
      });
    }

    if (CCUtils.isCourseCreator(project)) {
      return;
    }

    StepikSolutionsLoader courseSynchronizer = StepikSolutionsLoader.getInstance(project);
    courseSynchronizer.loadSolutionsInBackground();
    EduCounterUsageCollector.synchronizeCourse(EduCounterUsageCollector.SynchronizeCoursePlace.WIDGET);
  }

  @Override
  public boolean isAvailable(@Nullable Project project) {
    if (project == null) {
      return false;
    }

    if (!EduUtils.isEduProject(project)) {
      return false;
    }

    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course instanceof EduCourse && ((EduCourse)course).isRemote()) {
      return true;
    }
    return false;
  }
}
