package com.example.videoplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.sidesheet.SideSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.transition.platform.MaterialContainerTransform;
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

public class PlayerActivity extends AppCompatActivity {

    private PlayerView playerView;
    private ExoPlayer player;
    private String videoUriString;
    private String videoTitle;
    private long resumePosition = 0;

    private View playBtn, pauseBtn;
    private ImageButton btnAspectRatio, btnSpeed, btnLock, btnSubtitle;

    private LinearProgressIndicator wavyProgress;
    private View progressContainer;
    private TextView positionText, durationText;

    private View layoutRew, layoutFwd;
    private GestureDetector gestureDetector;

    private View topControlsContainer, bottomControlsContainer, centerControls;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isUserSeeking = false;
    private boolean isLocked = false;
    private int currentAspectRatioMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    private float currentSpeed = 1.0f;
    private long subtitleDelay = 0;
    private String subtitleSearchQuery = "";
    private float subtitleFontSize = 18f;

    private List<SubtitleModel> availableSubtitles = new ArrayList<>();
    private List<SubtitleModel> searchResults = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient();

    private StringBuilder formatBuilder;
    private Formatter formatter;

    private ActivityResultLauncher<String[]> subtitlePickerLauncher;
    private Uri activeSubtitleUri = null;
    private SubtitleAdapter availableSubsAdapter;
    private BottomSheetDialog tracksBottomSheet;

