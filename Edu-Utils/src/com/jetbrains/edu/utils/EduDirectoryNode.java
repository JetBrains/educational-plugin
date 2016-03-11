package com.jetbrains.edu.utils;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.Task;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.projectView.StudyDirectoryNode;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class EduDirectoryNode extends StudyDirectoryNode {
    public EduDirectoryNode(@NotNull Project project, PsiDirectory value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    @Override
    public void navigate(boolean requestFocus) {
        final String myValueName = myValue.getName();
        if (myValueName.contains(EduNames.TASK)) {
            PsiDirectory src = myValue.findSubdirectory("src");
            if (src == null) {
                return;
            }
            TaskFile taskFile = null;
            VirtualFile virtualFile =  null;
            for (PsiElement child : src.getChildren()) {
                VirtualFile childFile = child.getContainingFile().getVirtualFile();
                taskFile = StudyUtils.getTaskFile(myProject, childFile);
                if (taskFile != null) {
                    virtualFile = childFile;
                    break;
                }
            }
            if (taskFile != null) {
                VirtualFile taskDir = virtualFile.getParent();
                Task task = taskFile.getTask();
                for (VirtualFile openFile : FileEditorManager.getInstance(myProject).getOpenFiles()) {
                    FileEditorManager.getInstance(myProject).closeFile(openFile);
                }
                VirtualFile child = null;
                Map<String, TaskFile> taskFiles = task.getTaskFiles();
                for (Map.Entry<String, TaskFile> entry: taskFiles.entrySet()) {
                    VirtualFile file = taskDir.findChild(entry.getKey());
                    if (file != null) {
                        FileEditorManager.getInstance(myProject).openFile(file, true);
                    }
                    if (!entry.getValue().getAnswerPlaceholders().isEmpty()) {
                        child = file;
                    }
                }
                if (child != null) {
                    ProjectView.getInstance(myProject).select(child, child, false);
                    FileEditorManager.getInstance(myProject).openFile(child, true);
                } else {
                    VirtualFile[] children = taskDir.getChildren();
                    if (children.length > 0) {
                        ProjectView.getInstance(myProject).select(children[0], children[0], false);
                    }
                }
            }
        }
    }
}
