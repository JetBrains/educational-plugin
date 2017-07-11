package com.jetbrains.edu.kotlin.android;

import com.android.SdkConstants;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.jetbrains.edu.learning.checker.StudyCheckListener;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;


public class EduKotlinStudyCheckListener implements StudyCheckListener {

    @Override
    public void beforeCheck(@NotNull Project project, @NotNull Task task) {
        VirtualFile baseDir = project.getBaseDir();
        VirtualFile propertiesFile = baseDir.findChild(SdkConstants.FN_LOCAL_PROPERTIES);
        if (propertiesFile != null) {
            VirtualFile testDir = baseDir.findChild("edu-tests");
            if (testDir != null) {
                ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        VirtualFile child = testDir.findChild(propertiesFile.getName());
                        if (child != null) {
                            child.delete(this);
                        }
                        VfsUtil.copy(this, propertiesFile, testDir);
                        VirtualFileManager.getInstance().refreshWithoutFileWatcher(true);
                    } catch (IOException e) {
                        Logger.getInstance(EduKotlinStudyCheckListener.class).warn(e.getMessage());
                    }
                }));
            }
        }
    }
}
