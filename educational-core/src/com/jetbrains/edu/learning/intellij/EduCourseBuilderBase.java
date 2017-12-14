package com.jetbrains.edu.learning.intellij;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.intellij.generation.EduGradleModuleGenerator;
import com.jetbrains.edu.learning.intellij.generation.GradleCourseProjectGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.IOException;

public abstract class EduCourseBuilderBase implements EduCourseBuilder<JdkProjectSettings> {

  @Override
  public VirtualFile createTaskContent(@NotNull Project project, @NotNull Task task,
                                       @NotNull VirtualFile parentDirectory, @NotNull Course course) {
    initNewTask(task);
    ApplicationManager.getApplication().runWriteAction(() -> {
      try {
        EduGradleModuleGenerator.createTaskModule(parentDirectory, task);
      } catch (IOException e) {
        LOG.error("Failed to create task", e);
      }
    });

    ExternalSystemUtil.refreshProjects(project, GradleConstants.SYSTEM_ID, true, ProgressExecutionMode.MODAL_SYNC);
    return parentDirectory.findChild(EduNames.TASK + task.getIndex());
  }

  @NotNull
  @Override
  public LanguageSettings<JdkProjectSettings> getLanguageSettings() {
    return new JdkLanguageSettings();
  }

  @Nullable
  @Override
  public abstract GradleCourseProjectGenerator getCourseProjectGenerator(@NotNull Course course);

  @NotNull
  public abstract String getBuildGradleTemplateName();

  public abstract void initNewTask(@NotNull Task task);
}
