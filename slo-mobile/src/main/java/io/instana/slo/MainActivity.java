package io.instana.slo;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import io.instana.slo.ui.settings.SettingsActivity;
import io.instana.slo.ui.slolist.SloListFragment;
import io.instana.slo.util.PreferencesManager;

/**
 * Main activity that hosts the SLO list fragment
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private PreferencesManager preferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferencesManager = new PreferencesManager(this);

        // Set up toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setElevation(4);
        }

        // Check if this is the first run or if settings are not configured
        if (preferencesManager.isFirstRun() || !preferencesManager.isConfigured()) {
            showWelcomeDialog();
        } else {
            // Load the SLO list fragment
            loadSloListFragment();
        }
    }

    /**
     * Show welcome dialog on first run
     */
    private void showWelcomeDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.welcome_title)
                .setMessage(R.string.welcome_message)
                .setPositiveButton(R.string.configure_now, (dialog, which) -> {
                    preferencesManager.setFirstRun(false);
                    openSettings();
                })
                .setNegativeButton(R.string.later, (dialog, which) -> {
                    preferencesManager.setFirstRun(false);
                    loadSloListFragment();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Load the SLO list fragment
     */
    private void loadSloListFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment == null) {
            fragment = new SloListFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_settings) {
            openSettings();
            return true;
        } else if (id == R.id.action_refresh) {
            refreshSloList();
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    /**
     * Open settings activity
     */
    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Refresh the SLO list
     */
    private void refreshSloList() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof SloListFragment) {
            ((SloListFragment) fragment).refresh();
        }
    }

    /**
     * Show about dialog
     */
    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list when returning from settings
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof SloListFragment && preferencesManager.isConfigured()) {
            ((SloListFragment) fragment).refresh();
        }
    }
}
