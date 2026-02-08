package com.example.videoplayer;

import android.app.ActivityOptions;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Window;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;
import com.google.android.material.transition.platform.MaterialFadeThrough;
import com.google.android.material.transition.platform.MaterialSharedAxis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FolderVideosActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView titleTextView;
    private FavouritesAdapter adapter;
    private List<VideoModel> videoList = new ArrayList<>();
    private List<VideoModel> filteredList = new ArrayList<>();
    private BottomNavigationView bottomNavigationView;

    private SearchBar searchBar;
    private SearchView searchView;
    private RecyclerView searchResultsRv;
    private FavouritesAdapter searchAdapter;
    private MaterialButton btnSort;

    private int currentSortOrder = 0; // 0: Date, 1: Name, 2: Duration, 3: Size (Asc), 4: Size (Desc)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);

        MaterialSharedAxis enterTransition = new MaterialSharedAxis(MaterialSharedAxis.Z, true);
        enterTransition.excludeTarget(R.id.bottom_navigation, true);
        getWindow().setEnterTransition(enterTransition);

        MaterialSharedAxis returnTransition = new MaterialSharedAxis(MaterialSharedAxis.Z, false);
        returnTransition.excludeTarget(R.id.bottom_navigation, true);
        getWindow().setReturnTransition(returnTransition);

        MaterialFadeThrough exitTransition = new MaterialFadeThrough();
        exitTransition.excludeTarget(R.id.bottom_navigation, true);
        getWindow().setExitTransition(exitTransition);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_videos);

        initViews();
        setupSearch();
        setupSorting();
        setupBottomNavigation();

        String folderId = getIntent().getStringExtra("FOLDER_ID");
        String folderName = getIntent().getStringExtra("FOLDER_NAME");
        titleTextView.setText(folderName);

        loadVideosFromFolder(folderId);
    }

    private void initViews() {
        recyclerView = findViewById(R.id.videosRv);
        titleTextView = findViewById(R.id.folderTitle);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        searchBar = findViewById(R.id.search_bar);
        searchView = findViewById(R.id.search_view);
        searchResultsRv = findViewById(R.id.search_results_rv);
        btnSort = findViewById(R.id.btn_sort);

        bottomNavigationView.setSelectedItemId(R.id.nav_folders);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FavouritesAdapter(this, videoList);
        recyclerView.setAdapter(adapter);

        searchResultsRv.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new FavouritesAdapter(this, filteredList);
        searchAdapter.setSearchMode(true); // Enable path display for breadcrumbs
        searchResultsRv.setAdapter(searchAdapter);
    }

    private void setupSearch() {
        searchView.getEditText().addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterVideos(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        // Ensure filtered list is empty when first opened
        searchView.addTransitionListener((view, previousState, newState) -> {
            if (newState == SearchView.TransitionState.SHOWING) {
                filteredList.clear();
                searchAdapter.notifyDataSetChanged();
            }
        });
    }

    private void filterVideos(String query) {
        filteredList.clear();
        if (!query.trim().isEmpty()) {
            for (VideoModel video : videoList) {
                if (video.getTitle().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(video);
                }
            }
        }
        searchAdapter.notifyDataSetChanged();
    }

    private void setupSorting() {
        btnSort.setOnClickListener(v -> {
            String[] options = {"Date (Newest)", "Name", "Duration", "Size (Smallest)", "Size (Largest)"};
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Sort Videos By")
                    .setSingleChoiceItems(options, currentSortOrder, (dialog, which) -> {
                        currentSortOrder = which;
                        sortVideos();
                        dialog.dismiss();
                    })
                    .show();
        });
    }

    private void sortVideos() {
        if (currentSortOrder == 0) {
            // Assuming the list is initially loaded by Date Modified DESC
            // For simplicity, we'll just reload or sort by title if needed, 
            // but usually this is the default from MediaStore.
        } else if (currentSortOrder == 1) {
            Collections.sort(videoList, (v1, v2) -> v1.getTitle().compareToIgnoreCase(v2.getTitle()));
        } else if (currentSortOrder == 2) {
            Collections.sort(videoList, (v1, v2) -> v1.getDuration().compareTo(v2.getDuration()));
        } else if (currentSortOrder == 3) {
            Collections.sort(videoList, (v1, v2) -> Long.compare(v1.getSize(), v2.getSize()));
        } else if (currentSortOrder == 4) {
            Collections.sort(videoList, (v1, v2) -> Long.compare(v2.getSize(), v1.getSize()));
        }
        adapter.notifyDataSetChanged();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_folders) {
                finishAfterTransition();
                return false;
            } else if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, bottomNavigationView, "shared_navbar");
                startActivity(intent, options.toBundle());
                return false;
            }
            return false;
        });
    }

    @Override public void onBackPressed() {
        if (searchView.isShowing()) searchView.hide();
        else finishAfterTransition();
    }

    private void loadVideosFromFolder(String folderId) {
        new Thread(() -> {
            List<VideoModel> tempList = new ArrayList<>();
            String selection = MediaStore.Video.Media.BUCKET_ID + " = ?";
            String[] selectionArgs = {folderId};
            String[] projection = {
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media.SIZE
            };
            String sortOrder = MediaStore.Video.Media.DATE_MODIFIED + " DESC";

            try (Cursor cursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, sortOrder)) {
                if (cursor != null) {
                    int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                    int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                    int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
                    int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                    int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idColumn);
                        String name = cursor.getString(nameColumn);
                        int durationMs = cursor.getInt(durationColumn);
                        String data = cursor.getString(dataColumn);
                        long size = cursor.getLong(sizeColumn);

                        String folderPath = "";
                        if (data != null && data.contains("/")) {
                            folderPath = data.substring(0, data.lastIndexOf("/"));
                        }

                        Uri contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                        tempList.add(new VideoModel(name, formatDuration(durationMs), contentUri, folderPath, size));
                    }
                }
            }
            runOnUiThread(() -> {
                videoList.clear();
                videoList.addAll(tempList);
                sortVideos();
            });
        }).start();
    }

    private String formatDuration(int duration) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
