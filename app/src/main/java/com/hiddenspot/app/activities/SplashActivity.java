package com.hiddenspot.app.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.hiddenspot.app.R;
import com.hiddenspot.app.utils.FirebaseHelper;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Get references
        ImageView ivLogo   = findViewById(R.id.iv_logo);
        TextView tvName    = findViewById(R.id.tv_app_name);
        TextView tvTagline = findViewById(R.id.tv_tagline);

        // Alpha: fade, scale: zoom effect
        ObjectAnimator logoAlpha  = ObjectAnimator.ofFloat(ivLogo,   View.ALPHA,         0f, 1f);
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(ivLogo,   View.SCALE_X,    0.5f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(ivLogo,   View.SCALE_Y,    0.5f, 1f);

        // Play all animations together
        AnimatorSet logoSet = new AnimatorSet();
        logoSet.playTogether(logoAlpha, logoScaleX, logoScaleY);
        logoSet.setDuration(600); // lasts for 0.6 seconds
        logoSet.start();

        // APP NAME ANIMATION
        // Start slightly below (translationY = 20f)
        tvName.setTranslationY(20f);
        AnimatorSet nameSet = new AnimatorSet();
        nameSet.playTogether(
                ObjectAnimator.ofFloat(tvName, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(tvName, View.TRANSLATION_Y, 20f, 0f));
        nameSet.setDuration(500);
        nameSet.setStartDelay(300);
        nameSet.start();

        // Similar to name but smaller movement and later start
        tvTagline.setTranslationY(10f);
        AnimatorSet tagSet = new AnimatorSet();
        tagSet.playTogether(
                ObjectAnimator.ofFloat(tvTagline, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(tvTagline, View.TRANSLATION_Y, 10f, 0f));
        tagSet.setDuration(500);
        tagSet.setStartDelay(600);
        tagSet.start();

        // Wait 2.2 seconds before moving to next screen
        new Handler().postDelayed(() -> {
            // Check if user is already logged in via Firebase
            FirebaseUser user = FirebaseHelper.getInstance().getCurrentUser();

            // Decide which activity to open: if user is authenticated open Main activity; if not open the login activity
            Intent intent = user != null
                    ? new Intent(this, MainActivity.class)
                    : new Intent(this, AuthActivity.class);
            startActivity(intent);

            // Apply fade transition between activities
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2200);
    }
}
