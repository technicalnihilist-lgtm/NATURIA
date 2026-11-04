package com.naturia.app.playback;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.naturia.app.R;
import com.naturia.app.data.AppDb;
import com.naturia.app.data.CatalogRepository;
import com.naturia.app.model.MixLayer;
import com.naturia.app.model.Sound;
import com.naturia.app.ui.PlayerActivity;

import java.util.ArrayList;
import java.util.List;

/** Foreground media service: single-sound playback plus the multi-layer mix engine. */
@UnstableApi
public class PlaybackService extends MediaSessionService {

    public static final String ACTION_PLAY = "com.naturia.app.action.PLAY";
    public static final String ACTION_TOGGLE = "com.naturia.app.action.TOGGLE";
    public static final String ACTION_STOP = "com.naturia.app.action.STOP";
    public static final String ACTION_MIX = "com.naturia.app.action.MIX";
    public static final String ACTION_SLEEP = "com.naturia.app.action.SLEEP";
    public static final String ACTION_VOLUME = "com.naturia.app.action.VOLUME";

    public static final String EXTRA_ID = "id";
    public static final String EXTRA_IDS = "ids";
    public static final String EXTRA_VOLS = "vols";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_MINUTES = "minutes";
    public static final String EXTRA_VALUE = "value";

    private static final String CHANNEL_MIX = "naturia_mix";
    private static final int MIX_NOTIFICATION_ID = 42;
    private static final int MAX_LAYERS = 6;

    private ExoPlayer player;
    private MediaSession session;
    private final List<ExoPlayer> mixPlayers = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable sleepRunnable;
    private boolean sleeping = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(attrs, true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .build();
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.setVolume(PlaybackHub.volume);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                PlaybackHub.playing = player.isPlaying();
                PlaybackHub.buffering = state == Player.STATE_BUFFERING;
                pushState();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (PlaybackHub.mode == PlaybackHub.MODE_SINGLE) {
                    PlaybackHub.playing = isPlaying;
                    pushState();
                }
            }

