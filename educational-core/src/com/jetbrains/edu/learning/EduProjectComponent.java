package com.jetbrains.edu.learning;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.containers.hash.HashMap;
import com.intellij.util.messages.MessageBusConnection;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.actions.DumbAwareActionWithShortcut;
import com.jetbrains.edu.learning.actions.NextPlaceholderAction;
import com.jetbrains.edu.learning.actions.PrevPlaceholderAction;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils;
import com.jetbrains.edu.learning.editor.EduEditorFactoryListener;
import com.jetbrains.edu.learning.intellij.generation.EduGradleUtils;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import com.jetbrains.edu.learning.projectView.CourseViewPane;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import com.jetbrains.edu.learning.stepik.*;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindow;
import kotlin.collections.ArraysKt;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.jetbrains.edu.learning.EduUtils.*;
import static com.jetbrains.edu.learning.stepik.StepikNames.STEP_ID;

public class EduProjectComponent implements ProjectComponent {
  private static final Logger LOG = Logger.getInstance(EduProjectComponent.class.getName());
  private final Project myProject;
  private FileCreatedByUserListener myListener;
  private final Map<Keymap, List<Pair<String, String>>> myDeletedShortcuts = new HashMap<>();
  private MessageBusConnection myBusConnection;

  private EduProjectComponent(@NotNull final Project project) {
    myProject = project;
  }

