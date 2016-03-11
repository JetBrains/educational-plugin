package com.jetbrains.edu.course.creator;

import com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix;
import com.intellij.execution.junit.JUnitExternalLibraryDescriptor;
import com.intellij.ide.projectView.actions.MarkRootActionBase;
import com.intellij.ide.util.projectWizard.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbModePermission;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.coursecreator.CCProjectService;
import com.jetbrains.edu.coursecreator.actions.CCCreateLesson;
import com.jetbrains.edu.coursecreator.actions.CCCreateTask;
import com.jetbrains.edu.coursecreator.ui.CCNewProjectPanel;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

class EduCCModuleBuilder extends JavaModuleBuilder {
    CCNewProjectPanel myPanel = new CCNewProjectPanel();
    private static final Logger LOG = Logger.getInstance(EduCCModuleBuilder.class);

    @Nullable
    @Override
    public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
        ModuleWizardStep javaSettingsStep = ProjectWizardStepFactory.getInstance().createJavaSettingsStep(settingsStep, this, Conditions.alwaysTrue());
        Function<JTextField, String> getValue = JTextComponent::getText;
        getWizardInputField("ccname", "", "Name:", myPanel.getNameField(), getValue).addToSettings(settingsStep);
        getWizardInputField("ccauthor", "", "Author:", myPanel.getAuthorField(), getValue).addToSettings(settingsStep);
        getWizardInputField("ccdescr", "", "Description:", myPanel.getDescriptionField(), JTextArea::getText).addToSettings(settingsStep);
        return javaSettingsStep;
    }

    @Override
    public void setupRootModel(ModifiableRootModel rootModel) throws ConfigurationException {
        setSourcePaths(Collections.emptyList());
        super.setupRootModel(rootModel);
    }



    @NotNull
    @Override
    public Module createModule(@NotNull ModifiableModuleModel moduleModel) throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {
        Module module = super.createModule(moduleModel);
        ExternalLibraryDescriptor descriptor = JUnitExternalLibraryDescriptor.JUNIT4;
        List<String> defaultRoots = descriptor.getLibraryClassesRoots();
        final List<String> urls = OrderEntryFix.refreshAndConvertToUrls(defaultRoots);
        ModuleRootModificationUtil.addModuleLibrary(module, descriptor.getPresentableName(), urls, Collections.emptyList());


        Project project = module.getProject();

        final CCProjectService service = CCProjectService.getInstance(project);
        final Course course = new Course();
        course.setName(myPanel.getName());
        course.setAuthors(myPanel.getAuthors());
        course.setDescription(myPanel.getDescription());
        course.setLanguage("JAVA");
        service.setCourse(course);

        StartupManager.getInstance(project).registerPostStartupActivity(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                final PsiDirectory projectDir = PsiManager.getInstance(project).findDirectory(project.getBaseDir());
                if (projectDir == null) return;
                PsiDirectory lessonDir = new CCCreateLesson().createItem(null, project, projectDir, course);
                if (lessonDir == null) {
                    return;
                }
                new CCCreateTask().createItem(null, project, lessonDir, course);
            });
        });


        VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileAdapter() {
            @Override
            public void fileCreated(@NotNull VirtualFileEvent event) {
                VirtualFile virtualFile = event.getFile();
                Project projectForFile = ProjectUtil.guessProjectForContentFile(virtualFile);
                if (projectForFile == null || CCProjectService.getInstance(projectForFile).getCourse() == null) {
                    return;
                }

                if (isTask(virtualFile)) {
                    markAsSourceRoot(virtualFile);
                }
            }

            private void markAsSourceRoot(VirtualFile virtualFile) {
                DumbService.allowStartingDumbModeInside(DumbModePermission.MAY_START_BACKGROUND, () -> ApplicationManager.getApplication().runWriteAction(() -> {
                    final ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
                    ContentEntry entry = MarkRootActionBase.findContentEntry(model, virtualFile);
                    if (entry == null) {
                        LOG.info("Failed to find contentEntry for archive folder");
                        return;
                    }
                    entry.addSourceFolder(virtualFile, false);
                    model.commit();
                    module.getProject().save();

                }));
            }

            private boolean isTask(VirtualFile virtualFile) {
                return virtualFile.isDirectory() && virtualFile.getName().contains(EduNames.TASK) && virtualFile.getParent() != null &&
                        virtualFile.getParent().getName().contains(EduNames.LESSON);
            }
        });

        return module;
    }

    @NotNull
    private <T extends JComponent> WizardInputField<T> getWizardInputField(String id,
                                                                           String defaultValue,
                                                                           String label,
                                                                           T component,
                                                                           Function<T, String> getValue) {
        return new WizardInputField<T>(id, defaultValue) {
            @Override
            public String getLabel() {
                return label;
            }

            @Override
            public T getComponent() {
                return component;
            }

            @Override
            public String getValue() {
                return getValue.apply(component);
            }
        };
    }
}
