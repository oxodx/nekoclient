/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.utils.misc.text;

/**
 * Allows arbitrary code execution in a click event
 */
public class RunnableClickEvent extends NekoClickEvent {
  public final Runnable runnable;

  public RunnableClickEvent(Runnable runnable) {
    super(null); // Should ensure no vanilla code is triggered, and only we handle it
    this.runnable = runnable;
  }
}
