package com.safti.timmicommit;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

/** Réglages globaux (Settings → Tools → Timmi Commit), équivalents de la section `timmiCommit` VS Code. */
@State(name = "TimmiCommitSettings", storages = @Storage("timmi-commit.xml"))
public final class TimmiSettings implements PersistentStateComponent<TimmiSettings.State> {

  public static final String DEFAULT_URL = "https://safti.ilucca.net/timmi-timesheet/submission";

  public static final class State {
    public boolean enabled = true;
    public String team = "EPSILON";
    public String customTeam = "";
    public String browser = "default";
    public String browserPath = "";
    public String url = DEFAULT_URL;
    public boolean searchBranchName = true;
    public boolean askBeforeOpening = false;
    public boolean openWithoutTicket = true;
  }

  private State state = new State();

  public static TimmiSettings getInstance() {
    return ApplicationManager.getApplication().getService(TimmiSettings.class);
  }

  @Override
  public @NotNull State getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull State loaded) {
    XmlSerializerUtil.copyBean(loaded, state);
  }
}
