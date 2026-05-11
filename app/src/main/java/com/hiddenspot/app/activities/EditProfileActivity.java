package com.hiddenspot.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
import com.hiddenspot.app.R;
import com.hiddenspot.app.utils.FirebaseHelper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {

    private static final int REQUEST_GALLERY = 300;
    private static final int REQUEST_PERM = 301;
    private static final int REQUEST_CAMERA = 302;

    private Uri cameraImageUri;
    private CircleImageView ivAvatar;
    private TextView tvAvatarLetter;
    private TextInputEditText etDisplayName;
    private TextInputEditText etEmail;
    private TextInputEditText etBio;
    private MaterialButton btnSave;
    private MaterialButton btnDeletePhoto;
    private Uri selectedImageUri;
    private boolean photoDeleted = false;
    private String currentAvatarUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        ivAvatar = findViewById(R.id.iv_avatar);
        tvAvatarLetter = findViewById(R.id.tv_avatar_letter);
        etDisplayName = findViewById(R.id.et_display_name);
        etEmail = findViewById(R.id.et_email);
        etBio = findViewById(R.id.et_bio);
        btnSave = findViewById(R.id.btn_save);
        ImageButton btnBack = findViewById(R.id.btn_back);

        ImageButton btnChangePhoto = findViewById(R.id.btn_change_photo);

        btnBack.setOnClickListener(v -> finish());
        btnChangePhoto.setOnClickListener(v -> showPhotoOptionsDialog());

        btnSave.setOnClickListener(v -> saveProfile());

        loadProfile();

        String action = getIntent().getStringExtra("action");
        if ("gallery".equals(action)) {
            checkPermissionAndPickGallery();
        } else if ("camera".equals(action)) {
            openCamera();
        }
    }

    private void loadProfile() {
        FirebaseUser user = FirebaseHelper.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etEmail.setText(user.getEmail() != null ? user.getEmail() : "");

        FirebaseHelper.getInstance().fetchUserProfile(user.getUid(), snap -> runOnUiThread(() -> {
            String displayName = snap.getString("displayName");
            String avatarUrl = snap.getString("avatarUrl");
            String bio = snap.getString("bio");

            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()
                        ? user.getDisplayName()
                        : deriveFallbackName(user);
            }

            if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                avatarUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";
            }

            currentAvatarUrl = avatarUrl != null ? avatarUrl : "";
            etDisplayName.setText(displayName);
            etBio.setText(bio != null ? bio : "");
            updateAvatar(displayName, currentAvatarUrl);
            updateDeleteButtonVisibility();

        }), e -> runOnUiThread(() -> {
            String fallbackName = user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()
                    ? user.getDisplayName()
                    : deriveFallbackName(user);
            currentAvatarUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";
            etDisplayName.setText(fallbackName);
            etBio.setText("");
            updateAvatar(fallbackName, currentAvatarUrl);
        }));
    }

    private void checkPermissionAndPickGallery() {
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{perm}, REQUEST_PERM);
        }
    }

    private void openGallery() {
        startActivityForResult(new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI), REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_GALLERY && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                tvAvatarLetter.setVisibility(View.GONE);
                Glide.with(this).load(selectedImageUri).centerCrop().into(ivAvatar);
                photoDeleted = false;
                updateDeleteButtonVisibility();
            }
        }
        if (resultCode == RESULT_OK && requestCode == REQUEST_CAMERA) {
            if (cameraImageUri != null) {
                selectedImageUri = cameraImageUri;
                tvAvatarLetter.setVisibility(View.GONE);
                Glide.with(this).load(cameraImageUri).centerCrop().into(ivAvatar);
                photoDeleted = false;
                updateDeleteButtonVisibility();
            }
        }
    }

    private void saveProfile() {
        FirebaseUser user = FirebaseHelper.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        String displayName = etDisplayName.getText() != null
                ? etDisplayName.getText().toString().trim() : "";
        String bio = etBio.getText() != null
                ? etBio.getText().toString().trim() : "";
        if (displayName.isEmpty()) {
            etDisplayName.setError("Display name is required");
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        if (photoDeleted) {
            persistProfile(user, displayName, bio, "");
        } else if (selectedImageUri != null) {
            persistProfile(user, displayName, bio, encodeImageToBase64(selectedImageUri));
        } else {
            persistProfile(user, displayName, bio, currentAvatarUrl);
        }
    }

    private String encodeImageToBase64(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (is != null) is.close();
            if (bitmap == null) return "";
            return encodeBitmapToBase64(bitmap);
        } catch (Exception e) {
            return "";
        }
    }

    private void showPhotoOptionsDialog() {
        boolean hasPhoto = (selectedImageUri != null)
                || (!photoDeleted && currentAvatarUrl != null
                && !currentAvatarUrl.trim().isEmpty());

        int dp16 = Math.round(16 * getResources().getDisplayMetrics().density);
        int dp12 = Math.round(12 * getResources().getDisplayMetrics().density);
        int dp1  = Math.round(1  * getResources().getDisplayMetrics().density);

        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setBackgroundColor(getResources().getColor(R.color.card, null));
        container.setPadding(0, dp12, 0, dp12);

        android.app.AlertDialog[] dialogRef = new android.app.AlertDialog[1];

        // Choose from library
        addDialogRow(container, "Choose from library", R.drawable.ic_upload,
                R.color.foreground, dp16, dp12, dp1, () -> {
                    if (dialogRef[0] != null) dialogRef[0].dismiss();
                    checkPermissionAndPickGallery();
                });

        // Take photo
        addDialogRow(container, "Take photo", R.drawable.ic_camera,
                R.color.foreground, dp16, dp12, dp1, () -> {
                    if (dialogRef[0] != null) dialogRef[0].dismiss();
                    openCamera();
                });

        // Delete — only if has photo
        if (hasPhoto) {
            addDialogRow(container, "Remove profile photo", R.drawable.ic_logout,
                    R.color.destructive, dp16, dp12, dp1, () -> {
                        if (dialogRef[0] != null) dialogRef[0].dismiss();
                        confirmDeletePhoto();
                    });
        }

        dialogRef[0] = new android.app.AlertDialog.Builder(this)
                .setView(container)
                .create();

        if (dialogRef[0].getWindow() != null) {
            dialogRef[0].getWindow().setBackgroundDrawableResource(
                    android.R.color.transparent);
        }
        dialogRef[0].show();
    }

    private void addDialogRow(android.widget.LinearLayout container, String label,
                              int iconRes, int colorRes,
                              int dp16, int dp12, int dp1, Runnable onClick) {
        android.widget.LinearLayout row = new android.widget.LinearLayout(this);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp16, dp12, dp16, dp12);
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackgroundColor(getResources().getColor(R.color.card, null));
        row.setOnClickListener(v -> onClick.run());

        android.widget.ImageView icon = new android.widget.ImageView(this);
        android.widget.LinearLayout.LayoutParams iconParams =
                new android.widget.LinearLayout.LayoutParams(dp16 * 2, dp16 * 2);
        iconParams.setMarginEnd(dp16);
        icon.setLayoutParams(iconParams);
        icon.setImageResource(iconRes);
        icon.setColorFilter(getResources().getColor(colorRes, null));

        android.widget.TextView tv = new android.widget.TextView(this);
        tv.setText(label);
        tv.setTextSize(14f);
        tv.setTextColor(getResources().getColor(colorRes, null));
        try {
            android.graphics.Typeface tf = android.graphics.Typeface.createFromAsset(
                    getAssets(), "fonts/inter.ttf");
            tv.setTypeface(tf);
        } catch (Exception ignored) {}

        row.addView(icon);
        row.addView(tv);
        container.addView(row);

        // Divider
        android.view.View divider = new android.view.View(this);
        android.widget.LinearLayout.LayoutParams divParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp1);
        divider.setLayoutParams(divParams);
        divider.setBackgroundColor(getResources().getColor(R.color.border_color, null));
        container.addView(divider);
    }

    private void openCamera() {
        // Check camera permission first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_PERM + 1);
            return;
        }

        try {
            java.io.File photoFile = java.io.File.createTempFile(
                    "avatar_", ".jpg", getExternalCacheDir());
            cameraImageUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    photoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            // Don't use resolveActivity on Android 11+ — just start directly
            startActivityForResult(intent, REQUEST_CAMERA);
        } catch (Exception e) {
            Toast.makeText(this, "Error opening camera: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private String encodeBitmapToBase64(Bitmap bitmap) {
        try {
            int maxDimension = 400;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float ratio = Math.min((float) maxDimension / width, (float) maxDimension / height);
            ratio = Math.min(ratio, 1f);

            int finalWidth = Math.round(ratio * width);
            int finalHeight = Math.round(ratio * height);
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 75, baos);
            byte[] bytes = baos.toByteArray();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    private void persistProfile(FirebaseUser user, String displayName, String bio, String avatarUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("uid", user.getUid());
        updates.put("displayName", displayName);
        updates.put("email", user.getEmail() != null ? user.getEmail() : "");
        updates.put("bio", bio);
        updates.put("avatarUrl", avatarUrl != null ? avatarUrl : "");

        FirebaseHelper.getInstance().updateUserProfile(user.getUid(), updates,
                v -> FirebaseHelper.getInstance().updateAuthProfile(displayName, avatarUrl,
                        v2 -> FirebaseHelper.getInstance().syncUserProfileToGems(user.getUid(), displayName,
                                avatarUrl != null ? avatarUrl : "",
                                v3 -> runOnUiThread(() -> {
                                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                }),
                                e3 -> runOnUiThread(() -> showSaveError(e3.getMessage()))),
                        e2 -> runOnUiThread(() -> showSaveError(e2.getMessage()))),
                e -> runOnUiThread(() -> showSaveError(e.getMessage())));
    }

    private void updateAvatar(String displayName, String avatarUrl) {
        String initial = displayName != null && !displayName.trim().isEmpty()
                ? displayName.substring(0, 1).toUpperCase()
                : "U";
        tvAvatarLetter.setText(initial);

        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            tvAvatarLetter.setVisibility(View.GONE);
            if (avatarUrl.startsWith("http")) {
                Glide.with(this).load(avatarUrl).centerCrop().into(ivAvatar);
            } else {
                try {
                    byte[] imageBytes = Base64.decode(avatarUrl, Base64.DEFAULT);
                    Glide.with(this).load(imageBytes).centerCrop().into(ivAvatar);
                } catch (Exception e) {
                    ivAvatar.setImageDrawable(null);
                    tvAvatarLetter.setVisibility(View.VISIBLE);
                }
            }
        } else {
            ivAvatar.setImageDrawable(null);
            tvAvatarLetter.setVisibility(View.VISIBLE);
        }
    }

    private String deriveFallbackName(FirebaseUser user) {
        if (user.getEmail() != null && user.getEmail().contains("@")) {
            return user.getEmail().split("@")[0];
        }
        return "User";
    }

    private void showSaveError(String message) {
        btnSave.setEnabled(true);
        btnSave.setText("Save Changes");
        Toast.makeText(this, message != null ? message : "Failed to update profile", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == REQUEST_PERM) {
                openGallery();
            } else if (requestCode == REQUEST_PERM + 1) {
                openCamera();
            }
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
    private void confirmDeletePhoto() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Remove Profile Photo")
                .setMessage("Are you sure you want to remove your profile photo?")
                .setPositiveButton("Remove", (dialog, which) -> deletePhoto())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePhoto() {
        ivAvatar.setImageDrawable(null);
        selectedImageUri = null;
        photoDeleted = true;
        String name = etDisplayName.getText() != null
                ? etDisplayName.getText().toString().trim() : "U";
        tvAvatarLetter.setText(name.isEmpty() ? "U" : name.substring(0, 1).toUpperCase());
        tvAvatarLetter.setVisibility(View.VISIBLE);
        updateDeleteButtonVisibility();
        Toast.makeText(this, "Photo removed — tap Save to confirm", Toast.LENGTH_SHORT).show();
    }

    private void updateDeleteButtonVisibility() {
        if (btnDeletePhoto == null) return;
        boolean hasPhoto = (selectedImageUri != null)
                || (!photoDeleted && currentAvatarUrl != null && !currentAvatarUrl.trim().isEmpty());
        btnDeletePhoto.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);
    }
}
