package com.jetbrains.edu.learning.intellij.generation;

import com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix;
import com.intellij.execution.junit.JUnitExternalLibraryDescriptor;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.highlighter.ModuleFileType;
import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ExternalLibraryDescriptor;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class UtilModuleBuilder extends JavaModuleBuilder {

  @Nullable
  private final Task myAdditionalMaterials;

  public UtilModuleBuilder(String moduleDir, @Nullable Task additionalMaterials) {
    myAdditionalMaterials = additionalMaterials;
    setName(EduNames.UTIL);
    setModuleFilePath(FileUtil.join(moduleDir, EduNames.UTIL, EduNames.UTIL + ModuleFileType.DOT_DEFAULT_EXTENSION));
  }

  private static void addTemplate(@NotNull final Project project, @NotNull VirtualFile baseDir, @NotNull @NonNls final String templateName) {
        final FileTemplate template = FileTemplateManager.getInstance(project).getInternalTemplate(templateName);
        try {
          GeneratorUtils.createChildFile(baseDir, templateName, template.getText());
        } catch (IOException exception) {
          Logger.getInstance(UtilModuleBuilder.class).error("Failed to create from file template ", exception);
        }
    }

  private static void addJUnit(Module baseModule) {
    ExternalLibraryDescriptor descriptor = JUnitExternalLibraryDescriptor.JUNIT4;
    List<String> defaultRoots = descriptor.getLibraryClassesRoots();
    final List<String> urls = OrderEntryFix.refreshAndConvertToUrls(defaultRoots);
    ModuleRootModificationUtil.addModuleLibrary(baseModule, descriptor.getPresentableName(), urls, Collections.emptyList());
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
    ApplicationManager.getApplication().runWriteAction(() -> addTemplate(project, src, "EduTestRunner.java"));
    addJUnit(baseModule);

    if (myAdditionalMaterials != null) {
      for (Map.Entry<String, String> entry : myAdditionalMaterials.getTestsText().entrySet()) {
          GeneratorUtils.createChildFile(project.getBaseDir(), entry.getKey(), entry.getValue());
        }
    }
    return baseModule;
  }

}
