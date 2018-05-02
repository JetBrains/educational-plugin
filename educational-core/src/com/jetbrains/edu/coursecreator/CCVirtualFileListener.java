package com.jetbrains.edu.coursecreator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.jetbrains.edu.learning.*;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.Section;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;

public class CCVirtualFileListener implements VirtualFileListener {
  private final Project myProject;

  public CCVirtualFileListener(Project project) {
    myProject = project;
  }

  @Override
  public void fileCreated(@NotNull VirtualFileEvent event) {
    VirtualFile createdFile = event.getFile();
    if (createdFile.isDirectory()) {
      return;
    }
    if (createdFile.getPath().contains(CCUtils.GENERATED_FILES_FOLDER)) {
      return;
    }
    if (myProject.getBasePath() !=null && !FileUtil.isAncestor(myProject.getBasePath(), createdFile.getPath(), true)) {
      return;
    }
    Course course = StudyTaskManager.getInstance(myProject).getCourse();
    if (course == null) {
      return;
    }
    TaskFile taskFile = EduUtils.getTaskFile(myProject, createdFile);
    if (taskFile != null) {
      return;
    }

    String taskRelativePath = EduUtils.pathRelativeToTask(myProject, createdFile);

    EduConfigurator configurator = EduConfiguratorManager.forLanguage(course.getLanguageById());
    if (configurator != null && configurator.excludeFromArchive(createdFile.getPath())) {
      return;
    }

    if (EduUtils.isTestsFile(myProject, createdFile)
        || EduUtils.isTaskDescriptionFile(createdFile.getName())
        || taskRelativePath.contains(EduNames.WINDOW_POSTFIX)
        || taskRelativePath.contains(EduNames.WINDOWS_POSTFIX)
        || taskRelativePath.contains(EduNames.ANSWERS_POSTFIX)) {
      return;
    }
    Task task = EduUtils.getTaskForFile(myProject, createdFile);
    if (task == null) {
      return;
    }

    if (!insideTaskFileDirectory(course, createdFile)) {
      return;
    }

    task.addTaskFile(taskRelativePath);
  }

  @Override
  public void fileDeleted(@NotNull VirtualFileEvent event) {
    VirtualFile removedFile = event.getFile();
    String path = removedFile.getPath();
    if (path.contains(CCUtils.GENERATED_FILES_FOLDER)) {
      return;
    }

    if (myProject == null) {
      return;
    }
    final VirtualFile courseDir = EduUtils.getCourseDir(myProject);
    if (!FileUtil.isAncestor(courseDir.getPath(), removedFile.getPath(), true)) {
      return;
    }
    Course course = StudyTaskManager.getInstance(myProject).getCourse();
    if (course == null) {
      return;
    }
    final TaskFile taskFile = EduUtils.getTaskFile(myProject, removedFile);
    if (taskFile != null) {
      deleteTaskFile(myProject, removedFile, taskFile);
      return;
    }
    final EduConfigurator<?> configurator = CourseExt.getConfigurator(course);
    if (EduUtils.isTaskDirectory(myProject, removedFile)) {
      deleteTask(course, removedFile);
      if (configurator != null) {
        ApplicationManager.getApplication().invokeLater(() -> configurator.getCourseBuilder().refreshProject(myProject));
      }
    }
    if (EduUtils.getLesson(removedFile, course) != null) {
      deleteLesson(course, removedFile);
    }
    if (course.getSection(removedFile.getName()) != null) {
      deleteSection(course, removedFile);
    }
  }

  private static void deleteLesson(@NotNull final Course course, @NotNull final VirtualFile removedLessonFile) {
    Lesson removedLesson = EduUtils.getLesson(removedLessonFile, course);
    if (removedLesson == null) {
      return;
    }
    final Section section = removedLesson.getSection();
    final VirtualFile parentDir = removedLessonFile.getParent();
    if (section != null) {
      CCUtils.updateHigherElements(parentDir.getChildren(), file -> section.getLesson(file.getName()), removedLesson.getIndex(), -1);
      section.removeLesson(removedLesson);
    }
    else {
      CCUtils.updateHigherElements(parentDir.getChildren(), file -> course.getItem(file.getName()), removedLesson.getIndex(), -1);
      course.removeLesson(removedLesson);
    }
  }

  private static void deleteSection(@NotNull final Course course, @NotNull final VirtualFile removedFile) {
    Section removedSection = course.getSection(removedFile.getName());
    if (removedSection == null || !removedSection.getItems().isEmpty()) {
      return;
    }
    final VirtualFile parentDir = removedFile.getParent();
    CCUtils.updateHigherElements(parentDir.getChildren(), file -> course.getItem(file.getName()), removedSection.getIndex(), -1);
    course.removeSection(removedSection);
  }

  private static void deleteTask(@NotNull final Course course, @NotNull final VirtualFile removedTask) {
    VirtualFile lessonDir = removedTask.getParent();
    assert lessonDir != null;
    Lesson lesson = EduUtils.getLesson(lessonDir, course);
    assert lesson != null;
    Task task = lesson.getTask(removedTask.getName());
    if (task == null) {
      return;
    }
    CCUtils.updateHigherElements(lessonDir.getChildren(), file -> lesson.getTask(file.getName()), task.getIndex(), -1);
    lesson.getTaskList().remove(task);
  }

  private static void deleteTaskFile(@NotNull Project project, @NotNull final VirtualFile removedTaskFile, @NotNull TaskFile taskFile) {
    Task task = taskFile.getTask();
    if (task == null) {
      return;
    }
    task.getTaskFiles().remove(EduUtils.pathRelativeToTask(project, removedTaskFile));
  }

  private static boolean insideTaskFileDirectory(@NotNull Course course, @NotNull VirtualFile createdFile) {
    String sourceDir = CourseExt.getSourceDir(course);
    if (sourceDir == null || sourceDir.isEmpty()) {
      return true;
    }

    VirtualFile taskDir = EduUtils.getTaskDir(course, createdFile);
    if (taskDir == null) {
      return false;
    }
    String relativePath = FileUtil.getRelativePath(taskDir.getPath(), createdFile.getPath(), VfsUtilCore.VFS_SEPARATOR_CHAR);
    if (relativePath != null && relativePath.startsWith(sourceDir)) {
      return true;
    }

    return false;
  }
}
