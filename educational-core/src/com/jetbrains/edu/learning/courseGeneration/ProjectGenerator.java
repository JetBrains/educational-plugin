package com.jetbrains.edu.learning.courseGeneration;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import com.jetbrains.edu.learning.stepic.StepicConnector;
import com.jetbrains.edu.learning.stepic.StepicNames;
import com.jetbrains.edu.learning.stepic.StepikSolutionsLoader;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.jetbrains.edu.learning.EduUtils.execCancelable;

public class ProjectGenerator {
  public void generateProject(@NotNull final Project project, @NotNull final VirtualFile baseDir,
                              @NotNull final Course courseInfo) {
    final Course course = initCourse(courseInfo, project);
    if (course.isAdaptive() && !EduUtils.isCourseValid(course)) {
      Messages.showWarningDialog("There is no recommended tasks for this adaptive course", "Error in Course Creation");
      return;
    }
    updateCourseFormat(course);
    StudyTaskManager.getInstance(project).setCourse(course);
    ApplicationManager.getApplication().runWriteAction(() -> {
      GeneratorUtils.createCourse(course, baseDir);
      EduUtils.openFirstTask(course, project);
      EduUsagesCollector.projectTypeCreated(course.isAdaptive() ? EduNames.ADAPTIVE : EduNames.STUDY);

      if (course instanceof RemoteCourse && EduSettings.getInstance().getUser() != null) {
        StepikSolutionsLoader stepikSolutionsLoader = StepikSolutionsLoader.getInstance(project);
        stepikSolutionsLoader.loadSolutions(ProgressIndicatorProvider.getGlobalProgressIndicator(), course);
        EduUsagesCollector.progressOnGenerateCourse();
        PropertiesComponent.getInstance(project).setValue(StepicNames.ARE_SOLUTIONS_UPDATED_PROPERTY, true, false);
      }
    });
  }

  protected static void updateCourseFormat(@NotNull final Course course) {
    final List<Lesson> lessons = course.getLessons(true);
    final Lesson additionalLesson = lessons.stream().
        filter(lesson -> StepicNames.PYCHARM_ADDITIONAL.equals(lesson.getName())).findFirst().orElse(null);
    if (additionalLesson != null) {
      additionalLesson.setName(EduNames.ADDITIONAL_MATERIALS);
      final List<Task> taskList = additionalLesson.getTaskList();
      taskList.stream().filter(task -> StepicNames.PYCHARM_ADDITIONAL.equals(task.getName())).findFirst().
          ifPresent(task -> task.setName(EduNames.ADDITIONAL_MATERIALS));
    }
  }

  @NotNull
  public Course initCourse(@NotNull final Course course, @NotNull final Project project) {
    if (course instanceof RemoteCourse) {
      return getCourseFromStepic(project, (RemoteCourse)course);
    }
    course.initCourse(false);
    return course;
  }

  private static RemoteCourse getCourseFromStepic(@NotNull Project project, RemoteCourse selectedCourse) {
    return ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
      return execCancelable(() -> {
        final RemoteCourse course = StepicConnector.getCourse(project, selectedCourse);
        if (EduUtils.isCourseValid(course)) {
          course.initCourse(false);
        }
        return course;
      });
    }, "Creating Course", true, project);
  }
}