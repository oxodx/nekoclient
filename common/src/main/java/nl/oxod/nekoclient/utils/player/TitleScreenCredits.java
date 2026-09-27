/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.utils.player;

import nl.oxod.nekoclient.NekoClient;
import nl.oxod.nekoclient.addons.AddonManager;
import nl.oxod.nekoclient.addons.GithubRepo;
import nl.oxod.nekoclient.addons.NekoAddon;
import nl.oxod.nekoclient.gui.GuiThemes;
import nl.oxod.nekoclient.gui.screens.CommitsScreen;
import nl.oxod.nekoclient.mixininterface.IComponent;
import nl.oxod.nekoclient.utils.network.Http;
import nl.oxod.nekoclient.utils.network.NekoExecutor;
import nl.oxod.nekoclient.utils.render.NekoToast;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Items;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static nl.oxod.nekoclient.NekoClient.mc;

public class TitleScreenCredits {
  private static final List<Credit> credits = new ArrayList<>();

  private TitleScreenCredits() {
  }

  private static void init() {
    // Add addons
    for (NekoAddon addon : AddonManager.ADDONS) add(addon);

    // Sort by width (NekoClient always first)
    credits.sort(Comparator.comparingInt(value -> value.addon == NekoClient.ADDON ? Integer.MIN_VALUE : -mc.font.width(value.text)));

    // Check for latest commits
    NekoExecutor.execute(() -> {
      for (Credit credit : credits) {
        if (credit.addon.getRepo() == null || credit.addon.getCommit() == null) continue;

        GithubRepo repo = credit.addon.getRepo();
        Http.Request request = Http.get("https://api.github.com/repos/%s/branches/%s".formatted(repo.getOwnerName(), repo.branch()));
        request.exceptionHandler(e -> NekoClient.LOG.error("Could not fetch repository information for addon '{}'.", credit.addon.name, e));
        repo.authenticate(request);
        HttpResponse<Response> res = request.sendJsonResponse(Response.class);

        switch (res.statusCode()) {
          case Http.UNAUTHORIZED -> {
            String message = "Invalid authentication token for repository '%s'".formatted(repo.getOwnerName());
            NekoToast toast = new NekoToast.Builder("GitHub: Unauthorized").icon(Items.BARRIER).text(message).build();
            mc.gui.toastManager().addToast(toast);
            NekoClient.LOG.warn(message);
            if (System.getenv("meteor.github.authorization") == null) {
              NekoClient.LOG.info("Consider setting an authorization " +
                "token with the 'meteor.github.authorization' environment variable.");
              NekoClient.LOG.info("See: https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens");
            }
          }
          case Http.FORBIDDEN ->
            NekoClient.LOG.warn("Could not fetch updates for addon '{}': Rate-limited by GitHub.", credit.addon.name);
          case Http.NOT_FOUND ->
            NekoClient.LOG.warn("Could not fetch updates for addon '{}': GitHub repository '{}' not found.", credit.addon.name, repo.getOwnerName());
          case Http.SUCCESS -> {
            if (!credit.addon.getCommit().equals(res.body().commit.sha)) {
              synchronized (credit.text) {
                credit.text.append(Component.literal("*").withStyle(ChatFormatting.RED));
                ((IComponent) ((Component) credit.text)).neko$invalidateCache(); // ???
              }
            }
          }
        }
      }
    });
  }

  private static void add(NekoAddon addon) {
    Credit credit = new Credit(addon);

    credit.text.append(Component.literal(addon.name).withStyle(style -> style.withColor(addon.color.getPacked())));
    credit.text.append(Component.literal(" by ").withStyle(ChatFormatting.GRAY));

    for (int i = 0; i < addon.authors.length; i++) {
      if (i > 0) {
        credit.text.append(Component.literal(i == addon.authors.length - 1 ? " & " : ", ").withStyle(ChatFormatting.GRAY));
      }

      credit.text.append(Component.literal(addon.authors[i]).withStyle(ChatFormatting.WHITE));
    }

    credits.add(credit);
  }

  public static void render(GuiGraphicsExtractor graphics) {
    if (credits.isEmpty()) init();

    int y = 3;
    for (Credit credit : credits) {
      synchronized (credit.text) {
        int x = mc.gui.screen().width - 3 - mc.font.width(credit.text);

        graphics.text(mc.font, credit.text, x, y, -1);
      }

      y += mc.font.lineHeight + 2;
    }
  }

  public static boolean onClicked(double mouseX, double mouseY) {
    int y = 3;
    for (Credit credit : credits) {
      int width;
      synchronized (credit.text) {
        width = mc.font.width(credit.text);
      }

      int x = mc.gui.screen().width - 3 - width;

      if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + mc.font.lineHeight + 2) {
        if (credit.addon.getRepo() != null && credit.addon.getCommit() != null) {
          mc.gui.setScreen(new CommitsScreen(GuiThemes.get(), credit.addon));
          return true;
        }
      }

      y += mc.font.lineHeight + 2;
    }

    return false;
  }

  private static class Credit {
    public final NekoAddon addon;
    public final MutableComponent text = Component.empty();

    public Credit(NekoAddon addon) {
      this.addon = addon;
    }
  }

  private static class Response {
    public Commit commit;
  }

  private static class Commit {
    public String sha;
  }
}
