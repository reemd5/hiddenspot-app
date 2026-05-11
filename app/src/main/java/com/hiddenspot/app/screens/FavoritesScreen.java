package com.hiddenspot.app.screens;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.hiddenspot.app.R;
import com.hiddenspot.app.adapters.PlaceAdapter;
import com.hiddenspot.app.models.Place;
import com.hiddenspot.app.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FavoritesScreen extends Fragment {

    private RecyclerView rvFavorites;
    private LinearLayout layoutEmpty;
    private TextView tvCount;
    private PlaceAdapter adapter;
    private final List<Place> favoritesList = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvFavorites = view.findViewById(R.id.rv_favorites);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        tvCount     = view.findViewById(R.id.tv_count);

        adapter = new PlaceAdapter(requireContext(), favoritesList);
        rvFavorites.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFavorites.setAdapter(adapter);

        //click on favorites button (heart) to remove post from favorites
        adapter.setOnFavoriteClickListener((place, pos) -> {
            String uid = FirebaseHelper.getInstance().getCurrentUser() != null
                    ? FirebaseHelper.getInstance().getCurrentUser().getUid() : null;
            if (uid == null || place.getId() == null) return;

            // remove it
            FirebaseHelper.getInstance().unsaveGem(uid, place.getId(),
                    v -> requireActivity().runOnUiThread(this::loadFavorites),
                    e -> requireActivity().runOnUiThread(() ->
                            loadFavorites()));
        });
        loadFavorites();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
    }
    // reload list so that post disappears
    private void loadFavorites() {
        String uid = FirebaseHelper.getInstance().getCurrentUser() != null
                ? FirebaseHelper.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) { showEmpty(); return; }

        FirebaseHelper.getInstance().fetchSavedGemIds(uid, savesSnap -> {
            Set<String> ids = new HashSet<>();
            for (DocumentSnapshot doc : savesSnap.getDocuments()) {
                String gemId = doc.getString("gemId");
                if (gemId != null) ids.add(gemId);
            }
            if (ids.isEmpty()) { showEmpty(); return; }

            FirebaseHelper.getInstance().fetchAllGems(gemsSnap -> {
                favoritesList.clear();
                for (DocumentSnapshot doc : gemsSnap.getDocuments()) {
                    if (ids.contains(doc.getId())) {
                        Place p = doc.toObject(Place.class);
                        if (p != null) { p.setId(doc.getId()); p.setFavorited(true); favoritesList.add(p); }
                    }
                }
                requireActivity().runOnUiThread(() -> {
                    adapter.updatePlaces(favoritesList);
                    tvCount.setText(String.format(Locale.getDefault(),
                            getString(R.string.saved_places), favoritesList.size()));
                    layoutEmpty.setVisibility(favoritesList.isEmpty() ? View.VISIBLE : View.GONE);
                    rvFavorites.setVisibility(favoritesList.isEmpty() ? View.GONE : View.VISIBLE);
                });
            }, e -> showEmpty());
        }, e -> showEmpty());
    }

    private void showEmpty() {
        if (getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvFavorites.setVisibility(View.GONE);
            tvCount.setText(String.format(Locale.getDefault(), getString(R.string.saved_places), 0));
        });
    }
}
