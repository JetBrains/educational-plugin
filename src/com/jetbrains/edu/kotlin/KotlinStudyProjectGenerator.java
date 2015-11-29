package com.jetbrains.edu.kotlin;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.jetbrains.edu.learning.courseGeneration.StudyProjectGenerator;
import org.jetbrains.annotations.NotNull;


public class KotlinStudyProjectGenerator extends StudyProjectGenerator {
    private static final Logger LOG = Logger.getInstance(KotlinStudyProjectGenerator.class.getName());

    @Override
    public void generateProject(@NotNull Project project, @NotNull VirtualFile baseDir) {
        super.generateProject(project, baseDir);
        final Project project1 = project;
        final VirtualFile baseDir1 = baseDir;
        StartupManager.getInstance(project1).registerPostStartupActivity(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        final FileTemplate template = FileTemplateManager.getInstance(project1).getInternalTemplate("TestHelper.java");
                        final PsiDirectory projectDir = PsiManager.getInstance(project1).findDirectory(baseDir1);
                        if (projectDir == null) return;
                        try {
                            FileTemplateUtil.createFromTemplate(template, "TestHelper.java", null, projectDir.createSubdirectory("util"));
                        }
                        catch (Exception exception) {
                            LOG.error("Can't copy fileTemplates.fileTemplates.TestHelper.java " + exception.getMessage());
                        }
                    }
                });
            }
        });
    }
}