    private final Runnable hideControllerAction = this::animateOutControls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        setExitSharedElementCallback(new MaterialContainerTransformSharedElementCallback());
        getWindow().setSharedElementsUseOverlay(false);

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_player);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());

        videoUriString = getIntent().getStringExtra("VIDEO_URI");
        videoTitle = getIntent().getStringExtra("VIDEO_TITLE");
        resumePosition = getIntent().getLongExtra("RESUME_POSITION", 0);

        subtitleSearchQuery = videoTitle != null ? videoTitle : "";

        if (videoUriString == null) {
            Toast.makeText(this, "Error: Video not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SharedPreferences historyPrefs = getSharedPreferences("VideoPlaybackHistory", Context.MODE_PRIVATE);
        if (resumePosition <= 0) {
            resumePosition = historyPrefs.getLong(videoUriString, 0);
        }
        
        String savedSubUri = historyPrefs.getString(videoUriString + "_sub", null);
        if (savedSubUri != null) {
            activeSubtitleUri = Uri.parse(savedSubUri);
            // Re-populate availableSubtitles with the saved one
            String savedName = historyPrefs.getString(videoUriString + "_sub_name", "Saved Subtitle");
            availableSubtitles.add(new SubtitleModel("saved", savedName, "Unknown", savedSubUri, 0));
        }
        
        subtitleFontSize = historyPrefs.getFloat("global_subtitle_font_size", 18f);

        playerView = findViewById(R.id.playerView);
        
        topControlsContainer = playerView.findViewById(R.id.topControlsContainer);
        bottomControlsContainer = playerView.findViewById(R.id.bottomControlsContainer);
        centerControls = playerView.findViewById(R.id.centerControls);

        TextView titleView = playerView.findViewById(R.id.videoTitle);
        ImageView backBtn = playerView.findViewById(R.id.btnBack);

        playBtn = playerView.findViewById(R.id.exo_play);
        pauseBtn = playerView.findViewById(R.id.exo_pause);
        btnAspectRatio = playerView.findViewById(R.id.btn_aspect_ratio);
        btnSpeed = playerView.findViewById(R.id.btn_speed);
        btnLock = playerView.findViewById(R.id.btn_lock);
        btnSubtitle = playerView.findViewById(R.id.btn_subtitle);

        wavyProgress = playerView.findViewById(R.id.wavy_progress);
        progressContainer = playerView.findViewById(R.id.progressContainer);
        positionText = playerView.findViewById(R.id.exo_position);
        durationText = playerView.findViewById(R.id.exo_duration);

        layoutRew = findViewById(R.id.layout_rew);
        layoutFwd = findViewById(R.id.layout_fwd);

        if (titleView != null) titleView.setText(videoTitle);
        if (backBtn != null) backBtn.setOnClickListener(v -> {
            savePlaybackState(); 
            finish();
        });

        subtitlePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            addLocalSubtitle(uri);
                        } catch (Exception e) {
                            e.printStackTrace();
                            addLocalSubtitle(uri);
                        }
                    }
                }
        );

        setupPlayerControls();
        setupWavySeeking();
        setupDoubleTapSeeking();
        
        if (topControlsContainer != null) topControlsContainer.setAlpha(0f);
        if (bottomControlsContainer != null) bottomControlsContainer.setAlpha(0f);
        if (centerControls != null) centerControls.setAlpha(0f);
    }

    private void animateInControls() {
        handler.removeCallbacks(hideControllerAction);
        float dp80 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());

        if (topControlsContainer != null) {
            topControlsContainer.animate().cancel();
            topControlsContainer.setAlpha(1f);
            topControlsContainer.setTranslationY(-dp80);
            topControlsContainer.animate()
                    .translationY(0)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
        if (bottomControlsContainer != null) {
            bottomControlsContainer.animate().cancel();
            bottomControlsContainer.setAlpha(1f);
            bottomControlsContainer.setTranslationY(dp80);
            bottomControlsContainer.animate()
                    .translationY(0)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
        if (centerControls != null) {
            centerControls.animate().cancel();
            centerControls.setAlpha(0f);
            centerControls.setScaleX(0.7f);
            centerControls.setScaleY(0.7f);
            centerControls.animate()
                    .alpha(1f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
        handler.postDelayed(hideControllerAction, 3000);
    }

    private void animateOutControls() {
        handler.removeCallbacks(hideControllerAction);
        float dp80 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());

        if (topControlsContainer != null) {
            topControlsContainer.animate()
                    .translationY(-dp80)
                    .alpha(0f)
                    .setDuration(300)
                    .setInterpolator(new AccelerateInterpolator())
                    .start();
        }
        if (bottomControlsContainer != null) {
            bottomControlsContainer.animate()
                    .translationY(dp80)
                    .alpha(0f)
                    .setDuration(300)
                    .setInterpolator(new AccelerateInterpolator())
                    .start();
        }
        if (centerControls != null) {
            centerControls.animate()
                    .alpha(0f)
                    .scaleX(0.7f)
                    .scaleY(0.7f)
                    .setDuration(300)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> playerView.hideController())
                    .start();
        } else {
            playerView.hideController();
        }
    }

    private void addLocalSubtitle(Uri uri) {
        String fileName = getFileName(uri);
        SubtitleModel localSub = new SubtitleModel("local_" + System.currentTimeMillis(), fileName, "Local", uri.toString(), 0);
        activeSubtitleUri = uri;
        addSubtitleToPlayer(localSub);
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private void setupPlayerControls() {
        if (btnAspectRatio != null) btnAspectRatio.setOnClickListener(v -> toggleAspectRatio());
        if (btnSpeed != null) btnSpeed.setOnClickListener(v -> showPlaybackSpeedDialog());
        if (btnLock != null) btnLock.setOnClickListener(v -> toggleLock());
        if (btnSubtitle != null) btnSubtitle.setOnClickListener(v -> showSubtitleDialog());
    }

    private void toggleAspectRatio() {
        if (isLocked) { showLockedToast(); return; }
        switch (currentAspectRatioMode) {
            case AspectRatioFrameLayout.RESIZE_MODE_FIT: currentAspectRatioMode = AspectRatioFrameLayout.RESIZE_MODE_FILL; break;
            case AspectRatioFrameLayout.RESIZE_MODE_FILL: currentAspectRatioMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM; break;
            default: currentAspectRatioMode = AspectRatioFrameLayout.RESIZE_MODE_FIT; break;
        }
        playerView.setResizeMode(currentAspectRatioMode);
    }

    private void showPlaybackSpeedDialog() {
        if (isLocked) { showLockedToast(); return; }
        String[] speeds = {"0.5x", "0.75x", "Normal", "1.25x", "1.5x", "2.0x"};
        float[] speedValues = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
        int checkedItem = 2;
        for (int i = 0; i < speedValues.length; i++) if (currentSpeed == speedValues[i]) checkedItem = i;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Playback Speed")
                .setSingleChoiceItems(speeds, checkedItem, (dialog, which) -> {
                    currentSpeed = speedValues[which];
                    if (player != null) player.setPlaybackSpeed(currentSpeed);
                    dialog.dismiss();
                }).show();
    }

    private void toggleLock() {
        isLocked = !isLocked;
        btnLock.setImageResource(isLocked ? R.drawable.ic_lock_24 : R.drawable.ic_lock_open_24);
        Toast.makeText(this, isLocked ? "Seeking Locked" : "Seeking Unlocked", Toast.LENGTH_SHORT).show();
    }

    private void showLockedToast() { Toast.makeText(this, "Controls are locked", Toast.LENGTH_SHORT).show(); }

    private void showSubtitleDialog() {
        if (isLocked) { showLockedToast(); return; }
        
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_subtitles, null);
        
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(dialogView);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            MaterialContainerTransform transform = new MaterialContainerTransform();
            transform.setStartView(btnSubtitle);
            transform.setEndView(dialogView);
            transform.setDuration(450L);
            transform.setInterpolator(new DecelerateInterpolator());
            transform.addTarget(dialogView);
            dialog.getWindow().setEnterTransition(transform);
            dialog.getWindow().setReturnTransition(transform);
        }

        TextInputEditText etSearch = dialogView.findViewById(R.id.et_subtitle_search);
        etSearch.setText(subtitleSearchQuery);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { subtitleSearchQuery = s.toString(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        dialogView.findViewById(R.id.btn_open_tracks).setOnClickListener(v -> { dialog.dismiss(); showSubtitleTracksBottomSheet(); });
        dialogView.findViewById(R.id.btn_open_appearance).setOnClickListener(v -> { dialog.dismiss(); showSubtitleAppearanceBottomSheet(); });
        dialogView.findViewById(R.id.btn_open_delay).setOnClickListener(v -> { dialog.dismiss(); showSubtitleDelayBottomSheet(); });

        dialogView.findViewById(R.id.btn_search_online).setOnClickListener(v -> {
            if (subtitleSearchQuery.trim().isEmpty()) Toast.makeText(this, "Enter a search term", Toast.LENGTH_SHORT).show();
            else { dialog.dismiss(); showSubtitleSideSheet(); }
        });

        dialog.show();
    }

    private void showSubtitleTracksBottomSheet() {
        tracksBottomSheet = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_subtitles, null);
        tracksBottomSheet.setContentView(view);

        RecyclerView rv = view.findViewById(R.id.rv_subtitles);
        TextView tvNoSubs = view.findViewById(R.id.tv_no_subs);
        MaterialButton btnAddLocal = view.findViewById(R.id.btn_add_local);
        MaterialButton btnNone = view.findViewById(R.id.btn_none_subs);

        if (btnNone != null) {
            btnNone.setOnClickListener(v -> {
                removeSubtitles();
                tracksBottomSheet.dismiss();
            });
        }

        btnAddLocal.setOnClickListener(v -> subtitlePickerLauncher.launch(new String[]{"*/*"}));

        rv.setLayoutManager(new LinearLayoutManager(this));
        availableSubsAdapter = new SubtitleAdapter(availableSubtitles, subtitle -> {
            activeSubtitleUri = Uri.parse(subtitle.getDownloadUrl());
            addSubtitleToPlayer(subtitle);
            Toast.makeText(this, "Subtitle track selected", Toast.LENGTH_SHORT).show();
        });
        rv.setAdapter(availableSubsAdapter);

        updateTracksUI(tvNoSubs, rv);
        tracksBottomSheet.show();
    }

    private void removeSubtitles() {
        activeSubtitleUri = null;
        if (player != null) {
            long currentPos = player.getCurrentPosition();
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(Uri.parse(videoUriString))
                    .setSubtitleConfigurations(Collections.emptyList())
                    .build();
            player.setMediaItem(mediaItem, false);
            player.prepare();
            player.seekTo(currentPos);
            player.play();
        }
        
        SharedPreferences historyPrefs = getSharedPreferences("VideoPlaybackHistory", Context.MODE_PRIVATE);
        historyPrefs.edit().remove(videoUriString + "_sub").remove(videoUriString + "_sub_name").apply();
        
        Toast.makeText(this, "Subtitles turned off", Toast.LENGTH_SHORT).show();
    }

    private void updateTracksUI(TextView tvNoSubs, RecyclerView rv) {
        if (availableSubtitles.isEmpty()) {
            tvNoSubs.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
        } else {
            tvNoSubs.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
        }
    }

    private void showSubtitleAppearanceBottomSheet() {
        BottomSheetDialog appearanceBottomSheet = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_subtitle_appearance, null);
        appearanceBottomSheet.setContentView(view);

        MaterialButton btnMinus = view.findViewById(R.id.btn_font_minus);
        MaterialButton btnPlus = view.findViewById(R.id.btn_font_plus);
        TextView tvSize = view.findViewById(R.id.tv_font_size);

        tvSize.setText(String.valueOf((int) subtitleFontSize));

        btnMinus.setOnClickListener(v -> {
            if (subtitleFontSize > 10) {
                subtitleFontSize -= 2;
                tvSize.setText(String.valueOf((int) subtitleFontSize));
                applySubtitleFontSize();
            }
        });

        btnPlus.setOnClickListener(v -> {
            if (subtitleFontSize < 60) {
                subtitleFontSize += 2;
                tvSize.setText(String.valueOf((int) subtitleFontSize));
                applySubtitleFontSize();
            }
        });

        appearanceBottomSheet.show();
    }

    private void showSubtitleDelayBottomSheet() {
        BottomSheetDialog delayBottomSheet = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_subtitle_delay, null);
        delayBottomSheet.setContentView(view);

        MaterialButton btnMinus = view.findViewById(R.id.btn_delay_minus);
        MaterialButton btnPlus = view.findViewById(R.id.btn_delay_plus);
        TextView tvDelay = view.findViewById(R.id.tv_delay_value);

        tvDelay.setText(subtitleDelay + " ms");

        btnMinus.setOnClickListener(v -> {
            subtitleDelay -= 100;
            tvDelay.setText(subtitleDelay + " ms");
        });

        btnPlus.setOnClickListener(v -> {
            subtitleDelay += 100;
            tvDelay.setText(subtitleDelay + " ms");
        });

        delayBottomSheet.show();
    }

    private void showSubtitleSideSheet() {
        SideSheetDialog sideSheetDialog = new SideSheetDialog(this);
        View sideSheetView = LayoutInflater.from(this).inflate(R.layout.side_sheet_subtitles, null);
        sideSheetDialog.setContentView(sideSheetView);

        RecyclerView rv = sideSheetView.findViewById(R.id.rv_search_results);
        View loader = sideSheetView.findViewById(R.id.search_loader);
        View noResults = sideSheetView.findViewById(R.id.tv_no_results);

        rv.setLayoutManager(new LinearLayoutManager(this));
        SubtitleAdapter adapter = new SubtitleAdapter(searchResults, subtitle -> {
            requestDownloadUrlAndAdd(subtitle);
            Toast.makeText(this, "Downloading subtitle...", Toast.LENGTH_SHORT).show();
        });
        rv.setAdapter(adapter);

        searchOpenSubtitles(subtitleSearchQuery, loader, rv, noResults, adapter);
        sideSheetDialog.show();
    }

    private void searchOpenSubtitles(String query, View loader, RecyclerView rv, View noResults, SubtitleAdapter adapter) {
        loader.setVisibility(View.VISIBLE);
        rv.setVisibility(View.GONE);
        noResults.setVisibility(View.GONE);

        String url = "https://api.opensubtitles.com/api/v1/subtitles?query=" + query;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "VideoPlayerApp v1.0") 
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    loader.setVisibility(View.GONE);
                    Toast.makeText(PlayerActivity.this, "Search failed", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray dataArray = jsonObject.getJSONArray("data");
                        searchResults.clear();
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject item = dataArray.getJSONObject(i);
                            JSONObject attributes = item.getJSONObject("attributes");
                            JSONArray files = attributes.getJSONArray("files");
                            if (files.length() > 0) {
                                JSONObject file = files.getJSONObject(0);
                                searchResults.add(new SubtitleModel(
                                        item.getString("id"),
                                        attributes.getString("release"),
                                        attributes.getString("language"),
                                        "", 
                                        file.getInt("file_id")
                                ));
                            }
                        }
                        runOnUiThread(() -> {
                            loader.setVisibility(View.GONE);
                            if (searchResults.isEmpty()) noResults.setVisibility(View.VISIBLE);
                            else { rv.setVisibility(View.VISIBLE); adapter.notifyDataSetChanged(); }
                        });
                    } catch (Exception e) { e.printStackTrace(); }
                } else { runOnUiThread(() -> loader.setVisibility(View.GONE)); }
            }
        });
    }

    private void requestDownloadUrlAndAdd(SubtitleModel subtitle) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("file_id", subtitle.getFileId());
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    jsonBody.toString(), okhttp3.MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url("https://api.opensubtitles.com/api/v1/download")
                    .post(body)
                    .addHeader("User-Agent", "VideoPlayerApp v1.0")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(PlayerActivity.this, "Download request failed", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response.body().string());
                            String downloadUrl = jsonResponse.getString("link");
                            subtitle.setDownloadUrl(downloadUrl);
                            activeSubtitleUri = Uri.parse(downloadUrl);
                            runOnUiThread(() -> {
                                addSubtitleToPlayer(subtitle);
                                if (availableSubsAdapter != null) availableSubsAdapter.notifyDataSetChanged();
                            });
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void applySubtitleFontSize() {
        if (playerView != null && playerView.getSubtitleView() != null) {
            playerView.getSubtitleView().setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleFontSize);
        }
    }

    private void addSubtitleToPlayer(SubtitleModel subtitle) {
        long currentPos = (player != null) ? player.getCurrentPosition() : resumePosition;
        activeSubtitleUri = Uri.parse(subtitle.getDownloadUrl());
        
        MediaItem.SubtitleConfiguration subtitleConfiguration = new MediaItem.SubtitleConfiguration.Builder(activeSubtitleUri)
                .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                .setLanguage(subtitle.getLanguage())
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build();

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(Uri.parse(videoUriString))
                .setSubtitleConfigurations(Collections.singletonList(subtitleConfiguration))
                .build();

        if (player != null) {
            player.setMediaItem(mediaItem, false); 
            player.prepare();
            player.seekTo(currentPos);
            player.play();
            applySubtitleFontSize();
        } else {
            resumePosition = currentPos;
        }
        
        boolean exists = false;
        for (SubtitleModel s : availableSubtitles) {
            if (s.getId().equals(subtitle.getId())) { exists = true; break; }
        }
        if (!exists) availableSubtitles.add(subtitle);
        
        // Save the subtitle name for later re-population
        SharedPreferences historyPrefs = getSharedPreferences("VideoPlaybackHistory", Context.MODE_PRIVATE);
        historyPrefs.edit().putString(videoUriString + "_sub_name", subtitle.getFileName()).apply();

        if (tracksBottomSheet != null && tracksBottomSheet.isShowing()) {
            RecyclerView rv = tracksBottomSheet.findViewById(R.id.rv_subtitles);
            TextView tvNoSubs = tracksBottomSheet.findViewById(R.id.tv_no_subs);
            if (rv != null && tvNoSubs != null) {
                updateTracksUI(tvNoSubs, rv);
                if (availableSubsAdapter != null) availableSubsAdapter.notifyDataSetChanged();
            }
        }

        Toast.makeText(this, "Subtitle added: " + subtitle.getFileName(), Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupWavySeeking() {
        if (progressContainer == null || wavyProgress == null) return;
        progressContainer.setOnTouchListener((view, event) -> {
            if (player == null || isLocked) { if (isLocked && event.getAction() == MotionEvent.ACTION_DOWN) showLockedToast(); return false; }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: case MotionEvent.ACTION_MOVE:
                    isUserSeeking = true;
                    float width = view.getWidth(), touchX = event.getX();
                    if (touchX < 0) touchX = 0; if (touchX > width) touchX = width;
                    float percentage = touchX / width;
                    long duration = player.getDuration();
                    if (duration > 0) {
                        long newPosition = (long) (duration * percentage);
                        wavyProgress.setProgress((int) (percentage * 1000));
                        if (positionText != null) positionText.setText(stringForTime(newPosition));
                        player.seekTo(newPosition);
                    }
                    return true;
                case MotionEvent.ACTION_UP: case MotionEvent.ACTION_CANCEL:
                    isUserSeeking = false; return true;
            }
            return false;
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupDoubleTapSeeking() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (player == null || isLocked) return super.onDoubleTap(e);
                float viewWidth = playerView.getWidth();
                if (e.getX() < viewWidth / 2) seekBackward();
                else seekForward();
                return true;
            }
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (playerView.isControllerFullyVisible()) animateOutControls();
                else { playerView.showController(); animateInControls(); }
                return true;
            }
        });
        playerView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    private void seekForward() {
        if (player == null) return;
        long newPos = player.getCurrentPosition() + 10000;
        player.seekTo(Math.min(newPos, player.getDuration()));
        showFeedback(layoutFwd);
        if (playerView.isControllerFullyVisible()) {
            handler.removeCallbacks(hideControllerAction);
            handler.postDelayed(hideControllerAction, 3000);
        }
    }

    private void seekBackward() {
        if (player == null) return;
        long newPos = player.getCurrentPosition() - 10000;
        player.seekTo(Math.max(newPos, 0));
        showFeedback(layoutRew);
        if (playerView.isControllerFullyVisible()) {
            handler.removeCallbacks(hideControllerAction);
            handler.postDelayed(hideControllerAction, 3000);
        }
    }

    private void showFeedback(View view) {
        if (view == null) return;
        view.animate().cancel();
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(200).withEndAction(() -> 
            view.animate().alpha(0f).setDuration(500).setStartDelay(500).withEndAction(() -> 
                view.setVisibility(View.INVISIBLE)
            ).start()
        ).start();
    }

    private final Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.getPlaybackState() != Player.STATE_IDLE && player.getPlaybackState() != Player.STATE_ENDED && !isUserSeeking) {
                long current = player.getCurrentPosition(), duration = player.getDuration();
                if (duration > 0) {
                    int progress = (int) ((current * 1000) / duration);
                    if (wavyProgress != null) wavyProgress.setProgressCompat(progress, true);
                    if (positionText != null) positionText.setText(stringForTime(current));
                    if (durationText != null) durationText.setText(stringForTime(duration));
                }
            }
            handler.postDelayed(this, 50);
        }
    };

    private String stringForTime(long timeMs) {
        if (timeMs <= 0 || timeMs >= 24 * 60 * 60 * 1000) return "00:00";
        long totalSeconds = timeMs / 1000, seconds = totalSeconds % 60, minutes = (totalSeconds / 60) % 60, hours = totalSeconds / 3600;
        formatBuilder.setLength(0);
        if (hours > 0) return formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        else return formatter.format("%02d:%02d", minutes, seconds).toString();
    }

    private void initializePlayer() {
        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);
            MediaItem.Builder mediaItemBuilder = new MediaItem.Builder().setUri(Uri.parse(videoUriString));
            if (activeSubtitleUri != null) {
                MediaItem.SubtitleConfiguration subtitleConfiguration = new MediaItem.SubtitleConfiguration.Builder(activeSubtitleUri)
                        .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build();
                mediaItemBuilder.setSubtitleConfigurations(Collections.singletonList(subtitleConfiguration));
            }
            player.setMediaItem(mediaItemBuilder.build());
            player.addListener(new Player.Listener() {
                @Override public void onIsPlayingChanged(boolean isPlaying) {
                    if (isPlaying) { if (playBtn != null) playBtn.setVisibility(View.GONE); if (pauseBtn != null) pauseBtn.setVisibility(View.VISIBLE); }
                    else { if (playBtn != null) playBtn.setVisibility(View.VISIBLE); if (pauseBtn != null) pauseBtn.setVisibility(View.GONE); }
                }
            });
            if (resumePosition > 0) player.seekTo(resumePosition);
            player.prepare();
            player.play();
            applySubtitleFontSize();
            if (playBtn != null) playBtn.setOnClickListener(v -> { if (isLocked) showLockedToast(); else player.play(); });
            if (pauseBtn != null) pauseBtn.setOnClickListener(v -> { if (isLocked) showLockedToast(); else player.pause(); });
        }
    }

    @Override protected void onStart() { super.onStart(); initializePlayer(); handler.post(updateProgressAction); }
    @Override protected void onStop() { super.onStop(); savePlaybackState(); handler.removeCallbacks(updateProgressAction); handler.removeCallbacks(hideControllerAction); if (player != null) { player.release(); player = null; } }
    @Override protected void onPause() { super.onPause(); savePlaybackState(); }

    private void savePlaybackState() {
        if (player != null && videoUriString != null) {
            long currentPos = player.getCurrentPosition(), duration = player.getDuration();
            if (duration > 0) {
                SharedPreferences globalPrefs = getSharedPreferences("VideoPlayerPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor globalEditor = globalPrefs.edit();
                globalEditor.putString("LAST_VIDEO_URI", videoUriString);
                globalEditor.putString("LAST_VIDEO_TITLE", videoTitle);
                globalEditor.putLong("LAST_VIDEO_POSITION", currentPos);
                globalEditor.putLong("LAST_VIDEO_DURATION", duration);
                globalEditor.apply();

                SharedPreferences historyPrefs = getSharedPreferences("VideoPlaybackHistory", Context.MODE_PRIVATE);
                SharedPreferences.Editor historyEditor = historyPrefs.edit();
                historyEditor.putLong(videoUriString, currentPos);
                if (activeSubtitleUri != null) {
                    historyEditor.putString(videoUriString + "_sub", activeSubtitleUri.toString());
                } else {
                    historyEditor.remove(videoUriString + "_sub");
                }
                historyEditor.putFloat("global_subtitle_font_size", subtitleFontSize);
                historyEditor.apply();
            }
        }
    }
}
