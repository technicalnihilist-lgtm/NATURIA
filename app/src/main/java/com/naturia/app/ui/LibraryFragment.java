package com.naturia.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.naturia.app.R;
import com.naturia.app.data.AppDb;
import com.naturia.app.data.CatalogRepository;
import com.naturia.app.model.Sound;

import java.util.ArrayList;
import java.util.List;

/** Favourites, play history and offline downloads. */
public class LibraryFragment extends Fragment {

    private Adapters.SoundRowAdapter adapter;
    private TextView empty, tabFav, tabRecent, tabNew;
    private int tab = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_library, container, false);
        empty = v.findViewById(R.id.tv_lib_empty);
        tabFav = v.findViewById(R.id.tab_fav);
        tabRecent = v.findViewById(R.id.tab_recent);
        tabNew = v.findViewById(R.id.tab_new);

        RecyclerView rv = v.findViewById(R.id.rv_library);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new Adapters.SoundRowAdapter(requireContext(),
                s -> ((MainActivity) requireActivity()).playSound(s));
        rv.setAdapter(adapter);

        tabFav.setOnClickListener(x -> setTab(0));
        tabRecent.setOnClickListener(x -> setTab(1));
        tabNew.setOnClickListener(x -> setTab(2));
        setTab(0);
        return v;
    }

    private void setTab(int index) {
        tab = index;
        int on = ContextCompat.getColor(requireContext(), R.color.chip_bg_on);
        int off = ContextCompat.getColor(requireContext(), R.color.chip_bg);
        int onText = ContextCompat.getColor(requireContext(), R.color.on_primary);
        int offText = ContextCompat.getColor(requireContext(), R.color.text_dim);
        TextView[] tabs = {tabFav, tabRecent, tabNew};
        for (int i = 0; i < tabs.length; i++) {
            boolean selected = i == index;
            tabs[i].setSelected(selected);
            tabs[i].setTextColor(selected ? onText : offText);
            tabs[i].setBackgroundResource(R.drawable.bg_chip);
        }
        load();
    }

    private void load() {
        CatalogRepository repo = CatalogRepository.get(requireContext());
        AppDb db = AppDb.get(requireContext());
        List<Sound> list = new ArrayList<>();
        if (tab == 2) {
            list.addAll(repo.newest(40));
        } else {
            List<String> ids = tab == 0 ? db.favorites() : db.recent();
            for (String id : ids) {
                Sound s = repo.byId(id);
                if (s != null) list.add(s);
            }
        }
        adapter.submit(list);
        empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        empty.setText(tab == 0 ? "No favourites yet. Tap the heart on any sound."
                : tab == 1 ? "Nothing played yet."
                : "The catalogue is being fetched. Pull it again in a moment.");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) load();
    }
}
