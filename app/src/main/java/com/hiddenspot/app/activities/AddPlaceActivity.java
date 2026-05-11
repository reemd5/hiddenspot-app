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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.hiddenspot.app.models.Place;
import com.hiddenspot.app.utils.FirebaseHelper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AddPlaceActivity extends AppCompatActivity {

    private static final int REQUEST_GALLERY = 100;
    private static final int REQUEST_CAMERA = 101;
    private static final int REQUEST_PERM = 200;

    public static final String EXTRA_IS_EDIT_MODE = "is_edit_mode";
    public static final String EXTRA_PLACE_ID = "place_id";
    public static final String EXTRA_PLACE_NAME = "place_name";
    public static final String EXTRA_PLACE_CITY = "place_city";
    public static final String EXTRA_PLACE_ADDRESS = "place_address";
    public static final String EXTRA_PLACE_PHONE = "place_phone";
    public static final String EXTRA_PLACE_DESC = "place_desc";
    public static final String EXTRA_PLACE_CATEGORY = "place_category";
    public static final String EXTRA_PLACE_IMAGE = "place_image";

    private TextInputEditText etPlaceName;
    private TextInputEditText etCity;
    private TextInputEditText etAddress;
    private TextInputEditText etPhone;
    private TextInputEditText etDescription;
    private Spinner spinnerCategory;
    private MaterialButton btnSubmit;
    private ImageButton btnBack;
    private LinearLayout btnUploadGallery;
    private LinearLayout btnOpenCamera;
    private ImageView ivPreview;
    private TextView tvHeaderTitle;

    private Uri selectedImageUri = null;
    private Bitmap capturedBitmap = null;
    private String selectedCategory = "Restaurant";
    private boolean isEditMode = false;
    private String editingPlaceId = "";
    private String existingImageData = "";

    private static final String[] CATEGORIES = {
            "Restaurant", "Garden", "Café", "Viewpoint",
            "Park", "Beach", "Library", "Shop", "Historical"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_place);
        bindViews();
        setupSpinner();
        loadIntentData();
        setupListeners();
    }

    public static Intent createEditIntent(android.content.Context context, Place place) {
        Intent intent = new Intent(context, AddPlaceActivity.class);
        intent.putExtra(EXTRA_IS_EDIT_MODE, true);
        intent.putExtra(EXTRA_PLACE_ID, place.getId());
        intent.putExtra(EXTRA_PLACE_NAME, place.getName());
        intent.putExtra(EXTRA_PLACE_CITY, place.getCity());
        intent.putExtra(EXTRA_PLACE_ADDRESS, place.getAddress());
        intent.putExtra(EXTRA_PLACE_PHONE, place.getPhone());
        intent.putExtra(EXTRA_PLACE_DESC, place.getDescription());
        intent.putExtra(EXTRA_PLACE_CATEGORY, place.getCategory());
        intent.putExtra(EXTRA_PLACE_IMAGE, place.getFirstImage());
        return intent;
    }

    private void bindViews() {
        etPlaceName = findViewById(R.id.et_place_name);
        etCity = findViewById(R.id.et_city);
        etAddress = findViewById(R.id.et_address);
        etPhone = findViewById(R.id.et_phone);
        etDescription = findViewById(R.id.et_description);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnSubmit = findViewById(R.id.btn_submit);
        btnBack = findViewById(R.id.btn_back);
        btnUploadGallery = findViewById(R.id.btn_upload_gallery);
        btnOpenCamera = findViewById(R.id.btn_open_camera);
        ivPreview = findViewById(R.id.iv_preview);
        tvHeaderTitle = findViewById(R.id.tv_header_title);
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, CATEGORIES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = CATEGORIES[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadIntentData() {
        Intent intent = getIntent();
        isEditMode = intent.getBooleanExtra(EXTRA_IS_EDIT_MODE, false);
        if (!isEditMode) {
            return;
        }

        editingPlaceId = intent.getStringExtra(EXTRA_PLACE_ID);
        existingImageData = intent.getStringExtra(EXTRA_PLACE_IMAGE);
        etPlaceName.setText(intent.getStringExtra(EXTRA_PLACE_NAME));
        etCity.setText(intent.getStringExtra(EXTRA_PLACE_CITY));
        etAddress.setText(intent.getStringExtra(EXTRA_PLACE_ADDRESS));
        etPhone.setText(intent.getStringExtra(EXTRA_PLACE_PHONE));
        etDescription.setText(intent.getStringExtra(EXTRA_PLACE_DESC));

        String category = intent.getStringExtra(EXTRA_PLACE_CATEGORY);
        if (category != null && !category.trim().isEmpty()) {
            selectedCategory = category;
            int index = findCategoryIndex(category);
            if (index >= 0) {
                spinnerCategory.setSelection(index);
            }
        }

        if (existingImageData != null && !existingImageData.trim().isEmpty()) {
            ivPreview.setVisibility(View.VISIBLE);
            if (existingImageData.startsWith("http")) {
                Glide.with(this).load(existingImageData).centerCrop().into(ivPreview);
            } else {
                try {
                    byte[] imageBytes = Base64.decode(existingImageData, Base64.DEFAULT);
                    Glide.with(this).load(imageBytes).centerCrop().into(ivPreview);
                } catch (Exception e) {
                    ivPreview.setVisibility(View.GONE);
                }
            }
        }

        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText("Edit Post");
        }
        btnSubmit.setText("Update Post");
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnUploadGallery.setOnClickListener(v -> checkPermissionAndPickGallery());
        btnOpenCamera.setOnClickListener(v -> checkPermissionAndOpenCamera());
        btnSubmit.setOnClickListener(v -> submitPlace());
    }

    private void checkPermissionAndPickGallery() {
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{perm}, REQUEST_PERM);
        }
    }

    private void openGallery() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_GALLERY);
    }

    private void checkPermissionAndOpenCamera() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(this,
                    "No camera available. Please use the Upload option instead.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_PERM + 1);
        }
    }

    private void openCamera() {
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (i.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(i, REQUEST_CAMERA);
        } else {
            Toast.makeText(this,
                    "Camera not available on this emulator. Use Upload instead.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        if (requestCode == REQUEST_GALLERY && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            capturedBitmap = null;
            Glide.with(this).load(selectedImageUri).centerCrop().into(ivPreview);
            ivPreview.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Photo selected!", Toast.LENGTH_SHORT).show();

        } else if (requestCode == REQUEST_CAMERA && data != null && data.getExtras() != null) {
            capturedBitmap = (Bitmap) data.getExtras().get("data");
            selectedImageUri = null;
            if (capturedBitmap != null) {
                ivPreview.setImageBitmap(capturedBitmap);
                ivPreview.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Photo captured!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void submitPlace() {
        String name = getText(etPlaceName);
        String city = getText(etCity);
        String address = getText(etAddress);
        String phone = getText(etPhone);
        String desc = getText(etDescription);

        if (name.isEmpty() || city.isEmpty() || address.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseHelper.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText(isEditMode ? "Updating..." : "Processing...");

        new Thread(() -> {
            String base64Image = "";
            if (selectedImageUri != null) {
                base64Image = encodeImageToBase64(selectedImageUri);
            } else if (capturedBitmap != null) {
                base64Image = encodeBitmapToBase64(capturedBitmap);
            }
            String finalImage = base64Image;
            runOnUiThread(() -> savePlace(name, city, address, phone, desc, user, finalImage));
        }).start();
    }

    private String encodeImageToBase64(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (is != null) is.close();
            return encodeBitmapToBase64(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String encodeBitmapToBase64(Bitmap bitmap) {
        try {
            int maxDim = 600;
            float ratio = Math.min((float) maxDim / bitmap.getWidth(),
                    (float) maxDim / bitmap.getHeight());
            Bitmap resized = Bitmap.createScaledBitmap(bitmap,
                    Math.round(ratio * bitmap.getWidth()),
                    Math.round(ratio * bitmap.getHeight()), true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void savePlace(String name, String city, String address, String phone,
                           String desc, FirebaseUser user, String imageData) {
        btnSubmit.setText(isEditMode ? "Updating..." : "Saving...");
        List<String> images = new ArrayList<>();
        if (!imageData.isEmpty()) images.add(imageData);

        String displayName = user.getDisplayName() != null ? user.getDisplayName() : "Anonymous";
        String fallbackAvatar = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";

        FirebaseHelper.getInstance().fetchUserProfile(user.getUid(), snap -> {
            String avatarUrl = snap.getString("avatarUrl");
            if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                avatarUrl = fallbackAvatar;
            }
            savePlaceDocument(name, city, address, phone, desc, images, user.getUid(), displayName, avatarUrl);
        }, e -> savePlaceDocument(name, city, address, phone, desc, images, user.getUid(), displayName, fallbackAvatar));
    }

    private void savePlaceDocument(String name, String city, String address, String phone,
                                   String desc, List<String> images, String userId,
                                   String displayName, String avatarUrl) {
        List<String> finalImages = new ArrayList<>();
        if (!images.isEmpty()) {
            finalImages.addAll(images);
        } else if (isEditMode && existingImageData != null && !existingImageData.trim().isEmpty()) {
            finalImages.add(existingImageData);
        }

        Place place = new Place(name, city, address, phone, desc,
                selectedCategory, finalImages, userId, displayName);
        place.setUserAvatar(avatarUrl != null ? avatarUrl : "");

        if (isEditMode && editingPlaceId != null && !editingPlaceId.trim().isEmpty()) {
            FirebaseHelper.getInstance().updateGem(editingPlaceId, place,
                    v -> runOnUiThread(() -> {
                        Toast.makeText(this, "Post updated successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    }),
                    e -> runOnUiThread(() -> {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Update Post");
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }));
            return;
        }

        FirebaseHelper.getInstance().addGem(place,
                docRef -> runOnUiThread(() -> {
                    Toast.makeText(this, "Gem added successfully!", Toast.LENGTH_LONG).show();
                    finish();
                }),
                e -> runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText(getString(R.string.submit_btn));
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }));
    }

    private int findCategoryIndex(String category) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(category)) {
                return i;
            }
        }
        return -1;
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            if (req == REQUEST_PERM) openGallery();
            if (req == REQUEST_PERM + 1) openCamera();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
