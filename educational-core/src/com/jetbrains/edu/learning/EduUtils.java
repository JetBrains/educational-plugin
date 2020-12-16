package com.jetbrains.edu.learning;

import com.intellij.ide.SaveAndSyncHandler;
import com.intellij.ide.lightEdit.LightEdit;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
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
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.util.ObjectUtils;
import com.intellij.util.PlatformUtils;
import com.intellij.util.TimeoutUtil;
import com.intellij.util.io.zip.JBZipEntry;
import com.intellij.util.io.zip.JBZipFile;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.edu.coursecreator.settings.CCSettings;
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector;
import com.jetbrains.edu.learning.configuration.EduConfigurator;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import com.jetbrains.edu.learning.projectView.ProgressUtil;
import com.jetbrains.edu.learning.stepik.OAuthDialog;
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse;
import com.jetbrains.edu.learning.taskDescription.TaskDescriptionUtil;
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView;
import org.intellij.markdown.ast.ASTNode;
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor;
import org.intellij.markdown.html.HtmlGenerator;
import org.intellij.markdown.parser.MarkdownParser;
import org.jetbrains.annotations.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;

public class EduUtils {

  private EduUtils() {
  }

  public static final Comparator<StudyItem> INDEX_COMPARATOR = Comparator.comparingInt(StudyItem::getIndex);
  private static final Logger LOG = Logger.getInstance(EduUtils.class.getName());

  public static boolean isZip(String fileName) {
    return StringUtil.endsWithIgnoreCase(fileName, ".zip");
  }

  @Nullable
  public static <T> T getFirst(@NotNull final Iterable<T> container) {
    Iterator<T> iterator = container.iterator();
    if (!iterator.hasNext()) {
      return null;
    }
    return iterator.next();
  }

  public static boolean indexIsValid(int index, @NotNull final Collection<?> collection) {
    int size = collection.size();
    return index >= 0 && index < size;
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

  public static void deleteFile(@Nullable final VirtualFile file) {
    if (file == null) {
      return;
    }
    try {
      file.delete(EduUtils.class);
    }
    catch (IOException e) {
      LOG.error(e);
    }
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
    if (task == null || task.getLesson() == null) {
      return null;
    }
    Lesson lesson = task.getLesson();
    VirtualFile lessonDir = lesson.getDir(OpenApiExtKt.getCourseDir(project));
    if (lessonDir == null) {
      return null;
    }
    VirtualFile taskDirectory = lesson instanceof FrameworkLesson ? lessonDir.findChild(task.getName())
                                                                  : task.getDir(OpenApiExtKt.getCourseDir(project));
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
    String taskDescription = ObjectUtils.chooseNotNull(getTextFromTaskTextFile(taskTextFile), task.getTaskDescription());
    if (taskTextFile != null && EduNames.TASK_MD.equals(taskTextFile.getName()) || task.getDescriptionFormat() == DescriptionFormat.MD) {
      return convertToHtml(taskDescription);
    }
    return taskDescription;
  }

  @Nullable
  private static VirtualFile getTaskTextFile(@NotNull VirtualFile taskDirectory) {
    VirtualFile taskTextFile = taskDirectory.findChild(EduNames.TASK_HTML);
    if (taskTextFile == null) {
      taskTextFile = taskDirectory.findChild(EduNames.TASK_MD);
    }
    return taskTextFile;
  }

  @Nullable
  private static String getTextFromTaskTextFile(@Nullable VirtualFile taskTextFile) {
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
  public static String getCourseModeForNewlyCreatedProject(@NotNull Project project) {
    if (project.isDefault() || LightEdit.owns(project)) return null;
    VirtualFile baseDir = OpenApiExtKt.getCourseDir(project);
    return baseDir.getUserData(CourseProjectGenerator.COURSE_MODE_TO_CREATE);
  }

  public static boolean isStudentProject(@NotNull Project project) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course != null && course.isStudy()) {
      return true;
    }
    return EduNames.STUDY.equals(getCourseModeForNewlyCreatedProject(project));
  }

  public static boolean hasJavaFx() {
    try {
      Class.forName("javafx.application.Platform");
      return true;
    }
    catch (ClassNotFoundException e) {
      return false;
    }
  }

