package com.naturia.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.naturia.app.R;
import com.naturia.app.data.AppDb;
import com.naturia.app.model.Mix;
import com.naturia.app.model.Sound;
import com.naturia.app.util.Art;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** RecyclerView adapters used across the app. */
public final class Adapters {

    public interface OnSound {
        void onSound(Sound s);
    }

    public interface OnMix {
        void onPlay(Mix m);
        void onDelete(Mix m);
    }

    /* ---------------- horizontal cards ---------------- */

    public static class SoundCardAdapter extends RecyclerView.Adapter<SoundCardAdapter.VH> {
        private final Context ctx;
        private final List<Sound> items = new ArrayList<>();
        private final OnSound listener;

        public SoundCardAdapter(Context c, OnSound l) {
            this.ctx = c;
            this.listener = l;
        }

        public void submit(List<Sound> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_sound_card, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Sound s = items.get(position);
            h.name.setText(s.name);
            h.sub.setText(s.group + " · " + s.durationLabel());
            Art.load(ctx, h.art, s.group);
            h.itemView.setOnClickListener(v -> listener.onSound(s));
            h.play.setOnClickListener(v -> listener.onSound(s));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            ImageView art, play;
            TextView name, sub;

            VH(View v) {
                super(v);
                art = v.findViewById(R.id.card_art);
                play = v.findViewById(R.id.card_play);
                name = v.findViewById(R.id.card_name);
                sub = v.findViewById(R.id.card_sub);
            }
        }
    }

    /* ---------------- list rows ---------------- */

    public static class SoundRowAdapter extends RecyclerView.Adapter<SoundRowAdapter.VH> {
        private final Context ctx;
        private final List<Sound> items = new ArrayList<>();
        private final OnSound listener;

        public SoundRowAdapter(Context c, OnSound l) {
            this.ctx = c;
            this.listener = l;
        }

        public void submit(List<Sound> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_sound_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Sound s = items.get(position);
            h.name.setText(s.name);
            h.sub.setText(s.subtitle());
            Art.load(ctx, h.art, s.group);
            boolean fav = AppDb.get(ctx).isFavorite(s.id);
            h.fav.setImageResource(fav ? R.drawable.ic_heart_filled : R.drawable.ic_heart);

            h.itemView.setOnClickListener(v -> listener.onSound(s));
            h.fav.setOnClickListener(v -> {
                AppDb.get(ctx).toggleFavorite(s.id);
                notifyItemChanged(h.getAdapterPosition());
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            ImageView art, fav;
            TextView name, sub;

            VH(View v) {
                super(v);
                art = v.findViewById(R.id.row_art);
                fav = v.findViewById(R.id.row_fav);
                name = v.findViewById(R.id.row_name);
                sub = v.findViewById(R.id.row_sub);
            }
        }
    }

    /* ---------------- A-Z letters ---------------- */

    public static class LetterAdapter extends RecyclerView.Adapter<LetterAdapter.VH> {
        public interface OnLetter {
            void onLetter(String letter);
        }

        private final Context ctx;
        private final List<Map.Entry<String, Integer>> items = new ArrayList<>();
        private final OnLetter listener;
        private String selected;

        public LetterAdapter(Context c, Map<String, Integer> data, OnLetter l) {
            this.ctx = c;
            this.listener = l;
            items.addAll(data.entrySet());
        }

        public void setSelected(String letter) {
            selected = letter;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_letter, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Map.Entry<String, Integer> e = items.get(position);
            h.letter.setText(e.getKey());
            h.count.setText(e.getValue() + " sounds");
            h.itemView.setSelected(e.getKey().equals(selected));
            h.itemView.setAlpha(e.getValue() == 0 ? 0.35f : 1f);
            h.itemView.setOnClickListener(v -> {
                if (e.getValue() == 0) return;
                listener.onLetter(e.getKey());
                setSelected(e.getKey());
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView letter, count;

            VH(View v) {
                super(v);
                letter = v.findViewById(R.id.letter_tv);
                count = v.findViewById(R.id.letter_count);
            }
        }
    }

    /* ---------------- saved mixes ---------------- */

    public static class MixAdapter extends RecyclerView.Adapter<MixAdapter.VH> {
        private final Context ctx;
        private final List<Mix> items = new ArrayList<>();
        private final OnMix listener;

        public MixAdapter(Context c, OnMix l) {
            this.ctx = c;
            this.listener = l;
        }

        public void submit(List<Mix> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_mix, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Mix m = items.get(position);
            h.name.setText(m.name);
            h.sub.setText(m.layers.size() + " layers");
            h.play.setOnClickListener(v -> listener.onPlay(m));
            h.delete.setOnClickListener(v -> listener.onDelete(m));
            h.itemView.setOnClickListener(v -> listener.onPlay(m));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView name, sub;
            ImageView play, delete;

            VH(View v) {
                super(v);
                name = v.findViewById(R.id.mix_name);
                sub = v.findViewById(R.id.mix_sub);
                play = v.findViewById(R.id.mix_play);
                delete = v.findViewById(R.id.mix_delete);
            }
        }
    }

    /* ---------------- attribution ---------------- */

    public static class AttributionAdapter extends RecyclerView.Adapter<AttributionAdapter.VH> {
        private final Context ctx;
        private final List<Sound> items;

        public AttributionAdapter(Context c, List<Sound> list) {
            this.ctx = c;
            this.items = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_attribution, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Sound s = items.get(position);
            h.name.setText(s.name);
            h.creator.setText("by " + s.creator + " · " + s.provider);
            h.license.setText(s.license + " · " + s.licenseUrl);
            h.itemView.setOnClickListener(v -> {
                if (s.sourcePage != null && !s.sourcePage.isEmpty()) {
                    try {
                        ctx.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(s.sourcePage)));
                    } catch (Exception ignored) {
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView name, creator, license;

            VH(View v) {
                super(v);
                name = v.findViewById(R.id.att_name);
                creator = v.findViewById(R.id.att_creator);
                license = v.findViewById(R.id.att_license);
            }
        }
    }
}
