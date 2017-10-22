package com.leo;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.TextView;

import com.leo.utils.Constants;
import com.leo.utils.Utility;
import com.skyfishjy.library.RippleBackground;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity {


    private TextView tv_speech_text;
    private TextView tv_icon_leo;

    List<ResolveInfo> pkgAppsList;

    private com.skyfishjy.library.RippleBackground rippleBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // MAKING TOOLBAR TRANSPARENT
        Utility.transparentToolbar(this);
        setContentView(R.layout.activity_main);

        /*INITIALIZE THE UI*/
        inItUi();
    }

    private void inItUi() {
        tv_speech_text = (TextView) findViewById(R.id.tv_speech_text);

        rippleBackground = (RippleBackground) findViewById(R.id.rippleBackground);
        tv_icon_leo = (TextView) findViewById(R.id.tv_icon_leo);
        tv_icon_leo.setTypeface(Utility.setFontAwesomeWebfont(this));

        rippleBackground.startRippleAnimation();


        tv_icon_leo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rippleBackground.stopRippleAnimation();
                askSpeechInput();
            }
        });
    }


    // Showing google speech input dialog

    private void askSpeechInput() {


        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        /*GETTING PACKAE LIST*/
        pkgAppsList = getApplicationContext().getPackageManager().queryIntentActivities(mainIntent, 0);


        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // intent.putExtra(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE,true);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Hi speak something");
        try {
            startActivityForResult(intent, Constants.REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {

        }
    }

    // Receiving speech input

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String wordStr;
        String[] words;
        String firstWord = null;
        String secondWord = null;

        switch (requestCode) {
            case Constants.REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> matches = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    tv_speech_text.setText(matches.get(0));
                    wordStr = matches.get(0);
                    words = wordStr.split(" ");

                    try {
                        firstWord = words[0];
                        secondWord = words[1];
                    } catch (Exception e) {
                        e.printStackTrace();

                    }

                    /*OPEN APPS USING OPEN AND APP NAME */

                    if (firstWord.equalsIgnoreCase("open")) {
                        PackageManager packageManager = getPackageManager();
                        List<PackageInfo> packs = packageManager
                                .getInstalledPackages(0);
                        int size = packs.size();
                        boolean uninstallApp = false;
                        boolean exceptFlg = false;
                        for (int v = 0; v < size; v++) {
                            PackageInfo p = packs.get(v);
                            String tmpAppName = p.applicationInfo.loadLabel(
                                    packageManager).toString();
                            String pname = p.packageName;
                            tmpAppName = tmpAppName.toLowerCase();
                            if (tmpAppName.trim().toLowerCase().
                                    equals(secondWord.trim().toLowerCase())) {
                                PackageManager pm = this.getPackageManager();
                                Intent appStartIntent = pm.getLaunchIntentForPackage(pname);
                                if (null != appStartIntent) {
                                    try {
                                        this.startActivity(appStartIntent);
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            }

        }
    }


}
