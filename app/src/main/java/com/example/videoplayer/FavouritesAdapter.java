package com.example.videoplayer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FavouritesAdapter extends RecyclerView.Adapter<FavouritesAdapter.ViewHolder> {

    private Context context;
    private List<VideoModel> videoList;
    private boolean isSearchMode = false;
    private boolean isFavoriteSection = false;
    private List<String> favoriteUris;
    private SharedPreferences favPrefs;
    private Gson gson = new Gson();
    
    private OnDataChangedListener dataChangedListener;

    public interface OnDataChangedListener {
        void onDataChanged(int size);
    }

    public FavouritesAdapter(Context context, List<VideoModel> videoList) {
        this.context = context;
        this.videoList = videoList;
        this.favPrefs = context.getSharedPreferences("FavVideos", Context.MODE_PRIVATE);
        loadFavorites();
    }

    public void setOnDataChangedListener(OnDataChangedListener listener) {
        this.dataChangedListener = listener;
    }

    public void setFavoriteSection(boolean favoriteSection) {
        this.isFavoriteSection = favoriteSection;
    }

    private void loadFavorites() {
        String json = favPrefs.getString("fav_list", null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        favoriteUris = gson.fromJson(json, type);
        if (favoriteUris == null) favoriteUris = new ArrayList<>();
    }

    private void saveFavorites() {
        favPrefs.edit().putString("fav_list", gson.toJson(favoriteUris)).apply();
    }

    public void setSearchMode(boolean searchMode) {
        this.isSearchMode = searchMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_video_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VideoModel video = videoList.get(position);
        String videoUriStr = video.getVideoUri().toString();

        holder.title.setText(video.getTitle());
        holder.duration.setText(video.getDuration());

        if (isSearchMode && video.getFolderPath() != null) {
            holder.path.setVisibility(View.VISIBLE);
            holder.path.setText(video.getFolderPath());
        } else {
            holder.path.setVisibility(View.GONE);
        }

        Glide.with(context)
                .load(video.getVideoUri())
                .centerCrop()
                .into(holder.thumbnail);

        if (favoriteUris.contains(videoUriStr)) {
            holder.ivFav.setImageResource(R.drawable.ic_favorite_24);
        } else {
            holder.ivFav.setImageResource(R.drawable.favourite_svgrepo_com);
        }

        holder.ivFav.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            if (favoriteUris.contains(videoUriStr)) {
                favoriteUris.remove(videoUriStr);
                animateFavoriteChange(holder.ivFav, false);
                
                if (isFavoriteSection) {
                    holder.itemView.animate()
                            .alpha(0f)
                            .translationX(holder.itemView.getWidth())
                            .setDuration(300)
                            .withEndAction(() -> {
                                videoList.remove(currentPos);
                                notifyItemRemoved(currentPos);
                                notifyItemRangeChanged(currentPos, videoList.size());
                                if (dataChangedListener != null) {
                                    dataChangedListener.onDataChanged(videoList.size());
                                }
                            }).start();
                }
            } else {
                favoriteUris.add(videoUriStr);
                animateFavoriteChange(holder.ivFav, true);
            }
            saveFavorites();
        });

        float radius = holder.itemView.getResources().getDisplayMetrics().density * 16;
        updateCorners(holder, position, radius);

        holder.rootCard.setOnClickListener(v -> {
            Intent intent = new Intent(context, PlayerActivity.class);
            intent.putExtra("VIDEO_URI", video.getVideoUri().toString());
            intent.putExtra("VIDEO_TITLE", video.getTitle());
            context.startActivity(intent);
        });
    }

    private void animateFavoriteChange(ImageView imageView, boolean isAdded) {
        imageView.animate()
                .scaleX(0.2f)
                .scaleY(0.2f)
                .setDuration(100)
                .withEndAction(() -> {
                    imageView.setImageResource(isAdded ? R.drawable.ic_favorite_24 : R.drawable.favourite_svgrepo_com);
                    imageView.animate()
                            .scaleX(1.2f)
                            .scaleY(1.2f)
                            .setDuration(150)
                            .setInterpolator(new OvershootInterpolator())
                            .withEndAction(() -> {
                                imageView.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                            }).start();
                }).start();
    }

    private void updateCorners(ViewHolder holder, int position, float radius) {
        ShapeAppearanceModel.Builder shapeBuilder = new ShapeAppearanceModel.Builder();
        if (getItemCount() == 1) {
            shapeBuilder.setAllCornerSizes(radius);
        } else if (position == 0) {
            shapeBuilder.setTopLeftCornerSize(radius).setTopRightCornerSize(radius);
            shapeBuilder.setBottomLeftCornerSize(0f).setBottomRightCornerSize(0f);
        } else if (position == getItemCount() - 1) {
            shapeBuilder.setBottomLeftCornerSize(radius).setBottomRightCornerSize(radius);
            shapeBuilder.setTopLeftCornerSize(0f).setTopRightCornerSize(0f);
        } else {
            shapeBuilder.setAllCornerSizes(0f);
        }
        holder.rootCard.setShapeAppearanceModel(shapeBuilder.build());
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView rootCard;
        ImageView thumbnail, ivFav;
        TextView title, duration, path;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rootCard = itemView.findViewById(R.id.rootCardView);
            thumbnail = itemView.findViewById(R.id.videoThumbnail);
            title = itemView.findViewById(R.id.videoTitle);
            duration = itemView.findViewById(R.id.videoDuration);
            path = itemView.findViewById(R.id.videoPath);
            ivFav = itemView.findViewById(R.id.iv_fav);
        }
    }
}
