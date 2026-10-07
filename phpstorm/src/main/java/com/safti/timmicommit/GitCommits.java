package com.safti.timmicommit;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.config.GitConfigUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/** Lecture des informations de commit via la commande git du plugin Git de l'IDE. */
final class GitCommits {

  record Commit(String hash, List<String> parents, long commitDateMs, String authorEmail, String message) {}

  private GitCommits() {}

  static Commit getCommit(Project project, VirtualFile root, String hash) throws VcsException {
    GitLineHandler handler = new GitLineHandler(project, root, GitCommand.LOG);
    handler.setSilent(true);
    handler.addParameters("-1", "--format=%P%n%ct%n%ae%n%B", hash);
    GitCommandResult result = Git.getInstance().runCommand(handler);
    List<String> lines = result.getOutputOrThrow().lines().toList();
    if (lines.size() < 3) {
      throw new VcsException("Sortie inattendue de git log pour " + hash);
    }
    List<String> parents = lines.get(0).isBlank() ? List.of() : Arrays.asList(lines.get(0).trim().split(" "));
    long date = Long.parseLong(lines.get(1).trim()) * 1000;
    String message = String.join("\n", lines.subList(3, lines.size())).strip();
    return new Commit(hash, parents, date, lines.get(2).trim(), message);
  }

  static @Nullable String getUserEmail(Project project, VirtualFile root) {
    try {
      return GitConfigUtil.getValue(project, root, "user.email");
    } catch (VcsException e) {
      return null;
    }
  }
}