            @Override
            public void onPlayerError(androidx.media3.common.PlaybackException error) {
                PlaybackHub.buffering = false;
                PlaybackHub.playing = false;
                pushState();
            }
        });

        Intent open = new Intent(this, PlayerActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        session = new MediaSession.Builder(this, player)
                .setSessionActivity(pi)
                .setCallback(new MediaSession.Callback() { })
                .build();

        handler.post(ticker);
        createMixChannel();
    }

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (PlaybackHub.mode == PlaybackHub.MODE_SINGLE && player != null) {
                PlaybackHub.positionMs = player.getCurrentPosition();
                PlaybackHub.durationMs = Math.max(0, player.getDuration());
                PlaybackHub.playing = player.isPlaying();
            }
            if (PlaybackHub.sleepEndsAt > 0 && System.currentTimeMillis() > PlaybackHub.sleepEndsAt) {
                PlaybackHub.sleepEndsAt = 0;
                stopEverything();
            }
            if (PlaybackHub.mode != PlaybackHub.MODE_NONE) PlaybackHub.notifyChanged();
            handler.postDelayed(this, 800);
        }
    };

    private void pushState() {
        PlaybackHub.notifyChanged();
        updateMixNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PLAY:
                    playSingle(intent.getStringExtra(EXTRA_ID));
                    break;
                case ACTION_TOGGLE:
                    toggle();
                    break;
                case ACTION_STOP:
                    stopEverything();
                    break;
                case ACTION_MIX:
                    startMix(intent.getStringArrayExtra(EXTRA_IDS),
                            intent.getFloatArrayExtra(EXTRA_VOLS),
                            intent.getStringExtra(EXTRA_TITLE));
                    break;
                case ACTION_SLEEP:
                    setSleepTimer(intent.getIntExtra(EXTRA_MINUTES, 0));
                    break;
                case ACTION_VOLUME:
                    setVolume(intent.getFloatExtra(EXTRA_VALUE, 1f));
                    break;
                default:
                    break;
            }
        }
        return START_STICKY;
    }

    /* ---------------- single sound ---------------- */

    private void playSingle(String id) {
        if (id == null) return;
        Sound s = CatalogRepository.get(this).byId(id);
        if (s == null) return;
        stopMix(false);
        sleeping = false;
        restoreVolume();

        Uri uri = Uri.parse(CatalogRepository.get(this).audioUrl(s));

        MediaItem item = new MediaItem.Builder()
                .setUri(uri)
                .setMediaId(s.id)
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(s.name)
                        .setArtist("NATURIA · " + s.group)
                        .setAlbumTitle("Nature sounds")
                        .build())
                .build();

        player.setMediaItem(item);
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.prepare();
        player.play();

        PlaybackHub.mode = PlaybackHub.MODE_SINGLE;
        PlaybackHub.soundId = s.id;
        PlaybackHub.title = s.name;
        PlaybackHub.subtitle = s.group + " · " + s.letter;
        PlaybackHub.playing = true;
        PlaybackHub.mixLayers = new ArrayList<>();
        AppDb.get(this).markPlayed(s.id);
        pushState();
    }

    private void toggle() {
        if (PlaybackHub.mode == PlaybackHub.MODE_MIX) {
            boolean play = !mixPlayers.isEmpty() && !mixPlayers.get(0).getPlayWhenReady();
            for (ExoPlayer p : mixPlayers) p.setPlayWhenReady(play);
            PlaybackHub.playing = play;
        } else if (player != null) {
            if (player.isPlaying()) player.pause();
            else if (player.getPlaybackState() == Player.STATE_IDLE) player.prepare();
            else player.play();
            PlaybackHub.playing = player.isPlaying();
        }
        pushState();
    }

    public void setVolume(float v) {
        PlaybackHub.volume = v;
        if (PlaybackHub.mode == PlaybackHub.MODE_MIX) {
            for (int i = 0; i < mixPlayers.size(); i++) {
                MixLayer l = i < PlaybackHub.mixLayers.size() ? PlaybackHub.mixLayers.get(i) : null;
                mixPlayers.get(i).setVolume(l == null ? v : l.volume * v);
            }
        } else if (player != null) {
            player.setVolume(v);
        }
        pushState();
    }

    /* ---------------- mix engine ---------------- */

    private void startMix(String[] ids, float[] vols, String title) {
        stopMix(true);
        stopSinglePlayer();
        if (ids == null || ids.length == 0) return;

        CatalogRepository repo = CatalogRepository.get(this);
        List<MixLayer> layers = new ArrayList<>();
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

        int count = Math.min(ids.length, MAX_LAYERS);
        for (int i = 0; i < count; i++) {
            Sound s = repo.byId(ids[i]);
            if (s == null) continue;
            float vol = (vols != null && i < vols.length) ? vols[i] : 0.8f;
            ExoPlayer p = new ExoPlayer.Builder(this)
                    .setAudioAttributes(attrs, true)
                    .setWakeMode(C.WAKE_MODE_LOCAL)
                    .build();
            p.setRepeatMode(Player.REPEAT_MODE_ONE);
            Uri uri = Uri.parse(repo.audioUrl(s));
            p.setMediaItem(MediaItem.fromUri(uri));
            p.setVolume(vol * PlaybackHub.volume);
            p.prepare();
            p.play();
            mixPlayers.add(p);
            layers.add(new MixLayer(s.id, vol));
        }

        PlaybackHub.mode = PlaybackHub.MODE_MIX;
        PlaybackHub.mixLayers = layers;
        PlaybackHub.soundId = null;
        PlaybackHub.title = title == null || title.isEmpty() ? "Custom mix" : title;
        PlaybackHub.subtitle = layers.size() + " layers";
        PlaybackHub.playing = true;
        pushState();
        startForeground(MIX_NOTIFICATION_ID, mixNotification());
        handler.post(mixSync);
    }

    /** Keeps every layer in step with layer 0 so lock-screen play/pause affects the whole mix. */
    private final Runnable mixSync = new Runnable() {
        @Override
        public void run() {
            if (PlaybackHub.mode == PlaybackHub.MODE_MIX && !mixPlayers.isEmpty()) {
                boolean want = mixPlayers.get(0).getPlayWhenReady();
                for (int i = 1; i < mixPlayers.size(); i++) {
                    if (mixPlayers.get(i).getPlayWhenReady() != want) {
                        mixPlayers.get(i).setPlayWhenReady(want);
                    }
                }
                PlaybackHub.playing = want;
                handler.postDelayed(this, 400);
            }
        }
    };

    private void stopMix(boolean release) {
        for (ExoPlayer p : mixPlayers) {
            p.setPlayWhenReady(false);
            p.stop();
            if (release) p.release();
        }
        mixPlayers.clear();
        if (release && PlaybackHub.mode == PlaybackHub.MODE_MIX) {
            PlaybackHub.mode = PlaybackHub.MODE_NONE;
            PlaybackHub.playing = false;
            PlaybackHub.mixLayers = new ArrayList<>();
        }
    }

    private void stopSinglePlayer() {
        if (player != null) {
            player.pause();
            player.stop();
        }
    }

    private void stopEverything() {
        sleeping = false;
        stopMix(true);
        stopSinglePlayer();
        PlaybackHub.mode = PlaybackHub.MODE_NONE;
        PlaybackHub.playing = false;
        PlaybackHub.soundId = null;
        PlaybackHub.sleepEndsAt = 0;
        PlaybackHub.mixLayers = new ArrayList<>();
        PlaybackHub.notifyChanged();
        stopForeground(true);
    }

    /* ---------------- sleep timer ---------------- */

    private void setSleepTimer(int minutes) {
        if (sleepRunnable != null) handler.removeCallbacks(sleepRunnable);
        if (minutes <= 0) {
            PlaybackHub.sleepEndsAt = 0;
            PlaybackHub.notifyChanged();
            return;
        }
        PlaybackHub.sleepEndsAt = System.currentTimeMillis() + minutes * 60_000L;
        sleepRunnable = () -> fadeOutAndStop();
        handler.postDelayed(sleepRunnable, minutes * 60_000L);
        PlaybackHub.notifyChanged();
    }

    private void fadeOutAndStop() {
        sleeping = true;
        final float start = PlaybackHub.volume;
        final int steps = 20;
        for (int i = 0; i <= steps; i++) {
            final float factor = 1f - (i / (float) steps);
            handler.postDelayed(() -> {
                float v = start * factor;
                if (player != null) player.setVolume(v);
                for (int j = 0; j < mixPlayers.size(); j++) {
                    MixLayer l = j < PlaybackHub.mixLayers.size() ? PlaybackHub.mixLayers.get(j) : null;
                    mixPlayers.get(j).setVolume((l == null ? 1f : l.volume) * v);
                }
            }, i * 400L);
        }
        handler.postDelayed(() -> {
            stopEverything();
            restoreVolume();
        }, (steps + 1) * 400L + 200);
    }

    private void restoreVolume() {
        if (player != null) player.setVolume(PlaybackHub.volume);
        for (int j = 0; j < mixPlayers.size(); j++) {
            MixLayer l = j < PlaybackHub.mixLayers.size() ? PlaybackHub.mixLayers.get(j) : null;
            mixPlayers.get(j).setVolume((l == null ? 1f : l.volume) * PlaybackHub.volume);
        }
    }

    /* ---------------- mix notification ---------------- */

    private void createMixChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_MIX, "Sound mixing",
                    NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification mixNotification() {
        Intent toggle = new Intent(this, PlaybackService.class).setAction(ACTION_TOGGLE);
        Intent stop = new Intent(this, PlaybackService.class).setAction(ACTION_STOP);
        PendingIntent pt = PendingIntent.getService(this, 1, toggle,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent ps = PendingIntent.getService(this, 2, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent open = new Intent(this, PlayerActivity.class);
        PendingIntent po = PendingIntent.getActivity(this, 3, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_MIX)
                .setSmallIcon(R.drawable.ic_leaf)
                .setContentTitle(PlaybackHub.title)
                .setContentText(PlaybackHub.subtitle)
                .setContentIntent(po)
                .setOngoing(PlaybackHub.playing)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(PlaybackHub.playing ? R.drawable.ic_pause : R.drawable.ic_play,
                        PlaybackHub.playing ? "Pause" : "Play", pt)
                .addAction(R.drawable.ic_close, "Stop", ps)
                .build();
    }

    private void updateMixNotification() {
        if (PlaybackHub.mode == PlaybackHub.MODE_MIX && PlaybackHub.playing) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(MIX_NOTIFICATION_ID, mixNotification());
        }
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return session;
    }

    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        if (PlaybackHub.mode == PlaybackHub.MODE_NONE || !PlaybackHub.playing) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        stopMix(true);
        if (session != null) {
            session.release();
            session = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        PlaybackHub.mode = PlaybackHub.MODE_NONE;
        PlaybackHub.playing = false;
        super.onDestroy();
    }

    /* ---------------- helpers for the UI ---------------- */

    public static void play(Context c, Sound s) {
        Intent i = new Intent(c, PlaybackService.class).setAction(ACTION_PLAY).putExtra(EXTRA_ID, s.id);
        start(c, i);
    }

    public static void toggle(Context c) {
        start(c, new Intent(c, PlaybackService.class).setAction(ACTION_TOGGLE));
    }

    public static void stop(Context c) {
        start(c, new Intent(c, PlaybackService.class).setAction(ACTION_STOP));
    }

    public static void playMix(Context c, List<MixLayer> layers, String title) {
        String[] ids = new String[layers.size()];
        float[] vols = new float[layers.size()];
        for (int i = 0; i < layers.size(); i++) {
            ids[i] = layers.get(i).soundId;
            vols[i] = layers.get(i).volume;
        }
        Intent i = new Intent(c, PlaybackService.class).setAction(ACTION_MIX)
                .putExtra(EXTRA_IDS, ids).putExtra(EXTRA_VOLS, vols).putExtra(EXTRA_TITLE, title);
        start(c, i);
    }

    public static void sleep(Context c, int minutes) {
        start(c, new Intent(c, PlaybackService.class).setAction(ACTION_SLEEP).putExtra(EXTRA_MINUTES, minutes));
    }

    public static void volume(Context c, float v) {
        start(c, new Intent(c, PlaybackService.class).setAction(ACTION_VOLUME).putExtra(EXTRA_VALUE, v));
    }

    private static void start(Context c, Intent i) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) c.startForegroundService(i);
        else c.startService(i);
    }
}
