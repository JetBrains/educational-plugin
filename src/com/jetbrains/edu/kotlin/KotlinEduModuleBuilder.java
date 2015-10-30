package com.jetbrains.edu.kotlin;

import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.WizardInputField;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.courseGeneration.StudyProjectGenerator;
import com.jetbrains.edu.learning.ui.StudyNewProjectPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class KotlinEduModuleBuilder extends JavaModuleBuilder {

    private static  final Logger LOG = Logger.getInstance(KotlinEduModuleBuilder.class);

    private final StudyProjectGenerator studyProjectGenerator = new StudyProjectGenerator();
    private final KotlinSdkComboBox mySdkComboBox = new KotlinSdkComboBox();
    private List<Pair<String, String>> mySourcePaths;

    private String myBuilderName = "KotlinEduModuleBuilder";
    private String myBuilderDescription = "Module builder for education Kotlin projects";

    private static final String DEFAULT_COURSE_PATH = "\\edu-for-kotlin\\classes\\courses\\Introduction to Kotlin.zip";

    public KotlinEduModuleBuilder() {
        studyProjectGenerator.addLocalCourse(PathManager.getPluginsPath() + DEFAULT_COURSE_PATH);
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
        return KotlinEduProjectTemplateFactory.GROUP_NAME;
    }

//    TODO: deal with it. maybe delete
//    public List<Pair<String, String>> getSourcePaths() {
//        return mySourcePaths;
//    }
//
//    public void setSourcePaths(final List<Pair<String, String>> sourcePaths) {
//        mySourcePaths = sourcePaths;
//    }
//
//    public void addSourcePath(final Pair<String, String> sourcePathInfo) {
//        if (mySourcePaths == null) {
//            mySourcePaths = new ArrayList<Pair<String, String>>();
//        }
//        mySourcePaths.add(sourcePathInfo);
//    }

    public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
        return modifyStep(settingsStep);
    }

    private Module oldCommitModule(@NotNull Project project, @Nullable ModifiableModuleModel model) {
        Module module = super.commitModule(project, model);
//        TODO: used to PyCharm. delete in future
//        if (module != null && myGenerator != null) {
//            ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
//            VirtualFile[] contentRoots = moduleRootManager.getContentRoots();
//            VirtualFile dir = module.getProject().getBaseDir();
//            if (contentRoots.length > 0 && contentRoots[0] != null) {
//                dir = contentRoots[0];
//            }
//            myGenerator.generateProject(project, dir, null, module);
//        }
        return module;
    }

    @Nullable
    @Override
    public Module commitModule(@NotNull final Project project, @Nullable ModifiableModuleModel model) {
        Module module = oldCommitModule(project, model);
        if (module != null) {
            final VirtualFile baseDir = project.getBaseDir();
            studyProjectGenerator.generateProject(project, baseDir);


//            TODO: used to Stepic
//            final FileTemplate template = FileTemplateManager.getInstance(project).getInternalTemplate("test_helper.py");
//
//            StartupManager.getInstance(project).runWhenProjectIsInitialized(new Runnable() {
//                @Override
//                public void run() {
//                    final PsiDirectory projectDir = PsiManager.getInstance(project).findDirectory(baseDir);
//                    if (projectDir != null) {
//                        try {
//                            FileTemplateUtil.createFromTemplate(template, "test_helper.py", null, projectDir);
//                        }
//                        catch (Exception e) {
//                            LOG.error("Failed to create test_helper", e);
//                        }
//                    }
//                }
//            });
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
        super.setupRootModel(rootModel);
        Sdk sdk = mySdkComboBox.getSelectedSdk();
        if (sdk != null) {
            rootModel.setSdk(sdk);
        }
    }
}