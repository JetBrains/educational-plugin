package com.jetbrains.edu.kotlin;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.WizardInputField;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import com.jetbrains.edu.EduUtils;
import com.jetbrains.edu.learning.courseGeneration.StudyProjectGenerator;
import com.jetbrains.edu.learning.ui.StudyNewProjectPanel;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.framework.KotlinModuleSettingStep;
import org.jetbrains.kotlin.resolve.TargetPlatform;
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class KotlinStudyModuleBuilder extends ModuleBuilder {

    private static  final Logger LOG = Logger.getInstance(KotlinStudyModuleBuilder.class);

    private final StudyProjectGenerator studyProjectGenerator = new StudyProjectGenerator();
    private final KotlinSdkComboBox mySdkComboBox = new KotlinSdkComboBox();
    private final TargetPlatform targetPlatform = JvmPlatform.INSTANCE;
    private Sdk mySdk;

    private String myBuilderName = "KotlinStudyModuleBuilder";
    private String myBuilderDescription = "Module builder for education Kotlin projects";

    private static final String DEFAULT_COURSE_NAME = "Introduction to Kotlin.zip";
    private static final String COURSE_FOLDER = "courses";

    public KotlinStudyModuleBuilder() {
        studyProjectGenerator.setSelectedCourse(studyProjectGenerator.addLocalCourse(FileUtil.toSystemDependentName(
                getCoursesRoot().getAbsolutePath() + "/" + DEFAULT_COURSE_NAME)));
    }

    @Override
    public String getBuilderId() {
        return "kotlin.edu.builder";
    }

    @Override
    public String getName() {
        return myBuilderName;
    }

    @Override
    public String getPresentableName() {
        return myBuilderName;
    }

    @Override
    public String getDescription() {
        return myBuilderDescription;
    }

    @Override
    public String getGroupName() {
        return KotlinStudyProjectTemplateFactory.GROUP_NAME;
    }

    public Sdk getSdk() {
        return mySdk;
    }

    public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
        return new KotlinModuleSettingStep(targetPlatform, this, settingsStep);
//        return modifyStep(settingsStep);
    }

    @Nullable
    @Override
    public Module commitModule(@NotNull final Project project, @Nullable ModifiableModuleModel model) {
        Module module = super.commitModule(project, model);
        if (module != null) {
            final VirtualFile baseDir = project.getBaseDir();
            studyProjectGenerator.generateProject(project, baseDir);
            System.out.println(project.getBaseDir().getCanonicalPath());
            EduUtils.synchronize();

            StartupManager.getInstance(project).runWhenProjectIsInitialized(new Runnable() {
                @Override
                public void run() {
                    for (VirtualFile lessonDir: project.getBaseDir().getChildren()) {
                        System.out.println(lessonDir.getName());
                        String name = lessonDir.getName();
                        if (lessonDir.isDirectory() && name != ".idea" && name != "Sandbox")
                            KotlinStudyUtils.markDirAsSourceRoot(lessonDir, project);
                    }
                }
            });
        }
        return module;
    }

    @Override
    protected List<WizardInputField> getAdditionalFields() {
        final StudyNewProjectPanel panel = new StudyNewProjectPanel(studyProjectGenerator);
        List<WizardInputField> wizardInputFields = new ArrayList<WizardInputField>();
        wizardInputFields.add(createWizardInputField("Edu.ProjectSdk", "JDK:", mySdkComboBox));
        wizardInputFields.add(createWizardInputField("Edu.Courses", "Courses:", panel.getCoursesComboBox()));
        wizardInputFields.add(createWizardInputField("Edu.InfoPanel", "Info", panel.getInfoPanel()));
        return wizardInputFields;
    }

    public static WizardInputField createWizardInputField(String id, final String label, final JComponent component) {
        return new WizardInputField(id, id) {
            @Override
            public String getLabel() {
                return label;
            }

            @Override
            public JComponent getComponent() {
                return component;
            }

            @Override
            public String getValue() {
                return "";
            }
        };
    }

    @Override
    public void setupRootModel(ModifiableRootModel rootModel) throws ConfigurationException {
        mySdk = mySdkComboBox.getSelectedSdk();
        if (mySdk != null) {
            rootModel.setSdk(mySdk);
        }
        doAddContentEntry(rootModel);
    }

    @Nullable
    @Override
    protected ContentEntry doAddContentEntry(ModifiableRootModel modifiableRootModel) {
        final String contentEntryPath = getContentEntryPath();
        if (contentEntryPath == null) return null;
        new File(contentEntryPath).mkdirs();
        final VirtualFile moduleContentRoot = LocalFileSystem.getInstance().refreshAndFindFileByPath(contentEntryPath.replace('\\', '/'));
        if (moduleContentRoot == null) return null;
        return modifiableRootModel.addContentEntry(moduleContentRoot);
    }

    @Override
    public ModuleType getModuleType() {
        return StdModuleTypes.JAVA;
    }

    private File getCoursesRoot() {
        @NonNls String jarPath = PathUtil.getJarPathForClass(KotlinStudyModuleBuilder.class);
        if (jarPath.endsWith(".jar")) {
            final File jarFile = new File(jarPath);
            File pluginBaseDir = jarFile.getParentFile();
            return new File(pluginBaseDir, "courses");
        }
        return new File(jarPath, "courses");
    }
}