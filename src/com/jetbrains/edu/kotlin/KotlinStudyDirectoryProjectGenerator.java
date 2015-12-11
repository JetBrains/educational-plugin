package com.jetbrains.edu.kotlin;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.jetbrains.edu.learning.courseGeneration.StudyProjectGenerator;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class KotlinStudyDirectoryProjectGenerator implements DirectoryProjectGenerator {
    private static final Logger LOG = Logger.getInstance(KotlinStudyDirectoryProjectGenerator.class.getName());
    private final StudyProjectGenerator myGenerator;
    public ValidationResult myValidationResult = new ValidationResult("selected course is not valid");

    public KotlinStudyDirectoryProjectGenerator() {
        myGenerator = new StudyProjectGenerator();
        myGenerator.addSettingsStateListener(new StudyProjectGenerator.SettingsListener() {
            @Override
            public void stateChanged(ValidationResult result) {
                setValidationResult(result);
            }
        });
    }

    @Nls
    @NotNull
    @Override
    public String getName() {
        return "Educational";
    }

    @Nullable
    @Override
    public Object showGenerationSettings(VirtualFile baseDir) throws ProcessCanceledException {
        return null;
    }

    @Nullable
    @Override
    public Icon getLogo() {
        return null; //InteractiveLearningPythonIcons.EducationalProjectType;
    }

    @Override
    public void generateProject(@NotNull final Project project, @NotNull final VirtualFile baseDir,
                                @Nullable Object settings, @NotNull Module module) {
        myGenerator.generateProject(project, baseDir);
        final FileTemplate template = FileTemplateManager.getInstance(project).getInternalTemplate("fileTemplates.fileTemplates.testHelper");
        final PsiDirectory projectDir = PsiManager.getInstance(project).findDirectory(baseDir);
        if (projectDir == null) return;
        try {
            FileTemplateUtil.createFromTemplate(template, "TestHelper.kt", null, projectDir);
        }
        catch (Exception exception) {
            LOG.error("Can't copy fileTemplates.fileTemplates.TestHelper.kt " + exception.getMessage());
        }
    }

    @NotNull
    @Override
    public ValidationResult validate(@NotNull String baseDirPath) {
        return myValidationResult;
    }

    public void setValidationResult(ValidationResult validationResult) {
        myValidationResult = validationResult;
    }
}
