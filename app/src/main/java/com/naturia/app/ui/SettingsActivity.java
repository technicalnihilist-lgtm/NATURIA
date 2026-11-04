package com.naturia.app.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.naturia.app.R;
import com.naturia.app.data.AppDb;
import com.naturia.app.data.CatalogRepository;

/** Appearance, storage and history controls. */
public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS = "naturia_prefs";
    public static final String KEY_THEME = "theme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        RadioGroup rg = findViewById(R.id.rg_theme);
        int mode = sp.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) rg.check(R.id.rb_light);
        else if (mode == AppCompatDelegate.MODE_NIGHT_YES) rg.check(R.id.rb_dark);
        else rg.check(R.id.rb_system);

        rg.setOnCheckedChangeListener((group, checkedId) -> {
            int target = checkedId == R.id.rb_light ? AppCompatDelegate.MODE_NIGHT_NO
                    : checkedId == R.id.rb_dark ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            sp.edit().putInt(KEY_THEME, target).apply();
            AppCompatDelegate.setDefaultNightMode(target);
        });

        TextView storage = findViewById(R.id.tv_storage);
        CatalogRepository repo = CatalogRepository.get(this);
        storage.setText(repo.all().size() + " sounds live from the CDN"
                + (repo.generated().isEmpty() ? "" : "\nCatalogue updated " + repo.generated())
                + "\n" + CatalogRepository.CDN_BASE + "\nTap to refresh now — new recordings appear without an app update.");
        storage.setOnClickListener(x -> {
            repo.refresh();
            Toast.makeText(this, "Checking for new sounds…", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_clear_history).setOnClickListener(v -> {
            AppDb.get(this).clearHistory();
            Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btn_about).setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class)));
    }
}
