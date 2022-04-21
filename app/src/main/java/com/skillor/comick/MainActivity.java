package com.skillor.comick;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;
import com.skillor.comick.databinding.ActivityMainBinding;
import com.skillor.comick.utils.ComickService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    private static final int REQUEST_FOLDER_PERMISSIONS_CODE = 123;

    private boolean systemUIHidden = false;

    private boolean rotationLocked = false;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor sharedPrefEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);


        binding.exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmExit();
            }
        });

        DrawerLayout drawer = binding.drawerLayout;
        drawer.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                showSystemUI();
                showNavbar();
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });
        NavigationView navigationView = binding.navView;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_download,
                R.id.nav_overview,
                R.id.nav_summary,
                R.id.nav_reader,
                R.id.nav_settings).setOpenableLayout(drawer).build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefEditor = sharedPref.edit();

        ComickService.getInstance().setActivity(this);
        this.loadDirectory(true);

        setOffline();
        binding.goOfflineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ComickService.getInstance().setOffline(!ComickService.getInstance().isOffline());
                setOffline();
            }
        });

        rotationLocked = sharedPref.getBoolean(getString(R.string.lock_rotation_key), false);
        setRotationLock();

        binding.lockRotationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotationLocked = !rotationLocked;
                setRotationLock();
            }
        });

        String lastRead = sharedPref.getString(getString(R.string.last_read_key), null);
        if (lastRead != null) {
            Bundle bundle = new Bundle();
            bundle.putString("comic_title", lastRead);
            navController.popBackStack();
            navController.navigate(R.id.nav_reader, bundle);
        }

    }

    public SharedPreferences getSharedPref() {
        return sharedPref;
    }

    public SharedPreferences.Editor getSharedPrefEditor() {
        return sharedPrefEditor;
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT < 23) return true;
        if (Build.VERSION.SDK_INT >= 30
                && permission.equals(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                && !Environment.isExternalStorageManager()) {
            return false;
        }
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasPermissions(String[] permissions) {
        if (Build.VERSION.SDK_INT < 23) return true;
        for (String permission: permissions) {
            if (!hasPermission(permission)) return true;
        }
        return true;
    }

    private void requestPermissions(String[] permissions) {
        if (Build.VERSION.SDK_INT < 23) return;
        List<String> missingPermissions = new ArrayList<>();
        for (String permission: permissions) {
            if (!hasPermission(permission)) {
                missingPermissions.add(permission);
            }
        }

        if (missingPermissions.isEmpty()) return;

        if (Build.VERSION.SDK_INT >= 30 && missingPermissions.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s",getApplicationContext().getPackageName())));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        }

        requestPermissions(missingPermissions.toArray(new String[0]), REQUEST_FOLDER_PERMISSIONS_CODE);
    }

    public void loadDirectory() {
        loadDirectory(false);
    }

    public void loadDirectory(boolean firstLoad) {
        boolean useExternalFiles = sharedPref.getBoolean(getString(R.string.use_external_files_key), false);

        if (useExternalFiles) {
            String externalFilePath = sharedPref.getString(getString(R.string.external_file_path_key), null);
            if (externalFilePath == null) {
                sharedPrefEditor.putBoolean(getString(R.string.use_external_files_key), false);
                sharedPrefEditor.apply();
                return;
            }
            if (Build.VERSION.SDK_INT >= 23) {
                String[] permissions = {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                };
                if (Build.VERSION.SDK_INT >= 30) {
                    permissions = new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.MANAGE_EXTERNAL_STORAGE
                    };
                }

                requestPermissions(permissions);
            }
            File externalFile = new File(externalFilePath);
            if (!externalFile.isDirectory()) {
                sharedPrefEditor.putBoolean(getString(R.string.use_external_files_key), false);
                sharedPrefEditor.apply();
                return;
            }
            ComickService.getInstance().setDirectory(externalFile);
        } else {
            ComickService.getInstance().setDirectory(getApplicationContext().getExternalFilesDir(null));
        }
        if (firstLoad) {
            ComickService.getInstance().initialize();
        } else {
            ComickService.getInstance().initializeThreaded();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_FOLDER_PERMISSIONS_CODE:
                loadDirectory();
                return;
            default:
    }
}

    @Override
    public void onBackPressed() {
        confirmExit();
    }

    private void confirmExit() {
        AlertDialog.Builder ab = new AlertDialog.Builder(MainActivity.this);
        ab.setTitle(getString(R.string.exit_dialog_headline));
        ab.setMessage(getString(R.string.exit_dialog_sentence));
        ab.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        ab.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        ab.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.dismiss();
                    finish();
                }
                return true;
            }
        });

        ab.show();
    }

    private void setOffline() {
        if (ComickService.getInstance().isOffline()) {
            ((Button) findViewById(R.id.goOfflineButton)).setText(R.string.go_online);
        } else {
            ((Button) findViewById(R.id.goOfflineButton)).setText(R.string.go_offline);
        }
    }

    private void setRotationLock() {
        if (rotationLocked) {
            lockRotation();
        } else {
            unlockRotation();
        }
    }

    private void lockRotation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        rotationLocked = true;
        sharedPrefEditor.putBoolean(getString(R.string.lock_rotation_key), rotationLocked);
        sharedPrefEditor.apply();
        ((Button) findViewById(R.id.lockRotationButton)).setText(R.string.unlock_rotation);
    }

    private void unlockRotation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        rotationLocked = false;
        sharedPrefEditor.putBoolean(getString(R.string.lock_rotation_key), rotationLocked);
        sharedPrefEditor.apply();
        ((Button) findViewById(R.id.lockRotationButton)).setText(R.string.lock_rotation);
    }

    public void triggerUI() {
        triggerSystemUI();
        if (systemUIHidden) {
            hideNavbar();
        } else {
            showNavbar();
        }
    }

    public void hideNavbar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    public void showNavbar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
        }
    }

    public void triggerSystemUI() {
        if (systemUIHidden) {
            showSystemUI();
        } else {
            hideSystemUI();
        }
    }

    public void hideSystemUI() {
        systemUIHidden = true;
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    public void showSystemUI() {
        systemUIHidden = false;
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}