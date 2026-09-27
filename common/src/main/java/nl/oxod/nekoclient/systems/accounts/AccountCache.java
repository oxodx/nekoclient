/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.accounts;

import com.mojang.util.UndashedUuid;
import nl.oxod.nekoclient.utils.misc.ISerializable;
import nl.oxod.nekoclient.utils.misc.NbtException;
import nl.oxod.nekoclient.utils.network.NekoExecutor;
import nl.oxod.nekoclient.utils.render.PlayerHeadTexture;
import nl.oxod.nekoclient.utils.render.PlayerHeadUtils;
import net.minecraft.nbt.CompoundTag;

import static nl.oxod.nekoclient.NekoClient.mc;

public class AccountCache implements ISerializable<AccountCache> {
    public String username = "";
    public String uuid = "";
    private PlayerHeadTexture headTexture;
    private volatile boolean loadingHead;

    public PlayerHeadTexture getHeadTexture() {
        return headTexture != null ? headTexture : PlayerHeadUtils.STEVE_HEAD;
    }

    public void loadHead() {
        loadHead(null);
    }

    public void loadHead(Runnable callback) {
        if (headTexture != null || uuid == null || uuid.isBlank()) {
            if (callback != null) mc.execute(callback);
            return;
        }

        if (loadingHead) return;

        loadingHead = true;

        NekoExecutor.execute(() -> {
            byte[] head = PlayerHeadUtils.fetchHead(UndashedUuid.fromStringLenient(uuid));

            mc.execute(() -> {
                if (head != null) headTexture = new PlayerHeadTexture(head, true);
                loadingHead = false;
                if (callback != null) callback.run();
            });
        });
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("username", username);
        tag.putString("uuid", uuid);

        return tag;
    }

    @Override
    public AccountCache fromTag(CompoundTag tag) {
        if (tag.getString("username").isEmpty() || tag.getString("uuid").isEmpty()) throw new NbtException();

        username = tag.getString("username").get();
        uuid = tag.getString("uuid").get();
        loadHead();

        return this;
    }
}
