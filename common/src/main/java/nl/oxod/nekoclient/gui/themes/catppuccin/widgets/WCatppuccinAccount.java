/*
 * Copyright (c) NekoClient.
 */

package nl.oxod.nekoclient.gui.themes.catppuccin.widgets;

import nl.oxod.nekoclient.gui.WidgetScreen;
import nl.oxod.nekoclient.gui.widgets.WAccount;
import nl.oxod.nekoclient.systems.accounts.Account;
import nl.oxod.nekoclient.utils.render.color.Color;
import nl.oxod.nekoclient.gui.themes.catppuccin.CatppuccinWidget;

public class WCatppuccinAccount extends WAccount implements CatppuccinWidget {
    public WCatppuccinAccount(WidgetScreen screen, Account<?> account) {
        super(screen, account);
    }

    @Override
    protected Color loggedInColor() {
        return theme().greenColor();
    }

    @Override
    protected Color accountTypeColor() {
        return theme().textSecondaryColor();
    }
}