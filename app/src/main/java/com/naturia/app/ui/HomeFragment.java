package com.naturia.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.naturia.app.R;
import com.naturia.app.data.AppDb;
import com.naturia.app.data.CatalogRepository;
import com.naturia.app.model.Sound;
import com.naturia.app.util.Art;

import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Landing screen: daily pick, featured, recent, popular and the A-Z index. */
public class HomeFragment extends Fragment implements CatalogRepository.OnCatalogChanged {

    private CatalogRepository repo;
    private TextView live;
    private Adapters.SoundCardAdapter featuredAdapter, popularAdapter, newAdapter;
    private RecyclerView newRail;
    private TextView newHeader;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        repo = CatalogRepository.get(requireContext());

        v.findViewById(R.id.search_bar).setOnClickListener(x ->
                ((MainActivity) requireActivity()).openExplore(""));
        v.findViewById(R.id.btn_settings).setOnClickListener(x ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));

        // daily recommendation
        Sound daily = repo.daily();
        if (daily != null) {
            ((TextView) v.findViewById(R.id.daily_name)).setText(daily.name);
            ((TextView) v.findViewById(R.id.daily_sub)).setText(
                    daily.group + " · " + daily.durationLabel() + " · " + daily.letter);
            Art.load(requireContext(), v.findViewById(R.id.daily_art), daily.group);
            View.OnClickListener open = c -> ((MainActivity) requireActivity()).playSound(daily);
            v.findViewById(R.id.daily_card).setOnClickListener(open);
            v.findViewById(R.id.daily_play).setOnClickListener(open);
        }

        live = v.findViewById(R.id.tv_live);
        newHeader = v.findViewById(R.id.tv_new_header);
        newRail = v.findViewById(R.id.rv_new);
        newRail.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        newAdapter = new Adapters.SoundCardAdapter(requireContext(),
                s -> ((MainActivity) requireActivity()).playSound(s));
        newRail.setAdapter(newAdapter);

        RecyclerView featured = v.findViewById(R.id.rv_featured);
        featured.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        featuredAdapter = new Adapters.SoundCardAdapter(requireContext(),
                s -> ((MainActivity) requireActivity()).playSound(s));
        featured.setAdapter(featuredAdapter);
        featuredAdapter.submit(repo.featured());

        RecyclerView recent = v.findViewById(R.id.rv_recent);
        recent.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        Adapters.SoundCardAdapter ra = new Adapters.SoundCardAdapter(requireContext(),
                s -> ((MainActivity) requireActivity()).playSound(s));
        recent.setAdapter(ra);
        List<Sound> recentSounds = new ArrayList<>();
        for (String id : AppDb.get(requireContext()).recent()) {
            Sound s = repo.byId(id);
            if (s != null && recentSounds.size() < 12) recentSounds.add(s);
        }
        if (recentSounds.isEmpty()) {
            v.findViewById(R.id.tv_recent_header).setVisibility(View.GONE);
            recent.setVisibility(View.GONE);
        } else {
            ra.submit(recentSounds);
        }

        RecyclerView popular = v.findViewById(R.id.rv_popular);
        popular.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        popularAdapter = new Adapters.SoundCardAdapter(requireContext(),
                s -> ((MainActivity) requireActivity()).playSound(s));
        popular.setAdapter(popularAdapter);
        popularAdapter.submit(repo.popular());

        RecyclerView letters = v.findViewById(R.id.rv_letters);
        letters.setLayoutManager(new GridLayoutManager(requireContext(), 5));
        Adapters.LetterAdapter la = new Adapters.LetterAdapter(requireContext(),
                repo.letterIndex(), letter -> ((MainActivity) requireActivity()).openExplore(letter));
        letters.setAdapter(la);

        renderNew();
        renderLive();
        repo.addListener(this);
        return v;
    }

    private void renderNew() {
        List<Sound> newest = repo.newest(8);
        newAdapter.submit(newest);
        boolean show = newest.size() > 4;
        newRail.setVisibility(show ? View.VISIBLE : View.GONE);
        newHeader.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void renderLive() {
        if (live == null) return;
        String when = repo.generated().isEmpty() ? "" : " · " + repo.generated();
        live.setText(repo.isOffline()
                ? "Offline · " + repo.all().size() + " cached sounds"
                : "Live · " + repo.all().size() + " sounds" + when);
        live.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(),
                repo.isOffline() ? R.color.text_faint : R.color.primary));
    }

    @Override
    public void onCatalogChanged(int total, int added, boolean fromNetwork) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            featuredAdapter.submit(repo.featured());
            popularAdapter.submit(repo.popular());
            renderNew();
            renderLive();
        });
    }

    @Override
    public void onDestroyView() {
        repo.removeListener(this);
        super.onDestroyView();
    }
}
