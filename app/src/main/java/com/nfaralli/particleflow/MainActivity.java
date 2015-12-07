package com.nfaralli.particleflow;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

/**
 * Super basic Activity:
 * Just create a GLSurfaceView and set it as the content view.
 * All the logic is in the GLSurfaceView, especially its renderer.
 */
public class MainActivity extends Activity {

    private ParticlesSurfaceView mGLView;
    private GearView mGearView;
    private SettingsView mSettingsView;
    private Dialog mSettingsDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.particles);
        mGLView = (ParticlesSurfaceView)findViewById(R.id.particles_view);
        mSettingsView = new SettingsView(this);
        mSettingsDialog = getSettingsDialog();
        mGearView = (GearView)findViewById(R.id.gear_view);
        mGearView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mGearView.isGearVisible()) {
                    mGLView.onPause();
                    mGearView.hideGear();
                    mSettingsDialog.show();
                } else {
                    mGearView.showGear();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLView.onPause();
        mGearView.hideGear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs =
                getSharedPreferences(ParticlesSurfaceView.SHARED_PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean("ShowSettingsHint", true)) {
            Toast.makeText(this, R.string.settings_hint, Toast.LENGTH_LONG).show();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("ShowSettingsHint", false);
            editor.commit();
        }
        mGLView.onResume();
    }

    Dialog getSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.settings_title)
                .setIcon(R.drawable.gear_icon_00)
                .setView(mSettingsView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mSettingsView.saveValues();
                        mGLView.onResume();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // reset the dialog with its previous values.
                        mSettingsView.loadValues();
                        mGLView.onResume();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        // reset the dialog with its previous values.
                        mSettingsView.loadValues();
                        mGLView.onResume();
                    }
                });
        return builder.create();
    }
}
