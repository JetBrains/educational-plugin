package com.jetbrains.edu.learning;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
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
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.intellij.generation.EduGradleUtils;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import com.jetbrains.edu.learning.projectView.CourseViewPane;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import com.jetbrains.edu.learning.stepik.*;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindow;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.util.*;

import static com.jetbrains.edu.learning.EduUtils.*;
import static com.jetbrains.edu.learning.stepik.StepikNames.STEP_ID;

public class EduProjectComponent implements ProjectComponent {
  private static final Logger LOG = Logger.getInstance(EduProjectComponent.class.getName());
  private final Project myProject;
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

        if (!course.isAdaptive() && course instanceof RemoteCourse) {
          StepikConnector.updateCourseIfNeeded(myProject, (RemoteCourse)course);
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
  }

  @Override
  public void initComponent() {
    if (isStudentProject(myProject)) {
      VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileListener() {
        @Override
        public void fileCreated(@NotNull VirtualFileEvent event) {
          if (myProject.isDisposed()) return;
          final VirtualFile createdFile = event.getFile();
          if (canBeAddedAsTaskFile(myProject, createdFile)) {
            final Task task = getTaskForFile(myProject, createdFile);
            assert task != null;
            String path = pathRelativeToTask(myProject, createdFile);
            task.addTaskFile(path);
            TaskFile taskFile = task.getTaskFile(path);
            assert taskFile != null;
            taskFile.setUserCreated(true);
          }
        }

        @Override
        public void fileDeleted(@NotNull VirtualFileEvent event) {
          if (myProject.isDisposed()) return;
          VirtualFile removedFile = event.getFile();
          final TaskFile taskFile = getTaskFile(myProject, removedFile);
          if (taskFile == null) {
            return;
          }
          Task task = taskFile.getTask();
          task.getTaskFiles().remove(pathRelativeToTask(myProject, removedFile));
        }
      });
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
}
