package com.jetbrains.edu.kotlin.android;

import com.android.tools.idea.gradle.util.GradleWrapper;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbModePermission;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.DirectoryProjectGenerator;
import com.jetbrains.edu.learning.EduPluginConfigurator;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.intellij.generation.EduProjectGenerator;
import com.jetbrains.edu.learning.newproject.EduCourseProjectGenerator;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

import static com.android.tools.idea.gradle.util.Projects.getBaseDirPath;

class EduKotlinAndroidCourseProjectGenerator implements EduCourseProjectGenerator {
  private static final Logger LOG = Logger.getInstance(EduKotlinAndroidCourseProjectGenerator.class);
  private Course myCourse;
  @NotNull
  @Override
  public DirectoryProjectGenerator getDirectoryProjectGenerator() {
    return new DirectoryProjectGenerator() {
      @Nls
      @NotNull
      @Override
      public String getName() {
        return "Kotlin Android generator";
      }

      @Nullable
      @Override
      public Icon getLogo() {
        return null;
      }

      @Override
      public void generateProject(@NotNull Project project, @NotNull VirtualFile baseDir, @Nullable Object settings, @NotNull Module module) {
        EduProjectGenerator generator = new EduProjectGenerator();
        generator.setSelectedCourse(myCourse);
        generator.generateProject(project, project.getBaseDir());

        EduPluginConfigurator.INSTANCE.
          forLanguage(myCourse.getLanguageById()).createCourseModuleContent(ModuleManager.getInstance(project).getModifiableModel(),
          project, myCourse, project.getBasePath());
        ApplicationManager.getApplication().invokeLater(() -> DumbService.allowStartingDumbModeInside(DumbModePermission.MAY_START_BACKGROUND,
          () -> ApplicationManager.getApplication().runWriteAction(() -> StudyUtils.registerStudyToolWindow(myCourse, project))));
      }

      @NotNull
      @Override
      public ValidationResult validate(@NotNull String baseDirPath) {
        return ValidationResult.OK;
      }
    };
  }

  @Nullable
  @Override
  public Object getProjectSettings() {
    return null;
  }

  @Override
  public void setCourse(@NotNull Course course) {
    myCourse = course;
  }

  @Override
  public ValidationResult validate() {
    return ValidationResult.OK;
  }

  @Override
  public boolean beforeProjectGenerated() {
    return true;
  }

  @Override
  public void afterProjectGenerated(@NotNull Project project) {
    File projectPath = getBaseDirPath(project);
    try {
      GradleWrapper.create(projectPath);
    } catch (IOException e) {
      LOG.error(e);
    }

    File gradlew = new File(projectPath, "gradlew");
    if (gradlew.exists() && !gradlew.canExecute()) {
      if (!gradlew.setExecutable(true)) {
        LOG.warn("Unable to make gradlew executable");
      }
    }
  }
}