/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.screens.accounts;

import nl.oxod.nekoclient.gui.GuiTheme;
import nl.oxod.nekoclient.gui.WindowScreen;
import nl.oxod.nekoclient.gui.widgets.containers.WHorizontalList;
import nl.oxod.nekoclient.gui.widgets.pressable.WButton;
import nl.oxod.nekoclient.systems.accounts.Account;
import nl.oxod.nekoclient.systems.accounts.AccountType;
import nl.oxod.nekoclient.systems.accounts.TokenAccount;
import nl.oxod.nekoclient.utils.render.color.Color;

import static nl.oxod.nekoclient.NekoClient.mc;

public class AccountInfoScreen extends WindowScreen {
  private final Account<?> account;

  public AccountInfoScreen(GuiTheme theme, Account<?> account) {
    super(theme, account.getUsername() + " details");
    this.account = account;
  }

  @Override
  public void initWidgets() {
    TokenAccount e = (TokenAccount) account;
    WHorizontalList l = add(theme.horizontalList()).expandX().widget();

    String tokenLabel = account.getType() + " token:";
    if (account.getType() == AccountType.Session) tokenLabel = "";

    WButton copy = theme.button("Copy");
    copy.action = () -> mc.keyboardHandler.setClipboard(e.getToken());

    l.add(theme.label(tokenLabel));
    l.add(theme.label(account.getType() == AccountType.Session ? "Click to copy Token" : e.getToken()).color(Color.GRAY)).pad(5);
    l.add(copy);
  }
}