  public static boolean hasJCEF() {
    return JBCefApp.isSupported();
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

  public static String convertToHtml(@NotNull final String content) {
    // markdown parser is supposed to work with normalized text from document
    String normalizedContent = StringUtil.convertLineSeparators(content);

    return generateMarkdownHtml(normalizedContent);
  }

  @NotNull
  private static String generateMarkdownHtml(@NotNull String text) {
    GFMFlavourDescriptor flavour = new GFMFlavourDescriptor();
    final ASTNode parsedTree = new MarkdownParser(flavour).buildMarkdownTreeFromString(text);

    return new HtmlGenerator(text, parsedTree, flavour, false).generateHtml();
  }

  public static boolean isTaskDescriptionFile(@NotNull final String fileName) {
    return EduNames.TASK_HTML.equals(fileName) || EduNames.TASK_MD.equals(fileName);
  }

  @Nullable
  public static VirtualFile findTaskFileInDir(@NotNull TaskFile taskFile, @NotNull VirtualFile taskDir) {
    return taskDir.findFileByRelativePath(taskFile.getName());
  }

  @NotNull
  public static DescriptionFormat getDefaultTaskDescriptionFormat() {
    return CCSettings.getInstance().useHtmlAsDefaultTaskFormat() ? DescriptionFormat.HTML : DescriptionFormat.MD;
  }

  @Nullable
  public static Document getDocument(@NotNull Project project, int lessonIndex, int taskIndex, String fileName) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) return null;
    Lesson lesson = course.getLessons().get(lessonIndex - 1);
    Task task = lesson.getTaskList().get(taskIndex - 1);
    @SystemIndependent String basePath = project.getBasePath();
    if (basePath == null) return null;
    String filePath = FileUtil.join(basePath, lesson.getName(), task.getName(), fileName);

