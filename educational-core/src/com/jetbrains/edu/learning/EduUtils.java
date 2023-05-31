package com.jetbrains.edu.learning;

import com.intellij.ide.SaveAndSyncHandler;
import com.intellij.ide.lightEdit.LightEdit;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.command.undo.UndoableAction;
import com.intellij.openapi.command.undo.UnexpectedUndoException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts.Command;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.ObjectUtils;
import com.intellij.util.PlatformUtils;
import com.intellij.util.TimeoutUtil;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.edu.learning.configuration.EduConfigurator;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.courseFormat.ext.StudyItemExtKt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import com.jetbrains.edu.learning.projectView.ProgressUtil;
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse;
import com.jetbrains.edu.learning.taskDescription.TaskDescriptionUtil;
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Iterator;
import java.util.concurrent.*;

import static com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_HTML;
import static com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_MD;

public class EduUtils {

  private EduUtils() {
  }

  private static final Logger LOG = Logger.getInstance(EduUtils.class.getName());

  @Nullable
  public static <T> T getFirst(@NotNull final Iterable<T> container) {
    Iterator<T> iterator = container.iterator();
    if (!iterator.hasNext()) {
      return null;
    }
    return iterator.next();
  }

  public static void updateAction(@NotNull final AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    presentation.setEnabled(false);
    final Project project = e.getProject();
    if (project != null) {
      TaskFile taskFile = OpenApiExtKt.getSelectedTaskFile(project);
      if (taskFile != null) {
        presentation.setEnabledAndVisible(true);
      }
    }
  }

  public static void updateToolWindows(@NotNull final Project project) {
    TaskDescriptionView.getInstance(project).updateTaskDescription();
    ProgressUtil.updateCourseProgress(project);
  }

  public static boolean isTestsFile(@NotNull Task task, @NotNull String path) {
    Course course = task.getCourse();
    EduConfigurator<?> configurator = CourseExt.getConfigurator(course);
    if (configurator == null) {
      return false;
    }
    return configurator.isTestFile(task, path);
  }

  @Nullable
  public static String getTaskTextFromTask(@NotNull final Project project, @Nullable final Task task) {
    if (task == null) {
      return null;
    }
    Lesson lesson = task.getLesson();
    VirtualFile lessonDir = StudyItemExtKt.getDir(lesson, OpenApiExtKt.getCourseDir(project));
    if (lessonDir == null) {
      return null;
    }
    VirtualFile taskDirectory = lesson instanceof FrameworkLesson ? lessonDir.findChild(task.getName())
                                                                  : StudyItemExtKt.getDir(task, OpenApiExtKt.getCourseDir(project));
    String text = getTaskTextByTaskName(task, taskDirectory);
    if (text == null) {
      LOG.warn("Cannot find task description file for a task: " + task.getName());
      return null;
    }
    text = StringUtil.replace(text, "%IDE_NAME%", ApplicationNamesInfo.getInstance().getFullProductName());
    if (lesson instanceof FrameworkLesson) {
      text = TaskDescriptionUtil.addHeader(task, lesson.getTaskList().size(), text);
    }
    StringBuffer textBuffer = new StringBuffer(text);
    TaskDescriptionUtil.replaceActionIDsWithShortcuts(textBuffer);
    if (task.getCourse() instanceof HyperskillCourse) {
      TaskDescriptionUtil.removeHyperskillTags(textBuffer);
    }
    return textBuffer.toString();
  }

  @Nullable
  private static String getTaskTextByTaskName(@NotNull Task task, @Nullable VirtualFile taskDirectory) {
    if (taskDirectory == null) return null;

    VirtualFile taskTextFile = getTaskTextFile(taskDirectory);
    String taskDescription = ObjectUtils.chooseNotNull(getTextFromTaskTextFile(taskTextFile), task.getDescriptionText());
    if (taskTextFile != null && TASK_MD.equals(taskTextFile.getName())) {
      return EduUtilsKt.convertToHtml(taskDescription);
    }
    return taskDescription;
  }

  @Nullable
  private static VirtualFile getTaskTextFile(@NotNull VirtualFile taskDirectory) {
    VirtualFile taskTextFile = taskDirectory.findChild(TASK_HTML);
    if (taskTextFile == null) {
      taskTextFile = taskDirectory.findChild(TASK_MD);
    }
    return taskTextFile;
  }

  @Nullable
  public static String getTextFromTaskTextFile(@Nullable VirtualFile taskTextFile) {
    if (taskTextFile != null) {
      Document document = FileDocumentManager.getInstance().getDocument(taskTextFile);
      return document == null ? null : document.getText();
    }
    return null;
  }

  @Nullable
  public static Task getCurrentTask(@NotNull final Project project) {
    VirtualFile[] files = FileEditorManager.getInstance(project).getSelectedFiles();
    for (VirtualFile file : files) {
      Task task = VirtualFileExt.getContainingTask(file, project);
      if (task != null) return task;
    }
    return null;
  }

