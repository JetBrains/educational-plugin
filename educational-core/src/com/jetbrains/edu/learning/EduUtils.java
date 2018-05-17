package com.jetbrains.edu.learning;

import com.google.common.collect.Lists;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.ide.SaveAndSyncHandler;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
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
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.IdeFrameImpl;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.content.Content;
import com.intellij.util.PathUtil;
import com.intellij.util.PlatformUtils;
import com.intellij.util.TimeoutUtil;
import com.intellij.util.io.ZipUtil;
import com.intellij.util.io.zip.JBZipEntry;
import com.intellij.util.io.zip.JBZipFile;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.edu.coursecreator.settings.CCSettings;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils;
import com.jetbrains.edu.learning.editor.EduEditor;
import com.jetbrains.edu.learning.handlers.AnswerPlaceholderDeleteHandler;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import com.jetbrains.edu.learning.projectView.CourseViewPane;
import com.jetbrains.edu.learning.serialization.SerializationUtils;
import com.jetbrains.edu.learning.stepik.OAuthDialog;
import com.jetbrains.edu.learning.stepik.StepicUser;
import com.jetbrains.edu.learning.stepik.StepikConnector;
import com.jetbrains.edu.learning.stepik.StepikUserWidget;
import com.jetbrains.edu.learning.twitter.TwitterPluginConfigurator;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindow;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindowFactory;
import kotlin.text.StringsKt;
import org.apache.commons.codec.binary.Base64;
import org.intellij.markdown.IElementType;
import org.intellij.markdown.ast.ASTNode;
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor;
import org.intellij.markdown.html.GeneratingProvider;
import org.intellij.markdown.html.HtmlGenerator;
import org.intellij.markdown.parser.LinkMap;
import org.intellij.markdown.parser.MarkdownParser;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URI;
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
  private static final String SHORTCUT_ENTITY = "&shortcut:";
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
    final TaskDescriptionToolWindow taskDescriptionToolWindow = getStudyToolWindow(project);
    if (taskDescriptionToolWindow != null) {
      Task task = getCurrentTask(project);
      taskDescriptionToolWindow.updateTask(project, task);
      final AbstractProjectViewPane pane = ProjectView.getInstance(project).getCurrentProjectViewPane();
      if (pane instanceof CourseViewPane && isStudentProject(project)) {
        ((CourseViewPane)pane).updateCourseProgress();
      }
    }
  }

  @Nullable
  public static TaskDescriptionToolWindow getStudyToolWindow(@NotNull final Project project) {
    if (project.isDisposed()) return null;

    ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW);
    if (toolWindow != null) {
      Content[] contents = toolWindow.getContentManager().getContents();
      for (Content content : contents) {
        JComponent component = content.getComponent();
        if (component instanceof TaskDescriptionToolWindow) {
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
    final EduEditor eduEditor = getSelectedEduEditor(project);
    Editor editor = eduEditor != null ? eduEditor.getEditor() : FileEditorManager.getInstance(project).getSelectedTextEditor();
    assert editor != null;
    balloon.show(computeLocation(editor), Balloon.Position.above);
    Disposer.register(project, balloon);
  }

  public static RelativePoint computeLocation(Editor editor) {

    final Rectangle visibleRect = editor.getComponent().getVisibleRect();
    Point point = new Point(visibleRect.x + visibleRect.width + 10,
                            visibleRect.y + 10);
    return new RelativePoint(editor.getComponent(), point);
  }


  public static boolean isTestsFile(@NotNull Project project, @NotNull VirtualFile file) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return false;
    }
    Language language = course.getLanguageById();
    if (language == null) {
      return false;
    }
    EduConfigurator configurator = EduConfiguratorManager.forLanguage(language);
    if (configurator == null) {
      return false;
    }
    return configurator.isTestFile(file);
  }

  public static List<VirtualFile> getTestFiles(@NotNull Task task, @NotNull Project project) {
    final Course course = task.getLesson().getCourse();
    final Language language = course.getLanguageById();

    final EduConfigurator configurator = EduConfiguratorManager.forLanguage(language);
    if (configurator == null) {
      LOG.warn(String.format("Can't find configurator for `%s` language", language.getDisplayName()));
      return Collections.emptyList();
    }

    final VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) {
      LOG.warn(String.format("Can't find task dir for `%s` task", task.getName()));
      return Collections.emptyList();
    }

    final VirtualFile testDir = TaskExt.findTestDir(task, taskDir);
    if (testDir == null) {
      LOG.warn(String.format("Can't find test dir for `%s` task", task.getName()));
      return Collections.emptyList();
    }

    return Arrays.stream(testDir.getChildren())
      .filter(configurator::isTestFile)
      .collect(Collectors.toList());
  }

  @Nullable
  public static TaskFile getTaskFile(@NotNull final Project project, @NotNull final VirtualFile file) {
    Task task = getTaskForFile(project, file);
    return task == null ? null : task.getTaskFile(pathRelativeToTask(project, file));
  }

  public static void drawAllAnswerPlaceholders(Editor editor, TaskFile taskFile) {
    final Project project = editor.getProject();
    if (project == null) return;
    if (!taskFile.isValid(editor.getDocument().getText())) return;
    for (AnswerPlaceholder answerPlaceholder : taskFile.getAnswerPlaceholders()) {
      NewPlaceholderPainter.INSTANCE.paintPlaceholder(editor, answerPlaceholder);
    }

    final Document document = editor.getDocument();
    EditorActionManager.getInstance()
      .setReadonlyFragmentModificationHandler(document, new AnswerPlaceholderDeleteHandler(editor));
  }

  @Nullable
  public static EduEditor getSelectedEduEditor(@NotNull final Project project) {
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
    final EduEditor eduEditor = getSelectedEduEditor(project);
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

  public static boolean isRenameAndMoveForbidden(@NotNull final Project project, @NotNull final Course course, @NotNull final PsiElement element) {
    if (element instanceof PsiFile) {
      VirtualFile virtualFile = ((PsiFile)element).getVirtualFile();
      if (project.getBaseDir().equals(virtualFile.getParent())) {
        return false;
      }
      TaskFile file = getTaskFile(project, virtualFile);
      if (file != null) {
        return false;
      }
      return !isTestsFile(project, virtualFile) && !isTaskDescriptionFile(virtualFile.getName());
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
      Lesson lesson = getLesson(parent, course);
      if (lesson != null) {
        Task task = lesson.getTask(virtualFile.getName());
        if (task != null) {
          return false;
        }
      }
    }
    return true;
  }

  public static boolean renameAndMoveForbidden(DataContext dataContext) {
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
    if (element == null || project == null) {
      return false;
    }
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null || !course.isStudy()) {
      return false;
    }

    return !isRenameAndMoveForbidden(project, course, element);
  }

  @Nullable
  public static String getTaskTextFromTask(@Nullable final VirtualFile taskDirectory, @Nullable final Task task) {
    if (task == null || task.getLesson() == null || task.getLesson().getCourse() == null) {
      return null;
    }
    String text = task.getTaskDescription(taskDirectory) != null ? task.getTaskDescription(taskDirectory) : getTaskTextByTaskName(task, taskDirectory);

    if (text == null) return null;

    return text;
  }

  @Nullable
  private static String getTaskTextByTaskName(@NotNull Task task, @Nullable VirtualFile taskDirectory) {
    if (taskDirectory == null) return null;

    DescriptionFormat format = task.getDescriptionFormat();
    String taskDescription = getTextByTaskFileFormat(taskDirectory, format.getDescriptionFileName());
    if (format == DescriptionFormat.MD) {
      return convertToHtml(taskDescription, taskDirectory);
    }
    return taskDescription;
  }

  @Nullable
  private static String getTextByTaskFileFormat(@NotNull VirtualFile taskDirectory, @NotNull String taskTextFileName) {
    VirtualFile taskTextFile = taskDirectory.findChild(taskTextFileName);

    if (taskTextFile != null) {
      return String.valueOf(LoadTextUtil.loadText(taskTextFile));
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
  public static Task getCurrentTask(@NotNull final Project project) {
    VirtualFile[] files = FileEditorManager.getInstance(project).getSelectedFiles();
    for (VirtualFile file : files) {
      Task task = getTaskForFile(project, file);
      if (task != null) return task;
    }
    return null;
  }

  public static boolean isStudyProject(@NotNull Project project) {
    return StudyTaskManager.getInstance(project).getCourse() != null || getCourseModeForNewlyCreatedProject(project) != null;
  }

  @Nullable
  public static String getCourseModeForNewlyCreatedProject(@NotNull Project project) {
    VirtualFile baseDir = project.getBaseDir();
    if (baseDir == null) {
      return null;
    }
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
  public static VirtualFile getTaskDir(@NotNull Course course, @NotNull VirtualFile taskFile) {
    VirtualFile file = taskFile.getParent();
    while (file != null) {
      VirtualFile lessonDirCandidate = file.getParent();
      if (lessonDirCandidate == null) {
        return null;
      }
      Lesson lesson = getLesson(lessonDirCandidate, course);
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

  @NotNull
  public static VirtualFile getCourseDir(@NotNull Project project) {
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      return project.getBaseDir();
    }
    Module module = ModuleManager.getInstance(project).getModules()[0];
    return ModuleRootManager.getInstance(module).getContentRoots()[0];
  }

  @Nullable
  public static Task getTaskForFile(@NotNull Project project, @NotNull VirtualFile file) {
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return null;
    }
    VirtualFile taskDir = getTaskDir(course, file);
    if (taskDir == null) {
      return null;
    }
    final VirtualFile lessonDir = taskDir.getParent();
    if (lessonDir != null) {
      final Lesson lesson = getLesson(lessonDir, course);
      if (lesson == null) {
        return null;
      }

      if (lesson instanceof FrameworkLesson && course.isStudy()) {
        return ((FrameworkLesson)lesson).currentTask();
      } else {
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
  public static String convertToHtml(@Nullable final String content, @NotNull VirtualFile virtualFile) {
    if (content == null) return null;

    if (isHtml(content)) {
      return content;
    }
    return generateMarkdownHtml(virtualFile, content);
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

  private static boolean isHtml(@NotNull String content) {
    return (content.contains("<h") && content.contains("</h")) ||
           ((content.contains("<code>") || content.contains("<code ")) && content.contains("</code>") ||
            content.contains("<b>") || content.contains("<p>") || content.contains("<div>") || content.contains("<br"));
  }

  public static boolean isTaskDescriptionFile(@NotNull final String fileName) {
    return EduNames.TASK_HTML.equals(fileName) || EduNames.TASK_MD.equals(fileName);
  }

  public static void replaceActionIDsWithShortcuts(StringBuffer text) {
    int lastIndex = 0;
    while (lastIndex < text.length()) {
      lastIndex = text.indexOf(SHORTCUT_ENTITY, lastIndex);
      if (lastIndex < 0) {
        return;
      }
      final int actionIdStart = lastIndex + SHORTCUT_ENTITY.length();
      int actionIdEnd = text.indexOf(";", actionIdStart);
      if (actionIdEnd < 0) {
        return;
      }
      final String actionId = text.substring(actionIdStart, actionIdEnd);
      String shortcutText = KeymapUtil.getFirstKeyboardShortcutText(actionId);
      if (shortcutText.isEmpty()) {
        shortcutText = "<no shortcut for action " + actionId + ">";
      }
      text.replace(lastIndex, actionIdEnd + 1, shortcutText);
      lastIndex += shortcutText.length();
    }
  }

  @Nullable
  public static VirtualFile findTaskFileInDir(@NotNull TaskFile taskFile, @NotNull VirtualFile taskDir) {
    return taskDir.findFileByRelativePath(taskFile.getPathInTask());
  }

  @NotNull
  public static DescriptionFormat getDefaultTaskDescriptionFormat() {
    return CCSettings.getInstance().useHtmlAsDefaultTaskFormat() ? DescriptionFormat.HTML : DescriptionFormat.MD;
  }

  @Nullable
  public static Document getDocument(@NotNull Project project, int lessonIndex, int taskIndex, String fileName) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) return null;
    String sourceDir = CourseExt.getSourceDir(course);
    Lesson lesson = course.getLessons().get(lessonIndex - 1);
    Task task = lesson.getTaskList().get(taskIndex - 1);
    String taskPath = FileUtil.join(project.getBasePath(), lesson.getName(), task.getName());
    String filePath;
    if (StringUtil.isNotEmpty(sourceDir)) {
      filePath = FileUtil.join(taskPath, sourceDir, fileName);
    }
    else {
      filePath = FileUtil.join(taskPath, fileName);
    }

    VirtualFile taskFile = LocalFileSystem.getInstance().findFileByPath(filePath);
    return taskFile == null ? null : FileDocumentManager.getInstance().getDocument(taskFile);
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
    final List<AnswerPlaceholder> placeholders = eduEditor.getTaskFile().getAnswerPlaceholders();
    if (placeholders.isEmpty() || !eduEditor.getTaskFile().isValid(editor.getDocument().getText())) return;
    final AnswerPlaceholder placeholder = placeholders.get(0);
    Pair<Integer, Integer> offsets = getPlaceholderOffsets(placeholder);
    editor.getSelectionModel().setSelection(offsets.first, offsets.second);
    editor.getCaretModel().moveToOffset(offsets.first);
    editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
  }

  @Nullable
  public static AnswerPlaceholder getAnswerPlaceholder(int offset, List<AnswerPlaceholder> placeholders) {
    for (AnswerPlaceholder placeholder : placeholders) {
      int placeholderStart = placeholder.getOffset();
      int placeholderEnd = placeholderStart + placeholder.getRealLength();
      if (placeholderStart <= offset && offset <= placeholderEnd) {
        return placeholder;
      }
    }
    return null;
  }

  @NotNull
  public static String pathRelativeToTask(@NotNull Project project, @NotNull VirtualFile file) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) return file.getName();

    String sourceDir = CourseExt.getSourceDir(course);
    String testDir = CourseExt.getTestDir(course);
    List<String> prefixToRemove = new ArrayList<>(2);
    if (StringUtil.isNotEmpty(sourceDir)) {
      prefixToRemove.add(sourceDir + VfsUtilCore.VFS_SEPARATOR_CHAR);
    }
    if (StringUtil.isNotEmpty(testDir)) {
      prefixToRemove.add(testDir + VfsUtilCore.VFS_SEPARATOR_CHAR);
    }

    VirtualFile taskDir = getTaskDir(course, file);
    if (taskDir == null) return file.getName();

    String fullRelativePath = FileUtil.getRelativePath(taskDir.getPath(), file.getPath(), VfsUtilCore.VFS_SEPARATOR_CHAR);
    if (fullRelativePath == null) return file.getName();

    for (String prefix : prefixToRemove) {
      if (fullRelativePath.startsWith(prefix)) return StringsKt.removePrefix(fullRelativePath, prefix);
    }
    return fullRelativePath;
  }

  public static Pair<Integer, Integer> getPlaceholderOffsets(@NotNull final AnswerPlaceholder answerPlaceholder) {
    int startOffset = answerPlaceholder.getOffset();
    final int length = answerPlaceholder.getRealLength();
    final int endOffset = startOffset + length;
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
    final StudyItem firstItem = getFirst(course.getItems());
    if (firstItem == null) return;
    final Lesson firstLesson;
    if (firstItem instanceof Section) {
      firstLesson = getFirst(((Section)firstItem).getLessons());
    }
    else {
      firstLesson = (Lesson)firstItem;
    }
    if (firstLesson == null) {
      return;
    }
    final Task firstTask = getFirst(firstLesson.getTaskList());
    if (firstTask == null) return;
    final VirtualFile taskDir = firstTask.getTaskDir(project);
    if (taskDir == null) return;
    final Map<String, TaskFile> taskFiles = firstTask.getTaskFiles();
    final VirtualFile activeVirtualFile = getActiveVirtualFile(taskDir, taskFiles);
    if (activeVirtualFile != null) {
      final PsiFile file = PsiManager.getInstance(project).findFile(activeVirtualFile);
      if (ApplicationManager.getApplication().isUnitTestMode()) {
        ProjectView.getInstance(project).select(file, activeVirtualFile, false);
      }
      else {
        StartupManager.getInstance(project).registerPostStartupActivity(
          () -> ProjectView.getInstance(project).select(file, activeVirtualFile, false));
      }
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

  private static VirtualFile getActiveVirtualFile(VirtualFile taskDir, Map<String, TaskFile> taskFiles) {
    VirtualFile activeVirtualFile = null;
    for (Map.Entry<String, TaskFile> entry : taskFiles.entrySet()) {
      final TaskFile taskFile = entry.getValue();
      taskDir.refresh(false, true);
      final VirtualFile virtualFile = findTaskFileInDir(taskFile, taskDir);
      if (virtualFile != null) {
        if (!taskFile.getAnswerPlaceholders().isEmpty()) {
          activeVirtualFile = virtualFile;
        }
      }
    }
    return activeVirtualFile;
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
    Ref<Task> taskRef = new Ref<>();
    course.visitLessons((lesson) -> {
      Task task = lesson.getTask(stepId);
      if (task != null) {
        taskRef.set(task);
        return false;
      }
      return true;
    });
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

  @Nullable
  public static TaskFile createStudentFile(@NotNull Project project, @NotNull VirtualFile answerFile, @Nullable Task task) {
    try {
      if (task == null) {
        task = getTaskForFile(project, answerFile);
        if (task == null) {
          return null;
        }
        task = task.copy();
      }
      TaskFile taskFile = task.getTaskFile(pathRelativeToTask(project, answerFile));
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
      EduDocumentListener listener = new EduDocumentTransformListener(project, taskFile);
      studentDocument.addDocumentListener(listener);
      taskFile.setTrackLengths(false);
      for (AnswerPlaceholder placeholder : taskFile.getAnswerPlaceholders()) {
        replaceAnswerPlaceholder(studentDocument, placeholder, placeholder.getPossibleAnswer().length(), placeholder.getPlaceholderText());
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
  public static Task getTask(@NotNull VirtualFile taskDir, @NotNull final Course course) {
    String sourceDir = CourseExt.getSourceDir(course);
    if (taskDir.getName().equals(sourceDir)) {
      taskDir = taskDir.getParent();
      if (taskDir == null) {
        return null;
      }
    }
    VirtualFile lessonDir = taskDir.getParent();
    if (lessonDir == null) {
      return null;
    }
    Lesson lesson = getLesson(lessonDir, course);
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
        for (AnswerPlaceholder answerPlaceholder : taskFile.getAnswerPlaceholders()) {
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
          .registerTypeAdapter(StudyItem.class, new SerializationUtils.Json.LessonSectionAdapter())
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
    Lesson lesson = getLesson(lessonDirCandidate, course);
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
    return getLesson(virtualFile, course) != null;
  }

  @Nullable
  public static Lesson getLesson(@NotNull VirtualFile lessonDir, @NotNull final Course course) {
    if (!lessonDir.isDirectory()) {
      return null;
    }
    VirtualFile sectionDir = lessonDir.getParent();
    if (sectionDir == null) {
      return null;
    }
    final Section section = getSection(sectionDir, course);
    if (section != null) {
      return section.getLesson(lessonDir.getName());
    }

    return course.getLesson(lessonDir.getName());
  }

  @Nullable
  public static Section getSection(@NotNull VirtualFile sectionDir, @NotNull final Course course) {
    if (!sectionDir.isDirectory()) return null;
    return course.getSection(sectionDir.getName());
  }
}
