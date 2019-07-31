package com.jetbrains.edu.learning;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.messages.MessageBusConnection;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.configuration.EduConfigurator;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.handlers.UserCreatedFileListener;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import com.jetbrains.edu.learning.projectView.CourseViewPane;
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionView;
import com.jetbrains.edu.learning.yaml.EduYamlUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;

import static com.jetbrains.edu.learning.EduUtils.*;

@SuppressWarnings("ComponentNotRegistered") // educational-core.xml
public class EduProjectComponent implements ProjectComponent {
  private static final Logger LOG = Logger.getInstance(EduProjectComponent.class.getName());
  private final Project myProject;
  private static final String HINTS_IN_DESCRIPTION_PROPERTY = "HINTS_IN_TASK_DESCRIPTION";
  private MessageBusConnection myBusConnection;

  private EduProjectComponent(@NotNull final Project project) {
    myProject = project;
  }

  @Override
  public void projectOpened() {
    if (myProject.isDisposed() || !isEduProject(myProject)) {
      return;
    }

    if (!OpenApiExtKt.isUnitTestMode()) {
      EduDocumentListener.setGlobalListener(myProject);
      ToolWindowManager.getInstance(myProject).invokeLater(() -> selectProjectView());
    }
    StartupManager.getInstance(myProject).runWhenProjectIsInitialized(
      () -> {
        Course course = StudyTaskManager.getInstance(myProject).getCourse();
        if (course == null) {
          LOG.warn("Opened project is with null course");
          return;
        }

        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(myProject);
        if (CCUtils.isCourseCreator(myProject) && !propertiesComponent.getBoolean(HINTS_IN_DESCRIPTION_PROPERTY)) {
          moveHintsToTaskDescription(course);
          propertiesComponent.setValue(HINTS_IN_DESCRIPTION_PROPERTY, true);
        }

        setupProject(course);
        ApplicationManager.getApplication().invokeLater(
          () -> ApplicationManager.getApplication()
            .runWriteAction(() -> EduCounterUsageCollector.eduProjectOpened(course.getCourseMode())));
      }
    );

    myBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
    myBusConnection.subscribe(EditorColorsManager.TOPIC, new EditorColorsListener() {
      @Override
      public void globalSchemeChange(EditorColorsScheme scheme) {
        TaskDescriptionView.updateAllTabs(myProject, TaskDescriptionView.getInstance(myProject));
      }
    });

    // we need opened project to get project for a course using `CourseExt.getProject`,
    // that's why can't use `ProjectComponent#projectClosed`
    myBusConnection.subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectClosing(@NotNull Project project) {
        Course course = StudyTaskManager.getInstance(project).getCourse();
        if (course != null && !OpenApiExtKt.isUnitTestMode()) {
          EduYamlUtil.saveAll(project);
        }
      }
    });

  }

  private void setupProject(@NotNull Course course) {
    EduConfigurator<?> configurator = CourseExt.getConfigurator(course);
    if (configurator == null) {
      LOG.warn(String.format("Failed to refresh gradle project: configurator for `%s` is null", course.getLanguageID()));
      return;
    }

    if (myProject.getUserData(CourseProjectGenerator.EDU_PROJECT_CREATED) == Boolean.TRUE) {
      configurator.getCourseBuilder().refreshProject(myProject);
    } else if (isAndroidStudio()) {
      // Unexpectedly, Android Studio corrupts content root paths after course project reopening
      // And project structure can't show project tree because of it.
      // We don't know better and cleaner way how to fix it than to refresh project.
      configurator.getCourseBuilder().refreshProject(myProject, new EduCourseBuilder.ProjectRefreshListener() {
        @Override
        public void onSuccess() {
          // We have to open current opened file in project view manually
          // because it can't restore previous state.
          VirtualFile[] files = FileEditorManager.getInstance(myProject).getSelectedFiles();
          for (VirtualFile file : files) {
            Task task = getTaskForFile(myProject, file);
            if (task != null) {
              ProjectView.getInstance(myProject).select(file, file, false);
            }
          }
        }

        @Override
        public void onFailure(@NotNull String errorMessage) {
          LOG.warn("Failed to refresh gradle project: " + errorMessage);
        }
      });
    }

    // Android Studio creates `gradlew` not via VFS so we have to refresh project dir
    VfsUtil.markDirtyAndRefresh(false, true, true, OpenApiExtKt.getCourseDir(myProject));
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

  @VisibleForTesting
  public void moveHintsToTaskDescription(@NotNull Course course) {
    course.visitLessons(lesson -> {
      for (Task task : lesson.getTaskList()) {
        StringBuffer text = new StringBuffer(task.getDescriptionText());
        String hintBlocks = TaskExt.taskDescriptionHintBlocks(task);
        text.append(hintBlocks);
        task.setDescriptionText(text.toString());
        VirtualFile file = TaskExt.getDescriptionFile(task, myProject);
        if (file != null) {
          ApplicationManager.getApplication().runWriteAction(() -> {
            try {
              VfsUtil.saveText(file, text.toString());
            }
            catch (IOException e) {
              LOG.warn(e.getMessage());
            }
          });
        }

        for (TaskFile value : task.getTaskFiles().values()) {
          for (AnswerPlaceholder placeholder : value.getAnswerPlaceholders()) {
            placeholder.setHints(Collections.emptyList());
          }
        }
      }
    });
  }

  @Override
  public void initComponent() {
    if (!OpenApiExtKt.isUnitTestMode() && isStudentProject(myProject)) {
      VirtualFileManager.getInstance().addVirtualFileListener(new UserCreatedFileListener(myProject), myProject);
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
