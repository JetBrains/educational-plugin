package com.jetbrains.edu.learning.intellij;

import com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix;
import com.intellij.execution.junit.JUnitExternalLibraryDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ExternalLibraryDescriptor;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.intellij.generation.EduGradleModuleGenerator;
import com.jetbrains.edu.learning.intellij.generation.GradleCourseProjectGenerator;
import com.jetbrains.edu.learning.intellij.generation.LessonModuleBuilder;
import com.jetbrains.edu.learning.intellij.generation.EduModuleBuilderUtils;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public abstract class EduCourseBuilderBase implements EduCourseBuilder<JdkProjectSettings> {

  @Override
  public VirtualFile createLessonContent(@NotNull Project project, @NotNull Lesson lesson, @NotNull VirtualFile parentDirectory) {
    if (EduUtils.isAndroidStudio()) {
      return EduCourseBuilder.super.createLessonContent(project, lesson, parentDirectory);
    }
    String courseDirPath = parentDirectory.getPath();
    Module utilModule = ModuleManager.getInstance(project).findModuleByName(EduNames.UTIL);
    if (utilModule == null) {
      return null;
    }
    EduModuleBuilderUtils.createModule(project, new LessonModuleBuilder(courseDirPath, lesson, utilModule), "");
    return parentDirectory.findChild(EduNames.LESSON + lesson.getIndex());
  }

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

  @Override
  public void configureModule(@NotNull Module module) {
    ExternalLibraryDescriptor descriptor = JUnitExternalLibraryDescriptor.JUNIT4;
    List<String> defaultRoots = descriptor.getLibraryClassesRoots();
    final List<String> urls = OrderEntryFix.refreshAndConvertToUrls(defaultRoots);
    ModuleRootModificationUtil.addModuleLibrary(module, descriptor.getPresentableName(), urls, Collections.emptyList());
  }

  @Override
  public void createCourseModuleContent(@NotNull ModifiableModuleModel moduleModel,
                                        @NotNull Project project,
                                        @NotNull Course course,
                                        @Nullable String moduleDir) {
    try {
      EduModuleBuilderUtils.createCourseModuleContent(moduleModel, project, course, moduleDir);
    } catch (IOException | ModuleWithNameAlreadyExists | ConfigurationException | JDOMException e) {
      Logger.getInstance(EduCourseBuilderBase.class).error(e);
    }
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
