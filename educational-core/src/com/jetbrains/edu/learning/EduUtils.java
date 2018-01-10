package com.jetbrains.edu.learning;

import com.google.common.collect.Lists;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.ide.SaveAndSyncHandler;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageExtensionPoint;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.command.undo.UndoableAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl;
import com.intellij.openapi.wm.*;
import com.intellij.openapi.wm.impl.IdeFrameImpl;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.content.Content;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.ZipUtil;
import com.intellij.util.io.zip.JBZipEntry;
import com.intellij.util.io.zip.JBZipFile;
import com.intellij.util.text.MarkdownUtil;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils;
import com.jetbrains.edu.learning.editor.EduEditor;
import com.jetbrains.edu.learning.handlers.AnswerPlaceholderDeleteHandler;
import com.jetbrains.edu.learning.stepik.OAuthDialog;
import com.jetbrains.edu.learning.stepik.StepicUser;
import com.jetbrains.edu.learning.stepik.StepikConnector;
import com.jetbrains.edu.learning.stepik.StepikUserWidget;
import com.jetbrains.edu.learning.twitter.TwitterPluginConfigurator;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindow;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindowFactory;
import com.petebevin.markdown.MarkdownProcessor;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.jetbrains.edu.learning.navigation.NavigationUtils.navigateToTask;

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
      final EduEditor eduEditor = getSelectedStudyEditor(project);
      if (eduEditor != null) {
        presentation.setEnabledAndVisible(true);
      }
    }
  }

  public static void updateToolWindows(@NotNull final Project project) {
    final TaskDescriptionToolWindow taskDescriptionToolWindow = getStudyToolWindow(project);
    if (taskDescriptionToolWindow != null) {
      Task task = getTaskForCurrentSelectedFile(project);
      taskDescriptionToolWindow.updateTask(project, task);
      taskDescriptionToolWindow.updateCourseProgress(project);
    }
  }

  public static void initToolWindow(@NotNull final Project project) {
    final ToolWindowManager windowManager = ToolWindowManager.getInstance(project);
    windowManager.getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW).getContentManager().removeAllContents(false);
    TaskDescriptionToolWindowFactory factory = new TaskDescriptionToolWindowFactory();
    factory.createToolWindowContent(project, windowManager.getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW));

  }

  @Nullable
  public static TaskDescriptionToolWindow getStudyToolWindow(@NotNull final Project project) {
    if (project.isDisposed()) return null;

    ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW);
    if (toolWindow != null) {
      Content[] contents = toolWindow.getContentManager().getContents();
      for (Content content: contents) {
        JComponent component = content.getComponent();
        if (component != null && component instanceof TaskDescriptionToolWindow) {
          return (TaskDescriptionToolWindow)component;
        }
      }
    }
    return null;
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
   * shows pop up in the center of "check task" button in study editor
   */
  public static void showCheckPopUp(@NotNull final Project project, @NotNull final Balloon balloon) {
    final EduEditor eduEditor = getSelectedStudyEditor(project);
    Editor editor = eduEditor != null ? eduEditor.getEditor() : FileEditorManager.getInstance(project).getSelectedTextEditor();
    assert editor != null;
    balloon.show(computeLocation(editor), Balloon.Position.above);
    Disposer.register(project, balloon);
  }

  public static RelativePoint computeLocation(Editor editor){

    final Rectangle visibleRect = editor.getComponent().getVisibleRect();
    Point point = new Point(visibleRect.x + visibleRect.width + 10,
                            visibleRect.y + 10);
    return new RelativePoint(editor.getComponent(), point);
  }


  public static boolean isTestsFile(@NotNull Project project, @NotNull final String name) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return false;
    }
    EduConfigurator configurator = EduConfiguratorManager.forLanguage(course.getLanguageById());
    if (configurator == null) {
      return false;
    }
    String testFileName = configurator.getTestFileName();
    return name.equals(testFileName) ||
           name.startsWith(FileUtil.getNameWithoutExtension(testFileName)) && name.contains(EduNames.SUBTASK_MARKER);
  }

  public static List<VirtualFile> getTestFiles(@NotNull Task task, @NotNull Project project) {
    final Course course = task.getLesson().getCourse();
    final Language language = course.getLanguageById();

    List<VirtualFile> testFiles = new ArrayList<>();
    VirtualFile taskDir = task.getTaskDir(project);
    final EduConfigurator configurator = EduConfiguratorManager.forLanguage(language);

    if (taskDir == null || configurator == null) {
      return testFiles;
    }
    if (!(task instanceof TaskWithSubtasks)) {
      testFiles.addAll(Arrays.stream(taskDir.getChildren())
              .filter(configurator::isTestFile)
              .collect(Collectors.toList()));
      return testFiles;
    }
    testFiles.addAll(Arrays.stream(taskDir.getChildren())
            .filter(file -> isTestsFile(project, file.getName()))
            .collect(Collectors.toList()));
    return testFiles;
  }

  @Nullable
  public static TaskFile getTaskFile(@NotNull final Project project, @NotNull final VirtualFile file) {
    Task task = getTaskForFile(project, file);
    return task == null ? null : task.getTaskFile(pathRelativeToTask(file));
  }

  public static void drawAllAnswerPlaceholders(Editor editor, TaskFile taskFile) {
    editor.getMarkupModel().removeAllHighlighters();
    final Project project = editor.getProject();
    if (project == null) return;
    if (!taskFile.isValid(editor.getDocument().getText())) return;
    final StudyTaskManager studyTaskManager = StudyTaskManager.getInstance(project);
    for (AnswerPlaceholder answerPlaceholder : taskFile.getAnswerPlaceholders()) {
      final JBColor color = studyTaskManager.getColor(answerPlaceholder);
      AnswerPlaceholderPainter.drawAnswerPlaceholder(editor, answerPlaceholder, color);
    }

    final Document document = editor.getDocument();
    EditorActionManager.getInstance()
      .setReadonlyFragmentModificationHandler(document, new AnswerPlaceholderDeleteHandler(editor));
    AnswerPlaceholderPainter.createGuardedBlocks(editor, taskFile);
    editor.getColorsScheme().setColor(EditorColors.READONLY_FRAGMENT_BACKGROUND_COLOR, null);
  }

  @Nullable
  public static EduEditor getSelectedStudyEditor(@NotNull final Project project) {
    try {
      final FileEditor fileEditor = FileEditorManagerEx.getInstanceEx(project).getSplitters().getCurrentWindow().
        getSelectedEditor().getSelectedEditorWithProvider().getFirst();
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
    final EduEditor eduEditor = getSelectedStudyEditor(project);
    if (eduEditor != null) {
      return eduEditor.getEditor();
    }
    return null;
  }

  public static void deleteGuardedBlocks(@NotNull final Document document) {
    if (document instanceof DocumentImpl) {
      final DocumentImpl documentImpl = (DocumentImpl)document;
      List<RangeMarker> blocks = documentImpl.getGuardedBlocks();
      for (final RangeMarker block : blocks) {
        ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> document.removeGuardedBlock(block)));
      }
    }
  }

  public static boolean isRenameableOrMoveable(@NotNull final Project project, @NotNull final Course course, @NotNull final PsiElement element) {
    if (element instanceof PsiFile) {
      VirtualFile virtualFile = ((PsiFile)element).getVirtualFile();
      if (project.getBaseDir().equals(virtualFile.getParent())) {
        return false;
      }
      TaskFile file = getTaskFile(project, virtualFile);
      if (file != null) {
        return false;
      }
      String name = virtualFile.getName();
      return !isTestsFile(project, name) && !isTaskDescriptionFile(name);
    }
    if (element instanceof PsiDirectory) {
      VirtualFile virtualFile = ((PsiDirectory)element).getVirtualFile();
      VirtualFile parent = virtualFile.getParent();
      if (parent == null) {
        return true;
      }
      if (project.getBaseDir().equals(parent)) {
        return false;
      }
      Lesson lesson = course.getLesson(parent.getName());
      if (lesson != null) {
        Task task = lesson.getTask(virtualFile.getName());
        if (task != null) {
          return false;
        }
      }
    }
    return true;
  }

  public static boolean canRenameOrMove(DataContext dataContext) {
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
    if (element == null || project == null) {
      return false;
    }
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null || !course.isStudy()) {
      return false;
    }

    return !isRenameableOrMoveable(project, course, element);
  }

  @Nullable
  public static String getTaskTextFromTask(@Nullable final VirtualFile taskDirectory, @Nullable final Task task) {
    if (task == null || task.getLesson() == null || task.getLesson().getCourse() == null) {
      return null;
    }
    String text = task.getTaskDescription() != null ? task.getTaskDescription() : getTaskTextByTaskName(task, taskDirectory);

    if (text == null) return null;

    return text;
  }

  @NotNull
  public static String constructTaskTextFilename(@NotNull Task task, @NotNull String defaultName) {
    String fileNameWithoutExtension = FileUtil.getNameWithoutExtension(defaultName);
    if (task instanceof TaskWithSubtasks) {
      int activeStepIndex = ((TaskWithSubtasks)task).getActiveSubtaskIndex();
      fileNameWithoutExtension += EduNames.SUBTASK_MARKER + activeStepIndex;
    }
    return addExtension(fileNameWithoutExtension, defaultName);
  }

  @NotNull
  private static String addExtension(@NotNull String fileNameWithoutExtension, @NotNull String defaultName) {
    return fileNameWithoutExtension + "." + FileUtilRt.getExtension(defaultName);
  }

  @Nullable
  private static String getTaskTextByTaskName(@NotNull Task task, @Nullable VirtualFile taskDirectory) {
    if (taskDirectory == null) return null;

    String textFromHtmlFile = getTextByTaskFileFormat(task, taskDirectory, EduNames.TASK_HTML);
    if (textFromHtmlFile != null) {
      return textFromHtmlFile;
    }

    String taskTextFromMd = getTextByTaskFileFormat(task, taskDirectory, EduNames.TASK_MD);
    return convertToHtml(taskTextFromMd);
  }

  @Nullable
  private static String getTextByTaskFileFormat(@NotNull Task task, @NotNull VirtualFile taskDirectory, @NotNull String taskTextFileName) {
    String textFilename = constructTaskTextFilename(task, taskTextFileName);
    VirtualFile taskTextFile = taskDirectory.findChild(textFilename);

    if (taskTextFile != null) {
      return String.valueOf(LoadTextUtil.loadText(taskTextFile));
    }

    VirtualFile srcDir = taskDirectory.findChild(EduNames.SRC);
    if (srcDir != null) {
      VirtualFile taskTextSrcFile = srcDir.findChild(textFilename);
      if (taskTextSrcFile != null) {
        return String.valueOf(LoadTextUtil.loadText(taskTextSrcFile));
      }
    }

    return null;
  }

  @Nullable
  public static TwitterPluginConfigurator getTwitterConfigurator(@NotNull final Project project) {
    TwitterPluginConfigurator[] extensions = TwitterPluginConfigurator.EP_NAME.getExtensions();
    for (TwitterPluginConfigurator extension: extensions) {
      if (extension.accept(project)) {
        return extension;
      }
    }
    return null;
  }

  @Nullable
  public static String getTaskText(@NotNull final Project project) {
    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor == null) {
      return TaskDescriptionToolWindow.EMPTY_TASK_TEXT;
    }
    Document document = editor.getDocument();
    VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
    if (virtualFile == null) {
      return TaskDescriptionToolWindow.EMPTY_TASK_TEXT;
    }
    final Task task = getTaskForFile(project, virtualFile);
    if (task != null) {
      return getTaskTextFromTask(task.getTaskDir(project), task);
    }
    return null;
  }

  @Nullable
  public static TaskFile getSelectedTaskFile(@NotNull Project project) {
    VirtualFile[] files = FileEditorManager.getInstance(project).getSelectedFiles();
    TaskFile taskFile = null;
    for (VirtualFile file : files) {
      taskFile = getTaskFile(project, file);
      if (taskFile != null) {
        break;
      }
    }
    return taskFile;
  }

  @Nullable
  public static Task getCurrentTask(@NotNull final Project project) {
    final TaskFile taskFile = getSelectedTaskFile(project);
    if (taskFile != null) {
      return taskFile.getTask();
    }
    return !isConfiguredWithGradle(project) ? null : findTaskFromTestFiles(project);
  }

  @Nullable
  public static Task getTaskForCurrentSelectedFile(@NotNull Project project) {
    VirtualFile[] files = FileEditorManager.getInstance(project).getSelectedFiles();
    Task task = null;
    for (VirtualFile file : files) {
      task = getTaskForFile(project, file);
      if (task != null) {
        break;
      }
    }
    return task;
  }

  @Nullable
  private static Task findTaskFromTestFiles(@NotNull Project project) {
    for (VirtualFile testFile : FileEditorManager.getInstance(project).getSelectedFiles()) {
      VirtualFile parentDir = testFile.getParent();
      if (EduNames.TEST.equals(parentDir.getName())) {
        VirtualFile srcDir = parentDir.getParent().findChild(EduNames.SRC);
        if (srcDir == null) {
          return null;
        }
        for (VirtualFile file : srcDir.getChildren()) {
          TaskFile taskFile = getTaskFile(project, file);
          if (taskFile != null) {
            return taskFile.getTask();
          }
        }
      }
    }
    return null;
  }

  public static boolean isStudyProject(@NotNull Project project) {
    return StudyTaskManager.getInstance(project).getCourse() != null;
  }

  public static boolean isStudentProject(@NotNull Project project) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    return course != null && course.isStudy();
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
  public static VirtualFile getTaskDir(@NotNull VirtualFile taskFile) {
    VirtualFile parent = taskFile.getParent();

    while (parent != null) {
      String name = parent.getName();

      if (name.contains(EduNames.TASK) && parent.isDirectory()) {
        return parent;
      }
      if (EduNames.SRC.equals(name)) {
        return parent.getParent();
      }

      parent = parent.getParent();
    }
    return null;
  }

  @Nullable
  public static Task getTaskForFile(@NotNull Project project, @NotNull VirtualFile file) {
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return null;
    }
    VirtualFile taskDir = getTaskDir(file);
    if (taskDir == null) {
      return null;
    }
    //need this because of multi-module generation
    if (EduNames.SRC.equals(taskDir.getName())) {
      taskDir = taskDir.getParent();
      if (taskDir == null) {
        return null;
      }
    }
    final String taskDirName = taskDir.getName();
    if (taskDirName.contains(EduNames.TASK)) {
      final VirtualFile lessonDir = taskDir.getParent();
      if (lessonDir != null) {
        int lessonIndex = getIndex(lessonDir.getName(), EduNames.LESSON);
        List<Lesson> lessons = course.getLessons();
        if (!indexIsValid(lessonIndex, lessons)) {
          return null;
        }
        final Lesson lesson = lessons.get(lessonIndex);
        int taskIndex = getIndex(taskDirName, EduNames.TASK);
        final List<Task> tasks = lesson.getTaskList();
        if (!indexIsValid(taskIndex, tasks)) {
          return null;
        }
        return tasks.get(taskIndex);
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
  public static String convertToHtml(@Nullable final String content) {
    if (content == null) return null;
    ArrayList<String> lines = ContainerUtil.newArrayList(content.split("\n|\r|\r\n"));
    if ((content.contains("<h") && content.contains("</h")) || (content.contains("<code>") && content.contains("</code>"))) {
      return content;
    }
    MarkdownUtil.replaceHeaders(lines);
    MarkdownUtil.replaceCodeBlock(lines);
    return new MarkdownProcessor().markdown(StringUtil.join(lines, "\n"));
  }

  public static boolean isTaskDescriptionFile(@NotNull final String fileName) {
    if (EduNames.TASK_HTML.equals(fileName) || EduNames.TASK_MD.equals(fileName)) {
      return true;
    }
    String extension = FileUtilRt.getExtension(fileName);
    if (!extension.equals(FileUtilRt.getExtension(EduNames.TASK_HTML)) && !extension.equals(FileUtilRt.getExtension(EduNames.TASK_MD))) {
      return false;
    }
    return fileName.contains(EduNames.TASK) && fileName.contains(EduNames.SUBTASK_MARKER);
  }

  @Nullable
  public static VirtualFile findTaskDescriptionVirtualFile(@NotNull Project project, @NotNull VirtualFile taskDir) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return null;
    }
    Task task = getTask(taskDir, course);
    if (task == null) {
      return null;
    }

    return ObjectUtils.chooseNotNull(taskDir.findChild(constructTaskTextFilename(task, EduNames.TASK_HTML)),
                                     taskDir.findChild(constructTaskTextFilename(task, EduNames.TASK_MD)));
  }

  @NotNull
  public static String getTaskDescriptionFileName(final boolean useHtml) {
    return useHtml ? EduNames.TASK_HTML : EduNames.TASK_MD;
  }

  @Nullable
  public static Document getDocument(String basePath, int lessonIndex, int taskIndex, String fileName) {
    String taskPath = FileUtil.join(basePath, EduNames.LESSON + lessonIndex, EduNames.TASK + taskIndex);
    VirtualFile taskFile = LocalFileSystem.getInstance().findFileByPath(FileUtil.join(taskPath, fileName));
    if (taskFile == null) {
      taskFile = LocalFileSystem.getInstance().findFileByPath(FileUtil.join(taskPath, EduNames.SRC, fileName));
    }
    if (taskFile == null) {
      return null;
    }
    return FileDocumentManager.getInstance().getDocument(taskFile);
  }

  public static void showErrorPopupOnToolbar(@NotNull Project project, String content) {
    final Balloon balloon =
      JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(content, MessageType.ERROR, null).createBalloon();
    showCheckPopUp(project, balloon);
  }

  public static void selectFirstAnswerPlaceholder(@Nullable final EduEditor eduEditor, @NotNull final Project project) {
    if (eduEditor == null) return;
    final Editor editor = eduEditor.getEditor();
    IdeFocusManager.getInstance(project).requestFocus(editor.getContentComponent(), true);
    final List<AnswerPlaceholder> placeholders = eduEditor.getTaskFile().getActivePlaceholders();
    if (placeholders.isEmpty() || !eduEditor.getTaskFile().isValid(editor.getDocument().getText())) return;
    final AnswerPlaceholder placeholder = placeholders.get(0);
    Pair<Integer, Integer> offsets = getPlaceholderOffsets(placeholder, editor.getDocument());
    editor.getSelectionModel().setSelection(offsets.first, offsets.second);
    editor.getCaretModel().moveToOffset(offsets.first);
    editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
  }

  public static void registerStudyToolWindow(Project project) {
    final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
    if (toolWindowManager == null) {
      return;
    }
    ToolWindow studyToolWindow = toolWindowManager.getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW);
    if (studyToolWindow == null) {
      studyToolWindow = toolWindowManager.registerToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW,
                                                                          true, ToolWindowAnchor.RIGHT, project, true);
    }
    studyToolWindow.show(null);
    initToolWindow(project);
  }

  @Nullable public static AnswerPlaceholder getAnswerPlaceholder(int offset, List<AnswerPlaceholder> placeholders) {
    for (AnswerPlaceholder placeholder : placeholders) {
      int placeholderStart = placeholder.getOffset();
      int placeholderEnd = placeholderStart + placeholder.getRealLength();
      if (placeholderStart <= offset && offset <= placeholderEnd) {
        return placeholder;
      }
    }
    return null;
  }

  public static String pathRelativeToTask(VirtualFile file) {
    VirtualFile taskDir = getTaskDir(file);
    if (taskDir == null) return file.getName();
    VirtualFile srcDir = taskDir.findChild(EduNames.SRC);
    if (srcDir != null) {
      taskDir = srcDir;
    }
    return FileUtil.getRelativePath(taskDir.getPath(), file.getPath(), VfsUtilCore.VFS_SEPARATOR_CHAR);
  }

  public static Pair<Integer, Integer> getPlaceholderOffsets(@NotNull final AnswerPlaceholder answerPlaceholder,
                                                             @NotNull final Document document) {
    int startOffset = answerPlaceholder.getOffset();
    int delta = 0;
    final int length = answerPlaceholder.getRealLength();
    int nonSpaceCharOffset = DocumentUtil.getFirstNonSpaceCharOffset(document, startOffset, startOffset + length);
    if (nonSpaceCharOffset != startOffset) {
      delta = startOffset - nonSpaceCharOffset;
      startOffset = nonSpaceCharOffset;
    }
    final int endOffset = startOffset + length + delta;
    return Pair.create(startOffset, endOffset);
  }

  public static boolean isCourseValid(@Nullable Course course) {
    if (course == null) return false;
    if (course.isAdaptive()) {
      final List<Lesson> lessons = course.getLessons();
      if (lessons.size() == 1) {
        return !lessons.get(0).getTaskList().isEmpty();
      }
    }
    return true;
  }

  public static void createFromTemplate(@NotNull Project project,
                                        @NotNull VirtualFile taskDirectory,
                                        @NotNull String name) {
    FileTemplate template = FileTemplateManager.getInstance(project).getInternalTemplate(name);
    if (template == null) {
      LOG.info("Template " + name + " wasn't found");
      return;
    }
    try {
      GeneratorUtils.createChildFile(taskDirectory, name, template.getText());
    } catch (IOException e) {
      LOG.error(e);
    }
  }
  public static void openFirstTask(@NotNull final Course course, @NotNull final Project project) {
    LocalFileSystem.getInstance().refresh(false);
    final Lesson firstLesson = getFirst(course.getLessons());
    if (firstLesson == null) return;
    final Task firstTask = getFirst(firstLesson.getTaskList());
    if (firstTask == null) return;
    final VirtualFile taskDir = firstTask.getTaskDir(project);
    if (taskDir == null) return;
    final Map<String, TaskFile> taskFiles = firstTask.getTaskFiles();
    VirtualFile activeVirtualFile = null;
    for (Map.Entry<String, TaskFile> entry : taskFiles.entrySet()) {
      final String relativePath = entry.getKey();
      final TaskFile taskFile = entry.getValue();
      taskDir.refresh(false, true);
      final VirtualFile virtualFile = taskDir.findFileByRelativePath(relativePath);
      if (virtualFile != null) {
        if (!taskFile.getActivePlaceholders().isEmpty()) {
          activeVirtualFile = virtualFile;
        }
      }
    }
    if (activeVirtualFile != null) {
      final PsiFile file = PsiManager.getInstance(project).findFile(activeVirtualFile);
      ProjectView.getInstance(project).select(file, activeVirtualFile, false);
      final FileEditor[] editors = FileEditorManager.getInstance(project).openFile(activeVirtualFile, true);
      if (editors.length == 0) {
        return;
      }
      final FileEditor studyEditor = editors[0];
      if (studyEditor instanceof EduEditor) {
        selectFirstAnswerPlaceholder((EduEditor)studyEditor, project);
      }
      FileEditorManager.getInstance(project).openFile(activeVirtualFile, true);
    }
    else {
      String first = getFirst(taskFiles.keySet());
      if (first != null) {
        NewVirtualFile firstFile = ((VirtualDirectoryImpl)taskDir).refreshAndFindChild(first);
        if (firstFile != null) {
          FileEditorManager.getInstance(project).openFile(firstFile, true);
        }
      }
    }
  }

  public static void navigateToStep(@NotNull Project project, @NotNull Course course, int stepId) {
    if (stepId == 0 || course.isAdaptive()) {
      return;
    }
    Task task = getTask(course, stepId);
    if (task != null) {
      navigateToTask(project, task);
    }
  }

  @Nullable
  private static Task getTask(@NotNull Course course, int stepId) {
    for (Lesson lesson : course.getLessons()) {
      Task task = lesson.getTask(stepId);
      if (task != null) {
        return task;
      }
    }
    return null;
  }

  @Nullable
  public static StepikUserWidget getStepikWidget() {
    JFrame frame = WindowManager.getInstance().findVisibleFrame();
    if (frame instanceof IdeFrameImpl) {
      return (StepikUserWidget)((IdeFrameImpl)frame).getStatusBar().getWidget(StepikUserWidget.ID);
    }
    return null;
  }

  public static void showOAuthDialog() {
    OAuthDialog dialog = new OAuthDialog();
    if (dialog.showAndGet()) {
      StepicUser user = dialog.getUser();
      EduSettings.getInstance().setUser(user);
    }
  }

  public static File getBundledCourseRoot(final String courseName, Class clazz) {
    @NonNls String jarPath = PathUtil.getJarPathForClass(clazz);
    if (jarPath.endsWith(".jar")) {
      final File jarFile = new File(jarPath);
      File pluginBaseDir = jarFile.getParentFile();
      File coursesDir = new File(pluginBaseDir, "courses");

      if (!coursesDir.exists()) {
        if (!coursesDir.mkdir()) {
          LOG.info("Failed to create courses dir");
          return coursesDir;
        }
      }
      try {
        ZipUtil.extract(jarFile, pluginBaseDir, (dir, name) -> name.equals(courseName));
      } catch (IOException e) {
        LOG.info("Failed to extract default course", e);
      }
      return coursesDir;
    }
    return new File(jarPath, "courses");
  }

  /**
   * Save current description into task if `StudyToolWindow` in editing mode
   * and exit from this mode. Otherwise do nothing.
   *
   * @param project current project
   */
  public static void saveToolWindowTextIfNeeded(@NotNull Project project) {
    TaskDescriptionToolWindow toolWindow = getStudyToolWindow(project);
    TaskDescriptionToolWindow.StudyToolWindowMode toolWindowMode = StudyTaskManager.getInstance(project).getToolWindowMode();
    if (toolWindow != null && toolWindowMode == TaskDescriptionToolWindow.StudyToolWindowMode.EDITING) {
      toolWindow.leaveEditingMode(project);
    }
  }

  public static void enableAction(@NotNull final AnActionEvent event, boolean isEnable) {
    final Presentation presentation = event.getPresentation();
    presentation.setVisible(isEnable);
    presentation.setEnabled(isEnable);
  }

  /**
   * Gets number index in directory names like "task1", "lesson2"
   *
   * @param fullName    full name of directory
   * @param logicalName part of name without index
   * @return index of object
   */
  public static int getIndex(@NotNull final String fullName, @NotNull final String logicalName) {
    if (!fullName.startsWith(logicalName)) {
      return -1;
    }
    try {
      return Integer.parseInt(fullName.substring(logicalName.length())) - 1;
    }
    catch (NumberFormatException e) {
      return -1;
    }
  }

  private static void replaceWithTaskText(Document studentDocument, AnswerPlaceholder placeholder, int toSubtaskIndex) {
    AnswerPlaceholderSubtaskInfo info = placeholder.getSubtaskInfos().get(toSubtaskIndex);
    if (info == null) {
      return;
    }
    String replacementText;
    if (Collections.min(placeholder.getSubtaskInfos().keySet()) == toSubtaskIndex) {
      replacementText = info.getPlaceholderText();
    }
    else {
      Integer max = Collections.max(ContainerUtil.filter(placeholder.getSubtaskInfos().keySet(), i -> i < toSubtaskIndex));
      replacementText = placeholder.getSubtaskInfos().get(max).getPossibleAnswer();
    }
    replaceAnswerPlaceholder(studentDocument, placeholder, placeholder.getVisibleLength(toSubtaskIndex), replacementText);
  }

  @Nullable
  public static TaskFile createStudentFile(Project project, VirtualFile answerFile, @Nullable Task task, int targetSubtaskIndex) {
    try {
      if (task == null) {
        task = getTaskForFile(project, answerFile);
        if (task == null) {
          return null;
        }
        task = task.copy();
      }
      TaskFile taskFile = task.getTaskFile(pathRelativeToTask(answerFile));
      if (taskFile == null) {
        return null;
      }
      if (isImage(taskFile.name)) {
        taskFile.text = Base64.encodeBase64String(answerFile.contentsToByteArray());
        return taskFile;
      }
      Document document = FileDocumentManager.getInstance().getDocument(answerFile);
      if (document == null) {
        return null;
      }
      FileDocumentManager.getInstance().saveDocument(document);
      final LightVirtualFile studentFile = new LightVirtualFile("student_task", PlainTextFileType.INSTANCE, document.getText());
      Document studentDocument = FileDocumentManager.getInstance().getDocument(studentFile);
      if (studentDocument == null) {
        return null;
      }
      EduDocumentListener listener = new EduDocumentListener(taskFile, false);
      studentDocument.addDocumentListener(listener);
      taskFile.setTrackLengths(false);
      for (AnswerPlaceholder placeholder : taskFile.getAnswerPlaceholders()) {
        if (task instanceof TaskWithSubtasks) {
          int fromSubtask = ((TaskWithSubtasks)task).getActiveSubtaskIndex();
          placeholder.switchSubtask(studentDocument, fromSubtask, targetSubtaskIndex);
        }
      }
      for (AnswerPlaceholder placeholder : taskFile.getAnswerPlaceholders()) {
        replaceWithTaskText(studentDocument, placeholder, targetSubtaskIndex);
      }
      taskFile.setTrackChanges(true);
      studentDocument.removeDocumentListener(listener);
      taskFile.text = studentDocument.getImmutableCharSequence().toString();
      return taskFile;
    }
    catch (IOException e) {
      LOG.error("Failed to convert answer file to student one");
    }

    return null;
  }

  @Nullable
  public static String getTextFromInternalTemplate(@NotNull String templateName) {
    FileTemplate template = FileTemplateManager.getDefaultInstance().findInternalTemplate(templateName);
    if (template == null) {
      LOG.info("Failed to obtain internal template: " + templateName);
      return null;
    }
    return template.getText();
  }

  public static void runUndoableAction(Project project, String name, UndoableAction action, UndoConfirmationPolicy confirmationPolicy) {
    new WriteCommandAction(project, name) {
      protected void run(@NotNull final Result result) throws Throwable {
        action.redo();
        UndoManager.getInstance(project).undoableActionPerformed(action);
      }

      @Override
      protected UndoConfirmationPolicy getUndoConfirmationPolicy() {
        return confirmationPolicy;
      }
    }.execute();
  }

  public static boolean isAndroidStudio() {
    return "AndroidStudio".equals(PlatformUtils.getPlatformPrefix());
  }

  public static boolean isConfiguredWithGradle(@NotNull Project project) {
    return new File(project.getBasePath(), GradleConstants.DEFAULT_SCRIPT_NAME).exists();
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
  public static Task getTask(@NotNull VirtualFile directory, @NotNull final Course course) {
    if (EduNames.SRC.equals(directory.getName())) {
      directory = directory.getParent();
      if (directory == null) {
        return null;
      }
    }
    VirtualFile lessonDir = directory.getParent();
    if (lessonDir == null) {
      return null;
    }
    Lesson lesson = course.getLesson(lessonDir.getName());
    if (lesson == null) {
      return null;
    }
    return lesson.getTask(directory.getName());
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
      String name = entry.getKey();
      VirtualFile virtualFile = taskDir.findFileByRelativePath(name);
      if (virtualFile == null) {
        continue;
      }
      String windowsFileName = virtualFile.getNameWithoutExtension() + EduNames.WINDOWS_POSTFIX;
      VirtualFile parentDir = virtualFile.getParent();
      deleteWindowsFile(parentDir, windowsFileName);
    }
  }

  public static void replaceAnswerPlaceholder(@NotNull final Document document,
                                              @NotNull final AnswerPlaceholder answerPlaceholder,
                                              int length,
                                              String replacementText) {
    final int offset = answerPlaceholder.getOffset();
    CommandProcessor.getInstance().runUndoTransparentAction(() -> ApplicationManager.getApplication().runWriteAction(() -> {
      document.replaceString(offset, offset + length, replacementText);
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
        for (AnswerPlaceholder answerPlaceholder : taskFile.getActivePlaceholders()) {
          int length = answerPlaceholder.getRealLength();
          int start = answerPlaceholder.getOffset();
          final String windowDescription = document.getText(new TextRange(start, start + length));
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

  /**
   * @return null if process was canceled, otherwise not null list of courses
   */
  @Nullable
  public static List<Course> getCoursesUnderProgress() {
    try {
      return ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
        ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
        List<Course> courses = execCancelable(() -> StepikConnector.getCourses(EduSettings.getInstance().getUser()));
        if (courses == null) return Lists.newArrayList();
        List<Course> bundledCourses = getBundledCourses();
        for (Course bundledCourse : bundledCourses) {
          if (courses.stream().anyMatch(course -> course.getName().equals(bundledCourse.getName()))) {
            continue;
          }
          courses.add(bundledCourse);
        }
        Collections.sort(courses, (c1, c2) -> Boolean.compare(c1.isAdaptive(), c2.isAdaptive()));
        return courses;
      }, "Getting Available Courses", true, null);
    } catch (ProcessCanceledException e) {
      return null;
    } catch (RuntimeException e) {
      return Lists.newArrayList();
    }
  }

  @NotNull
  private static List<Course> getBundledCourses() {
    final ArrayList<Course> courses = new ArrayList<>();
    final List<LanguageExtensionPoint<EduConfigurator<?>>> extensions = EduConfiguratorManager.allExtensions();
    for (LanguageExtensionPoint<EduConfigurator<?>> extension : extensions) {
      final EduConfigurator configurator = extension.getInstance();
      //noinspection unchecked
      final List<String> paths = configurator.getBundledCoursePaths();
      for (String path : paths) {
        final Course localCourse = getLocalCourse(path);
        if (localCourse != null) {
          courses.add(localCourse);
        }
      }
    }
    return courses;
  }

  @Nullable
  public static Course getLocalCourse(@NotNull final String zipFilePath) {
    try {
      final JBZipFile zipFile = new JBZipFile(zipFilePath);
      final JBZipEntry entry = zipFile.getEntry(EduNames.COURSE_META_FILE);
      if (entry == null) {
        return null;
      }
      byte[] bytes = entry.getData();
      final String jsonText = new String(bytes, CharsetToolkit.UTF8_CHARSET);
      Gson gson = new GsonBuilder()
          .registerTypeAdapter(Task.class, new SerializationUtils.Json.TaskAdapter())
          .registerTypeAdapter(Lesson.class, new SerializationUtils.Json.LessonAdapter())
          .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
          .create();
      zipFile.close();
      return gson.fromJson(jsonText, Course.class);
    }
    catch (IOException e) {
      LOG.error("Failed to unzip course archive");
      LOG.error(e);
    }
    return null;
  }
}
