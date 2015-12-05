package com.jetbrains.edu.kotlin;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.EduNames;
import com.jetbrains.edu.courseFormat.TaskFile;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.projectView.StudyDirectoryNode;
import com.jetbrains.edu.learning.projectView.StudyTreeStructureProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public class KotlinStudyTreeStructureProvider extends StudyTreeStructureProvider {
    private static final String UTIL_DIR = "util";
    private static final String OUT_DIR = "out";
    @NotNull
    @Override
    public Collection<AbstractTreeNode> modify(@NotNull AbstractTreeNode parent,
                                               @NotNull Collection<AbstractTreeNode> children,
                                               ViewSettings settings) {
        if (!isCourseBasedProject(parent)) {
            return children;
        }
        Collection<AbstractTreeNode> nodes = new ArrayList<AbstractTreeNode>();
        for (AbstractTreeNode node : children) {
            final Project project = node.getProject();
            if (project != null) {
                if (node.getValue() instanceof PsiDirectory) {
                    final PsiDirectory nodeValue = (PsiDirectory)node.getValue();
                    final String name = nodeValue.getName();
                    if (!name.equals(UTIL_DIR) && !name.equals(OUT_DIR) && !name.contains(EduNames.USER_TESTS) && !name.startsWith(".")) {
                        AbstractTreeNode newNode = createStudyDirectoryNode(settings, project, nodeValue);
                        nodes.add(newNode);
                    }
                }
                else {
                    if (parent instanceof StudyDirectoryNode && node instanceof PsiFileNode) {
                        final PsiFileNode psiFileNode = (PsiFileNode)node;
                        final VirtualFile virtualFile = psiFileNode.getVirtualFile();
                        if (virtualFile == null) {
                            return nodes;
                        }
                        final TaskFile taskFile = StudyUtils.getTaskFile(project, virtualFile);
                        if (taskFile != null) {
                            nodes.add(node);
                        }
                        final String parentName = parent.getName();
                        if (parentName != null) {
                            if (parentName.equals(EduNames.SANDBOX_DIR)) {
                                nodes.add(node);
                            }
                            if (parentName.startsWith(EduNames.TASK)) {
                                addNonInvisibleFiles(nodes, node, project, virtualFile);
                            }
                        }
                    }
                }
            }
        }
        return nodes;
    }

    private static void addNonInvisibleFiles(@NotNull final Collection<AbstractTreeNode> nodes,
                                             @NotNull final AbstractTreeNode node,
                                             @NotNull final Project project,
                                             @NotNull final VirtualFile virtualFile) {
        if (!StudyTaskManager.getInstance(project).isInvisibleFile(virtualFile.getPath())) {
            String fileName = virtualFile.getName();
            if (!fileName.contains(EduNames.WINDOW_POSTFIX) && !fileName.contains(EduNames.WINDOWS_POSTFIX)
                    && !StudyUtils.isTestsFile(project, fileName) && !EduNames.TASK_HTML.equals(fileName) && !fileName.contains(".answer")) {
                nodes.add(node);
            }
        }
    }
}
