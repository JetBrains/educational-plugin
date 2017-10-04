package com.jetbrains.edu.learning.intellij;

import com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix;
import com.intellij.execution.junit.JUnitExternalLibraryDescriptor;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.projectView.actions.MarkRootActionBase;
import com.intellij.ide.util.newProjectWizard.AbstractProjectWizard;
import com.intellij.ide.util.newProjectWizard.StepSequence;
import com.intellij.ide.util.projectWizard.ProjectBuilder;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.ui.configuration.actions.NewModuleAction;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseGeneration.StudyGenerator;
import com.jetbrains.edu.learning.intellij.generation.EduTaskModuleBuilder;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.edu.learning.StudyUtils.createFromTemplate;


public class EduIntellijUtils {
    private static final Logger LOG = Logger.getInstance(EduIntellijUtils.class);

    private EduIntellijUtils() {
    }

  private static void commitAndSaveModel(final ModifiableRootModel model) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                model.commit();
                model.getProject().save();
            }
        });
    }

    @Nullable
    private static ModifiableRootModel getModel(@NotNull VirtualFile dir, @NotNull Project project) {
        final Module module = ModuleUtilCore.findModuleForFile(dir, project);
        if (module == null) {
            LOG.info("Module for " + dir.getPath() + " was not found");
            return null;
        }
        return ModuleRootManager.getInstance(module).getModifiableModel();
    }

    static void markDirAsSourceRoot(@NotNull final VirtualFile dir, @NotNull final Project project) {
        final ModifiableRootModel model = getModel(dir, project);
        if (model == null) {
            return;
        }
        final ContentEntry entry = MarkRootActionBase.findContentEntry(model, dir);
        if (entry == null) {
            LOG.info("Content entry for " + dir.getPath() + " was not found");
            return;
        }
        entry.addSourceFolder(dir, false);
        commitAndSaveModel(model);
    }


    public static void addTemplate(@NotNull final Project project, @NotNull VirtualFile baseDir, @NotNull @NonNls final String templateName) {
        final FileTemplate template = FileTemplateManager.getInstance(project).getInternalTemplate(templateName);
        try {
          StudyGenerator.createChildFile(baseDir, templateName, template.getText());
        } catch (IOException exception) {
            LOG.error("Failed to create from file template ", exception);
        }
    }

  public static VirtualFile createTask(@NotNull Project project, @NotNull Task task, @NotNull VirtualFile parentDirectory,
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
        return new EduTaskModuleBuilder(parentDirectory.getPath(), lessonDirName, task, utilModule) {
          @Override
          protected void createTask(Project project, Course course, VirtualFile src) throws IOException {
            if (taskFileName == null) {
              return;
            }

            if (course.isAdaptive()) {
              createFromText(project, taskFileName, task);
            } else {
              createFromTemplate(project, src, taskFileName);
              task.addTaskFile(taskFileName, task.taskFiles.size());
              if (testFileName != null) {
                createFromTemplate(project, src, testFileName);
              }
            }
          }
        };
      }
    });
    return parentDirectory.findChild(EduNames.LESSON + task.getLesson().getIndex() + "-" + EduNames.TASK + task.getIndex());
  }

  private static void createFromText(@NotNull Project project, @Nullable String taskFileName, @NotNull Task task) {
    TaskFile taskFile = task.getTaskFile(taskFileName);
    VirtualFile taskDir = task.getTaskDir(project);
    if (taskFile != null && taskDir != null) {
      taskFile.text = StringUtil.notNullize(taskFile.text);
      nameTaskFileAfterContainingClass(task, taskFile, project);

      try {
        StudyGenerator.createTaskFile(taskDir, taskFile);
      }
      catch (IOException e) {
        LOG.warn(e.getMessage());
      }
    }
  }

  public static void nameTaskFileAfterContainingClass(@NotNull Task task,
                                                      @NotNull TaskFile taskFile,
                                                      @NotNull Project project) {
    Language language = task.getLesson().getCourse().getLanguageById();
    if (language.getAssociatedFileType() == null) {
      LOG.warn("Cannot rename task file. Unable to find associated file type for language: " + language.getID());
      return;
    }
    task.getTaskFiles().remove(taskFile.name);
    taskFile.name = publicClassName(project, taskFile, language.getAssociatedFileType()) + "." + language.getAssociatedFileType().getDefaultExtension();
    task.taskFiles.put(taskFile.name, taskFile);
  }

  @NotNull
  private static String publicClassName(@NotNull Project project, @NotNull TaskFile taskFile, @NotNull LanguageFileType fileType) {
    String fileName = "Main";
    PsiFile file = PsiFileFactory.getInstance(project).createFileFromText(taskFile.name, fileType, taskFile.text);
    if (file instanceof PsiClassOwner) {
      PsiClassOwner fileFromText = (PsiClassOwner) file;
      PsiClass[] classes = fileFromText.getClasses();
      for (PsiClass aClass : classes) {
        boolean isPublic = aClass.hasModifierProperty(PsiModifier.PUBLIC);
        if (isPublic && aClass.getName() != null) {
          fileName = aClass.getName();
          break;
        }
      }
    }

    return fileName;
  }

  public static void addJUnit(Module baseModule) {
    ExternalLibraryDescriptor descriptor = JUnitExternalLibraryDescriptor.JUNIT4;
    List<String> defaultRoots = descriptor.getLibraryClassesRoots();
    final List<String> urls = OrderEntryFix.refreshAndConvertToUrls(defaultRoots);
    ModuleRootModificationUtil.addModuleLibrary(baseModule, descriptor.getPresentableName(), urls, Collections.emptyList());
  }
}
