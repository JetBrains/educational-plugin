package com.jetbrains.edu.learning.intellij.generation;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.CompilerProjectExtension;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.ui.configuration.JdkComboBox;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.intellij.CCModuleBuilder;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils;
import com.jetbrains.edu.learning.intellij.JdkProjectSettings;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class IntellijCourseProjectGeneratorBase extends CourseProjectGenerator<JdkProjectSettings> {

  private static final Logger LOG = Logger.getInstance(IntellijCourseProjectGeneratorBase.class);

  public IntellijCourseProjectGeneratorBase(@NotNull Course course) {
    super(course);
  }

  @Override
  protected void createCourseStructure(@NotNull Project project, @NotNull VirtualFile baseDir, @NotNull JdkProjectSettings settings) {
    configureProject(project, settings);
    createCourseStructure(project);
  }

  private void configureProject(@NotNull Project project, @NotNull JdkProjectSettings settings) {
    setJdk(project, settings);
    setCompilerOutput(project);
  }

  private void createCourseStructure(@NotNull Project project) {
    final CourseModuleBuilder moduleBuilder;
    if (myCourse.isStudy()) {
      myCourse = GeneratorUtils.initializeCourse(project, myCourse);
      moduleBuilder = studyModuleBuilder();
    } else {
      moduleBuilder = courseCreationModuleBuilder();
    }
    if (moduleBuilder == null) {
      LOG.warn(String.format("Can't create course %s - corresponding module builder is null", myCourse.getName()));
      return;
    }
    EduModuleBuilderUtils.createModule(project, moduleBuilder, project.getBaseDir().getPath());
  }

  @Nullable
  protected abstract CourseModuleBuilder studyModuleBuilder();

  @Nullable
  protected CourseModuleBuilder courseCreationModuleBuilder() {
    return new CCModuleBuilder(myCourse);
  }

  protected static void setJdk(@NotNull Project project, @NotNull JdkProjectSettings settings) {
    final Sdk jdk = getJdk(settings);

    // Try to apply model, i.e. commit changes from sdk model into ProjectJdkTable
    try {
      settings.getModel().apply();
    } catch (ConfigurationException e) {
      LOG.error(e);
    }

    ApplicationManager.getApplication().runWriteAction(() -> ProjectRootManager.getInstance(project).setProjectSdk(jdk));
  }

  @Nullable
  private static Sdk getJdk(@NotNull JdkProjectSettings settings) {
    JdkComboBox.JdkComboBoxItem selectedItem = settings.getJdkItem();
    if (selectedItem == null) return null;
    if (selectedItem instanceof JdkComboBox.SuggestedJdkItem) {
      SdkType type = ((JdkComboBox.SuggestedJdkItem) selectedItem).getSdkType();
      String path = ((JdkComboBox.SuggestedJdkItem) selectedItem).getPath();
      Ref<Sdk> jdkRef = new Ref<>();
      settings.getModel().addSdk(type, path, jdkRef::set);
      return jdkRef.get();
    }
    return selectedItem.getJdk();
  }

  protected void setCompilerOutput(@NotNull Project project) {
    CompilerProjectExtension compilerProjectExtension = CompilerProjectExtension.getInstance(project);
    String basePath = project.getBasePath();
    if (compilerProjectExtension != null && basePath != null) {
      compilerProjectExtension.setCompilerOutputUrl(VfsUtilCore.pathToUrl(FileUtilRt.toSystemDependentName(basePath)));
    }
  }
}
