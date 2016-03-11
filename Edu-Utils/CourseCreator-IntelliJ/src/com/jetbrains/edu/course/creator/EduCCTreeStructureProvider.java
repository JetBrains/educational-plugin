package com.jetbrains.edu.course.creator;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.coursecreator.CCProjectService;
import com.jetbrains.edu.coursecreator.projectView.CCStudentInvisibleFileNode;
import com.jetbrains.edu.coursecreator.projectView.CCTreeStructureProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public class EduCCTreeStructureProvider extends CCTreeStructureProvider {
    @NotNull
    @Override
    public Collection<AbstractTreeNode> modify(@NotNull AbstractTreeNode parent, @NotNull Collection<AbstractTreeNode> children, ViewSettings settings) {
        Collection<AbstractTreeNode> oldNodes = super.modify(parent, children, settings);
        Project project = parent.getProject();
        if (project == null) {
            return oldNodes;
        }
        if (CCProjectService.getInstance(project).getCourse() == null) {
            return oldNodes;
        }

        Collection<AbstractTreeNode> newNodes = new ArrayList<>();
        for (AbstractTreeNode child : children) {
            if (child instanceof PsiDirectoryNode) {
                String name = ((PsiDirectoryNode) child).getValue().getName();
                if (".idea".equals(name) || "out".equals(name)) {
                    continue;
                }
            }
            if (child instanceof PsiFileNode) {
                String name = ((PsiFileNode) child).getValue().getName();
                if (name.contains(".iml")) {
                    continue;
                }
            }
            Object value = child.getValue();
            if (value instanceof PsiElement) {
                PsiFile psiFile = ((PsiElement) value).getContainingFile();
                if (psiFile != null) {
                    VirtualFile virtualFile = psiFile.getVirtualFile();
                    if (virtualFile.getParent() != null && virtualFile.getParent().getName().contains(EduNames.TASK)) {
                        if (!CCProjectService.getInstance(project).isTaskFile(virtualFile)) {
                            newNodes.add(new CCStudentInvisibleFileNode(project, psiFile, settings));
                            continue;
                        }
                    }
                }
            }
            newNodes.add(child);
        }
        return newNodes;
    }
}
