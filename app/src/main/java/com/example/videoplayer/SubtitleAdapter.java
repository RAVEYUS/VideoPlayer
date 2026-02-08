package com.example.videoplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SubtitleAdapter extends RecyclerView.Adapter<SubtitleAdapter.SubtitleViewHolder> {

    private List<SubtitleModel> subtitleList;
    private OnSubtitleClickListener listener;

    public interface OnSubtitleClickListener {
        void onSubtitleClick(SubtitleModel subtitle);
    }

    public SubtitleAdapter(List<SubtitleModel> subtitleList, OnSubtitleClickListener listener) {
        this.subtitleList = subtitleList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SubtitleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new SubtitleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubtitleViewHolder holder, int position) {
        SubtitleModel subtitle = subtitleList.get(position);
        holder.text1.setText(subtitle.getFileName());
        holder.text2.setText(subtitle.getLanguage());
        holder.itemView.setOnClickListener(v -> listener.onSubtitleClick(subtitle));
    }

    @Override
    public int getItemCount() {
        return subtitleList.size();
    }

    static class SubtitleViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;

        public SubtitleViewHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }
}
