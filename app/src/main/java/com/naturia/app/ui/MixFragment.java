package com.naturia.app.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.naturia.app.R;
import com.naturia.app.data.AppDb;
import com.naturia.app.data.CatalogRepository;
import com.naturia.app.model.Mix;
import com.naturia.app.model.MixLayer;
import com.naturia.app.model.Sound;
import com.naturia.app.util.Art;

import java.util.ArrayList;
import java.util.List;

/** Sound mixing studio: layered playback with per-layer volume and saved mixes. */
public class MixFragment extends Fragment {

    public static final List<MixLayer> DRAFT = new ArrayList<>();

    private CatalogRepository repo;
    private LinearLayout layersBox;
    private TextView noLayers;
    private Adapters.MixAdapter mixAdapter;
    private RecyclerView mixesView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_mix, container, false);
        repo = CatalogRepository.get(requireContext());
        layersBox = v.findViewById(R.id.layers_box);
        noLayers = v.findViewById(R.id.tv_no_layers);

        mixesView = v.findViewById(R.id.rv_mixes);
        mixesView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mixAdapter = new Adapters.MixAdapter(requireContext(), new Adapters.OnMix() {
            @Override
            public void onPlay(Mix m) {
                ((MainActivity) requireActivity()).playMix(m.layers, m.name);
            }

            @Override
            public void onDelete(Mix m) {
                AppDb.get(requireContext()).deleteMix(m.id);
                refreshMixes();
            }
        });
        mixesView.setAdapter(mixAdapter);

        RecyclerView presets = v.findViewById(R.id.rv_presets);
        presets.setLayoutManager(new LinearLayoutManager(requireContext()));
        Adapters.MixAdapter presetAdapter = new Adapters.MixAdapter(requireContext(), new Adapters.OnMix() {
            @Override
            public void onPlay(Mix m) {
                DRAFT.clear();
                DRAFT.addAll(m.layers);
                renderLayers();
                ((MainActivity) requireActivity()).playMix(m.layers, m.name);
            }

            @Override
            public void onDelete(Mix m) { }
        });
        presets.setAdapter(presetAdapter);
        presetAdapter.submit(presets());

        v.findViewById(R.id.btn_add_sound).setOnClickListener(x -> openAddSheet());
        v.findViewById(R.id.btn_play_mix).setOnClickListener(x -> {
            if (DRAFT.isEmpty()) {
                Toast.makeText(requireContext(), "Add at least one sound", Toast.LENGTH_SHORT).show();
                return;
            }
            ((MainActivity) requireActivity()).playMix(new ArrayList<>(DRAFT), "Custom mix");
        });
        v.findViewById(R.id.btn_save_mix).setOnClickListener(x -> saveMix());

        renderLayers();
        refreshMixes();
        return v;
    }

    private List<Mix> presets() {
        List<Mix> out = new ArrayList<>();
        out.add(preset("Rain + Thunder", "gentle-rain", "distant-thunder"));
        out.add(preset("Ocean + Seagulls", "ocean-waves", "gulls"));
        out.add(preset("Forest + Birds", "forest-ambience", "birds-chirping"));
        out.add(preset("River + Wind", "river-flowing", "wind-in-trees"));
        out.add(preset("Campfire + Night Crickets", "campfire", "night-crickets"));
        List<Mix> clean = new ArrayList<>();
        for (Mix m : out) if (!m.layers.isEmpty()) clean.add(m);
        return clean;
    }

    private Mix preset(String name, String... ids) {
        Mix m = new Mix(name);
        for (String id : ids) {
            if (repo.byId(id) != null) m.layers.add(new MixLayer(id, 0.75f));
        }
        return m;
    }

    private void refreshMixes() {
        List<Mix> list = AppDb.get(requireContext()).mixes();
        mixAdapter.submit(list);
        mixesView.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void renderLayers() {
        layersBox.removeAllViews();
        noLayers.setVisibility(DRAFT.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < DRAFT.size(); i++) {
            final int index = i;
            MixLayer layer = DRAFT.get(i);
            Sound s = repo.byId(layer.soundId);
            View row = inflater.inflate(R.layout.item_layer, layersBox, false);
            ((TextView) row.findViewById(R.id.layer_name)).setText(s == null ? layer.soundId : s.name);
            Art.load(requireContext(), row.findViewById(R.id.layer_art), s == null ? "Forest" : s.group);
            SeekBar bar = row.findViewById(R.id.layer_slider);
            bar.setProgress((int) (layer.volume * 100));
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                    DRAFT.get(index).volume = p / 100f;
                }
                @Override public void onStartTrackingTouch(SeekBar sb) { }
                @Override public void onStopTrackingTouch(SeekBar sb) {
                    if (PlaybackHubMode.isMix()) {
                        com.naturia.app.playback.PlaybackService.playMix(requireContext(),
                                new ArrayList<>(DRAFT), "Custom mix");
                    }
                }
            });
            row.findViewById(R.id.layer_remove).setOnClickListener(x -> {
                DRAFT.remove(index);
                renderLayers();
            });
            layersBox.addView(row);
        }
    }

    private void openAddSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.sheet_add_sound, null);
        sheet.setContentView(v);
        RecyclerView list = v.findViewById(R.id.sheet_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        Adapters.SoundRowAdapter adapter = new Adapters.SoundRowAdapter(requireContext(), s -> {
            if (DRAFT.size() >= 6) {
                Toast.makeText(requireContext(), "Maximum 6 layers", Toast.LENGTH_SHORT).show();
                return;
            }
            for (MixLayer l : DRAFT) if (l.soundId.equals(s.id)) return;
            DRAFT.add(new MixLayer(s.id, 0.75f));
            renderLayers();
            sheet.dismiss();
        });
        list.setAdapter(adapter);
        adapter.submit(repo.all());
        EditText search = v.findViewById(R.id.sheet_search);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                adapter.submit(repo.search(s.toString()));
            }
        });
        sheet.show();
    }

    private void saveMix() {
        if (DRAFT.isEmpty()) {
            Toast.makeText(requireContext(), "Nothing to save", Toast.LENGTH_SHORT).show();
            return;
        }
        EditText input = new EditText(requireContext());
        input.setHint("Mix name");
        new AlertDialog.Builder(requireContext())
                .setTitle("Save mix")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    Mix m = new Mix(input.getText().toString().trim().isEmpty()
                            ? "My mix" : input.getText().toString().trim());
                    m.layers.addAll(DRAFT);
                    AppDb.get(requireContext()).saveMix(m);
                    refreshMixes();
                    Toast.makeText(requireContext(), "Mix saved", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Tiny helper so the slider can tell whether a mix is currently playing. */
    static class PlaybackHubMode {
        static boolean isMix() {
            return com.naturia.app.playback.PlaybackHub.mode ==
                    com.naturia.app.playback.PlaybackHub.MODE_MIX;
        }
    }
}
