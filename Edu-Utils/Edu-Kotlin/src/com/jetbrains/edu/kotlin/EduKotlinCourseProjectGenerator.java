package com.jetbrains.edu.kotlin;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.util.newProjectWizard.AbstractProjectWizard;
import com.intellij.ide.util.newProjectWizard.StepSequence;
import com.intellij.ide.util.projectWizard.ProjectBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.ui.configuration.JdkComboBox;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.actions.NewModuleAction;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.ui.FixedSizeButton;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.newproject.EduCourseProjectGenerator;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinIcons;

import javax.swing.*;
import java.awt.*;

class EduKotlinCourseProjectGenerator implements EduCourseProjectGenerator {
  private static final Logger LOG = Logger.getInstance(EduKotlinCourseProjectGenerator.class);
  private Course myCourse;
  private JdkComboBox myJdkComboBox;
  private ProjectSdksModel myModel;

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

  @NotNull
  @Override
  public DirectoryProjectGenerator getDirectoryProjectGenerator() {
    return new DirectoryProjectGenerator() {
      @Nls
      @NotNull
      @Override
      public String getName() {
        return "Kotlin Koans generator";
      }

      @Nullable
      @Override
      public Icon getLogo() {
        return KotlinIcons.SMALL_LOGO;
      }

      @Override
      public void generateProject(@NotNull Project project, @NotNull VirtualFile baseDir, @Nullable Object settings, @NotNull Module module) {
        new NewModuleAction().createModuleFromWizard(project, null, new AbstractProjectWizard("", project, baseDir.getPath()) {
          @Override
          public StepSequence getSequence() {
            return null;
          }

          @Override
          public ProjectBuilder getProjectBuilder() {
            return new EduKotlinKoansModuleBuilder();
          }
        });
        setJdk(project);
      }

      @NotNull
      @Override
      public ValidationResult validate(@NotNull String baseDirPath) {
        return ValidationResult.OK;
      }
    };

  }

  private void setJdk(@NotNull Project project) {
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

  @Nullable
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
    return true;
  }

  @Override
  public void afterProjectGenerated(@NotNull Project project) {

  }
}
