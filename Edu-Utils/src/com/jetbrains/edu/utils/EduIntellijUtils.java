package com.jetbrains.edu.utils;

import com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix;
import com.intellij.execution.junit.JUnitExternalLibraryDescriptor;
import com.intellij.ide.IdeView;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.ide.util.newProjectWizard.AbstractProjectWizard;
import com.intellij.ide.util.newProjectWizard.StepSequence;
import com.intellij.ide.util.projectWizard.ProjectBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.ui.configuration.actions.NewModuleAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.util.PathUtil;
import com.intellij.util.io.ZipUtil;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.utils.generation.EduTaskModuleBuilder;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;


public class EduIntellijUtils {
  private static final Logger LOG = Logger.getInstance(EduIntellijUtils.class);

  private EduIntellijUtils() {
  }

  public static File getBundledCourseRoot(final String courseName, Class clazz) {
    @NonNls String jarPath = PathUtil.getJarPathForClass(clazz);
    if (jarPath.endsWith(".jar")) {
      final File jarFile = new File(jarPath);
      File pluginBaseDir = jarFile.getParentFile();
      File coursesDir = new File(pluginBaseDir, "courses");
      if (!coursesDir.exists()) {
        if (!coursesDir.mkdir()) {
          LOG.info("Failed to create courses dir");
        } else {
          try {
            ZipUtil.extract(jarFile, pluginBaseDir, (dir, name) -> name.equals(courseName));
          } catch (IOException e) {
            LOG.info("Failed to extract default course", e);
          }
        }
      }
      return coursesDir;
    }
    return new File(jarPath, "courses");
  }


  public static void addTemplate(@NotNull final Project project, @NotNull VirtualFile baseDir, @NotNull @NonNls final String templateName) {
    final FileTemplate template = FileTemplateManager.getInstance(project).getInternalTemplate(templateName);
    final PsiDirectory projectDir = PsiManager.getInstance(project).findDirectory(baseDir);
    if (projectDir == null) return;
    try {
      FileTemplateUtil.createFromTemplate(template, templateName, null, projectDir);
    } catch (Exception exception) {
      LOG.error("Failed to create from file template ", exception);
    }

  }

  public static PsiDirectory createTask(@NotNull Project project, @NotNull Task task, @Nullable IdeView view, @NotNull PsiDirectory parentDirectory,
                                        @Nullable String taskFileName, @Nullable String testFileName) {
    String lessonDirName = parentDirectory.getName();
    NewModuleAction newModuleAction = new NewModuleAction();
    Module lessonModule = ModuleManager.getInstance(project).findModuleByName(lessonDirName);
    Module utilModule = ModuleManager.getInstance(project).findModuleByName(EduIntelliJNames.UTIL);
    if (lessonModule == null || utilModule == null) {
      return null;
    }
    newModuleAction.createModuleFromWizard(project, null, new AbstractProjectWizard("", project, "") {
      @Override
      public StepSequence getSequence() {
        return null;
      }

      @Override
      public ProjectBuilder getProjectBuilder() {
        return new EduTaskModuleBuilder(parentDirectory.getVirtualFile().getPath(), lessonDirName, task, utilModule) {
          @Override
          protected void createTask(Project project, Course course, VirtualFile src) throws IOException {
            PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(src);
            if (psiDirectory == null) {
              return;
            }
            createIfNotNull(project, psiDirectory, taskFileName, view);
            createIfNotNull(project, psiDirectory, testFileName, view);
          }
        };
      }
    });
    return parentDirectory.findSubdirectory(EduNames.LESSON + task.getLesson().getIndex() + "-" + EduNames.TASK + task.getIndex());
  }

  private static void createIfNotNull(@NotNull Project project, @NotNull PsiDirectory psiDirectory, @Nullable String fileName, @Nullable IdeView view) {
    if (fileName != null) {
      StudyUtils.createFromTemplate(project, psiDirectory, fileName, view, false);
    }
  }

  public static void addJUnit(Module baseModule) {
    ExternalLibraryDescriptor descriptor = JUnitExternalLibraryDescriptor.JUNIT4;
    List<String> defaultRoots = descriptor.getLibraryClassesRoots();
    final List<String> urls = OrderEntryFix.refreshAndConvertToUrls(defaultRoots);
    ModuleRootModificationUtil.addModuleLibrary(baseModule, descriptor.getPresentableName(), urls, Collections.emptyList());
  }
}
