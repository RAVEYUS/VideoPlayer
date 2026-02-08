package com.example.videoplayer;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.transition.platform.MaterialFadeThrough;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private RecyclerView favoritesRecyclerView;
    private BottomNavigationView bottomNavigationView;
    private TextView emptyFavMessage, lastPlayedTitle, lastPlayedLabel;
    private MaterialCardView lastPlayedCard;
    private ImageView lastPlayedThumbnail;
    private LinearProgressIndicator lastPlayedProgress;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private List<VideoModel> allVideosList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);

        // Configure all transition types for a seamless experience
        MaterialFadeThrough fadeThrough = new MaterialFadeThrough();
        fadeThrough.excludeTarget(android.R.id.statusBarBackground, true);
        fadeThrough.excludeTarget(android.R.id.navigationBarBackground, true);
        fadeThrough.excludeTarget(R.id.bottom_navigation, true);
        fadeThrough.excludeTarget(R.id.lastPlayedCard, true); // Exclude card from transition

        getWindow().setEnterTransition(fadeThrough);
        getWindow().setExitTransition(fadeThrough);
        getWindow().setReenterTransition(fadeThrough);
        getWindow().setReturnTransition(fadeThrough);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        favoritesRecyclerView = findViewById(R.id.favoritesRecyclerView);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        emptyFavMessage = findViewById(R.id.empty_fav_message);
        lastPlayedCard = findViewById(R.id.lastPlayedCard);
        lastPlayedTitle = findViewById(R.id.lastPlayedTitle);
        lastPlayedLabel = findViewById(R.id.lastPlayedLabel);
        lastPlayedThumbnail = findViewById(R.id.lastPlayedThumbnail);
        lastPlayedProgress = findViewById(R.id.lastPlayedProgress);

        setupBottomNavigation();

        if (checkPermission()) {
            loadVideos();
        } else {
            requestPermission();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermission()) {
            loadVideos();
        }
        setupLastPlayed();
    }

    private void setupLastPlayed() {
        SharedPreferences prefs = getSharedPreferences("VideoPlayerPrefs", Context.MODE_PRIVATE);
        String lastUri = prefs.getString("LAST_VIDEO_URI", null);
        String lastTitle = prefs.getString("LAST_VIDEO_TITLE", null);
        long lastPosition = prefs.getLong("LAST_VIDEO_POSITION", 0);
        long lastDuration = prefs.getLong("LAST_VIDEO_DURATION", 0);

        lastPlayedCard.setVisibility(View.VISIBLE);

        if (lastUri != null) {
            lastPlayedTitle.setText(lastTitle);
            lastPlayedLabel.setText("Continue Watching");

            Glide.with(this)
                    .load(Uri.parse(lastUri))
                    .centerCrop()
                    .into(lastPlayedThumbnail);

            if (lastDuration > 0) {
                int progress = (int) ((lastPosition * 100) / lastDuration);
                lastPlayedProgress.setVisibility(View.VISIBLE);
                lastPlayedProgress.setProgress(progress);
            } else {
                lastPlayedProgress.setVisibility(View.GONE);
            }

            lastPlayedCard.setOnClickListener(v -> {
                Intent intent = new Intent(this, PlayerActivity.class);
                intent.putExtra("VIDEO_URI", lastUri);
                intent.putExtra("VIDEO_TITLE", lastTitle);
                intent.putExtra("RESUME_POSITION", lastPosition);
                startActivity(intent);
            });
        } else {
            lastPlayedTitle.setText("Start your journey!");
            lastPlayedLabel.setText("Ready to watch?");
            lastPlayedThumbnail.setImageResource(R.drawable.ic_launcher_background);
            lastPlayedProgress.setVisibility(View.GONE);
            
            lastPlayedCard.setOnClickListener(v -> {
                Intent intent = new Intent(this, FoldersActivity.class);
                ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this);
                startActivity(intent, options.toBundle());
            });
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) return true;
            else if (itemId == R.id.nav_folders) {
                Intent intent = new Intent(this, FoldersActivity.class);
                ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this);
                startActivity(intent, options.toBundle());
                return false;
            }
            return false;
        });
    }

    private void loadVideos() {
        new Thread(() -> {
            List<VideoModel> tempAllList = new ArrayList<>();
            Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.DATA
            };
            String sortOrder = MediaStore.Video.Media.DATE_MODIFIED + " DESC";

            try (Cursor cursor = getContentResolver().query(uri, projection, null, null, sortOrder)) {
                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                    int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                    int durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
                    int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idCol);
                        String name = cursor.getString(nameCol);
                        int duration = cursor.getInt(durCol);
                        String data = cursor.getString(dataCol);
                        String folderPath = (data != null && data.contains("/")) ? data.substring(0, data.lastIndexOf("/")) : "";
                        Uri contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                        tempAllList.add(new VideoModel(name, formatDuration(duration), contentUri, folderPath));
                    }
                }
            }

            runOnUiThread(() -> {
                allVideosList.clear();
                allVideosList.addAll(tempAllList);
                displayFilteredVideos();
            });
        }).start();
    }

    private void displayFilteredVideos() {
        SharedPreferences favPrefs = getSharedPreferences("FavVideos", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = favPrefs.getString("fav_list", null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        ArrayList<String> favoriteUris = gson.fromJson(json, type);
        if (favoriteUris == null) favoriteUris = new ArrayList<>();

        List<VideoModel> favoriteList = new ArrayList<>();
        for (VideoModel video : allVideosList) {
            if (favoriteUris.contains(video.getVideoUri().toString())) {
                favoriteList.add(video);
            }
        }

        updateEmptyState(favoriteList.size());

        FavouritesAdapter adapter = new FavouritesAdapter(MainActivity.this, favoriteList);
        adapter.setFavoriteSection(true);
        adapter.setOnDataChangedListener(size -> updateEmptyState(size));
        
        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        favoritesRecyclerView.setAdapter(adapter);
    }

    private void updateEmptyState(int size) {
        if (size == 0) {
            emptyFavMessage.setVisibility(View.VISIBLE);
            favoritesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyFavMessage.setVisibility(View.GONE);
            favoritesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private String formatDuration(int duration) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_VIDEO}, STORAGE_PERMISSION_CODE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadVideos();
        }
    }
}
