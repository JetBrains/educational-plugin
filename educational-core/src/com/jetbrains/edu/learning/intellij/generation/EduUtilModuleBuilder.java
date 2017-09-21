package com.jetbrains.edu.learning.intellij.generation;

import com.intellij.ide.highlighter.ModuleFileType;
import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseGeneration.StudyGenerator;
import com.jetbrains.edu.learning.intellij.EduIntelliJNames;
import com.jetbrains.edu.learning.intellij.EduIntellijUtils;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Map;

class EduUtilModuleBuilder extends JavaModuleBuilder {

  @Nullable
  private final Task myAdditionalMaterials;

  public EduUtilModuleBuilder(String moduleDir, @Nullable Task additionalMaterials) {
    myAdditionalMaterials = additionalMaterials;
    setName(EduIntelliJNames.UTIL);
    setModuleFilePath(FileUtil.join(moduleDir, EduIntelliJNames.UTIL, EduIntelliJNames.UTIL + ModuleFileType.DOT_DEFAULT_EXTENSION));
  }

  @NotNull
  @Override
  public Module createModule(@NotNull ModifiableModuleModel moduleModel) throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {
    Module baseModule = super.createModule(moduleModel);
    String directory = getModuleFileDirectory();
    if (directory == null) {
      return baseModule;
    }
    VirtualFile moduleDir = VfsUtil.findFileByIoFile(new File(directory), true);
    if (moduleDir == null) {
      return baseModule;
    }
    VirtualFile src = moduleDir.findChild(EduNames.SRC);
    if (src == null) {
      return baseModule;
    }
    Project project = baseModule.getProject();
    ApplicationManager.getApplication().runWriteAction(() -> EduIntellijUtils.addTemplate(project, src, "EduTestRunner.java"));
    EduIntellijUtils.addJUnit(baseModule);

    if (myAdditionalMaterials != null) {
      for (Map.Entry<String, String> entry : myAdditionalMaterials.getTestsText().entrySet()) {
          StudyGenerator.createChildFile(project.getBaseDir(), entry.getKey(), entry.getValue());
        }
    }
    return baseModule;
  }

}
