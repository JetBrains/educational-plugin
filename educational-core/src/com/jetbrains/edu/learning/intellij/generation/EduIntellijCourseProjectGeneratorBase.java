package com.jetbrains.edu.learning.intellij.generation;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
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
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.intellij.JdkProjectSettings;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import com.jetbrains.edu.learning.stepic.StepicConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class EduIntellijCourseProjectGeneratorBase implements CourseProjectGenerator<JdkProjectSettings> {

  private static final Logger LOG = DefaultLogger.getInstance(EduIntellijCourseProjectGeneratorBase.class);

  protected final Course myCourse;

  public EduIntellijCourseProjectGeneratorBase(@NotNull Course course) {
    myCourse = course;
  }

  @Override
  public boolean beforeProjectGenerated() {
    if (!(myCourse instanceof RemoteCourse)) return true;
    RemoteCourse remoteCourse = (RemoteCourse) myCourse;
    if (remoteCourse.getId() > 0) {
      ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
        ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
        return StudyUtils.execCancelable(() -> StepicConnector.enrollToCourse(remoteCourse.getId(),
                EduSettings.getInstance().getUser()));
      }, "Creating Course", true, ProjectManager.getInstance().getDefaultProject());
    }
    return true;
  }
  @Override
  public void generateProject(@NotNull Project project, @NotNull VirtualFile virtualFile,
                              @NotNull JdkProjectSettings settings, @NotNull Module module) {
    configureProject(project, settings);
    createCourseStructure(project);
  }

  private void configureProject(@NotNull Project project, @NotNull JdkProjectSettings settings) {
    setJdk(project, settings);
    setCompilerOutput(project);
  }

  private void createCourseStructure(@NotNull Project project) {
    final EduCourseModuleBuilder moduleBuilder;
    if (myCourse.isStudy()) {
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
  protected abstract EduCourseModuleBuilder studyModuleBuilder();

  @Nullable
  protected EduCourseModuleBuilder courseCreationModuleBuilder() {
    return new CCModuleBuilder(myCourse);
  }

  protected void setJdk(@NotNull Project project, @NotNull JdkProjectSettings settings) {
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
  private Sdk getJdk(@NotNull JdkProjectSettings settings) {
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
