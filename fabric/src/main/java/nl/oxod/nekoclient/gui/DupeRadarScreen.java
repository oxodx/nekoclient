package nl.oxod.nekoclient.gui;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.utils.render.color.Color;
import nl.oxod.nekoclient.systems.modules.misc.DupeRadarModule;
import nl.oxod.nekoclient.util.DupeRadar;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class DupeRadarScreen extends WindowScreen {
  private final DupeRadarModule module;
  private WLabel statusLabel;
  private WLabel userLabel;
  private WLabel errorLabel;
  private WLabel countLabel;
  private WView matchesView;
  private WTable matchesTable;
  private WTextBox pluginBox;
  private WButton authButton;
  private List<DupeRadar.RadarMatch> lastMatches;

  public DupeRadarScreen(GuiTheme theme, DupeRadarModule module) {
    super(theme, "DupeDB Radar");
    this.module = module;
  }

  @Override
  public void initWidgets() {
    WTable header = add(theme.table()).expandX().widget();

    WHorizontalList authList = header.add(theme.horizontalList()).expandCellX().widget();
    authButton = authList.add(theme.button("Login")).widget();
    authButton.action = () -> {
      DupeRadar.RadarState state = DupeRadar.state();
      if (state.authenticated()) {
        DupeRadar.logout();
      } else {
        DupeRadar.login();
      }
      authButton.set("...");
      statusLabel.set("Working...");
    };

    WButton refreshButton = authList.add(theme.button("Refresh User")).widget();
    refreshButton.action = DupeRadar::refreshUser;

    WButton copyButton = authList.add(theme.button("Copy Report")).widget();
    copyButton.action = () -> mc.keyboardHandler.setClipboard(DupeRadar.copyReport());

    header.row();

    WTable serverTable = header.add(theme.table()).expandX().widget();
    WLabel checkTitle = serverTable.add(theme.label("Server / plugin check:")).top().widget();
    checkTitle.color = new Color(0xFFFF3B3B);

    WHorizontalList checkList = serverTable.add(theme.horizontalList()).expandCellX().widget();
    pluginBox = checkList.add(theme.textBox("")).minWidth(180).expandX().widget();
    WButton checkButton = checkList.add(theme.button("Check")).widget();
    checkButton.action = () -> check(pluginBox.get().isBlank() ? null : pluginBox.get());
    WButton clearButton = checkList.add(theme.button("Clear")).widget();
    clearButton.action = DupeRadar::clearServerResults;

    add(theme.horizontalSeparator("RADAR")).expandX();

    userLabel = add(theme.label("User: --")).expandX().widget();
    statusLabel = add(theme.label("Status: --")).expandX().widget();
    errorLabel = add(theme.label("")).expandX().widget();
    errorLabel.color = new Color(0xFFFF5555);
    countLabel = add(theme.label("Matches: 0")).expandX().widget();

    add(theme.horizontalSeparator("MATCHES")).expandX();

    matchesView = add(theme.view()).expandX().widget();
    matchesView.minWidth = 400;
    matchesView.maxHeight = theme.scale(130);
    matchesTable = matchesView.add(theme.table()).expandX().widget();
  }

  @Override
  public void tick() {
    super.tick();
    DupeRadar.RadarState state = DupeRadar.state();
    if (userLabel != null) {
      userLabel.set("User: " + (state.username() == null || state.username().isBlank() ? "--" : state.username()));
    }
    if (authButton != null) {
      authButton.set(state.authenticated() ? "Logout" : "Login");
    }
    if (statusLabel != null) {
      statusLabel.set("Status: " + (state.status() == null ? "--" : state.status()));
      statusLabel.color = state.authenticating() || state.checking() ? new Color(0xFF59C8E8) : state.error() != null && !state.error().isBlank()
        ? new Color(0xFFFF5555) : new Color(0xFF3FE87E);
    }
    if (errorLabel != null) {
      errorLabel.set(state.error() == null || state.error().isBlank() ? "" : "Error: " + state.error());
    }
    if (countLabel != null) {
      countLabel.set("Matches: " + state.matches().size() + "  (plugins checked: " + state.detectedPluginCount() + ")");
    }
    refreshMatches(state);
  }

  private void check(String plugin) {
    if (plugin == null || plugin.isBlank()) {
      DupeRadar.checkServer(List.of(), false);
      return;
    }
    DupeRadar.RadarPluginSnapshot snapshot = DupeRadar.pluginSnapshot(plugin);
    DupeRadar.checkServer(List.of(snapshot), false);
  }

  private void refreshMatches(DupeRadar.RadarState state) {
    List<DupeRadar.RadarMatch> matches = state.matches();
    if (matchesTable == null || matches == lastMatches) return;
    lastMatches = matches;
    matchesTable.clear();
    if (matches.isEmpty()) {
      matchesTable.add(theme.label("No matches.")).expandX();
      return;
    }
    int shown = Math.min(matches.size(), 30);
    for (int i = 0; i < shown; i++) {
      DupeRadar.RadarMatch match = matches.get(i);
      String status = DupeRadar.highestStatusLabel(match.findings());
      WLabel statusLabel = matchesTable.add(theme.label(status)).widget();
      statusLabel.color = new Color(DupeRadar.statusColor(status));
      matchesTable.add(theme.label(shortLabel(match.displayLabel()))).expandCellX().widget();
      matchesTable.add(theme.label(match.matchConfidence() + " " + match.matchSource().label())).widget();
      matchesTable.add(theme.label("x" + match.findings().size())).widget();

      WButton openButton = matchesTable.add(theme.button("Open")).widget();
      String url = match.sourceUrl();
      openButton.action = () -> DupeRadar.open(url);

      matchesTable.row();
    }
    if (matches.size() > shown) {
      matchesTable.add(theme.label("+" + (matches.size() - shown) + " more")).expandX();
    }
  }

  private static String shortLabel(String text) {
    if (text == null) return "";
    if (text.length() <= 26) return text;
    return text.substring(0, 26) + "...";
  }
}
