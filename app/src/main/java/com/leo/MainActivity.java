package com.leo;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
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

public class MainActivity extends BaseActivity implements TextToSpeech.OnInitListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ActivityCompat.OnRequestPermissionsResultCallback,
        PermissionUtils.PermissionResultCallback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private TextToSpeech textToSpeech;
    private TextView tv_speech_text;
    private TextView tv_icon_leo;

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
    // list of permissions

    ArrayList<String> permissions = new ArrayList<>();
    PermissionUtils permissionUtils;

    boolean isPermissionGranted;

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
        /*sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);*/


        calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);


        tv_speech_text = (TextView) findViewById(R.id.tv_speech_text);

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

    public void selectContact() {
        // Start an activity for the user to pick a phone number from contacts
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_SELECT_PHONE_NUMBER);
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

                    if (matches.size() == 0) {
                        Toast.makeText(this, "Recognizer not present", Toast.LENGTH_SHORT).show();
                    }
                    //  hello plsese set 6:30 P.M
                    Utility.showLog("matches", "matches >>>>>>>>>>" + matches);
                    wordStr = matches.get(0);

                    if (wordStr.contains("information")) {
                        //Toast.makeText(this, "information", Toast.LENGTH_SHORT).show();
                        informationMenu();
                        /*SETTING ALARM AT PARTICULAR TIME*/
                    } else if (wordStr.trim().contains("location")) {
                        getMyLocation();

                    } else if (wordStr.contains("P.M") || wordStr.contains("A.M")
                            || wordStr.contains("a.m") || wordStr.contains("p.m")) {

                        /*  words = wordStr.split(" ");



                    try {
                        firstWord = words[0];
                        secondWord = words[1];
                    } catch (Exception e) {
                        e.printStackTrace();

                    }*/

                        //6:30 PM

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
                        String hours = parts[0]; // 004
                        String minutes = parts[1]; //

                        Utility.showLog("Alarm Time", "" + hours + ":" + minutes);

                        int finalHour = Integer.valueOf(hours);
                        int finalMinute = Integer.valueOf(minutes);
                        createAlarm("Alarm Setted From Leo App", finalHour, finalMinute, isPmORam);
                    } else if (wordStr.contains("hey Leo")) {
                        String findSearchQuery = wordStr.replace("hey Leo", "");
                        searchDynamicData(findSearchQuery);
                    } else if (wordStr.contains("Google") || wordStr.equalsIgnoreCase("google")) {
                        forSearching();
                    } else if (wordStr.contains("contact")) {
                        selectContact();
                        /*CALL SELECT NAME OF THE PHONE*/
                    } else if (wordStr.contains("call")) {
                        Toast.makeText(this, "call", Toast.LENGTH_SHORT).show();
                        String contactName = wordStr.replace("call", "").trim();
                        Utility.showLog("Updated Contact name", "" + contactName);
                        for (int i = 0; i < storeContacts.size(); i++) {

                            Utility.showLog("NameAndPhone", storeContacts.get(i).getContactName() + " :" + storeContacts.get(i).getContactPhoneNumber());

                            String contctNameFromPhone = storeContacts.get(i).getContactName();

                            if (contctNameFromPhone.trim().equalsIgnoreCase(contactName)) {

                                /*SPEAKING CONTACT NAME*/
                                speakOut("Calling " + contactName);
                                String contactNumber = storeContacts.get(i).getContactPhoneNumber();

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

                                speakOut("Since I am  having  Trouble finding " + contactName + " Go ahead pick contact on Your Contacts Screen ");/* else {
                                Toast.makeText(this, "There is no Contact Found Named By ::" + contactName, Toast.LENGTH_SHORT).show();
                                // askSpeechInput("There is no Contact Found Named By ::" + contactName);
                            }*/
                            }


                        }

                    }


                    // open open music


                  /*  words = wordStr.split(" ");



                    try {
                        firstWord = words[0];
                        secondWord = words[1];
                    } catch (Exception e) {
                        e.printStackTrace();

                    }*/

                    /*OPEN APPS USING OPEN AND APP NAME */

                    // if (firstWord.equalsIgnoreCase("open")) {
                    Utility.showLog("Open text", "Open text" + wordStr
                    );


                    /*if (wordStr.contains("music") || wordStr.contains("Google Play Music")) {

                        Intent it = new Intent(Intent.ACTION_VIEW);
                        Uri uri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
                        // Uri uri = Uri.withAppendedPath(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, "1");
                        //Uri uri = Uri.parse("file:///sdcard/song.mp3");
                        it.setDataAndType(uri, "audio/mp3");
                        startActivity(it);

                       *//* Uri uri = Uri.withAppendedPath(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, "1");
                        Intent it = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(it);*//*

                    }*/
                    if (wordStr.contains("open")) {
                        String appName = wordStr.replace("open", "");

                        Utility.showLog("appName", "" + appName);

                        if (appName.equalsIgnoreCase("my location")) {


                            getMyLocation();
                        }

                       /* if (appName.trim().contains("music")) {
                            MediaPlayer mediaPlayer = new MediaPlayer();
                            mediaPlayer.start();
                        }*/
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
                            }
                        }
                    } else if (wordStr.contains("set")) {
                        askSpeechInput("At What Time?");
                        //createAlarm("Good Mornig",6,50,true);
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

    private void forSearching() {

        Intent intent = getIntent();
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_SEARCH)) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            // handleVoiceQuery(query);
        }
    }

    public void informationMenu() {

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);


        calendar.set(Calendar.HOUR_OF_DAY, 8);
        calendar.set(Calendar.MINUTE, 30);


        // startActivity(new Intent("android.content.Intent.EXTRA_TEXT"));


    }

    public void createAlarm(String message, int hour, int minutes, boolean isPm) {

        Utility.showLog("OPPM OR AM", "" + isPm);
        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_MESSAGE, message)
                .putExtra(AlarmClock.EXTRA_HOUR, hour)
                .putExtra(AlarmClock.EXTRA_IS_PM, isPm)
                .putExtra(AlarmClock.EXTRA_MINUTES, minutes);

        if (intent.resolveActivity(getPackageManager()) != null) {
            String opISPm;

            if (isPm) {
                opISPm = "P.M";
            } else {
                opISPm = "A.M";

            }
            speakOut("Alarm Setted At" + hour + ":" + minutes + opISPm);
            startActivity(intent);
        }
    }

    public void getAlarm() {


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
                speakOut("Hey Leo");
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





      /*  Location location = appLocationService
                .getLocation(LocationManager.GPS_PROVIDER);*/

        //you can hard-code the lat & long if you have issues with getting it
        //remove the below if-condition and use the following couple of lines
        //double latitude = 37.422005;
        //double longitude = -122.084095

       /* if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            LocationAddress locationAddress = new LocationAddress();
            locationAddress.getAddressFromLocation(latitude, longitude,
                    getApplicationContext(), new GeocoderHandler());
        } else {
            showSettingsAlert();
        }*/
    }

    public void getAddress() {

        Address locationAddress = getAddress(latitude, longitude);

        if (locationAddress != null) {
            String address = locationAddress.getAddressLine(0);
            String address1 = locationAddress.getAddressLine(1);
            String city = locationAddress.getLocality();
            String state = locationAddress.getAdminArea();
            String country = locationAddress.getCountryName();
            String postalCode = locationAddress.getPostalCode();

            String currentLocation;

            if (!TextUtils.isEmpty(address)) {
                currentLocation = address;

                if (!TextUtils.isEmpty(address1))
                    currentLocation += "\n" + address1;

                if (!TextUtils.isEmpty(city)) {
                    currentLocation += "\n" + city;

                    if (!TextUtils.isEmpty(postalCode))
                        currentLocation += " - " + postalCode;
                } else {
                    if (!TextUtils.isEmpty(postalCode))
                        currentLocation += "\n" + postalCode;
                }

                if (!TextUtils.isEmpty(state))
                    currentLocation += "\n" + state;

                if (!TextUtils.isEmpty(country))
                    currentLocation += "\n" + country;


                tv_speech_text.setText(currentLocation);
                tv_speech_text.setVisibility(View.VISIBLE);

                textToSpeech.speak(currentLocation, TextToSpeech.QUEUE_FLUSH, null);

            }

        }

    }

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

        // Once connected with google api, get the location
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
}
