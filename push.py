import os
import subprocess
import tempfile
from abc import ABC, abstractmethod

repo = os.getcwd()
check_tests = False
branches = ['studio-181', '181', 'studio-173', '173', '182-1.8-release', '181-1.8-release', '173-1.8-release']
master = 'master'


class ShellCommandExecutor(ABC):

    def __init__(self, root_dir):
        self.root_dir = root_dir

    @staticmethod
    @abstractmethod
    def __command__():
        pass

    def execute(self, params):
        return subprocess.check_output("{0} {1}".format(self.__command__(), params), cwd=self.root_dir, shell=True,
                                       universal_newlines=True)


class Git(ShellCommandExecutor):
    @staticmethod
    def __command__():
        return "git"

    def add_worktree(self, dir, branch):
        return self.execute("worktree add {0} origin/{1}".format(dir, branch))

    def prune_worktrees(self):
        return self.execute("worktree prune")

    def checkout_temp_branch(self, branch):
        return self.execute("checkout -b temp-{0} origin/{0}".format(branch))

    def fetch(self):
        return self.execute("fetch")

    def cherry_pick(self, hash):
        return self.execute("cherry-pick {0}".format(hash))

    def get_unpushed_master_commits(self):
        return self.execute("log origin/master..master --pretty=format:\"%h\"").split("\n")[::-1]

    def push(self, branch):
        return self.execute("push origin HEAD:{0}".format(branch))

    def remove_temp_branch(self, branch):
        return self.execute("branch -D temp-{0}".format(branch))


class Gradle(ShellCommandExecutor):
    @staticmethod
    def __command__():
        return "./gradlew"

    def build_plugin(self):
        return self.execute("buildPlugin")

    def run_tests(self):
        return self.execute("test --rerun-tasks")


if __name__ == '__main__':
    sourceRepo = Git(repo)
    sourceRepo.fetch()
    for branch in branches:
        try:
            with tempfile.TemporaryDirectory() as tempDir:
                sourceRepo.add_worktree(tempDir, branch)
                working_Tree = Git(tempDir)
                working_Tree.checkout_temp_branch(branch)

                for commit in sourceRepo.get_unpushed_master_commits():
                    working_Tree.cherry_pick(commit)

                gradle = Gradle(tempDir)
                gradle.build_plugin()
                if check_tests:
                    gradle.run_tests()

                working_Tree.push(branch)

        finally:
            sourceRepo.prune_worktrees()
            sourceRepo.remove_temp_branch(branch)

    sourceRepo.push(master)