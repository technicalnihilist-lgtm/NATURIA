package com.naturia.app.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.naturia.app.R;
import com.naturia.app.data.CatalogRepository;
import com.naturia.app.model.Sound;

import java.util.List;

/** Search plus the full A-Z catalogue. */
public class ExploreFragment extends Fragment {

    private CatalogRepository repo;
    private Adapters.SoundRowAdapter adapter;
    private RecyclerView results;
    private TextView empty;
    private EditText search;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_explore, container, false);
        repo = CatalogRepository.get(requireContext());
        results = v.findViewById(R.id.rv_results);
        empty = v.findViewById(R.id.tv_empty);
        search = v.findViewById(R.id.et_search);

        results.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new Adapters.SoundRowAdapter(requireContext(),
                s -> ((MainActivity) requireActivity()).playSound(s));
        results.setAdapter(adapter);

        RecyclerView letters = v.findViewById(R.id.rv_letters);
        letters.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));
        Adapters.LetterAdapter la = new Adapters.LetterAdapter(requireContext(),
                repo.letterIndex(), this::showLetter);
        letters.setAdapter(la);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                String q = s.toString().trim();
                v.findViewById(R.id.btn_clear).setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);
                render(repo.search(q));
            }
        });
        v.findViewById(R.id.btn_clear).setOnClickListener(x -> {
            search.setText("");
            render(repo.all());
        });

        render(repo.all());
        return v;
    }

    private void showLetter(String letter) {
        search.setText("");
        render(repo.byLetter(letter));
    }

    public void search(String query) {
        if (search == null) return;
        if (query != null && query.length() == 1 && Character.isLetter(query.charAt(0))
                && repo.byLetter(query).size() > 0) {
            render(repo.byLetter(query));
            return;
        }
        search.setText(query == null ? "" : query);
        render(repo.search(query));
    }

    private void render(List<Sound> list) {
        adapter.submit(list);
        empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        results.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