    VirtualFile taskFile = LocalFileSystem.getInstance().findFileByPath(filePath);
    return taskFile == null ? null : FileDocumentManager.getInstance().getDocument(taskFile);
  }

  public static Pair<Integer, Integer> getPlaceholderOffsets(@NotNull final AnswerPlaceholder answerPlaceholder) {
    int startOffset = answerPlaceholder.getOffset();
    final int endOffset = answerPlaceholder.getEndOffset();
    return Pair.create(startOffset, endOffset);
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

  public static void navigateToStep(@NotNull Project project, @NotNull Course course, int stepId) {
    if (stepId == 0) {
      return;
    }
    Task task = getTask(course, stepId);
    if (task != null) {
      NavigationUtils.navigateToTask(project, task);
    }
  }

  @Nullable
  private static Task getTask(@NotNull Course course, int stepId) {
    Ref<Task> taskRef = new Ref<>();
    course.visitLessons((lesson) -> {
      Task task = lesson.getTask(stepId);
      if (task != null) {
        taskRef.set(task);
      }
    });
    return taskRef.get();
  }

  public static void showOAuthDialog() {
    OAuthDialog dialog = new OAuthDialog();
    dialog.show();
  }

  public static void runUndoableAction(Project project,
                                       @Nls(capitalization = Nls.Capitalization.Title) String name,
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

  public static boolean isAndroidStudio() {
    return "AndroidStudio".equals(PlatformUtils.getPlatformPrefix());
  }

  public static void runUndoableAction(Project project,
                                       @Nls(capitalization = Nls.Capitalization.Title) String name,
                                       UndoableAction action) {
    runUndoableAction(project, name, action, UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION);
  }

  static void deleteWindowsFile(@NotNull final VirtualFile taskDir, @NotNull final String name) {
    final VirtualFile fileWindows = taskDir.findChild(name);
    if (fileWindows != null && fileWindows.exists()) {
      ApplicationManager.getApplication().runWriteAction(() -> {
        try {
          fileWindows.delete(taskDir);
        }
        catch (IOException e) {
          LOG.warn("Tried to delete non existed _windows file");
        }
      });
    }
  }

  public static void deleteWindowDescriptions(@NotNull final Task task, @NotNull final VirtualFile taskDir) {
    for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
      VirtualFile virtualFile = findTaskFileInDir(entry.getValue(), taskDir);
      if (virtualFile == null) {
        continue;
      }
      String windowsFileName = virtualFile.getNameWithoutExtension() + EduNames.WINDOWS_POSTFIX;
      VirtualFile parentDir = virtualFile.getParent();
      deleteWindowsFile(parentDir, windowsFileName);
    }
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

  @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
  @Nullable
  public static VirtualFile flushWindows(@NotNull final TaskFile taskFile, @NotNull final VirtualFile file) {
    final VirtualFile taskDir = file.getParent();
    VirtualFile fileWindows = null;
    final Document document = FileDocumentManager.getInstance().getDocument(file);
    if (document == null) {
      LOG.debug("Couldn't flush windows");
      return null;
    }
    if (taskDir != null) {
      final String name = file.getNameWithoutExtension() + EduNames.WINDOWS_POSTFIX;
      deleteWindowsFile(taskDir, name);
      PrintWriter printWriter = null;
      try {
        fileWindows = taskDir.createChildData(taskFile, name);
        printWriter = new PrintWriter(new FileOutputStream(fileWindows.getPath()));
        for (AnswerPlaceholder answerPlaceholder : taskFile.getAnswerPlaceholders()) {
          int start = answerPlaceholder.getOffset();
          int end = answerPlaceholder.getEndOffset();
          final String windowDescription = document.getText(new TextRange(start, end));
          printWriter.println("#educational_plugin_window = " + windowDescription);
        }
        ApplicationManager.getApplication().runWriteAction(() -> FileDocumentManager.getInstance().saveDocument(document));
      }
      catch (IOException e) {
        LOG.error(e);
      }
      finally {
        if (printWriter != null) {
          printWriter.close();
        }
        synchronize();
      }
    }
    return fileWindows;
  }

  @Nullable
  public static Course getLocalCourse(@NotNull final String zipFilePath) {
    return getLocalCourse(zipFilePath, false);
  }

  @Nullable
  public static Course getLocalEncryptedCourse(@NotNull final String zipFilePath) {
    return getLocalCourse(zipFilePath, true);
  }

  @Nullable
  private static Course getLocalCourse(@NotNull final String zipFilePath, boolean isEncrypted) {
    try {
      final JBZipFile zipFile = new JBZipFile(zipFilePath);
      try {
        final JBZipEntry entry = zipFile.getEntry(EduNames.COURSE_META_FILE);
        if (entry == null) {
          zipFile.close();
          return null;
        }
        byte[] bytes = entry.getData();
        final String jsonText = new String(bytes, CharsetToolkit.UTF8_CHARSET);
        return CourseArchiveReader.readCourseJson(jsonText, isEncrypted);
      }
      finally {
        zipFile.close();
      }
    }
    catch (IOException e) {
      LOG.error("Failed to unzip course archive", e);
    }
    return null;
  }

  public static void showNotification(@NotNull Project project,
                                      @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String title,
                                      @Nullable AnAction action) {
    final Notification notification = new Notification(CCStepikConnector.PUSH_COURSE_GROUP_ID, title, "", NotificationType.INFORMATION);
    if (action != null) {
      notification.addAction(action);
    }
    notification.notify(project);
  }

  public static String addMnemonic(String text) {
    if (text.length() == 0) return text;
    return addMnemonic(text, text.charAt(0));
  }

  public static String addMnemonic(String text, char ch) {
    int index = text.indexOf(ch);
    if (index == -1) return text;
    return text.substring(0, index) + "&" + text.substring(index);
  }

  public static void putSelectedTaskFileFirst(List<TaskFile> taskFiles, TaskFile selectedTaskFile) {
    int selectedTaskFileIndex = taskFiles.indexOf(selectedTaskFile);
    if (selectedTaskFileIndex > 0) {
      Collections.swap(taskFiles, 0, selectedTaskFileIndex);
    }
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

  public static boolean isNewlyCreated(@NotNull Project project) {
    Boolean userData = project.getUserData(CourseProjectGenerator.EDU_PROJECT_CREATED);
    return userData != null && userData;
  }
}
