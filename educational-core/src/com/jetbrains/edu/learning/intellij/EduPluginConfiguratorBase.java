package com.jetbrains.edu.learning.intellij;

import com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix;
import com.intellij.execution.junit.JUnitExternalLibraryDescriptor;
import com.intellij.ide.util.newProjectWizard.AbstractProjectWizard;
import com.intellij.ide.util.newProjectWizard.StepSequence;
import com.intellij.ide.util.projectWizard.ProjectBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ExternalLibraryDescriptor;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.ui.configuration.actions.NewModuleAction;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import com.jetbrains.edu.learning.EduPluginConfigurator;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.core.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.intellij.generation.EduLessonModuleBuilder;
import com.jetbrains.edu.learning.intellij.generation.EduModuleBuilderUtils;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public abstract class EduPluginConfiguratorBase implements EduPluginConfigurator {
  @Override
  public VirtualFile createLessonContent(@NotNull Project project, @NotNull Lesson lesson, @NotNull VirtualFile parentDirectory) {
    if (EduUtils.isAndroidStudio()) {
      return EduPluginConfigurator.super.createLessonContent(project, lesson, parentDirectory);
    }
    NewModuleAction newModuleAction = new NewModuleAction();
    String courseDirPath = parentDirectory.getPath();
    Module utilModule = ModuleManager.getInstance(project).findModuleByName(EduIntelliJNames.UTIL);
    if (utilModule == null) {
      return null;
    }
    newModuleAction.createModuleFromWizard(project, null, new AbstractProjectWizard("", project, "") {
      @Override
      public StepSequence getSequence() {
        return null;
      }

      @Override
      public ProjectBuilder getProjectBuilder() {
        return new EduLessonModuleBuilder(courseDirPath, lesson, utilModule);
      }
    });
    return parentDirectory.findChild(EduNames.LESSON + lesson.getIndex());
  }

  @Override
  public VirtualFile createTaskContent(@NotNull Project project, @NotNull Task task,
                                       @NotNull VirtualFile parentDirectory, @NotNull Course course) {
    return EduIntellijUtils.createTask(project, task, parentDirectory, null, null);
  }

  @NotNull
  @Override
  public String getLanguageScriptUrl() {
    return getClass().getResource("/code_mirror/clike.js").toExternalForm();
  }

  @NotNull
  @Override
  public String getDefaultHighlightingMode() {
    return "text/x-java";
  }

  @Override
  public boolean excludeFromArchive(@NotNull String path) {
    final String name = PathUtil.getFileName(path);
    return "out".equals(name) || ".idea".equals(name) || "iml".equals(FileUtilRt.getExtension(name)) || EduIntelliJNames.TEST_RUNNER_FILE.equals(name);
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
      Logger.getInstance(EduPluginConfiguratorBase.class).error(e);
    }
  }
}
