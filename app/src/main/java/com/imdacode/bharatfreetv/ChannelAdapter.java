package com.imdacode.bharatfreetv;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public final class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder> {
    public interface OnChannelSelectedListener {
        void onChannelSelected(int position, Channel channel);
    }

    private final List<Channel> channels;
    private final OnChannelSelectedListener listener;
    private int selectedPosition;

    public ChannelAdapter(List<Channel> channels, OnChannelSelectedListener listener) {
        this.channels = channels;
        this.listener = listener;
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
        holder.name.setText(channel.getName());
        holder.group.setText(channel.getGroup());
        holder.itemView.setSelected(position == selectedPosition);
        holder.itemView.setOnClickListener(view -> select(holder.getBindingAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return channels.size();
    }

    public void select(int position) {
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        if (position == selectedPosition) {
            listener.onChannelSelected(position, channels.get(position));
            return;
        }
        int previousPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(previousPosition);
        notifyItemChanged(selectedPosition);
        listener.onChannelSelected(position, channels.get(position));
    }

    static final class ChannelViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView group;

        private ChannelViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.channel_name);
            group = itemView.findViewById(R.id.channel_group);
        }
    }
}
