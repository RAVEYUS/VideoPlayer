package com.example.videoplayer;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    private Context context;
    private ArrayList<VideoFolder> folderList;

    public FolderAdapter(Context context, ArrayList<VideoFolder> folderList) {
        this.context = context;
        this.folderList = folderList;
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        VideoFolder folder = folderList.get(position);
        holder.folderName.setText(folder.getName());
        holder.folderCount.setText(folder.getVideoCount() + " Videos");

        // Optimized thumbnail loading for folders
        if (folder.getFirstVideoPath() != null) {
            holder.folderIcon.setImageTintList(null);
            
            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.ic_folder_24)
                    .error(R.drawable.ic_folder_24)
                    .centerCrop()
                    .override(150, 150) // Load smaller version for folder preview
                    .diskCacheStrategy(DiskCacheStrategy.ALL);

            Glide.with(context)
                    .load(folder.getFirstVideoPath())
                    .apply(options)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.folderIcon);
        } else {
            holder.folderIcon.setImageResource(R.drawable.ic_folder_24);
            // Optional: reset tint if needed, but usually placeholder handles it
        }

        float radius = holder.itemView.getResources().getDisplayMetrics().density * 16;
        ShapeAppearanceModel.Builder shapeBuilder = new ShapeAppearanceModel.Builder();

        if (getItemCount() == 1) {
            shapeBuilder.setAllCornerSizes(radius);
        } else if (position == 0) {
            shapeBuilder.setTopLeftCornerSize(radius)
                    .setTopRightCornerSize(radius)
                    .setBottomLeftCornerSize(0f)
                    .setBottomRightCornerSize(0f);
        } else if (position == getItemCount() - 1) {
            shapeBuilder.setTopLeftCornerSize(0f)
                    .setTopRightCornerSize(0f)
                    .setBottomLeftCornerSize(radius)
                    .setBottomRightCornerSize(radius);
        } else {
            shapeBuilder.setAllCornerSizes(0f);
        }

        holder.rootCard.setShapeAppearanceModel(shapeBuilder.build());

        holder.rootCard.setOnClickListener(v -> {
            saveRecentFolder(folder);
            
            Intent intent = new Intent(context, FolderVideosActivity.class);
            intent.putExtra("FOLDER_ID", folder.getId());
            intent.putExtra("FOLDER_NAME", folder.getName());

            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(activity);
                context.startActivity(intent, options.toBundle());
            } else {
                context.startActivity(intent);
            }
        });
    }

    private void saveRecentFolder(VideoFolder folder) {
        SharedPreferences prefs = context.getSharedPreferences("RecentFolders", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("list", null);
        Type type = new TypeToken<ArrayList<VideoFolder>>() {}.getType();
        ArrayList<VideoFolder> recentList = gson.fromJson(json, type);

        if (recentList == null) recentList = new ArrayList<>();

        for (int i = 0; i < recentList.size(); i++) {
            if (recentList.get(i).getId().equals(folder.getId())) {
                recentList.remove(i);
                break;
            }
        }

        recentList.add(0, folder);

        if (recentList.size() > 10) {
            recentList.remove(recentList.size() - 1);
        }

        prefs.edit().putString("list", gson.toJson(recentList)).apply();
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    public static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView folderName, folderCount;
        MaterialCardView rootCard;
        ImageView folderIcon;

        public FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            rootCard = itemView.findViewById(R.id.rootCardView);
            folderName = itemView.findViewById(R.id.folderName);
            folderCount = itemView.findViewById(R.id.folderCount);
            folderIcon = itemView.findViewById(R.id.folderIcon);
        }
    }
}
