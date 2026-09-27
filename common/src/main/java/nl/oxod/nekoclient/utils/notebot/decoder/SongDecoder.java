/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.utils.notebot.decoder;

import nl.oxod.nekoclient.systems.modules.Modules;
import nl.oxod.nekoclient.systems.modules.misc.Notebot;
import nl.oxod.nekoclient.utils.notebot.song.Song;

import java.io.File;

public abstract class SongDecoder {
  protected Notebot notebot = Modules.get().get(Notebot.class);

  /**
   * Parse file to a {@link Song} object
   *
   * @param file Song file
   * @return A {@link Song} object
   */
  public abstract Song parse(File file) throws Exception;
}
