package com.leo;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.leo.LocationUtil.PermissionUtils;
import com.leo.model.ContactModel;
import com.leo.utils.Constants;
import com.leo.utils.Utility;
import com.skyfishjy.library.RippleBackground;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends BaseActivity implements TextToSpeech.OnInitListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ActivityCompat.OnRequestPermissionsResultCallback,
        PermissionUtils.PermissionResultCallback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private TextToSpeech textToSpeech;
    private TextView tv_tp_on_btn;
    private TextView tv_speech_text;
    private TextView tv_icon_leo;

    // private ArcProgress progressStorage;
    private int mProgressStatus = 0;
    int hourChanged;

    List<ResolveInfo> pkgAppsList;

    private Calendar calendar;
    static final int REQUEST_SELECT_PHONE_NUMBER = 1;
    private ArrayList<ContactModel> storeContacts;

    private String name, phonenumber;
    private com.skyfishjy.library.RippleBackground rippleBackground;

    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;

    double latitude;
    double longitude;

    private int finalHour;
    private int finalMinute;


    private String contctNameFromPhone;
    private String contactNumber;
    private boolean contactFound = false;
    // list of permissions

    ArrayList<String> permissions = new ArrayList<>();
    PermissionUtils permissionUtils;

    boolean isPermissionGranted;


    //used for register alarm manager
    PendingIntent pendingIntent;
    //used to store running alarmmanager instance
    AlarmManager alarmManager;
    //Callback function for Alarmmanager event
    BroadcastReceiver mReceiver;

    Window window;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // MAKING TOOLBAR TRANSPARENT
        Utility.transparentToolbar(this);
        setContentView(R.layout.activity_main);

        window = getWindow();

        /*INITIALIZE THE UI*/
        inItUi();
    }


    private void inItUi() {

        // Initialize a new IntentFilter instance
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

        // check availability of play services
        if (checkPlayServices()) {
            // Building the GoogleApi client
            buildGoogleApiClient();
        }


        permissionUtils = new PermissionUtils(MainActivity.this);

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        permissionUtils.check_permission(permissions, "Need GPS permission for getting your location", 1);


        textToSpeech = new TextToSpeech(this, this);

        storeContacts = new ArrayList<>();
        getContactsIntoArrayList();


        calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);


        tv_tp_on_btn = (TextView) findViewById(R.id.tv_tp_on_btn);
        tv_tp_on_btn.setTypeface(Utility.setTypeFace_Lato_Bold(getApplicationContext()));


        tv_speech_text = (TextView) findViewById(R.id.tv_speech_text);
        tv_speech_text.setTypeface(Utility.setTypeFace_Lato_Regular(getApplicationContext()));

        rippleBackground = (RippleBackground) findViewById(R.id.rippleBackground);
        tv_icon_leo = (TextView) findViewById(R.id.tv_icon_leo);
        tv_icon_leo.setTypeface(Utility.setFontAwesomeWebfont(this));

        rippleBackground.startRippleAnimation();


        tv_icon_leo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rippleBackground.stopRippleAnimation();
                askSpeechInput("Hi speak something");
                // speakOut("Hey Leo");
            }
        });

    }

    /**
     * Method to verify google play services on the device
     */

    private boolean checkPlayServices() {

        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();

        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode,
                        Constants.PLAY_SERVICES_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Creating google api client object
     */

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mGoogleApiClient.connect();

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult locationSettingsResult) {

                final Status status = locationSettingsResult.getStatus();

                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location requests here
                        getLocation();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(MainActivity
                                    .this, Constants.REQUEST_CHECK_SETTINGS);

                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });


    }


    // Showing google speech input dialog

    private void askSpeechInput(String text) {


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
                text);
        try {
            startActivityForResult(intent, Constants.REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {

        }
    }

    /**/

    public void selectContact() {
        // Start an activity for the user to pick a phone number from contacts
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_SELECT_PHONE_NUMBER);
        }
    }

    // RECEIVING SPEECH INPUT
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
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

                    if (matches.size() == 0) {
                        Toast.makeText(this, "Recognizer not present", Toast.LENGTH_SHORT).show();
                    }
                    wordStr = matches.get(0);
                    if (wordStr.contains("brightness")) {
                        try {

                            if (Utility.isMarshmallowOS()) {
                                if (Settings.System.canWrite(getApplicationContext())) {
                                    setBrightness(wordStr);
                                } else {
                                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS)
                                            .setData(Uri.parse("package:" + getPackageName()))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }

                            } else {
                                setBrightness(wordStr);

                            }


                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            Utility.showLog("brightness Exception", "" + e);

                        }


                    } else if (wordStr.trim().contains("location")) {
                        getMyLocation();

                    } else if (wordStr.contains("P.M") || wordStr.contains("A.M")
                            || wordStr.contains("a.m") || wordStr.contains("p.m")) {
                        String[] timeWord = wordStr.split(" ");

                        String time = timeWord[0];
                        String pmOrAm = timeWord[1];
                        boolean isPmORam = false;

                        if (pmOrAm.equalsIgnoreCase("P.M") || pmOrAm.equalsIgnoreCase("p.m")) {
                            isPmORam = true;

                        } else
                            isPmORam = false;

                        // String string = "004-034556";
                        String[] parts = time.split(":");
                        String hours;
                        String minutes;
                        if (parts.length > 1 && parts.length > 0) {
                            hours = parts[0]; // 004
                            minutes = parts[1]; //

                        } else if (parts.length == 1) {
                            hours = parts[0];
                            minutes = "00";

                        } else {
                            hours = "00";
                            minutes = "00";

                        }
                        if (TextUtils.isEmpty(minutes)) {
                            minutes = "00";
                        } else if (TextUtils.isEmpty(hours)) {
                            hours = "00";

                        }

                        try {
                            finalHour = Integer.valueOf(hours);
                            finalMinute = Integer.valueOf(minutes);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();

                        }

                        createAlarm("Alarm Setted From Leo App", finalHour, finalMinute, isPmORam);
                    } else if (wordStr.contains("Leo") || wordStr.contains("leo")) {
                        String findSearchQuery = wordStr.replace("Leo", "");
                        searchDynamicData(findSearchQuery);
                    } else if (wordStr.contains("contact")) {
                        selectContact();
                        /*CALL SELECT NAME OF THE PHONE*/
                    } else if (wordStr.contains("call")) {


                        Toast.makeText(this, "call", Toast.LENGTH_SHORT).show();
                        String contactName = wordStr.replace("call", "").trim();
                        Utility.showLog("Output Contact", "" + contactName);
                        Utility.showLog("Updated Contact name", "" + contactName);
                        for (int i = 0; i < storeContacts.size(); i++) {
                            contctNameFromPhone = storeContacts.get(i).getContactName();
                            if (contctNameFromPhone.trim().equalsIgnoreCase(contactName)) {
                                contactNumber = storeContacts.get(i).getContactPhoneNumber();
                                contactFound = true;
                            }
                        }
                        if (contactFound) {
                            contactFound = false;
                            speakOut("Calling " + contactName);
                            String uri = "tel:" + contactNumber.trim();
                            Intent intent = new Intent(Intent.ACTION_CALL);
                            intent.setData(Uri.parse(uri));
                            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                return;
                            }

                            startActivity(intent);

                        } else {
                            String numberNotFountText = "Since I am  having  Trouble finding " + contactName + " Go ahead pick contact on Your Contacts Screen ";
                            textToSpeech.speak(numberNotFountText, TextToSpeech.QUEUE_FLUSH, null);


                        }


                    }




                    /*OPEN APPS USING OPEN AND APP NAME */
                    if (wordStr.contains("open")) {
                        String appName = wordStr.replace("open", "");

                        Utility.showLog("appName", "" + appName);

                        if (appName.equalsIgnoreCase("my location")) {
                            /*GETTING PRESENT LOCATION*/
                            getMyLocation();
                        }
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
                                    equals(appName.trim().toLowerCase())) {
                                PackageManager pm = this.getPackageManager();
                                Intent appStartIntent = pm.getLaunchIntentForPackage(pname);
                                if (null != appStartIntent) {
                                    try {
                                        this.startActivity(appStartIntent);
                                    } catch (Exception e) {
                                    }
                                }
                            } else {
                                if (!Objects.equals(appName, ""))
                                    speakOut(appName + "Application is not Available ");
                                else
                                    speakOut("Application is not Available ");

                            }
                        }
                    } else if (wordStr.contains("alarm")) {
                        askSpeechInput("At What Time?");
                    } else {

                    }
                }
                break;
            }
            case REQUEST_SELECT_PHONE_NUMBER: {

                if (requestCode == REQUEST_SELECT_PHONE_NUMBER && resultCode == RESULT_OK) {
                    // Get the URI and query the content provider for the phone number
                    Uri contactUri = data.getData();
                    String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
                    Cursor cursor = getContentResolver().query(contactUri, projection,
                            null, null, null);
                    // If the cursor returned is valid, get the phone number
                    if (cursor != null && cursor.moveToFirst()) {
                        int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        String number = cursor.getString(numberIndex);

                        // Do something with the phone number

                        String uri = "tel:" + number.trim();
                        Intent intent = new Intent(Intent.ACTION_CALL);
                        intent.setData(Uri.parse(uri));
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        startActivity(intent);


                    }
                }
            }
            break;

            case Constants.REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        getLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        break;
                    default:
                        break;
                }
                break;
        }

    }

    private void finalBrightnessForOutput(Context context, int number) {

        try {
            android.provider.Settings.System.putInt(
                    context.getContentResolver(),
                    android.provider.Settings.System.SCREEN_BRIGHTNESS, number);


            android.provider.Settings.System.putInt(context.getContentResolver(),
                    android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

            android.provider.Settings.System.putInt(
                    context.getContentResolver(),
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    number);


        } catch (Exception e) {
            Log.e("Screen Brightness", "error changing screen brightness");
        }


      /*  Settings.System.putInt(this.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, number);

        startActivity(new Intent(this, MainActivity.class));*/
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void setBrightness(String wordStr) {

        String number = wordStr.replaceAll("[^0-9]+", "");

        Utility.showLog("Number", "Number >>>>>" + number);
        if (!Utility.isValueNullOrEmpty(number)) {
            speakOut("brightness setted at " + number + "%");

            if (Objects.equals(number, "")) {
                number = "127";
                finalBrightnessForOutput(this.getApplicationContext(), Integer.parseInt(number));

            } else {
                if (Integer.parseInt(number) <= 100) {
                    int calculateNumber = Math.round(Integer.parseInt(number) * 255) / 100;
                    finalBrightnessForOutput(this.getApplicationContext(), calculateNumber);

                    //  finalBrightnessForOutput(calculateNumber);
                   /* Settings.System.putInt(
                            getApplicationContext().getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS,
                            calculateNumber);
                    Settings.System.putInt(getApplicationContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, calculateNumber
                    );*/
                } else {
                    number = "255";
                    finalBrightnessForOutput(this.getApplicationContext(), Integer.parseInt(number));
                    //finalBrightnessForOutput(Integer.parseInt(number));
                    /*Settings.System.putInt(getApplicationContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, Integer.parseInt(number)
                    );*/
                }

            }
        } else
            speakOut("Sorry I am unable  to process your request");
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
    }

    private void searchDynamicData(String query) {

        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.setClassName("com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox.SearchActivity");
        intent.putExtra("query", query);
        startActivity(intent);


    }

    public void
    createAlarm(String message, int hour, int minutes, boolean isPm) {
        String opISPm;
        /* calendar.set(Calendar.AM_PM, hour < 12 ? Calendar.AM : Calendar.PM);

        int am_pm = calendar.get(Calendar.AM_PM);
        String time = calendar.HOUR + ((am_pm == Calendar.AM) ? "am" : "pm"))*/
        if (isPm) {
            if (hour == 12) {
                hour = 0;
            }
            hourChanged = hour + 12;

            opISPm = "P.M";

        } else {
            hourChanged = hour;
            opISPm = "A.M";
        }
        Intent alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_MESSAGE, message)
                .putExtra(AlarmClock.EXTRA_HOUR, hourChanged)
                .putExtra(AlarmClock.EXTRA_MINUTES, minutes);

        if (alarmIntent.resolveActivity(getPackageManager()) != null) {

            if (hour == 0) {
                speakOut("Alarm Setted At" + 12 + ":" + minutes + "P.M");
            } else {
                speakOut("Alarm Setted At" + hour + ":" + minutes + opISPm);
            }

            startActivity(alarmIntent);
        }
    }


    public void getContactsIntoArrayList() {

        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur != null && cur.moveToNext()) {
                String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));
                name = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));

                if (cur.getInt(cur.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        phonenumber = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                        ContactModel contactModel = new ContactModel();
                        contactModel.setContactName(name);
                        contactModel.setContactPhoneNumber(phonenumber);
                        storeContacts.add(contactModel);

                        //storeContacts.add(name + " " + ":" + " " + phonenumber);
                    }
                    pCur.close();
                }
            }
        }
        if (cur != null) {
            cur.close();
        }

    }


    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            int result = textToSpeech.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
                speakOut(Utility.getStrings(getApplicationContext(), R.string.wake_up));
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }

    }

    private void speakOut(String text) {

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    private void getMyLocation() {
        getLocation();
        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
            getAddress();

        } else {
            textToSpeech.speak("Couldn't get the location. Make sure location is enabled on the device", TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    public void getAddress() {
        Address locationAddress = getAddress(latitude, longitude);

        if (locationAddress != null) {
            String address = locationAddress.getAddressLine(0);

            String currentLocation;

            if (!TextUtils.isEmpty(address)) {
                currentLocation = address;
                tv_speech_text.setVisibility(View.VISIBLE);
                tv_speech_text.setText(currentLocation);
                textToSpeech.speak(currentLocation, TextToSpeech.QUEUE_FLUSH, null);

            }

        }

    }

    /*FOR ADDRESS*/
    public Address getAddress(double latitude, double longitude) {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            return addresses.get(0);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    private void getLocation() {
        if (isPermissionGranted) {
            try {
                mLastLocation = LocationServices.FusedLocationApi
                        .getLastLocation(mGoogleApiClient);
            } catch (SecurityException e) {
                e.printStackTrace();
            }

        }

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        // ONCE CONNECTED WITH GOOGLE API, GET THE LOCATION
        getLocation();

    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());

    }


    @Override
    public void PermissionGranted(int request_code) {
        Log.i("PERMISSION", "GRANTED");
        isPermissionGranted = true;

    }

    @Override
    public void PartialPermissionGranted(int request_code, ArrayList<String> granted_permissions) {

        Log.i("PERMISSION PARTIALLY", "GRANTED");

    }

    @Override
    public void PermissionDenied(int request_code) {
        Log.i("PERMISSION", "DENIED");

    }

    @Override
    public void NeverAskAgain(int request_code) {
        Log.i("PERMISSION", "NEVER ASK AGAIN");

    }

    private void UnregisterAlarmBroadcast() {
        alarmManager.cancel(pendingIntent);
        getBaseContext().unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        //unregisterReceiver(mReceiver);
        super.onDestroy();
    }


}
