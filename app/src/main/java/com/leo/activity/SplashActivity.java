package com.leo.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.widget.ImageView;
import android.widget.TextView;

import com.leo.BaseActivity;
import com.leo.MainActivity;
import com.leo.R;
import com.leo.utils.Constants;
import com.leo.utils.Permissions;
import com.leo.utils.Utility;

public class SplashActivity extends BaseActivity {

    private ImageView  iv_spalash;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme_NoActionBar);
        Utility.transparentToolbar(this);
        setContentView(R.layout.activity_splash);


        if (Utility.isMarshmallowOS()) {
            Permissions.getInstance().setActivity(this);
            CheckForPermissions(Manifest.permission.RECORD_AUDIO);
        } else {
            iniTUI();
        }


    }

    private void CheckForPermissions(final String... mPermisons) {
        Permissions.getInstance().requestPermissions(new Permissions.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permissions.ResultSet resultSet) {
                if (resultSet.isPermissionGranted(Manifest.permission.RECORD_AUDIO)) {
                    iniTUI();
                } else {
                    android.app.AlertDialog.Builder adb = new android.app.AlertDialog.Builder(SplashActivity.this);
                    adb.setTitle(Permissions.TITLE);
                    adb.setMessage(Permissions.MESSAGE);
                    adb.setCancelable(false);
                    adb.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            CheckForPermissions(mPermisons);
                        }
                    });
                    adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            onBackPressed();
                        }
                    });
                    adb.show();
                }
            }


            @Override
            public void onRationaleRequested(Permissions.IOnRationaleProvided callback, String... permissions) {
                Permissions.getInstance().showRationaleInDialog(Permissions.TITLE,
                        Permissions.MESSAGE, "Retry", callback);
            }
        }, mPermisons);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Utility.isMarshmallowOS()) {
            Permissions.getInstance().onRequestPermissionResult(requestCode, permissions, grantResults);
        }
    }


    private void iniTUI() {

        iv_spalash = (ImageView) findViewById(R.id.iv_spalash);
        Handler mSplashHandler = new Handler();
        Runnable action = new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
               /* navigateToSignIn();
                if (!Utility.isValueNullOrEmpty(Utility.getSharedPrefStringData(SplashActivity.this, Constants.TOKEN))) {
                    Intent intent = new Intent(SplashActivity.this, DashboardActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    navigateToSignIn();
                }*/
            }
        };
        mSplashHandler.postDelayed(action, Constants.SPLASH_TIME_OUT);


    }
}
