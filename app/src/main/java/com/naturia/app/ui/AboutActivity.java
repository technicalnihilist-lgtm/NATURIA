package com.naturia.app.ui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.naturia.app.R;
import com.naturia.app.data.CatalogRepository;
import com.naturia.app.model.Sound;

import java.util.List;

/** Attribution register: creator, licence and original source for every recording. */
public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        List<Sound> sounds = CatalogRepository.get(this).all();
        TextView intro = findViewById(R.id.about_intro);
        intro.setText(sounds.size() + " recordings. Every file was licence-checked before download: "
                + "only CC0, public domain, CC BY and CC BY-SA are accepted, and anything marked "
                + "non-commercial or no-derivatives is skipped automatically. Audio is transcoded to "
                + "96 kbps mono for streaming and offline playback, so the Creative Commons "
                + "attribution below is shown for each sound. Tap any row to open the original source. "
                + "Artwork: CC-licensed nature photography from Wikimedia Commons.");

        RecyclerView rv = findViewById(R.id.rv_attribution);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new Adapters.AttributionAdapter(this, sounds));
    }
}
