package com.skillor.comick;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.widget.Button;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.ActionBar;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.skillor.comick.databinding.ActivityMainBinding;
import com.skillor.comick.utils.ComickService;


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

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

        sharedPref = getPreferences(Context.MODE_PRIVATE);
        sharedPrefEditor = sharedPref.edit();

        rotationLocked = sharedPref.getBoolean(getString(R.string.lock_rotation_key), false);
        setRotationLock();

        binding.lockRotationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotationLocked = !rotationLocked;
                setRotationLock();
            }
        });

        binding.exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmExit();
            }
        });

        DrawerLayout drawer = binding.drawerLayout;
        drawer.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                showSystemUI();
                showNavbar();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_overview, R.id.nav_reader).setOpenableLayout(drawer).build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        ComickService.getInstance().initialize(getApplicationContext().getExternalFilesDir(null));

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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}