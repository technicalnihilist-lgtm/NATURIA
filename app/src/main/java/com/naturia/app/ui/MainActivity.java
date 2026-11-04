package com.naturia.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.naturia.app.R;
import com.naturia.app.data.CatalogRepository;
import com.naturia.app.model.MixLayer;
import com.naturia.app.model.Sound;
import com.naturia.app.playback.PlaybackHub;
import com.naturia.app.playback.PlaybackService;
import com.naturia.app.util.Art;

import java.util.List;

/** Shell: tab navigation, mini player and the shared playback bridge. */
public class MainActivity extends AppCompatActivity
        implements PlaybackHub.Listener, CatalogRepository.OnCatalogChanged {

    private View mini;
    private TextView miniTitle, miniSub;
    private ImageView miniPlay, miniThumb;
    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mini = findViewById(R.id.mini);
        miniTitle = findViewById(R.id.mini_title);
        miniSub = findViewById(R.id.mini_sub);
        miniPlay = findViewById(R.id.mini_play);
        miniThumb = findViewById(R.id.mini_thumb);

        findViewById(R.id.nav_home).setOnClickListener(v -> select(0));
        findViewById(R.id.nav_explore).setOnClickListener(v -> select(1));
        findViewById(R.id.nav_mix).setOnClickListener(v -> select(2));
        findViewById(R.id.nav_library).setOnClickListener(v -> select(3));

        miniPlay.setOnClickListener(v -> PlaybackService.toggle(this));
        findViewById(R.id.mini_close).setOnClickListener(v -> PlaybackService.stop(this));
        mini.setOnClickListener(v -> startActivity(new Intent(this, PlayerActivity.class)));

        select(0);
        PlaybackHub.addListener(this);
        CatalogRepository.get(this).addListener(this);
        CatalogRepository.get(this).refresh();
        onPlaybackState();
        askNotificationPermission();
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
        }
    }

    private void select(int index) {
        currentTab = index;
        Fragment f;
        switch (index) {
            case 1: f = new ExploreFragment(); break;
            case 2: f = new MixFragment(); break;
            case 3: f = new LibraryFragment(); break;
            default: f = new HomeFragment(); break;
        }
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        t.replace(R.id.container, f);
        t.commitAllowingStateLoss();
        tintNav(index);
    }

    private void tintNav(int index) {
        int active = ContextCompat.getColor(this, R.color.primary);
        int idle = ContextCompat.getColor(this, R.color.text_faint);
        int[] icons = {R.id.ic_nav_home, R.id.ic_nav_explore, R.id.ic_nav_mix, R.id.ic_nav_library};
        int[] labels = {R.id.tv_nav_home, R.id.tv_nav_explore, R.id.tv_nav_mix, R.id.tv_nav_library};
        for (int i = 0; i < icons.length; i++) {
            ImageView iv = findViewById(icons[i]);
            TextView tv = findViewById(labels[i]);
            int color = i == index ? active : idle;
            iv.setColorFilter(color);
            tv.setTextColor(color);
        }
    }

    public void openExplore(String query) {
        select(1);
        getSupportFragmentManager().executePendingTransactions();
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.container);
        if (f instanceof ExploreFragment) ((ExploreFragment) f).search(query);
    }

    public void playSound(Sound s) {
        PlaybackService.play(this, s);
        startActivity(new Intent(this, PlayerActivity.class).putExtra("id", s.id));
    }

    public void playMix(List<MixLayer> layers, String title) {
        if (layers.isEmpty()) return;
        PlaybackService.playMix(this, layers, title);
        startActivity(new Intent(this, PlayerActivity.class));
    }

    @Override
    public void onPlaybackState() {
        runOnUiThread(() -> {
            boolean show = PlaybackHub.hasSession();
            mini.setVisibility(show ? View.VISIBLE : View.GONE);
            if (!show) return;
            miniTitle.setText(PlaybackHub.title);
            miniSub.setText(PlaybackHub.subtitle);
            miniPlay.setImageResource(PlaybackHub.playing ? R.drawable.ic_pause_dark : R.drawable.ic_play_dark);
            if (PlaybackHub.mode == PlaybackHub.MODE_MIX) {
                miniThumb.setImageResource(R.drawable.ic_mix);
            } else {
                Sound s = com.naturia.app.data.CatalogRepository.get(this).byId(PlaybackHub.soundId);
                if (s != null) Art.load(this, miniThumb, s.group);
            }
        });
    }

    @Override
    public void onCatalogChanged(int total, int added, boolean fromNetwork) {
        if (added > 0 && fromNetwork) {
            runOnUiThread(() -> android.widget.Toast.makeText(this,
                    added + " new sound" + (added == 1 ? "" : "s") + " added live",
                    android.widget.Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    protected void onDestroy() {
        PlaybackHub.removeListener(this);
        CatalogRepository.get(this).removeListener(this);
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
