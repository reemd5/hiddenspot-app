package com.hiddenspot.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.hiddenspot.app.R;
import com.hiddenspot.app.screens.FavoritesScreen;
import com.hiddenspot.app.screens.HomeScreen;
import com.hiddenspot.app.screens.ProfileScreen;
import com.hiddenspot.app.screens.SearchScreen;
import com.hiddenspot.app.models.AppNotification;
import com.hiddenspot.app.utils.FirebaseHelper;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // Listener for real-time notifications from Firestore
    private ListenerRegistration notificationsListener;
    // Stores already seen notification IDs to avoid duplicates
    private final Set<String> knownNotificationIds = new HashSet<>();
    // Flag to skip showing old notifications on first load
    private boolean notificationsPrimed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        if (savedInstanceState == null) loadFragment(new HomeScreen());

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home)      { loadFragment(new HomeScreen());      return true; }
            if (id == R.id.nav_search)    { loadFragment(new SearchScreen());    return true; }
            if (id == R.id.nav_favorites) { loadFragment(new FavoritesScreen()); return true; }
            if (id == R.id.nav_profile)   { loadFragment(new ProfileScreen());   return true; }
            if (id == R.id.nav_add) {
                startActivity(new Intent(this, AddPlaceActivity.class));
                return false;
            }
            return false;
        });

        startNotificationsListener();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment).commit();
    }

    private void  startNotificationsListener() {
        String uid = FirebaseHelper.getInstance().getCurrentUser() != null
                ? FirebaseHelper.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        // Listen to notifications collection in real-time
        notificationsListener = FirebaseHelper.getInstance().listenForNotifications(uid, (snap, error) -> {
            if (error != null || snap == null) return;

            // First load → store existing notifications but don't show them
            if (!notificationsPrimed) {
                for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                    knownNotificationIds.add(doc.getId());
                }
                notificationsPrimed = true;
                return;
            }

            for (DocumentChange change : snap.getDocumentChanges()) {
                if (change.getType() != DocumentChange.Type.ADDED) continue;
                String docId = change.getDocument().getId();
                if (!knownNotificationIds.add(docId)) continue;

                AppNotification notification = change.getDocument().toObject(AppNotification.class);
                String message = notification.getMessage() != null && !notification.getMessage().trim().isEmpty()
                        ? notification.getMessage() : "New activity on your post";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (notificationsListener != null) {
            notificationsListener.remove();
            notificationsListener = null;
        }
        super.onDestroy();
    }
}

// startNotificationsListener:
// get current user ID
// listen to Firestore notifications
// first load → store old notifications (don’t show)
// after that → detect NEW ones only
// show new notifications using Toast