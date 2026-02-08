package com.example.videoplayer;

import android.app.ActivityOptions;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;
import com.google.android.material.transition.platform.MaterialFadeThrough;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class FoldersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArrayList<VideoFolder> folderList = new ArrayList<>();
    private ArrayList<VideoModel> allVideosList = new ArrayList<>();
    private ArrayList<VideoModel> filteredVideos = new ArrayList<>();
    private FolderAdapter adapter;
    private BottomNavigationView bottomNavigationView;
    
    private SearchBar searchBar;
    private SearchView searchView;
    private RecyclerView searchResultsRv;
    private FavouritesAdapter searchAdapter;
    private MaterialButton btnSort;
    
    private ChipGroup recentChips;
    private TextView recentTitle;

    private int currentSortOrder = 0; // 0: Name, 1: Count, 2: Size (Asc), 3: Size (Desc)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);

        // Apply MaterialFadeThrough to all transition states for a consistent experience
        MaterialFadeThrough fadeThrough = new MaterialFadeThrough();
        fadeThrough.excludeTarget(android.R.id.statusBarBackground, true);
        fadeThrough.excludeTarget(android.R.id.navigationBarBackground, true);
        fadeThrough.excludeTarget(R.id.bottom_navigation, true);
        
        getWindow().setEnterTransition(fadeThrough);
        getWindow().setExitTransition(fadeThrough);
        getWindow().setReenterTransition(fadeThrough);
        getWindow().setReturnTransition(fadeThrough);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folders);

        initViews();
        setupSearch();
        setupSorting();
        setupBottomNavigation();

        fetchFolders();
        fetchAllVideosAsync();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentFolders();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.foldersRv);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        searchBar = findViewById(R.id.search_bar);
        searchView = findViewById(R.id.search_view);
        searchResultsRv = findViewById(R.id.search_results_rv);
        btnSort = findViewById(R.id.btn_sort);
        recentChips = findViewById(R.id.recent_chips);
        recentTitle = findViewById(R.id.recent_title);

        bottomNavigationView.setSelectedItemId(R.id.nav_folders);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FolderAdapter(this, folderList);
        recyclerView.setAdapter(adapter);

        searchResultsRv.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new FavouritesAdapter(this, filteredVideos);
        searchAdapter.setSearchMode(true); // Enable path display
        searchResultsRv.setAdapter(searchAdapter);
    }

    private void loadRecentFolders() {
        SharedPreferences prefs = getSharedPreferences("RecentFolders", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("list", null);
        Type type = new TypeToken<ArrayList<VideoFolder>>() {}.getType();
        ArrayList<VideoFolder> recentList = gson.fromJson(json, type);

        recentChips.removeAllViews();
        if (recentList != null && !recentList.isEmpty()) {
            recentTitle.setVisibility(View.VISIBLE);
            recentChips.setVisibility(View.VISIBLE);
            
            int surfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceVariant, getResources().getColor(R.color.light_surface));
            int onSurfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, getResources().getColor(R.color.dark_text));
            int strokeColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant, getResources().getColor(R.color.greyish_white));
            float density = getResources().getDisplayMetrics().density;

            for (VideoFolder folder : recentList) {
                MaterialButton button = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonStyle);
                button.setText(folder.getName());
                button.setAllCaps(false);
                button.setCornerRadius((int) (12 * density));
                button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(surfaceColor));
                button.setTextColor(onSurfaceColor);
                button.setRippleColorResource(R.color.white);
                button.setElevation(0); 
                button.setTranslationZ(0);
                
                button.setStrokeWidth((int) (1 * density));
                button.setStrokeColor(android.content.res.ColorStateList.valueOf(strokeColor));
                
                int hPadding = (int) (16 * density);
                int vPadding = (int) (8 * density);
                button.setPadding(hPadding, vPadding, hPadding, vPadding);
                button.setInsetTop(0);
                button.setInsetBottom(0);
                button.setMinimumHeight(0);
                button.setMinHeight(0);
                button.setTextSize(13);

                ChipGroup.LayoutParams layoutParams = new ChipGroup.LayoutParams(
                        ChipGroup.LayoutParams.WRAP_CONTENT,
                        ChipGroup.LayoutParams.WRAP_CONTENT
                );
                int margin = (int) (4 * density);
                layoutParams.setMargins(margin, margin, margin, margin);
                button.setLayoutParams(layoutParams);

                button.setOnClickListener(v -> {
                    Intent intent = new Intent(this, FolderVideosActivity.class);
                    intent.putExtra("FOLDER_ID", folder.getId());
                    intent.putExtra("FOLDER_NAME", folder.getName());
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this);
                    startActivity(intent, options.toBundle());
                });
                recentChips.addView(button);
            }
        } else {
            recentTitle.setVisibility(View.GONE);
            recentChips.setVisibility(View.GONE);
        }
    }

    private void setupSearch() {
        searchView.getEditText().addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterVideos(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        
        searchView.addTransitionListener((view, previousState, newState) -> {
            if (newState == SearchView.TransitionState.SHOWING) {
                filteredVideos.clear();
                searchAdapter.notifyDataSetChanged();
            }
        });
    }

    private void filterVideos(String query) {
        filteredVideos.clear();
        if (!query.trim().isEmpty()) {
            for (VideoModel video : allVideosList) {
                if (video.getTitle().toLowerCase().contains(query.toLowerCase())) {
                    filteredVideos.add(video);
                }
            }
        }
        searchAdapter.notifyDataSetChanged();
    }

    private void fetchAllVideosAsync() {
        new Thread(() -> {
            Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media.SIZE
            };

            try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                    int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                    int durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
                    int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                    int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idCol);
                        String name = cursor.getString(nameCol);
                        int duration = cursor.getInt(durCol);
                        String data = cursor.getString(dataCol);
                        long size = cursor.getLong(sizeCol);
                        String folderPath = "";
                        if (data != null && data.contains("/")) {
                            folderPath = data.substring(0, data.lastIndexOf("/"));
                        }
                        Uri contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                        allVideosList.add(new VideoModel(name, formatDuration(duration), contentUri, folderPath, size));
                    }
                }
            }
        }).start();
    }

    private String formatDuration(int duration) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void setupSorting() {
        btnSort.setOnClickListener(v -> {
            String[] options = {"Name", "Video Count", "Size (Smallest)", "Size (Largest)"};
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Sort Folders By")
                    .setSingleChoiceItems(options, currentSortOrder, (dialog, which) -> {
                        currentSortOrder = which;
                        sortFolders();
                        dialog.dismiss();
                    })
                    .show();
        });
    }

    private void sortFolders() {
        if (currentSortOrder == 0) {
            Collections.sort(folderList, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
        } else if (currentSortOrder == 1) {
            Collections.sort(folderList, (f1, f2) -> Integer.compare(f2.getVideoCount(), f1.getVideoCount()));
        } else if (currentSortOrder == 2) {
            Collections.sort(folderList, (f1, f2) -> Long.compare(f1.getTotalSize(), f2.getTotalSize()));
        } else if (currentSortOrder == 3) {
            Collections.sort(folderList, (f1, f2) -> Long.compare(f2.getTotalSize(), f1.getTotalSize()));
        }
        adapter.notifyDataSetChanged();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_folders) return true;
            else if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                // Use ActivityOptions for a proper Material transition
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
                finish();
                return true;
            }
            return false;
        });
    }

    @Override public void onBackPressed() {
        if (searchView.isShowing()) searchView.hide();
        else {
            // Replaced with finishAfterTransition for a better exit animation
            finishAfterTransition();
        }
    }

    private void fetchFolders() {
        new Thread(() -> {
            ArrayList<VideoFolder> tempFolders = new ArrayList<>();
            HashMap<String, VideoFolder> folderMap = new HashMap<>();
            
            Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {
                MediaStore.Video.Media.BUCKET_ID, 
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media._ID
            };
            
            String sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

            try (Cursor cursor = getContentResolver().query(uri, projection, null, null, sortOrder)) {
                if (cursor != null) {
                    int idIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID);
                    int nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
                    int sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
                    int videoIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);

                    while (cursor.moveToNext()) {
                        String bucketId = cursor.getString(idIdx);
                        String name = cursor.getString(nameIdx);
                        long size = cursor.getLong(sizeIdx);
                        long videoId = cursor.getLong(videoIdIdx);
                        Uri videoUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId);

                        if (folderMap.containsKey(bucketId)) {
                            VideoFolder f = folderMap.get(bucketId);
                            f.setVideoCount(f.getVideoCount() + 1);
                            f.setTotalSize(f.getTotalSize() + size);
                            if (f.getFirstVideoPath() == null) {
                                f.setFirstVideoPath(videoUri.toString());
                            }
                        } else {
                            VideoFolder newFolder = new VideoFolder(bucketId, name, 1, size);
                            newFolder.setFirstVideoPath(videoUri.toString());
                            folderMap.put(bucketId, newFolder);
                            tempFolders.add(newFolder);
                        }
                    }
                }
            }

            runOnUiThread(() -> {
                folderList.clear();
                folderList.addAll(tempFolders);
                sortFolders();
                adapter.notifyDataSetChanged();
            });
        }).start();
    }
}
