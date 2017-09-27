package com.jetbrains.edu.learning.intellij.generation;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.diagnostic.Logger;
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
import com.intellij.ui.ComboboxWithBrowseButton;
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


public abstract class EduIntellijCourseProjectGeneratorBase implements EduCourseProjectGenerator {
  private Logger LOG = DefaultLogger.getInstance(EduIntellijCourseProjectGeneratorBase.class);
  protected Course myCourse;
  private JdkComboBox myJdkComboBox;
  private ProjectSdksModel myModel;

  @NotNull
  @Override
  public Object getProjectSettings() {
    return new Object();
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
    if (myCourse == null || !(myCourse instanceof RemoteCourse)) return true;
    if (((RemoteCourse)myCourse).getId() > 0) {
      ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
        ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
        return StudyUtils.execCancelable(() -> EduStepicConnector.enrollToCourse(((RemoteCourse)myCourse).getId(),
                EduSettings.getInstance().getUser()));
      }, "Creating Course", true, ProjectManager.getInstance().getDefaultProject());
    }
    return true;
  }

  @Nullable
  @Override
  public LabeledComponent<JComponent> getLanguageSettingsComponent(@NotNull Course selectedCourse) {
    myModel = ProjectStructureConfigurable.getInstance(ProjectManager.getInstance().getDefaultProject()).getProjectJdksModel();
    myJdkComboBox = new JdkComboBox(myModel, sdkTypeId -> sdkTypeId instanceof JavaSdkType && !((JavaSdkType)sdkTypeId).isDependent(), sdk -> true, sdkTypeId -> sdkTypeId instanceof JavaSdkType && !((JavaSdkType)sdkTypeId).isDependent(), true);
    ComboboxWithBrowseButton comboboxWithBrowseButton = new ComboboxWithBrowseButton(myJdkComboBox);
    FixedSizeButton setupButton = comboboxWithBrowseButton.getButton();
    myJdkComboBox.setSetupButton(setupButton, null, myModel, (JdkComboBox.JdkComboBoxItem) myJdkComboBox.getModel().getSelectedItem(), null, false);
    return LabeledComponent.create(comboboxWithBrowseButton, "Jdk", BorderLayout.WEST);
  }

  @Override
  public void afterProjectGenerated(@NotNull Project project) {

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
