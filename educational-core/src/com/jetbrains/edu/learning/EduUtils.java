package com.jetbrains.edu.learning;

import com.intellij.ide.SaveAndSyncHandler;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
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
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.testFramework.LightVirtualFile;
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
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.editor.EduEditor;
import com.jetbrains.edu.learning.exceptions.BrokenPlaceholderException;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import com.jetbrains.edu.learning.projectView.CourseViewPane;
import com.jetbrains.edu.learning.stepik.OAuthDialog;
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse;
import com.jetbrains.edu.learning.taskDescription.TaskDescriptionUtil;
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView;
import com.jetbrains.edu.learning.twitter.TwitterPluginConfigurator;
import kotlin.Unit;
import org.apache.commons.codec.binary.Base64;
import org.intellij.markdown.IElementType;
import org.intellij.markdown.ast.ASTNode;
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor;
import org.intellij.markdown.html.GeneratingProvider;
import org.intellij.markdown.html.HtmlGenerator;
import org.intellij.markdown.parser.LinkMap;
import org.intellij.markdown.parser.MarkdownParser;
import org.jetbrains.annotations.*;

import javax.imageio.ImageIO;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

public class EduUtils {

  private EduUtils() {
  }

  public static final Comparator<StudyItem> INDEX_COMPARATOR = Comparator.comparingInt(StudyItem::getIndex);
  private static final Logger LOG = Logger.getInstance(EduUtils.class.getName());

  public static void closeSilently(@Nullable final Closeable stream) {
    if (stream != null) {
      try {
        stream.close();
      }
      catch (IOException e) {
        // close silently
      }
    }
  }

  public static boolean isZip(String fileName) {
    return fileName.contains(".zip");
  }

  @Nullable
  public static <T> T getFirst(@NotNull final Iterable<T> container) {
    Iterator<T> iterator = container.iterator();
    if (!iterator.hasNext()) {
      return null;
    }
    return iterator.next();
  }

  public static boolean indexIsValid(int index, @NotNull final Collection collection) {
    int size = collection.size();
    return index >= 0 && index < size;
  }

