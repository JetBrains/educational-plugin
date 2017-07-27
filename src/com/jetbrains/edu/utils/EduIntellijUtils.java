package com.jetbrains.edu.utils;

import com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix;
import com.intellij.execution.junit.JUnitExternalLibraryDescriptor;
import com.intellij.ide.IdeView;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
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
import com.intellij.util.PathUtil;
import com.intellij.util.io.ZipUtil;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseGeneration.StudyGenerator;
import com.jetbrains.edu.utils.generation.EduTaskModuleBuilder;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.edu.learning.StudyUtils.createFromTemplate;


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
                        ZipUtil.extract(jarFile, pluginBaseDir, new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                return name.equals(courseName);
                            }
                        });
                    } catch (IOException e) {
                        LOG.info("Failed to extract default course", e);
                    }
                }
            }
            return coursesDir;
        }
        return new File(jarPath, "courses");
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
        final PsiDirectory projectDir = PsiManager.getInstance(project).findDirectory(baseDir);
        if (projectDir == null) return;
        try {
//            PsiDirectory utilDir = projectDir.findSubdirectory("util");
//            if (utilDir == null) {
//                utilDir = projectDir.createSubdirectory("util");
//            }
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

            if (taskFileName == null) {
              return;
            }

            if (course.isAdaptive()) {
              createFromText(project, taskFileName, task);
            }
            else {
              createFromTemplate(project, psiDirectory, taskFileName, view, false);
              if (testFileName != null) {
                createFromTemplate(project, psiDirectory, testFileName, view, false);
              }
            }
          }
        };
      }
    });
    return parentDirectory.findSubdirectory(EduNames.LESSON + task.getLesson().getIndex() + "-" + EduNames.TASK + task.getIndex());
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
