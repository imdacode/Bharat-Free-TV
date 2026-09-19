package com.imdacode.bharatfreetv;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder> {
    public interface Listener {
        void onChannelSelected(Channel channel);

        void onFavoriteToggled(Channel channel);
    }

    private final Listener listener;
    private final LogoLoader logoLoader;
    private final List<Channel> channels = new ArrayList<>();
    private Set<String> favoriteUrls = Collections.emptySet();
    private String selectedStreamUrl = "";

    public ChannelAdapter(Listener listener, LogoLoader logoLoader) {
        this.listener = listener;
        this.logoLoader = logoLoader;
        setHasStableIds(true);
    }

    public void submit(List<Channel> updatedChannels, Set<String> favorites) {
        int previousSize = channels.size();
        channels.clear();
        if (previousSize > 0) {
            notifyItemRangeRemoved(0, previousSize);
        }
        channels.addAll(updatedChannels);
        favoriteUrls = new HashSet<>(favorites);
        if (!channels.isEmpty()) {
            notifyItemRangeInserted(0, channels.size());
        }
    }

    public void setSelectedChannel(Channel channel) {
        int previousPosition = findPosition(selectedStreamUrl);
        selectedStreamUrl = channel == null ? "" : channel.getStreamUrl();
        int selectedPosition = findPosition(selectedStreamUrl);
        if (previousPosition >= 0) {
            notifyItemChanged(previousPosition);
        }
        if (selectedPosition >= 0 && selectedPosition != previousPosition) {
            notifyItemChanged(selectedPosition);
        }
    }

    @Override
    public long getItemId(int position) {
        return channels.get(position).getStreamUrl().hashCode();
    }

    @NonNull
    @Override
    public ChannelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_channel, parent, false);
        return new ChannelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder holder, int position) {
        Channel channel = channels.get(position);
        holder.number.setText(String.format(Locale.US, "%03d", channel.getNumber()));
        holder.name.setText(channel.getName());
        holder.meta.setText(holder.itemView.getContext().getString(
                R.string.channel_meta, channel.getCategory(), channel.getQuality()));
        holder.itemView.setSelected(channel.getStreamUrl().equals(selectedStreamUrl));
        holder.itemView.setContentDescription(holder.itemView.getContext().getString(
                R.string.channel_description, channel.getNumber(), channel.getName(),
                channel.getQuality()));
        logoLoader.load(channel.getLogoUrl(), holder.logo);

        boolean favorite = favoriteUrls.contains(channel.getStreamUrl());
        holder.favorite.setImageResource(favorite
                ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline);
        holder.favorite.setContentDescription(holder.itemView.getContext().getString(
                favorite ? R.string.remove_favorite : R.string.add_favorite, channel.getName()));
        holder.itemView.setOnClickListener(view -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                listener.onChannelSelected(channels.get(adapterPosition));
            }
        });
        holder.favorite.setOnClickListener(view -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                listener.onFavoriteToggled(channels.get(adapterPosition));
            }
        });
        holder.itemView.setOnFocusChangeListener((view, hasFocus) -> {
            float scale = hasFocus ? 1.025f : 1f;
            view.animate().scaleX(scale).scaleY(scale).setDuration(120L).start();
        });
    }

    @Override
    public int getItemCount() {
        return channels.size();
    }

    private int findPosition(String streamUrl) {
        if (streamUrl == null || streamUrl.isEmpty()) {
            return -1;
        }
        for (int index = 0; index < channels.size(); index++) {
            if (streamUrl.equals(channels.get(index).getStreamUrl())) {
                return index;
            }
        }
        return -1;
    }

    static final class ChannelViewHolder extends RecyclerView.ViewHolder {
        private final TextView number;
        private final ImageView logo;
        private final TextView name;
        private final TextView meta;
        private final ImageButton favorite;

        private ChannelViewHolder(@NonNull View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.channel_number);
            logo = itemView.findViewById(R.id.channel_logo);
            name = itemView.findViewById(R.id.channel_name);
            meta = itemView.findViewById(R.id.channel_meta);
            favorite = itemView.findViewById(R.id.favorite_button);
        }
    }
}