  public static boolean isEduProject(@NotNull Project project) {
    return StudyTaskManager.getInstance(project).getCourse() != null || getCourseModeForNewlyCreatedProject(project) != null;
  }

  @Nullable
  public static CourseMode getCourseModeForNewlyCreatedProject(@NotNull Project project) {
    if (project.isDefault() || LightEdit.owns(project)) return null;
    VirtualFile baseDir = OpenApiExtKt.getCourseDir(project);
    return baseDir.getUserData(CourseProjectGenerator.COURSE_MODE_TO_CREATE);
  }

  public static boolean isStudentProject(@NotNull Project project) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course != null && course.isStudy()) {
      return true;
    }
    return CourseMode.STUDENT.equals(getCourseModeForNewlyCreatedProject(project));
  }

  // supposed to be called under progress
  @Nullable
  public static <T> T execCancelable(@NotNull final Callable<T> callable) {
    final Future<T> future = ApplicationManager.getApplication().executeOnPooledThread(callable);

    while (!future.isCancelled() && !future.isDone()) {
      ProgressManager.checkCanceled();
      TimeoutUtil.sleep(500);
    }
    T result = null;
    try {
      result = future.get();
    }
    catch (InterruptedException | ExecutionException e) {
      LOG.warn(e.getMessage());
    }
    return result;
  }

  public static boolean isTaskDescriptionFile(@NotNull final String fileName) {
    return TASK_HTML.equals(fileName) || TASK_MD.equals(fileName);
  }

  @Nullable
  public static VirtualFile findTaskFileInDir(@NotNull TaskFile taskFile, @NotNull VirtualFile taskDir) {
    return taskDir.findFileByRelativePath(taskFile.getName());
  }

  public static void openFirstTask(@NotNull final Course course, @NotNull final Project project) {
    final Task firstTask = getFirstTask(course);
    if (firstTask == null) return;
    NavigationUtils.navigateToTask(project, firstTask);
  }

  @Nullable
  public static Task getFirstTask(@NotNull final Course course) {
    LocalFileSystem.getInstance().refresh(false);
    final StudyItem firstItem = getFirst(course.getItems());
    if (firstItem == null) return null;
    final Lesson firstLesson;
    if (firstItem instanceof Section) {
      firstLesson = getFirst(((Section)firstItem).getLessons());
    }
    else {
      firstLesson = (Lesson)firstItem;
    }
    if (firstLesson == null) {
      return null;
    }
    return getFirst(firstLesson.getTaskList());
  }

  @Nullable
  public static Task getTask(@NotNull Course course, int stepId) {
    Ref<Task> taskRef = new Ref<>();
    course.visitLessons((lesson) -> {
      Task task = lesson.getTask(stepId);
      if (task != null) {
        taskRef.set(task);
      }
      return null;
    });
    return taskRef.get();
  }

  public static void runUndoableAction(Project project,
                                       @Command String name,
                                       UndoableAction action,
                                       UndoConfirmationPolicy confirmationPolicy) {
    try {
      WriteCommandAction.writeCommandAction(project)
        .withName(name)
        .withUndoConfirmationPolicy(confirmationPolicy)
        .run(() -> {
          action.redo();
          UndoManager.getInstance(project).undoableActionPerformed(action);
        });
    }
    catch (UnexpectedUndoException e) {
      LOG.error(e);
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  public static boolean isAndroidStudio() {
    return "AndroidStudio".equals(PlatformUtils.getPlatformPrefix());
  }

  public static void runUndoableAction(Project project,
                                       @Nls(capitalization = Nls.Capitalization.Title) String name,
                                       UndoableAction action) {
    runUndoableAction(project, name, action, UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION);
  }

  public static void replaceAnswerPlaceholder(@NotNull final Document document,
                                              @NotNull final AnswerPlaceholder answerPlaceholder) {
    CommandProcessor.getInstance().runUndoTransparentAction(() -> ApplicationManager.getApplication().runWriteAction(() -> {
      document.replaceString(answerPlaceholder.getOffset(), answerPlaceholder.getEndOffset(), answerPlaceholder.getPlaceholderText());
      FileDocumentManager.getInstance().saveDocument(document);
    }));
  }

  public static void synchronize() {
    FileDocumentManager.getInstance().saveAllDocuments();
    SaveAndSyncHandler.getInstance().refreshOpenFiles();
    VirtualFileManager.getInstance().refreshWithoutFileWatcher(true);
  }

  @TestOnly
  public static <T> void waitAndDispatchInvocationEvents(@NotNull Future<T> future) {
    if (!OpenApiExtKt.isUnitTestMode()) {
      LOG.error("`waitAndDispatchInvocationEvents` should be invoked only in unit tests");
    }
    while (true) {
      try {
        UIUtil.dispatchAllInvocationEvents();
        future.get(10, TimeUnit.MILLISECONDS);
        return;
      }
      catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      }
      catch (TimeoutException ignored) {
      }
    }
  }
}
