/*
 * Copyright (c) 2020 EKA2L1 Team
 *
 * This file is part of EKA2L1 project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.eka2l1;

import static com.github.eka2l1.emu.Constants.PREF_STORAGE_WARNING_SHOWN;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.github.eka2l1.applist.AppsListFragment;
import com.github.eka2l1.base.BaseActivity;
import com.github.eka2l1.emu.Emulator;
import com.github.eka2l1.settings.AppDataStore;
import com.github.eka2l1.util.FileUtils;

import java.util.Map;

public class MainActivity extends BaseActivity {

    private static final String[] STORAGE_PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private final ActivityResultLauncher<String[]> permissionsLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            this::onPermissionResult);

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Emulator.initializePath(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!FileUtils.isExternalStorageLegacy() || ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            initialize();
        } else {
            permissionsLauncher.launch(STORAGE_PERMISSIONS);
        }
    }

    private void initialize() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        if (configurationInfo.reqGlEsVersion < 0x30000) {
            showOpenGLDialog();
            return;
        }

        AppDataStore dataStore = AppDataStore.getAndroidStore();
        boolean warningShown = dataStore.getBoolean(PREF_STORAGE_WARNING_SHOWN, false);
        if (!FileUtils.isExternalStorageLegacy() && !warningShown) {
            showScopedStorageDialog();
            dataStore.putBoolean(PREF_STORAGE_WARNING_SHOWN, true);
            dataStore.save();
            return;
        }

        showAppList();
    }

    private void showAppList() {
        Emulator.initializeFolders();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        AppsListFragment appsListFragment = new AppsListFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, appsListFragment).commitNowAllowingStateLoss();
    }

    private void showOpenGLDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error)
                .setCancelable(false)
                .setMessage(R.string.opengl_required)
                .setPositiveButton(android.R.string.ok, (d, w) -> finish())
                .show();
    }

    private void showScopedStorageDialog() {
        String message = getString(R.string.scoped_storage_warning) + Emulator.getEmulatorDir();
        new AlertDialog.Builder(this)
                .setTitle(R.string.warning)
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (d, w) -> showAppList())
                .show();
    }

    private void onPermissionResult(Map<String, Boolean> status) {
        if (!status.containsValue(false)) {
            // Restart to apply theme
            recreate();
        } else {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
