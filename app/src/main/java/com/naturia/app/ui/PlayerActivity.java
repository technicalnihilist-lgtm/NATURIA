package com.naturia.app.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.naturia.app.R;
import com.naturia.app.data.AppDb;
import com.naturia.app.data.CatalogRepository;
import com.naturia.app.model.MixLayer;
import com.naturia.app.model.Sound;
import com.naturia.app.playback.PlaybackHub;
import com.naturia.app.playback.PlaybackService;
import com.naturia.app.util.Art;

import java.util.ArrayList;

/** Full player: artwork, transport, repeat, volume, sleep timer, licence sheet, mix hand-off. */
public class PlayerActivity extends AppCompatActivity implements PlaybackHub.Listener {

    private ImageView art, play, favorite, info, repeat, sleep, addMix;
    private TextView title, sub, pos, dur, license;
    private SeekBar seek, volume;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Sound sound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        art = findViewById(R.id.player_art);
        play = findViewById(R.id.btn_play);
        favorite = findViewById(R.id.btn_favorite);
        info = findViewById(R.id.btn_info);
        repeat = findViewById(R.id.btn_repeat);
        sleep = findViewById(R.id.btn_sleep);
        addMix = findViewById(R.id.btn_add_mix);
        title = findViewById(R.id.player_title);
        sub = findViewById(R.id.player_sub);
        pos = findViewById(R.id.player_pos);
        dur = findViewById(R.id.player_dur);
        license = findViewById(R.id.player_license);
        seek = findViewById(R.id.player_seek);
        volume = findViewById(R.id.player_volume);

        String id = getIntent().getStringExtra("id");
        if (id == null) id = PlaybackHub.soundId;
        sound = CatalogRepository.get(this).byId(id);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        play.setOnClickListener(v -> PlaybackService.toggle(this));
        favorite.setOnClickListener(v -> {
            if (sound == null) return;
            AppDb.get(this).toggleFavorite(sound.id);
            refreshFavorite();
        });
        info.setOnClickListener(v -> showLicence());
        repeat.setOnClickListener(v -> {
            PlaybackHub.repeat = !PlaybackHub.repeat;
            Toast.makeText(this, PlaybackHub.repeat ? "Loop on" : "Loop off", Toast.LENGTH_SHORT).show();
            repeat.setAlpha(PlaybackHub.repeat ? 1f : 0.45f);
        });
        sleep.setOnClickListener(v -> showSleepDialog());
        addMix.setOnClickListener(v -> {
            if (sound == null) return;
            for (MixLayer l : MixFragment.DRAFT) {
                if (l.soundId.equals(sound.id)) {
                    Toast.makeText(this, "Already in the studio", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            if (MixFragment.DRAFT.size() >= 6) {
                Toast.makeText(this, "Studio is full (6 layers)", Toast.LENGTH_SHORT).show();
                return;
            }
            MixFragment.DRAFT.add(new MixLayer(sound.id, 0.75f));
            Toast.makeText(this, sound.name + " added to studio", Toast.LENGTH_SHORT).show();
        });

        volume.setProgress((int) (PlaybackHub.volume * 100));
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) { }
            @Override public void onStartTrackingTouch(SeekBar sb) { }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                PlaybackService.volume(PlayerActivity.this, sb.getProgress() / 100f);
            }
        });

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) { }
            @Override public void onStartTrackingTouch(SeekBar sb) { }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                if (PlaybackHub.durationMs > 0) {
                    long target = (long) (PlaybackHub.durationMs * (sb.getProgress() / 1000f));
                    PlaybackService.play(PlayerActivity.this, sound);
                }
            }
        });

        bind();
        PlaybackHub.addListener(this);
        handler.post(tick);
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            bind();
            handler.postDelayed(this, 500);
        }
    };

    private void bind() {
        if (sound != null) {
            title.setText(PlaybackHub.mode == PlaybackHub.MODE_MIX ? PlaybackHub.title : sound.name);
            sub.setText(PlaybackHub.mode == PlaybackHub.MODE_MIX ? PlaybackHub.subtitle : sound.subtitle());
            Art.load(this, art, sound.group);
            license.setText(sound.license + " · " + sound.creator + " · " + sound.provider);
        } else {
            title.setText(PlaybackHub.title);
            sub.setText(PlaybackHub.subtitle);
        }
        play.setImageResource(PlaybackHub.playing ? R.drawable.ic_pause_dark : R.drawable.ic_play_dark);
        pos.setText(Art.clock(PlaybackHub.positionMs));
        dur.setText(Art.clock(PlaybackHub.durationMs > 0 ? PlaybackHub.durationMs
                : (sound == null ? 0 : (long) (sound.duration * 1000))));
        if (PlaybackHub.durationMs > 0) {
            seek.setProgress((int) (PlaybackHub.positionMs * 1000 / PlaybackHub.durationMs));
        }
        repeat.setAlpha(PlaybackHub.repeat ? 1f : 0.45f);
        refreshFavorite();
    }

    private void refreshFavorite() {
        if (sound == null) return;
        favorite.setImageResource(AppDb.get(this).isFavorite(sound.id)
                ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
    }

    /** Every sound streams online, so the player shows provenance instead of a download button. */
    private void showLicence() {
        if (sound == null) return;
        String body = sound.name + "\n\n" + sound.license
                + "\nCreator: " + sound.creator
                + "\nSource: " + sound.provider
                + "\nLength: " + sound.durationLabel() + " (seamless loop)"
                + "\nStreamed live from the NATURIA CDN.";
        new AlertDialog.Builder(this)
                .setTitle("Licence & source")
                .setMessage(body)
                .setPositiveButton("Open source page", (d, w) -> {
                    if (sound.sourcePage != null && !sound.sourcePage.isEmpty()) {
                        try {
                            startActivity(new android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(sound.sourcePage)));
                        } catch (Exception ignored) {
                        }
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showSleepDialog() {
        final int[] minutes = {5, 10, 15, 30, 60};
        String[] labels = {"5 minutes", "10 minutes", "15 minutes", "30 minutes", "60 minutes", "Off"};
        new AlertDialog.Builder(this)
                .setTitle(R.string.sleep_timer)
                .setItems(labels, (d, which) -> {
                    int m = which < minutes.length ? minutes[which] : 0;
                    PlaybackService.sleep(this, m);
                    Toast.makeText(this, m == 0 ? "Sleep timer off"
                            : "Sleeping in " + m + " min", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    @Override
    public void onPlaybackState() {
        runOnUiThread(this::bind);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(tick);
        PlaybackHub.removeListener(this);
        super.onDestroy();
    }
}
