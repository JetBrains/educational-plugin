package com.jetbrains.edu.learning.intellij.generation;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.CompilerProjectExtension;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.ui.configuration.JdkComboBox;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.ui.FixedSizeButton;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.jetbrains.edu.coursecreator.intellij.CCModuleBuilder;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.newproject.EduCourseProjectGenerator;
import com.jetbrains.edu.learning.stepic.EduStepicConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public abstract class EduIntellijCourseProjectGeneratorBase implements EduCourseProjectGenerator<Object> {

  private static final Logger LOG = DefaultLogger.getInstance(EduIntellijCourseProjectGeneratorBase.class);

  protected final Course myCourse;

  private JdkComboBox myJdkComboBox;
  private ProjectSdksModel myModel;

  public EduIntellijCourseProjectGeneratorBase(@NotNull Course course) {
    myCourse = course;
  }

  @NotNull
  @Override
  public Object getProjectSettings() {
    return new Object();
  }

  @Override
  public boolean beforeProjectGenerated() {
    if (!(myCourse instanceof RemoteCourse)) return true;
    RemoteCourse remoteCourse = (RemoteCourse) myCourse;
    if (remoteCourse.getId() > 0) {
      ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
        ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
        return StudyUtils.execCancelable(() -> EduStepicConnector.enrollToCourse(remoteCourse.getId(),
                EduSettings.getInstance().getUser()));
      }, "Creating Course", true, ProjectManager.getInstance().getDefaultProject());
    }
    return true;
  }

  @Nullable
  @Override
  public LabeledComponent<JComponent> getLanguageSettingsComponent() {
    myModel = ProjectStructureConfigurable.getInstance(ProjectManager.getInstance().getDefaultProject()).getProjectJdksModel();
    myJdkComboBox = new JdkComboBox(myModel, sdkTypeId -> sdkTypeId instanceof JavaSdkType && !((JavaSdkType)sdkTypeId).isDependent(), sdk -> true, sdkTypeId -> sdkTypeId instanceof JavaSdkType && !((JavaSdkType)sdkTypeId).isDependent(), true);
    ComboboxWithBrowseButton comboboxWithBrowseButton = new ComboboxWithBrowseButton(myJdkComboBox);
    FixedSizeButton setupButton = comboboxWithBrowseButton.getButton();
    myJdkComboBox.setSetupButton(setupButton, null, myModel, (JdkComboBox.JdkComboBoxItem) myJdkComboBox.getModel().getSelectedItem(), null, false);
    return LabeledComponent.create(comboboxWithBrowseButton, "Jdk", BorderLayout.WEST);
  }

  @Override
  public void generateProject(@NotNull Project project, @NotNull VirtualFile virtualFile, @NotNull Object o, @NotNull Module module) {
    configureProject(project);
    createCourseStructure(project);
  }

  private void configureProject(@NotNull Project project) {
    setJdk(project);
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

  protected void setJdk(@NotNull Project project) {
    JdkComboBox.JdkComboBoxItem selectedItem = myJdkComboBox.getSelectedItem();
    if (selectedItem instanceof JdkComboBox.SuggestedJdkItem) {
      SdkType type = ((JdkComboBox.SuggestedJdkItem)selectedItem).getSdkType();
      String path = ((JdkComboBox.SuggestedJdkItem)selectedItem).getPath();
      myModel.addSdk(type, path, sdk -> {
        myJdkComboBox.reloadModel(new JdkComboBox.ActualJdkComboBoxItem(sdk), project);
        myJdkComboBox.setSelectedJdk(sdk);
      });
    }
    try {
      myModel.apply();
    } catch (ConfigurationException e) {
      LOG.error(e);
    }
    ApplicationManager.getApplication().runWriteAction(() -> {
      ProjectRootManager.getInstance(project).setProjectSdk(myJdkComboBox.getSelectedJdk());
    });
  }

  protected void setCompilerOutput(@NotNull Project project) {
    CompilerProjectExtension compilerProjectExtension = CompilerProjectExtension.getInstance(project);
    String basePath = project.getBasePath();
    if (compilerProjectExtension != null && basePath != null) {
      compilerProjectExtension.setCompilerOutputUrl(VfsUtilCore.pathToUrl(FileUtilRt.toSystemDependentName(basePath)));
    }
  }
}
