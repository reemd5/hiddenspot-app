package com.hiddenspot.app.screens;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.hiddenspot.app.R;
import com.hiddenspot.app.adapters.PlaceAdapter;
import com.hiddenspot.app.models.Place;
import com.hiddenspot.app.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeScreen extends Fragment {

    private RecyclerView rvPlaces;
    private LinearLayout layoutCategories, layoutEmpty;
    private TextView tvSectionTitle, tvPlacesCount;
    private TextInputEditText etSearch;
    private PlaceAdapter adapter;
    private final List<Place> allPlaces      = new ArrayList<>();
    private final List<Place> filteredPlaces = new ArrayList<>();
    private String selectedCategory = "All";

    private static final String[] CATEGORIES = {
            "All", "Restaurant", "Garden", "Café", "Viewpoint",
            "Park", "Beach", "Library", "Shop", "Historical"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvPlaces         = view.findViewById(R.id.rv_places);
        layoutCategories = view.findViewById(R.id.layout_categories);
        layoutEmpty      = view.findViewById(R.id.layout_empty);
        tvSectionTitle   = view.findViewById(R.id.tv_section_title);
        tvPlacesCount    = view.findViewById(R.id.tv_places_count);
        etSearch         = view.findViewById(R.id.et_search);

        adapter = new PlaceAdapter(requireContext(), filteredPlaces);
        rvPlaces.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPlaces.setAdapter(adapter);
        rvPlaces.setNestedScrollingEnabled(false);

        adapter.setOnFavoriteClickListener((place, pos) -> {
            boolean newState = !place.isFavorited();
            place.setFavorited(newState);
            adapter.notifyItemChanged(pos);
            String uid = FirebaseHelper.getInstance().getCurrentUser() != null
                    ? FirebaseHelper.getInstance().getCurrentUser().getUid() : null;
            if (uid != null && place.getId() != null) {
                if (newState) FirebaseHelper.getInstance().saveGem(uid, place.getId(), v -> {}, e -> {});
                else          FirebaseHelper.getInstance().unsaveGem(uid, place.getId(), v -> {}, e -> {});
            }
        });

        buildCategoryChips();
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { applyFilters(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
        loadPlaces();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPlaces();
    }

    private void loadPlaces() {
        String uid = FirebaseHelper.getInstance().getCurrentUser() != null
                ? FirebaseHelper.getInstance().getCurrentUser().getUid() : null;

        if (uid == null) {
            fetchPlacesWithFavoriteIds(new HashSet<>());
            return;
        }

        FirebaseHelper.getInstance().fetchSavedGemIds(uid, savesSnap -> {
            Set<String> favoriteIds = new HashSet<>();
            for (com.google.firebase.firestore.DocumentSnapshot doc : savesSnap.getDocuments()) {
                String gemId = doc.getString("gemId");
                if (gemId != null) favoriteIds.add(gemId);
            }
            fetchPlacesWithFavoriteIds(favoriteIds);
        }, e -> fetchPlacesWithFavoriteIds(new HashSet<>()));
    }

    private void fetchPlacesWithFavoriteIds(Set<String> favoriteIds) {
        FirebaseHelper.getInstance().fetchAllGems(snap -> {
            allPlaces.clear();
            for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                Place p = doc.toObject(Place.class);
                if (p != null) {
                    p.setId(doc.getId());
                    p.setFavorited(favoriteIds.contains(doc.getId()));
                    allPlaces.add(p);
                }
            }
            applyFilters(etSearch.getText() != null ? etSearch.getText().toString() : "");
        }, e -> Toast.makeText(requireContext(), "Failed to load places", Toast.LENGTH_SHORT).show());
    }

    private void buildCategoryChips() {
        layoutCategories.removeAllViews();
        int dp8 = dp(8), dp16 = dp(16);
        for (String cat : CATEGORIES) {
            TextView chip = new TextView(requireContext());
            chip.setText(cat);
            chip.setTextSize(12f);
            chip.setPadding(dp16, dp8, dp16, dp8);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp8);
            chip.setLayoutParams(lp);
            boolean active = cat.equals(selectedCategory);
            chip.setBackgroundResource(active ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
            chip.setTextColor(active ? Color.WHITE : getResources().getColor(R.color.foreground, null));
            chip.setOnClickListener(v -> {
                selectedCategory = cat;
                buildCategoryChips();
                tvSectionTitle.setText(selectedCategory.equals("All")
                        ? getString(R.string.hidden_gems_near_you)
                        : selectedCategory + getString(R.string.spots_suffix));
                applyFilters(etSearch.getText() != null ? etSearch.getText().toString() : "");
            });
            layoutCategories.addView(chip);
        }
    }

    private void applyFilters(String query) {
        String q = query.toLowerCase(Locale.getDefault()).trim();
        filteredPlaces.clear();
        for (Place p : allPlaces) {
            boolean matchCat = selectedCategory.equals("All") || p.getCategory().equals(selectedCategory);
            boolean matchQ   = q.isEmpty() || p.getName().toLowerCase().contains(q)
                    || p.getCity().toLowerCase().contains(q);
            if (matchCat && matchQ) filteredPlaces.add(p);
        }
        adapter.updatePlaces(filteredPlaces);
        tvPlacesCount.setText(String.format(Locale.getDefault(),
                getString(R.string.places_discovered), filteredPlaces.size()));
        layoutEmpty.setVisibility(filteredPlaces.isEmpty() ? View.VISIBLE : View.GONE);
        rvPlaces.setVisibility(filteredPlaces.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private int dp(int val) { return Math.round(val * getResources().getDisplayMetrics().density); }
}
