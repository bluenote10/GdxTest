package com.fablauncher;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.fab.*;

public class LauncherDesktop {
  public static void main (String[] args) {
    new LwjglApplication(new Game(), "Game", 1024, 768, false);
  }
}
