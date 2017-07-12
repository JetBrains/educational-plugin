package com.jetbrains.edu.kotlin.android;

import com.android.SdkConstants;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.DocumentUtil;
import com.jetbrains.edu.learning.checker.StudyCheckListener;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;


public class EduKotlinStudyCheckListener implements StudyCheckListener {

  @Override
  public void beforeCheck(@NotNull Project project, @NotNull Task task) {
    VirtualFile baseDir = project.getBaseDir();
    VirtualFile propertiesFile = baseDir.findChild(SdkConstants.FN_LOCAL_PROPERTIES);
    if (propertiesFile == null) {
      return;
    }
    Document propertiesDocument = FileDocumentManager.getInstance().getDocument(propertiesFile);
    if (propertiesDocument == null) {
      return;
    }
    FileDocumentManager.getInstance().saveDocument(propertiesDocument);
    VirtualFile testDir = baseDir.findChild("edu-tests");
    if (testDir == null) {
      return;
    }
    ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
      try {
        VirtualFile testPropertiesFile = testDir.findChild(propertiesFile.getName());
        if (testPropertiesFile == null) {
          VfsUtil.copy(this, propertiesFile, testDir);
          VirtualFileManager.getInstance().refreshWithoutFileWatcher(true);
        } else {
          Document testPropertiesDocument = FileDocumentManager.getInstance().getDocument(testPropertiesFile);
          if (testPropertiesDocument == null) {
            return;
          }
          testPropertiesDocument.setText(propertiesDocument.getText());
          FileDocumentManager.getInstance().saveDocument(testPropertiesDocument);
        }
      } catch (IOException e) {
        Logger.getInstance(EduKotlinStudyCheckListener.class).warn(e.getMessage());
      }
    }));
  }
}
