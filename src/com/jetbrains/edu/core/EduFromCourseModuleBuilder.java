package com.jetbrains.edu.core;

import com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix;
import com.intellij.execution.junit.JUnitExternalLibraryDescriptor;
import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ExternalLibraryDescriptor;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.courseGeneration.StudyProjectGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public abstract class EduFromCourseModuleBuilder extends JavaModuleBuilder {

    @Override
    public void setupRootModel(ModifiableRootModel rootModel) throws ConfigurationException {
        setSourcePaths(Collections.<Pair<String, String>>emptyList());
        super.setupRootModel(rootModel);
    }

    abstract protected StudyProjectGenerator getStudyProjectGenerator();

    @Nullable
    @Override
    public Module commitModule(@NotNull final Project project, @Nullable ModifiableModuleModel model) {
        final VirtualFile baseDir = project.getBaseDir();
        getStudyProjectGenerator().generateProject(project, baseDir);
        ExternalLibraryDescriptor descriptor = JUnitExternalLibraryDescriptor.JUNIT4;
        List<String> defaultRoots = descriptor.getLibraryClassesRoots();
        final List<String> urls = OrderEntryFix.refreshAndConvertToUrls(defaultRoots);
        Module module = super.commitModule(project, model);
        if (module != null) {
            ModuleRootModificationUtil.addModuleLibrary(module, descriptor.getPresentableName(), urls, Collections.<String>emptyList());
        }

        StartupManager.getInstance(project).registerPostStartupActivity(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        EduIntellijUtils.addTemplate(project, baseDir, "EduTestRunner.java");
                    }
                });
            }
        });
        StartupManager.getInstance(project).runWhenProjectIsInitialized(new Runnable() {
            @Override
            public void run() {
                for (VirtualFile lessonDir : project.getBaseDir().getChildren()) {
                    if (lessonDir.isDirectory() && !lessonDir.getName().equals(".idea"))
                        EduIntellijUtils.markDirAsSourceRoot(lessonDir, project);
                }
                VirtualFile util = project.getBaseDir().findChild("util");
                if (util != null) {
                    EduIntellijUtils.markDirAsSourceRoot(util, project);
                }
            }
        });
        return module;
    }
}
