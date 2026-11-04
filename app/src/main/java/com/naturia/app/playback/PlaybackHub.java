package com.naturia.app.playback;

import com.naturia.app.model.MixLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** In-process playback state shared by the service and every screen. */
public class PlaybackHub {

    public static final int MODE_NONE = 0;
    public static final int MODE_SINGLE = 1;
    public static final int MODE_MIX = 2;

    public interface Listener {
        void onPlaybackState();
    }

    private static final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public static int mode = MODE_NONE;
    public static boolean playing = false;
    public static boolean buffering = false;
    public static String soundId;
    public static String title = "";
    public static String subtitle = "";
    public static long positionMs = 0;
    public static long durationMs = 0;
    public static float volume = 1f;
    public static boolean repeat = true;
    public static String error = null;
    public static long sleepEndsAt = 0;
    public static List<MixLayer> mixLayers = new ArrayList<>();

    public static void addListener(Listener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public static void removeListener(Listener l) {
        listeners.remove(l);
    }

    public static void notifyChanged() {
        for (Listener l : listeners) l.onPlaybackState();
    }

    public static boolean hasSession() {
        return mode != MODE_NONE;
    }
}
