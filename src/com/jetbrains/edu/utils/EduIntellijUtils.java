package com.jetbrains.edu.utils;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.ide.projectView.actions.MarkRootActionBase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.util.PathUtil;
import com.intellij.util.io.ZipUtil;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.courseFormat.TaskFile;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.ui.StudyToolWindow;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;


public class EduIntellijUtils {
    public static final String TEST_RUNNER = "EduTestRunner";
    public static final String SRC = "src";

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

    private static class StudyFileEditorManagerListener implements FileEditorManagerListener {

        private StudyToolWindow myStudyToolWindow;
        private Project myProject;
        private static final String EMPTY_TASK_TEXT = "Please, open any task to see task description";

        public StudyFileEditorManagerListener(StudyToolWindow studyToolWindow, Project project) {
            myStudyToolWindow = studyToolWindow;
            myProject = project;
        }

        @Nullable
        private Task getTask(@NotNull VirtualFile file) {
            TaskFile taskFile = StudyUtils.getTaskFile(myProject, file);
            if (taskFile != null) {
                return taskFile.getTask();
            }
            else {
                return null;
            }
        }

        private void setTaskText(@Nullable final Task task, @Nullable final VirtualFile taskDirectory) {
            String text = StudyUtils.getTaskTextFromTask(task, taskDirectory);
            if (text == null) {
                myStudyToolWindow.setTaskText(EMPTY_TASK_TEXT);
                return;
            }
            myStudyToolWindow.setTaskText(text);
        }

        @Override
        public void fileOpened(@NotNull FileEditorManager fileEditorManager, @NotNull VirtualFile file) {
            Task task = getTask(file);
            setTaskText(task, file.getParent());
        }

        @Override
        public void fileClosed(@NotNull FileEditorManager fileEditorManager, @NotNull VirtualFile virtualFile) {
            myStudyToolWindow.setTaskText(EMPTY_TASK_TEXT);
        }

        @Override
        public void selectionChanged(@NotNull FileEditorManagerEvent event) {
            VirtualFile file = event.getNewFile();
            if (file != null) {
                Task task = getTask(file);
                setTaskText(task, file.getParent());
            }
        }
    }

    public static FileEditorManagerListener getFileEditorManagerListener(StudyToolWindow studyToolWindow, Project project) {
        return new StudyFileEditorManagerListener(studyToolWindow, project);
    }
}