  @Override
  public void projectOpened() {
    if (myProject.isDisposed()) {
      return;
    }

    if (!isStudyProject(myProject)) {
      return;
    }
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      ToolWindowManager.getInstance(myProject).invokeLater(() -> selectProjectView());
    }
    StartupManager.getInstance(myProject).runWhenProjectIsInitialized(
      () -> {
        Course course = StudyTaskManager.getInstance(myProject).getCourse();
        if (course == null) {
          LOG.warn("Opened project is with null course");
          return;
        }

        if (!course.isAdaptive() && !course.isUpToDate()) {
          updateAvailable(course);
        }

        final StepicUser currentUser = EduSettings.getInstance().getUser();
        if (currentUser != null && !course.getAuthors().contains(currentUser) && !CCUtils.isCourseCreator(myProject)) {
          loadSolutionsFromStepik(course);
        }

        if (EduGradleUtils.isConfiguredWithGradle(myProject)) {
          setupGradleProject();
        }

        addStepikWidget();
        selectStep(course);

        ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
            registerShortcuts();
            EduUsagesCollector.projectTypeOpened(course.isAdaptive() ? EduNames.ADAPTIVE : EduNames.STUDY);
          }));
      }
    );

    myBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
    myBusConnection.subscribe(EditorColorsManager.TOPIC, new EditorColorsListener() {
      @Override
      public void globalSchemeChange(EditorColorsScheme scheme) {
        final TaskDescriptionToolWindow toolWindow = getStudyToolWindow(myProject);
        if (toolWindow != null) {
          toolWindow.updateFonts(myProject);
        }
      }
    });
  }

  private void setupGradleProject() {
    String projectBasePath = myProject.getBasePath();
    if (projectBasePath == null) {
      LOG.error("Failed to refresh gradle project");
      return;
    }

    // Android Studio imports gradle project itself but not in tests
    boolean needImportForCurrentPlatform = ApplicationManager.getApplication().isUnitTestMode() || !isAndroidStudio();
    if (needImportForCurrentPlatform && myProject.getUserData(CourseProjectGenerator.EDU_PROJECT_CREATED) == Boolean.TRUE) {
      EduGradleUtils.importGradleProject(myProject, projectBasePath);
    }

    // Android Studio creates `gradlew` not via VFS so we have to refresh project dir
    VfsUtil.markDirtyAndRefresh(false, true, true, myProject.getBaseDir());
    // Android Studio creates non executable `gradlew`
    new File(FileUtil.toSystemDependentName(projectBasePath), "gradlew").setExecutable(true);
  }

  private void selectProjectView() {
    ProjectView projectView = ProjectView.getInstance(myProject);
    if (projectView != null) {
      String selectedViewId = ProjectView.getInstance(myProject).getCurrentViewId();
      if (!CourseViewPane.ID.equals(selectedViewId)) {
        projectView.changeView(CourseViewPane.ID);
      }
    }
    else {
      LOG.warn("Failed to select Project View");
    }
  }

  private void loadSolutionsFromStepik(@NotNull Course course) {
    if (!(course instanceof RemoteCourse) || !((RemoteCourse) course).isLoadSolutions()) return;
    if (PropertiesComponent.getInstance(myProject).getBoolean(StepikNames.ARE_SOLUTIONS_UPDATED_PROPERTY)) {
      PropertiesComponent.getInstance(myProject).setValue(StepikNames.ARE_SOLUTIONS_UPDATED_PROPERTY, false);
      return;
    }
    StepikSolutionsLoader stepikSolutionsLoader = StepikSolutionsLoader.getInstance(myProject);
    try {
      List<Task> tasksToUpdate = stepikSolutionsLoader.tasksToUpdateUnderProgress();
      stepikSolutionsLoader.loadSolutionsInBackground(tasksToUpdate);
    }
    catch (Exception e) {
      LOG.warn(e.getMessage());
    }
  }

  private void addStepikWidget() {
    StepikUserWidget widget = getStepikWidget();
    StatusBar statusBar = WindowManager.getInstance().getStatusBar(myProject);
    if (widget != null) {
      statusBar.removeWidget(StepikUserWidget.ID);
    }
    statusBar.addWidget(new StepikUserWidget(myProject), "before Position");
  }

  private void selectStep(@NotNull Course course) {
    int stepId = PropertiesComponent.getInstance().getInt(STEP_ID, 0);
    if (stepId != 0) {
      navigateToStep(myProject, course, stepId);
    }
  }

  private void updateAvailable(Course course) {
    final Notification notification =
      new Notification("Update.course", "Course Updates", "Course is ready to <a href=\"update\">update</a>", NotificationType.INFORMATION,
                       new NotificationListener() {
                         @Override
                         public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                           FileEditorManagerEx.getInstanceEx(myProject).closeAllFiles();

                           ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                             ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
                             return execCancelable(() -> {
                               updateCourse();
                               return true;
                             });
                           }, "Updating Course", true, myProject);
                           synchronize();
                           course.setUpdated();
                         }
                       });
    notification.notify(myProject);
  }

  private void registerShortcuts() {
    TaskDescriptionToolWindow window = getStudyToolWindow(myProject);
    if (window != null) {
      List<AnAction> actionsOnToolbar = window.getActions(true);
      for (AnAction action : actionsOnToolbar) {
        if (action instanceof DumbAwareActionWithShortcut) {
          String id = ((DumbAwareActionWithShortcut)action).getActionId();
          String[] shortcuts = ((DumbAwareActionWithShortcut)action).getShortcuts();
          if (shortcuts != null) {
            addShortcut(id, shortcuts);
          }
        }
      }
      addShortcut(NextPlaceholderAction.ACTION_ID, new String[]{NextPlaceholderAction.SHORTCUT, NextPlaceholderAction.SHORTCUT2});
      addShortcut(PrevPlaceholderAction.ACTION_ID, new String[]{PrevPlaceholderAction.SHORTCUT});
    }
  }

  private void updateCourse() {
    final Course currentCourse = StudyTaskManager.getInstance(myProject).getCourse();
    if (!(currentCourse instanceof RemoteCourse)) return;
    final Course course = StepikConnector.getCourse(myProject, (RemoteCourse)currentCourse);
    if (course == null) return;
    course.init(null, null, false);

    EduConfigurator configurator = EduConfiguratorManager.forLanguage(course.getLanguageById());
    if (configurator == null) {
      LOG.info("EduConfigurator not found for language " + course.getLanguageById().getDisplayName());
      return;
    }

    final ArrayList<Lesson> updatedLessons = new ArrayList<>();

    int lessonIndex = 0;

    //TODO: handle sections
    for (Lesson lesson : course.getLessons(true)) {
      lessonIndex += 1;
      Lesson studentLesson = currentCourse.getLesson(lesson.getId());

      final VirtualFile baseDir = myProject.getBaseDir();
      final VirtualFile lessonDir = baseDir.findChild(lesson.getName());
      if (lessonDir == null) {
        lesson.setIndex(lessonIndex);
        lesson.init(currentCourse, null, false);
        try {
          GeneratorUtils.createLesson(lesson, baseDir);
        }
        catch (IOException e) {
          LOG.error("Failed to create lesson");
        }
        for (int i = 1; i <= lesson.getTaskList().size(); i++) {
          Task task = lesson.getTaskList().get(i - 1);
          task.setIndex(i);
        }
        updatedLessons.add(lesson);
        continue;
      }
      studentLesson.setIndex(lessonIndex);
      updatedLessons.add(studentLesson);

      int index = 0;
      final ArrayList<Task> tasks = new ArrayList<>();
      for (Task task : lesson.getTaskList()) {
        index += 1;
        final Task studentTask = studentLesson.getTask(task.getStepId());
        if (studentTask != null && CheckStatus.Solved.equals(studentTask.getStatus())) {
          studentTask.setIndex(index);
          tasks.add(studentTask);
          continue;
        }
        task.init(currentCourse, studentLesson, false);
        task.setIndex(index);

        final VirtualFile taskDir = lessonDir.findChild(task.getName());

        if (taskDir != null) return;
        try {
          GeneratorUtils.createTask(task, lessonDir);
        }
        catch (IOException e) {
          LOG.error("Failed to create task");
        }
        tasks.add(task);
      }
      studentLesson.updateTaskList(tasks);
    }

    final Notification notification =
      new Notification("Update.course", "Course update", "Current course is synchronized", NotificationType.INFORMATION);
    notification.notify(myProject);
  }

  private void addShortcut(@NotNull final String actionIdString, @NotNull final String[] shortcuts) {
    KeymapManagerEx keymapManager = KeymapManagerEx.getInstanceEx();
    for (Keymap keymap : keymapManager.getAllKeymaps()) {
      List<Pair<String, String>> pairs = myDeletedShortcuts.get(keymap);
      if (pairs == null) {
        pairs = new ArrayList<>();
        myDeletedShortcuts.put(keymap, pairs);
      }
      for (String shortcutString : shortcuts) {
        Shortcut studyActionShortcut = new KeyboardShortcut(KeyStroke.getKeyStroke(shortcutString), null);
        String[] actionsIds = keymap.getActionIds(studyActionShortcut);
        for (String actionId : actionsIds) {
          pairs.add(Pair.create(actionId, shortcutString));
          keymap.removeShortcut(actionId, studyActionShortcut);
        }
        keymap.addShortcut(actionIdString, studyActionShortcut);
      }
    }
  }

  @Override
  public void projectClosed() {
    final Course course = StudyTaskManager.getInstance(myProject).getCourse();
    if (course != null) {
      KeymapManagerEx keymapManager = KeymapManagerEx.getInstanceEx();
      for (Keymap keymap : keymapManager.getAllKeymaps()) {
        List<Pair<String, String>> pairs = myDeletedShortcuts.get(keymap);
        if (pairs != null && !pairs.isEmpty()) {
          for (Pair<String, String> actionShortcut : pairs) {
            keymap.addShortcut(actionShortcut.first, new KeyboardShortcut(KeyStroke.getKeyStroke(actionShortcut.second), null));
          }
        }
      }
    }
    myListener = null;
  }

  @Override
  public void initComponent() {
    EditorFactory.getInstance().addEditorFactoryListener(new EduEditorFactoryListener(), myProject);
    ActionManager.getInstance().addAnActionListener(new AnActionListener() {
      @Override
      public void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
        String actionId = ActionManager.getInstance().getId(action);
        Set<String> ids = newFilesActionsIds();
        if (ids.contains(actionId)) {
          myListener = new FileCreatedByUserListener();
          VirtualFileManager.getInstance().addVirtualFileListener(myListener);
        }
      }

      @Override
      public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
        String actionId = ActionManager.getInstance().getId(action);
        Set<String> ids = newFilesActionsIds();
        if (ids.contains(actionId) && myListener != null) {
          VirtualFileManager.getInstance().removeVirtualFileListener(myListener);
        }
      }

      @Override
      public void beforeEditorTyping(char c, DataContext dataContext) {

      }
    });
  }

  @NotNull
  private static Set<String> newFilesActionsIds() {
    Set<String> actionsIds = new HashSet<>(actionsIds("NewGroup"));
    actionsIds.addAll(actionsIds("NewGroup1"));
    return actionsIds;
  }

  @NotNull
  private static List<String> actionsIds(@NotNull String actionGroupId) {
    ActionManager actionManager = ActionManager.getInstance();
    AnAction action = actionManager.getAction(actionGroupId);
    if (action instanceof ActionGroup) {
      return ArraysKt.map(((ActionGroup)action).getChildren(null), actionManager::getId);
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public void disposeComponent() {
    if (myBusConnection != null) {
      myBusConnection.disconnect();
    }
  }

  @NotNull
  @Override
  public String getComponentName() {
    return "StudyTaskManager";
  }

  public static EduProjectComponent getInstance(@NotNull final Project project) {
    final Module module = ModuleManager.getInstance(project).getModules()[0];
    return module.getComponent(EduProjectComponent.class);
  }

  private class FileCreatedByUserListener implements VirtualFileListener {
    @Override
    public void fileCreated(@NotNull VirtualFileEvent event) {
      if (myProject.isDisposed()) return;
      if (!isStudentProject(myProject)) return;
      final VirtualFile createdFile = event.getFile();
      final Task task = getTaskForFile(myProject, createdFile);
      if (task == null) return;
      final TaskFile taskFile = new TaskFile();
      taskFile.initTaskFile(task, false);
      taskFile.setUserCreated(true);
      final String name = pathRelativeToTask(myProject, createdFile);
      taskFile.name = name;
      task.getTaskFiles().put(name, taskFile);
    }
  }
}