  @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
  @Nullable
  public static String getFileText(@Nullable final String parentDir, @NotNull final String fileName, boolean wrapHTML,
                                   @NotNull final String encoding) {
    final File inputFile = parentDir != null ? new File(parentDir, fileName) : new File(fileName);
    if (!inputFile.exists()) return null;
    final StringBuilder taskText = new StringBuilder();
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), encoding));
      String line;
      while ((line = reader.readLine()) != null) {
        taskText.append(line).append("\n");
        if (wrapHTML) {
          taskText.append("<br>");
        }
      }
      return wrapHTML ? UIUtil.toHtml(taskText.toString()) : taskText.toString();
    }
    catch (IOException e) {
      LOG.info("Failed to get file text from file " + fileName, e);
    }
    finally {
      closeSilently(reader);
    }
    return null;
  }

  public static void updateAction(@NotNull final AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    presentation.setEnabled(false);
    final Project project = e.getProject();
    if (project != null) {
      final EduEditor eduEditor = getSelectedEduEditor(project);
      if (eduEditor != null) {
        presentation.setEnabledAndVisible(true);
      }
    }
  }

  public static void updateToolWindows(@NotNull final Project project) {
    TaskDescriptionView.getInstance(project).updateTaskDescription();
    updateCourseProgress(project);
  }

  public static void updateCourseProgress(@NotNull Project project) {
    final AbstractProjectViewPane pane = ProjectView.getInstance(project).getCurrentProjectViewPane();
    if (pane instanceof CourseViewPane && isStudentProject(project) && !ApplicationManager.getApplication().isUnitTestMode()) {
      ((CourseViewPane)pane).updateCourseProgress();
    }
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

  /**
   * @return true, if file doesn't belong to task (in term of course structure)
   * but can be added to it as task, test or additional file.
   * Otherwise, returns false
   */
  public static boolean canBeAddedToTask(@NotNull Project project, @NotNull VirtualFile file) {
    if (file.isDirectory()) return false;
    Task task = getTaskForFile(project, file);
    if (task == null) return false;
    EduConfigurator<?> configurator = CourseExt.getConfigurator(task.getCourse());
    if (configurator == null) return false;
    if (configurator.excludeFromArchive(project, file)) return false;
    return !belongToTask(project, file);
  }

  /**
   * @return true, if some task contains given {@code file} as task, test or additional file.
   * Otherwise, returns false
   */
  public static boolean belongToTask(@NotNull Project project, @NotNull VirtualFile file) {
    Task task = getTaskForFile(project, file);
    if (task == null) return false;
    String relativePath = pathRelativeToTask(project, file);
    return task.getTaskFile(relativePath) != null;
  }

  public static boolean isTestsFile(@NotNull Project project, @NotNull VirtualFile file) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return false;
    }
    EduConfigurator configurator = CourseExt.getConfigurator(course);
    if (configurator == null) {
      return false;
    }
    return configurator.isTestFile(project, file);
  }

  @Nullable
  public static TaskFile getTaskFile(@NotNull final Project project, @NotNull final VirtualFile file) {
    Task task = getTaskForFile(project, file);
    return task == null ? null : task.getTaskFile(pathRelativeToTask(project, file));
  }

  @Nullable
  public static EduEditor getSelectedEduEditor(@NotNull final Project project) {
    try {
      final FileEditor fileEditor = FileEditorManagerEx.getInstanceEx(project)
        .getSplitters()
        .getCurrentWindow()
        .getSelectedEditor()
        .getSelectedWithProvider()
        .getFileEditor();
      if (fileEditor instanceof EduEditor) {
        return (EduEditor)fileEditor;
      }
    }
    catch (Exception e) {
      return null;
    }
    return null;
  }

  @Nullable
  public static Editor getSelectedEditor(@NotNull final Project project) {
    final EduEditor eduEditor = getSelectedEduEditor(project);
    if (eduEditor != null) {
      return eduEditor.getEditor();
    }
    return null;
  }

  @Nullable
  public static String getTaskTextFromTask(@NotNull final Project project, @Nullable final Task task) {
    if (task == null || task.getLesson() == null) {
      return null;
    }
    Lesson lesson = task.getLesson();
    VirtualFile lessonDir = lesson.getDir(project);
    if (lessonDir == null) {
      return null;
    }
    VirtualFile taskDirectory = lesson instanceof FrameworkLesson ? lessonDir.findChild(task.getName()) : task.getDir(project);
    String text = getTaskTextByTaskName(task, taskDirectory);
    if (text == null) {
      LOG.warn("Cannot find task description file for a task: " + task.getName());
      return null;
    }
    text = StringUtil.replace(text, "%IDE_NAME%", ApplicationNamesInfo.getInstance().getFullProductName());
    if (lesson instanceof FrameworkLesson) {
      text = "<h2>" + task.getUIName() + " #" + task.getIndex() + ": " + task.getName() + "<h2/> " + text;
    }
    StringBuffer textBuffer = new StringBuffer(text);
    TaskDescriptionUtil.replaceActionIDsWithShortcuts(textBuffer);
    if (task.getCourse() instanceof HyperskillCourse) {
      TaskDescriptionUtil.removeHyperskillTags(textBuffer);
    }
    textBuffer.append(TaskExt.taskDescriptionHintBlocks(task));
    return textBuffer.toString();
  }

  @Nullable
  private static String getTaskTextByTaskName(@NotNull Task task, @Nullable VirtualFile taskDirectory) {
    if (taskDirectory == null) return null;

    VirtualFile taskTextFile = getTaskTextFile(taskDirectory);
    String taskDescription = ObjectUtils.chooseNotNull(getTextFromTaskTextFile(taskTextFile), task.getTaskDescription());
    if (taskTextFile != null && EduNames.TASK_MD.equals(taskTextFile.getName()) || task.getDescriptionFormat() == DescriptionFormat.MD) {
      return convertToHtml(taskDescription, taskDirectory);
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
  public static TwitterPluginConfigurator getTwitterConfigurator(@NotNull final Project project) {
    TwitterPluginConfigurator[] extensions = TwitterPluginConfigurator.EP_NAME.getExtensions();
    for (TwitterPluginConfigurator extension : extensions) {
      if (extension.accept(project)) {
        return extension;
      }
    }
    return null;
  }

  @Nullable
  public static Task getCurrentTask(@NotNull final Project project) {
    VirtualFile[] files = FileEditorManager.getInstance(project).getSelectedFiles();
    for (VirtualFile file : files) {
      Task task = getTaskForFile(project, file);
      if (task != null) return task;
    }
    return null;
  }

  public static boolean isEduProject(@NotNull Project project) {
    return StudyTaskManager.getInstance(project).getCourse() != null || getCourseModeForNewlyCreatedProject(project) != null;
  }

  @Nullable
  public static String getCourseModeForNewlyCreatedProject(@NotNull Project project) {
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

  @Nullable
  public static VirtualFile getTaskDir(Project project, @NotNull Course course, @NotNull VirtualFile taskFile) {
    VirtualFile file = taskFile.getParent();
    while (file != null) {
      VirtualFile lessonDirCandidate = file.getParent();
      if (lessonDirCandidate == null) {
        return null;
      }
      Lesson lesson = getLesson(project, course, lessonDirCandidate);
      if (lesson != null) {
        if (lesson instanceof FrameworkLesson && EduNames.TASK.equals(file.getName()) ||
            lesson.getTask(file.getName()) != null) {
          return file;
        }
      }

      file = lessonDirCandidate;
    }
    return null;
  }

  @Nullable
  public static Task getTaskForFile(@NotNull Project project, @NotNull VirtualFile file) {
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return null;
    }
    VirtualFile taskDir = getTaskDir(project, course, file);
    if (taskDir == null) {
      return null;
    }
    final VirtualFile lessonDir = taskDir.getParent();
    if (lessonDir != null) {
      final Lesson lesson = getLesson(project, course, lessonDir);
      if (lesson == null) {
        return null;
      }

      if (lesson instanceof FrameworkLesson && course.isStudy()) {
        return ((FrameworkLesson)lesson).currentTask();
      }
      else {
        return lesson.getTask(taskDir.getName());
      }
    }
    return null;
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

  @Nullable
  public static String convertToHtml(@Nullable final String content, @NotNull VirtualFile taskDirectory) {
    if (content == null) return null;

    return generateMarkdownHtml(taskDirectory, content);
  }

  @NotNull
  public static String generateMarkdownHtml(@NotNull VirtualFile parent, @NotNull String text) {
    final URI baseUri = new File(parent.getPath()).toURI();

    GFMFlavourDescriptor flavour = new GFMFlavourDescriptor();
    final ASTNode parsedTree = new MarkdownParser(flavour).buildMarkdownTreeFromString(text);
    final Map<IElementType, GeneratingProvider> htmlGeneratingProviders =
      flavour.createHtmlGeneratingProviders(LinkMap.Builder.buildLinkMap(parsedTree, text), baseUri);

    return new HtmlGenerator(text, parsedTree, htmlGeneratingProviders, true).generateHtml();
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

  public static void selectFirstAnswerPlaceholder(@Nullable final EduEditor eduEditor, @NotNull final Project project) {
    if (eduEditor == null) return;
    final Editor editor = eduEditor.getEditor();
    IdeFocusManager.getInstance(project).requestFocus(editor.getContentComponent(), true);
    if (!eduEditor.getTaskFile().isValid(editor.getDocument().getText())) return;
    final List<AnswerPlaceholder> placeholders = eduEditor.getTaskFile().getAnswerPlaceholders();
    final AnswerPlaceholder placeholder = placeholders.stream().filter(p -> p.isVisible()).findFirst().orElse(null);
    if (placeholder == null) return;
    Pair<Integer, Integer> offsets = getPlaceholderOffsets(placeholder);
    editor.getSelectionModel().setSelection(offsets.first, offsets.second);
    editor.getCaretModel().moveToOffset(offsets.first);
    editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
  }

  @NotNull
  public static String pathRelativeToTask(@NotNull Project project, @NotNull VirtualFile file) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) return file.getName();

    VirtualFile taskDir = getTaskDir(project, course, file);
    if (taskDir == null) return file.getName();

    String fullRelativePath = FileUtil.getRelativePath(taskDir.getPath(), file.getPath(), VfsUtilCore.VFS_SEPARATOR_CHAR);
    if (fullRelativePath == null) return file.getName();
    return fullRelativePath;
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
    return null;
  }

  public static void showOAuthDialog() {
    OAuthDialog dialog = new OAuthDialog();
    dialog.show();
  }

  private static final String CONVERT_ERROR = "Failed to convert answer file to student one: ";

  @Nullable
  public static TaskFile createStudentFile(@NotNull Project project, @NotNull VirtualFile answerFile, @NotNull final Task task) {
    try {
      Task taskCopy = task.copy();

      TaskFile taskFile = taskCopy.getTaskFile(pathRelativeToTask(project, answerFile));
      if (taskFile == null) {
        return null;
      }
      if (isImage(taskFile.getName())) {
        taskFile.setText(Base64.encodeBase64String(answerFile.contentsToByteArray()));
        return taskFile;
      }
      Document document = FileDocumentManager.getInstance().getDocument(answerFile);
      if (document == null) {
        return null;
      }
      FileDocumentManager.getInstance().saveDocument(document);
      final LightVirtualFile studentFile = new LightVirtualFile("student_task", PlainTextFileType.INSTANCE, document.getText());
      EduDocumentListener.runWithListener(project, taskFile, studentFile, (studentDocument) -> {
        for (AnswerPlaceholder placeholder : taskFile.getAnswerPlaceholders()) {
          try {
            placeholder.setPossibleAnswer(studentDocument.getText(TextRange.create(placeholder.getOffset(), placeholder.getEndOffset())));
            replaceAnswerPlaceholder(studentDocument, placeholder);
          }
          catch (IndexOutOfBoundsException e) {
            // We are here because placeholder is broken. We need to put broken placeholder into exception.
            // We need to take it from original task, because taskCopy has issues with links (taskCopy.lesson is always null)
            TaskFile file = task.getTaskFile(taskFile.getName());
            AnswerPlaceholder answerPlaceholder = file != null ? file.getAnswerPlaceholder(placeholder.getOffset()) : null;
            throw new BrokenPlaceholderException(CONVERT_ERROR + answerFile.getPath(),
                                                 answerPlaceholder != null ? answerPlaceholder : placeholder);
          }
        }
        taskFile.setText(studentDocument.getImmutableCharSequence().toString());
        return Unit.INSTANCE;
      });
      return taskFile;
    }
    catch (IOException e) {
      LOG.error(CONVERT_ERROR + answerFile.getPath());
    }
    return null;
  }

  public static void runUndoableAction(Project project, String name, UndoableAction action, UndoConfirmationPolicy confirmationPolicy) {
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

  public static void runUndoableAction(Project project, String name, UndoableAction action) {
    runUndoableAction(project, name, action, UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION);
  }

  public static boolean isImage(String fileName) {
    final String[] readerFormatNames = ImageIO.getReaderFormatNames();
    for (@NonNls String format : readerFormatNames) {
      final String ext = format.toLowerCase();
      if (fileName.endsWith(ext)) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  public static Task getTask(@NotNull Project project, @NotNull Course course, @NotNull VirtualFile taskDir) {
    VirtualFile lessonDir = taskDir.getParent();
    if (lessonDir == null) {
      return null;
    }
    Lesson lesson = getLesson(project, course, lessonDir);
    if (lesson == null) {
      return null;
    }

    return lesson.getTask(taskDir.getName());
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
        return CourseArchiveReader.readCourseJson(jsonText);
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

  public static boolean isTaskDirectory(@NotNull Project project, @NotNull VirtualFile virtualFile) {
    if (!virtualFile.isDirectory()) {
      return false;
    }
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return false;
    }
    VirtualFile lessonDirCandidate = virtualFile.getParent();
    if (lessonDirCandidate == null) {
      return false;
    }
    Lesson lesson = getLesson(project, course, lessonDirCandidate);
    if (lesson == null) {
      return false;
    }
    return lesson.getTask(virtualFile.getName()) != null;
  }

  public static boolean isLessonDirectory(@NotNull Project project, @NotNull VirtualFile virtualFile) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return false;
    }
    return getLesson(project, course, virtualFile) != null;
  }

  @Nullable
  public static Lesson getLesson(@NotNull Project project, @NotNull final Course course, @NotNull VirtualFile lessonDir) {
    if (!lessonDir.isDirectory()) {
      return null;
    }
    VirtualFile lessonParent = lessonDir.getParent();
    if (lessonParent == null) {
      return null;
    }
    final Section section = getSection(project, course, lessonParent);
    if (section != null) {
      return section.getLesson(lessonDir.getName());
    }

    VirtualFile courseDir = course.getDir(project);
    if (courseDir.equals(lessonParent)) {
      return course.getLesson(lessonDir.getName());
    }

    return null;
  }

  @Nullable
  public static Section getSection(@NotNull Project project, @NotNull Course course, @NotNull VirtualFile sectionDir) {
    if (!sectionDir.isDirectory()) return null;

    VirtualFile courseDir = course.getDir(project);
    VirtualFile sectionParentDir = sectionDir.getParent();
    if (courseDir.equals(sectionParentDir)) {
      return course.getSection(sectionDir.getName());
    }

    return null;
  }

  public static boolean isSectionDirectory(@NotNull Project project, @NotNull VirtualFile file) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return false;
    }
    return getSection(project, course, file) != null;
  }

  public static void showNotification(@NotNull Project project, @NotNull String title, @Nullable AnAction action) {
    final Notification notification = new Notification(CCStepikConnector.PUSH_COURSE_GROUP_ID, title, "", NotificationType.INFORMATION);
    if (action != null) {
      notification.addAction(action);
    }
    notification.notify(project);
  }

  @Nullable
  public static StudyItem getStudyItem(@NotNull Project project, @NotNull VirtualFile dir) {
    Course course = OpenApiExtKt.getCourse(project);
    if (course == null) return null;

    VirtualFile courseDir = OpenApiExtKt.getCourseDir(project);
    if (courseDir.equals(dir)) return course;

    Section section = getSection(project, course, dir);
    if (section != null) return section;

    Lesson lesson = getLesson(project, course, dir);
    if (lesson != null) return lesson;

    Task task = getTask(project, course, dir);
    if (task != null) return task;

    return null;
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
}
