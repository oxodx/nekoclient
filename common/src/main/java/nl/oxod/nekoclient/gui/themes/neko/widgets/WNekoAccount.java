/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.themes.neko.widgets;

import nl.oxod.nekoclient.gui.WidgetScreen;
import nl.oxod.nekoclient.gui.themes.neko.NekoWidget;
import nl.oxod.nekoclient.gui.widgets.WAccount;
import nl.oxod.nekoclient.systems.accounts.Account;
import nl.oxod.nekoclient.utils.render.color.Color;

public class WNekoAccount extends WAccount implements NekoWidget {
  public WNekoAccount(WidgetScreen screen, Account<?> account) {
    super(screen, account);
  }

  @Override
  protected Color loggedInColor() {
    return theme().loggedInColor.get();
  }

  @Override
  protected Color accountTypeColor() {
    return theme().textSecondaryColor.get();
  }
}
