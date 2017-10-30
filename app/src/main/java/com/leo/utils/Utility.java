package com.leo.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.leo.BaseActivity;
import com.leo.R;

/**
 * Created by madhu on 01-Oct-17.
 */

public class Utility {

    /*CHECKING OS IS  MARSHMALLOW */
    public static boolean isMarshmallowOS() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }


    /*Get Font Awesome Web font Type face*/
    public static Typeface setFontAwesomeWebfont(Context context) {
        return Typeface.createFromAsset(context.getAssets(), "fonts/fontawesome-webfont.ttf");
    }


    /**
     * ASSIGN THE COLOR
     **/
    public static int getColor(Context context, int id) {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 23)
            return ContextCompat.getColor(context, id);
        else
            return context.getResources().getColor(id);
    }

    /**
     * HIDE THE KEYBOARD FOR FRAGMENT
     **/
    public static void hideSoftKeyboard(Activity activity, View view) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
    }

    /**
     * GET Resources String
     *
     * @param context Context of the class
     * @param id      Id of the resource
     * @return String
     */
    public static String getResourcesString(Context context, int id) {
        String value = null;
        if (context != null && id != -1) {
            value = context.getResources().getString(id);
        }
        return value;
    }

    /**
     * Check the value is null or empty
     *
     * @param value Value of that string
     * @return Boolean returns the value true or false
     */
    public static boolean isValueNullOrEmpty(String value) {
        boolean isValue = false;
        if (value == null || value.equals("") || value.equals("0.0")
                || value.equals("null") || value.trim().length() == 0) {
            isValue = true;
        }
        return isValue;
    }

    /**
     * Shows toast message
     *
     * @param context Context of the class
     * @param message What message you have to show
     */
    public static void showToastMessage(Context context, String message) {
        try {
            if (!isValueNullOrEmpty(message) && context != null) {
                final Toast toast = Toast.makeText(
                        context.getApplicationContext(), message,
                        Toast.LENGTH_SHORT);
                toast.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * To Print logcat this method is used.
     *
     * @param logMsg Purpose of the log
     * @param logVal What you want to print
     */
    public static void showLog(String logMsg, String logVal) {
        try {
            if (Constants.logMessageOnOrOff) {
                if (!isValueNullOrEmpty(logMsg) && !isValueNullOrEmpty(logVal)) {
                    Log.e(logMsg, logVal);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ASSIGN THE DRAWBLE
     **/
    public static Drawable getDrawable(Context context, int id) {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 21) {
            return ContextCompat.getDrawable(context, id);
        } else {
            return context.getResources().getDrawable(id);
        }
    }

    /**
     * Shared preference method to set and get boolean variable
     */
    public static void setSharedPrefBooleanData(Context context, String key, boolean value) {
        SharedPreferences appInstallInfoSharedPref = context.getSharedPreferences(Constants.APP_PREF,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor appInstallInfoEditor = appInstallInfoSharedPref.edit();
        appInstallInfoEditor.putBoolean(key, value);
        appInstallInfoEditor.apply();
    }

    public static boolean getSharedPrefBooleanData(Context context, String key) {
        SharedPreferences userAccountPreference = context.getSharedPreferences(Constants.APP_PREF, Context.MODE_PRIVATE);
        return userAccountPreference.getBoolean(key, false);
    }


    /*NETWORK CHECKING  DIALOG*/

    public static boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager connMgr = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                    .getState() == NetworkInfo.State.CONNECTED
                    || connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                    .getState() == NetworkInfo.State.CONNECTING) {
                return true;
            } else return connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                    .getState() == NetworkInfo.State.CONNECTED
                    || connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                    .getState() == NetworkInfo.State.CONNECTING;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public static void transparentToolbar(BaseActivity baseActivity) {
        if (Build.VERSION.SDK_INT >= 19 && Build.VERSION.SDK_INT < 21) {
            setWindowFlag(baseActivity, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, true);
        }
        if (Build.VERSION.SDK_INT >= 19) {
            baseActivity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            setWindowFlag(baseActivity, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
            baseActivity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private static void setWindowFlag(Activity activity, final int bits, boolean on) {
        Window win = activity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

  /*  public static void setSnackBar(AppCompatActivity parent, View mView, String message) {
        *//*Typeface mProximaNovaRegular = Utility.getProximaNovaRegular(parent);*//*
        SnackBar snackBarIconTitle = new SnackBar();
        snackBarIconTitle.view(mView)
                .text(message, "OK", 2)
                .textColors(Color.WHITE, Color.BLACK)
                *//*.customTitleFont(mProximaNovaRegular)
                .customActionFont(mProximaNovaRegular)*//*
                .backgroundColor(Utility.getColor(parent, R.color.colorPrimary))
                .duration(SnackBar.SnackBarDuration.SHORT)
                .show();
    }*/

    /**
     * ASSIGN THE STRINGS
     **/
    public static String getStrings(Context context, int id) {
        String value = null;
        if (context != null && id != -1) {
            value = context.getResources().getString(id);
        }
        return value;
    }

    @SuppressLint("SetTextI18n")
    public static Dialog showNetworkConnectDialog(final BaseActivity context) {


        final Dialog mDialog = new Dialog(context);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setContentView(R.layout.network_check);
        mDialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setCancelable(true);

        if (context.alertDialog != null) {
            context.alertDialog.dismiss();
            context.alertDialog = null;
        }
        context.alertDialog = mDialog;

        TextView tv_alert_dialog_title = (TextView) mDialog.findViewById(R.id.tv_alert_dialog_title);


        tv_alert_dialog_title.setText(getStrings(context, R.string.app_name));

        TextView tv_dialog_content = (TextView) mDialog.findViewById(R.id.tv_dialog_content);

        tv_dialog_content.setText(getStrings(context, R.string.no_internet_msg_main));


        Button btn_dialog_cancel = (Button) mDialog.findViewById(R.id.btn_dialog_cancel);
        btn_dialog_cancel.setText("OK");
        btn_dialog_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });


        Button btn_dialog_ok = (Button) mDialog.findViewById(R.id.btn_dialog_ok);
        btn_dialog_ok.setVisibility(View.GONE);


        mDialog.show();
        return mDialog;
    }


}
