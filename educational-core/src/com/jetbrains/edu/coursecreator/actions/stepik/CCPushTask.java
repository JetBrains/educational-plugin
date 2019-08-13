package com.jetbrains.edu.coursecreator.actions.stepik;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Modal;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.EduCourse;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.stepik.StepikNames;
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.*;
import static com.jetbrains.edu.learning.EduUtils.showNotification;

@SuppressWarnings("ComponentNotRegistered") // educational-core.xml
public class CCPushTask extends DumbAwareAction {
  public CCPushTask() {
    super("Update Task on Stepik", "Update Task on Stepik", null);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(false);
    final Project project = e.getData(CommonDataKeys.PROJECT);
    VirtualFile[] selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    if (project == null || selectedFiles == null || selectedFiles.length != 1) {
      return;
    }
    VirtualFile taskDir = selectedFiles[0];
    if (!taskDir.isDirectory()) {
      return;
    }

    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (!(course instanceof EduCourse) || !((EduCourse)course).isRemote()) {
      return;
    }
    if (!course.getCourseMode().equals(CCUtils.COURSE_MODE)) return;
    final VirtualFile lessonDir = taskDir.getParent();
    if (lessonDir == null) {
      return;
    }
    Lesson lesson = CCUtils.lessonFromDir(course, lessonDir, project);
    if (lesson != null && lesson.getId() > 0 && course.getId() > 0) {
      e.getPresentation().setEnabledAndVisible(true);
      final Task task = lesson.getTask(taskDir.getName());
      if (task != null && task.getId() <= 0) {
        e.getPresentation().setText("Upload Task to Stepik");
      }
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    VirtualFile[] selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    if (project == null || selectedFiles == null || selectedFiles.length != 1) {
      return;
    }
    VirtualFile taskDir = selectedFiles[0];
    if (!taskDir.isDirectory()) {
      return;
    }
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }
    final VirtualFile lessonDir = taskDir.getParent();
    if (lessonDir == null) return;

    Lesson lesson = CCUtils.lessonFromDir(course, lessonDir, project);
    if (lesson == null) return;

    final Task task = lesson.getTask(taskDir.getName());
    if (task == null) return;

    ProgressManager.getInstance().run(new Modal(project, "Uploading Task", true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setText("Uploading task to " + StepikNames.STEPIK_URL);
        if (task.getId() <= 0) {
          postNewTask(project, task, lesson);
        }
        else {
          updateTask(task, lesson, project);
        }
        YamlFormatSynchronizer.saveRemoteInfo(task);
      }
    });
  }

  private static void postNewTask(Project project, Task task, Lesson lesson) {
    int position = task.getIndex();
    if (!CCUtils.pushAvailable(lesson, task, project)) {
      return;
    }
    postTask(project, task, lesson, position);
  }

  private static void postTask(Project project, Task task, Lesson lesson, int position) {
    Task taskCopy = task.copy();
    taskCopy.setIndex(position);
    taskCopy.setLesson(lesson);
    boolean isPosted = CCStepikConnector.postTask(project, taskCopy, lesson.getId());
    if (isPosted) {
      task.setId(taskCopy.getId());
      task.setUpdateDate(taskCopy.getUpdateDate());
      showNotification(project, "Task " + task.getName() + " uploaded",
                       openOnStepikAction("/lesson/" + lesson.getId() + "/step/" + task.getIndex()));
    }
  }

  private static void updateTask(Task task, Lesson lesson, Project project) {
    int position = task.getIndex();
    int positionOnServer = getTaskPosition(task.getId());
    if (position != positionOnServer) {
      showErrorNotification(project, "Failed to update task",
                            "It's impossible to update one task since it's position changed. Please, use 'Update course' action.");
      return;
    }
    Task taskCopy = task.copy();
    taskCopy.setIndex(position);
    taskCopy.setLesson(task.getLesson());
    boolean updated = CCStepikConnector.updateTask(project, taskCopy);
    if (updated) {
      showNotification(project, "Task " + task.getName() + " updated",
                       openOnStepikAction("/lesson/" + lesson.getId() + "/step/" + task.getIndex()));
    }
  }
}