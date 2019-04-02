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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.messages.MessageBusConnection;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.handlers.UserCreatedFileListener;
import com.jetbrains.edu.learning.projectView.CourseViewPane;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionView;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jetbrains.edu.learning.EduUtils.isStudentProject;
import static com.jetbrains.edu.learning.EduUtils.isStudyProject;

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
    if (myProject.isDisposed() || !isStudyProject(myProject)) {
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

        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(myProject);
        if (CCUtils.isCourseCreator(myProject) && !propertiesComponent.getBoolean(HINTS_IN_DESCRIPTION_PROPERTY)) {
          moveHintsToTaskDescription(course);
          propertiesComponent.setValue(HINTS_IN_DESCRIPTION_PROPERTY, true);
        }

        ApplicationManager.getApplication().invokeLater(
          () -> ApplicationManager.getApplication().runWriteAction(() -> EduUsagesCollector.projectTypeOpened(course.getCourseMode())));
      }
    );

    myBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
    myBusConnection.subscribe(EditorColorsManager.TOPIC, new EditorColorsListener() {
      @Override
      public void globalSchemeChange(EditorColorsScheme scheme) {
        TaskDescriptionView.getInstance(myProject).updateTaskDescription();
      }
    });
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
    AtomicBoolean hasPlaceholderHints = new AtomicBoolean(false);
    course.visitLessons(lesson -> {
      for (Task task : lesson.getTaskList()) {
        StringBuffer text = new StringBuffer(task.getDescriptionText());
        String hintBlocks = TaskExt.taskDescriptionHintBlocks(task);
        if (!hintBlocks.isEmpty()) {
          hasPlaceholderHints.set(true);
        }
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

      return true;
    });

    if (hasPlaceholderHints.get()) {
      EduUsagesCollector.projectWithPlaceholderHintsAll();
    }
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
