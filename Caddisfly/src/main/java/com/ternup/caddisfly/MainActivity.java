/*
    This file is part of Caddisfly

    Caddisfly is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Caddisfly is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Caddisfly.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.ternup.caddisfly;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;


import android.support.v7.app.ActionBarActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.loopj.android.http.*;

public class MainActivity extends ActionBarActivity implements LocationListener {

    public static final String PREFS_NAME = "Caddisfly_Preferences";
    private static final String TAG = "Caddisfly";
    private static final boolean D = true;

    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_LOCATION = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int REQUEST_SETTINGS = 4;
    private static final int REQUEST_INTERNET = 5;
    private static final int REQUEST_PHOTO = 6;

    private static final String JPEG_FILE_PREFIX = "Caddisfly";
    private static final String JPEG_FILE_SUFFIX = "cad";

    // Layout Views
    private Button mSendButton;
    private Button startButton;

    //private Button resetButton;
    private TextView testType;
    private TextView testResult;
    private TextView testDate;
    private TextView testDetails;
    private LinearLayout startHelp;
    private LinearLayout resumeHelp;

    private LinearLayout container1;
    private LinearLayout container2;
    private LinearLayout detailsLayout;

    private LinearLayout topContainer;
    private TextView progressText;
    private LinearLayout progressLayout;

    private String gpsPlace = "";
    private String resultValue = "";
    private String resultDeviceId = "";
    private String resultTestId = "";
    private String resultTestType = "";
    private String resultPlace = "";
    private String resultDate = "";
    private String resultDisplayDate = "";
    private String resultPhotoPath = "";

    private Boolean isCameraAvailable = false;

    private int resultSource = 0;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // String buffer for outgoing messages
    //private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    private ProgressDialog progressDialog = null;

    private LocationManager locationManager;
    private static final long MIN_TIME = 1 * 60 * 1000; //1 minute

    private Location gpsLocation;
    private Boolean locationTimeout = false;
    private String locationProvider = null;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
            case R.id.menu_settings1:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivityForResult(settingsIntent, REQUEST_SETTINGS);
                break;
            case android.R.id.home:
                break;
            case R.id.menu_viewmap:
                gotoMap();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        setTitle(getString(R.string.app_name).toUpperCase());

        // find controls
        this.topContainer = (LinearLayout) findViewById(R.id.topcontainer);
        this.startButton = (Button) findViewById(R.id.startButton);
        this.startHelp = (LinearLayout) findViewById(R.id.startHelp);
        this.resumeHelp = (LinearLayout) findViewById(R.id.resumeHelp);

        progressLayout = (LinearLayout) findViewById(R.id.progress);
        progressText = (TextView) findViewById(R.id.progress_text);

        testDate = (TextView) findViewById(R.id.testDate);
        container1 = (LinearLayout) findViewById(R.id.container1);
        container1.setVisibility(View.GONE);
        detailsLayout = (LinearLayout) findViewById(R.id.details);
        detailsLayout.setVisibility(View.GONE);
        container2 = (LinearLayout) findViewById(R.id.container2);
        container2.setVisibility(View.GONE);
        testDate.setVisibility(View.GONE);

        mSendButton = (Button) findViewById(R.id.button_send);

        testDetails = (TextView) findViewById(R.id.testDetails);
        testType = (TextView) findViewById(R.id.testType);
        testResult = (TextView) findViewById(R.id.result);

        Context context = this;

        PackageManager packageManager = context.getPackageManager();
        isCameraAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA);

        getStoredResult();

        setLayout();

        gotoStart();

        if (startButton.getVisibility() == View.VISIBLE) {
            resultValue = "";
        }

        getPreferenceSettings(false);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startButton.setVisibility(View.GONE);
                //resetButton.setVisibility(View.GONE);
                startHelp.setVisibility(View.GONE);
                resumeHelp.setVisibility(View.GONE);

                progressText.setText("Connecting...");
                progressLayout.setVisibility(View.VISIBLE);

                if (resultValue.isEmpty()) {
                    resultPlace = "";
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            //progressDialog = ProgressDialog.show(MainActivity.this, "", "Connecting to Caddisfly ...");

                            if (mBluetoothAdapter == null) {
                                // Get local Bluetooth adapter
                                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                                // Register for broadcasts when a device is discovered
                                registerReceiver(new IntentFilter(BluetoothDevice.ACTION_FOUND));

                                // Register for broadcasts when discovery has finished
                                registerReceiver(new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

                                // If the adapter is null, then Bluetooth is not supported
                                if (mBluetoothAdapter == null) {
                                    Toast toast = Toast.makeText(MainActivity.this, "Bluetooth is not available", Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 40);
                                    toast.show();
                                    showManualEntryForm();
                                    return;
                                }
                            }

                            startConnecting();
                        }
                    }, 20);

                } else {
                    displayResults();
                }
            }
        });
    }

    private void getPreferenceSettings(Boolean allowReset) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String value = prefs.getString("pref_location_provider", null);
        int provider = value == null ? 1 : Integer.valueOf(value);
        locationTimeout = false;
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        isLocationListenerEnabled = false;

        switch (provider) {
            case 1:
                if (locationProvider == null || !locationProvider.equals(LocationManager.GPS_PROVIDER)) {
                    if (allowReset) {
                        resetCache(true);
                    }
                    locationProvider = LocationManager.GPS_PROVIDER;
                }
                break;
            case 2:
                if (locationProvider == null || !locationProvider.equals(LocationManager.NETWORK_PROVIDER)) {
                    if (allowReset) {
                        resetCache(true);
                    }
                    locationProvider = LocationManager.NETWORK_PROVIDER;
                }
                break;
            case 3:
                if (allowReset) {
                    resetCache(true);
                }
                locationProvider = null;
                locationTimeout = true;
                break;
        }
    }

    private void getStoredResult() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
        resultDate = preferences.getString("date", "");
        resultDisplayDate = preferences.getString("displayDate", "");

        resultValue = preferences.getString("result", "");
        resultTestType = preferences.getString("testType", "");
        resultDeviceId = preferences.getString("deviceId", "");
        resultTestId = preferences.getString("testId", "");
        resultPlace = preferences.getString("place", "");
        resultSource = preferences.getInt("sourceType", 0);
        gpsPlace = preferences.getString("gpsPlace", "");

        MainActivity.this.gpsLocation = new Location("gps");
        gpsLocation.setLatitude(preferences.getFloat("lat", 999));
        gpsLocation.setLongitude(preferences.getFloat("lon", 999));
        gpsLocation.setAccuracy(preferences.getFloat("accuracy", 999));
        if (gpsLocation.getLatitude() == 999 || gpsLocation.getLongitude() == 999) {
            gpsLocation = null;
        }

        resultPhotoPath = preferences.getString("photoPath", "");

    }

    private Boolean isValidAddress(String address) {
        if (address.isEmpty() || !address.contains(":")) {
            return false;
        }
        return true;
    }

    private String getDeviceName(String deviceName) {
        if (deviceName.equalsIgnoreCase("HC-05")) {
            deviceName = "Caddisfly";
        }
        return deviceName;
    }

    private void startConnectDevice(String address) {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        setupChat();
        //showEditForm();
        connectDevice(address, true);
    }

    private void showEditForm() {

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.activity_send);
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.setTitle("Enter Source Details");
        dialog.setCancelable(true);

        final Spinner sourceInput = (Spinner) dialog.findViewById(R.id.source_types);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.source_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        sourceInput.setAdapter(
                new NothingSelectedSpinnerAdapter(
                        adapter,
                        R.layout.spinner_unselected,
                        // R.layout.contact_spinner_nothing_selected_dropdown, // Optional
                        this));

        final EditText placeInput = (EditText) dialog.findViewById(R.id.place);

        //set up button
        Button button = (Button) dialog.findViewById(R.id.okButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultSource = sourceInput.getSelectedItemPosition();
                resultPlace = placeInput.getText().toString().trim();

                if (resultPlace.isEmpty()) {
                    placeInput.setError("Please enter a Place name");
                    placeInput.post(new Runnable() {
                        public void run() {
                            placeInput.requestFocusFromTouch();
                            InputMethodManager lManager = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                            lManager.showSoftInput(placeInput, 0);
                        }
                    });
                } else if (resultSource == 0) {
                    Toast toast = Toast.makeText(MainActivity.this, "Please select a Source type", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 30);
                    toast.show();
                } else {
                    if (locationProvider == null) {
                        useCustomCoordinates();
                    }
                    displayResults();
                    dialog.dismiss();
                }

            }
        });

        Button cancelButton = (Button) dialog.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayResults();
                dialog.dismiss();
            }
        });


        placeInput.setText(resultPlace);
        if (resultSource > 0) {
            sourceInput.setSelection(resultSource);
        }

        dialog.show();

        placeInput.post(new Runnable() {
            public void run() {
                placeInput.requestFocusFromTouch();
                InputMethodManager lManager = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                lManager.showSoftInput(placeInput, 0);
            }
        });
    }


    private boolean isValidData(int testType, float result) {
        if (testType == 1) {
            if (result < 0 || result > 10) {
                return false;
            }
        } else if (testType == 2) {
            if (result < 0 || result > 3000) {
                return false;
            }
        } else if (testType == 3) {
            if (result < 0 || result > 3000) {
                return false;
            }
        } else if (testType == 4) {
            if (result < 0 || result > 100) {
                return false;
            }
        } else if (testType == 5) {
            if (result < 0 || result > 3000) {
                return false;
            }
        }
        return true;
    }

    final Context context = this;

    private void showManualEntryForm() {
        disconnectBluetooth();
        progressLayout.setVisibility(View.GONE);

        final Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.activity_edit);
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.setTitle("Enter Test Results");
        dialog.setCancelable(true);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                gotoStart();
            }
        });
        final Spinner testTypeInput = (Spinner) dialog.findViewById(R.id.test_types);
        final EditText resultInput = (EditText) dialog.findViewById(R.id.test_result);

        //set up button
        Button button = (Button) dialog.findViewById(R.id.okButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int testType = testTypeInput.getSelectedItemPosition() + 1;
                resultTestType = String.valueOf(testType);
                resultValue = resultInput.getText().toString().trim();

                if (resultValue.isEmpty() || resultValue.equals(".")) {
                    resultInput.setError("Please enter a Result value");
                    resultInput.post(new Runnable() {
                        public void run() {
                            resultInput.requestFocusFromTouch();
                            InputMethodManager lManager = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                            lManager.showSoftInput(resultInput, 0);
                        }
                    });
                }
                if (!isValidData(testType, Float.valueOf(resultValue))) {
                    resultInput.setError("Please enter a valid result");
                    resultInput.post(new Runnable() {
                        public void run() {
                            resultInput.requestFocusFromTouch();
                            InputMethodManager lManager = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                            lManager.showSoftInput(resultInput, 0);
                        }
                    });
                } else {
                    if (resultValue.startsWith(".")) {
                        resultValue = "0" + resultValue;
                    }
                    if (resultValue.endsWith(".")) {
                        resultValue += "0";
                    }

                    SimpleDateFormat ts = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                    resultDate = ts.format(new Date());

                    SimpleDateFormat ts1 = new SimpleDateFormat("dd-MMM-yyyy, hh:mm aa");
                    resultDisplayDate = ts1.format(new Date());
                    resultTestId = java.util.UUID.randomUUID().toString();

                    dialog.dismiss();
                    if (locationProvider == null) {
                        useCustomCoordinates();
                    }

                    showEditForm();
                }
            }
        });

        Button cancelButton = (Button) dialog.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoStart();
                dialog.dismiss();
            }
        });

        dialog.show();

        resultInput.post(new Runnable() {
            public void run() {
                resultInput.requestFocusFromTouch();
                InputMethodManager lManager = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                lManager.showSoftInput(resultInput, 0);
            }
        });
    }

    BroadcastReceiver mReceiver;

    private void registerReceiver(IntentFilter filter) {
        // The BroadcastReceiver that listens for discovered devices and
        // changes the title when discovery is finished
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                    if (D) Log.d(TAG, "found device");
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // If it's already paired, skip it, because it's been listed already
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        String deviceName = getDeviceName(device.getName());
                        if (deviceName.equalsIgnoreCase("caddisfly")) {
                            deviceFound = true;
                            startConnectDevice(device.getAddress());
                        }
                    }
                    // When discovery is finished
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    if (!deviceFound) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage("Could not connect\r\n\r\nEnsure Caddisfly Tester is ready to send result.")
                                .setCancelable(false)
                                .setPositiveButton("Edit", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        showManualEntryForm();
                                    }
                                })
                                .setNegativeButton("Retry", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        startConnecting();
                                    }
                                });
                        AlertDialog alert = builder.create();
                        alert.show();
                        if (progressDialog != null) {
                            progressDialog.dismiss();
                        }
                    }

                }
            }
        };

        MainActivity.this.registerReceiver(mReceiver, filter);
    }

    boolean deviceFound = false;

    private void doDiscovery() {
        if (D) Log.d(TAG, "doDiscovery()");

        // If we're already discovering, stop it
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        deviceFound = false;
        mBluetoothAdapter.startDiscovery();
    }

    private void startConnecting() {

        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                return;
            } else {
                if (mChatService == null) setupChat();
            }
            deviceFound = false;
            // Get a set of currently paired devices
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

            // If there are paired devices, add each one to the ArrayAdapter
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    String deviceName = getDeviceName(device.getName());
                    if (deviceName.equalsIgnoreCase("caddisfly")) {
                        startConnectDevice(device.getAddress());
                        deviceFound = true;
                        break;
                    }
                }
            }
            if (!deviceFound) {
                doDiscovery();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (D) Log.i(TAG, "++ ON START ++");
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, 0, this);
        if (gpsAlert != null && gpsAlert.isShowing()) {
            gpsAlert.dismiss();
        }
        displayResults();
        if (D) Log.i(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null && mBluetoothAdapter != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services

                if (D) Log.i(TAG, "Start chat service");
                mChatService.start();
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_LOCATION:
                if (resultCode == 0) {
                    String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
                    if (provider != null) {
                        Log.v(TAG, " Location providers: " + provider);
                        setupLocationListener();
                    } else {
                        finish();
                    }
                }
                break;
            case REQUEST_SETTINGS:
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                getPreferenceSettings(true);
                if (locationProvider == null) {
                    useCustomCoordinates();
                    if (gpsLocation == null) {
                        Toast.makeText(MainActivity.this, "Please enter custom coordinates.", Toast.LENGTH_LONG).show();
                        Intent settingsIntent = new Intent(this, SettingsActivity.class);
                        startActivityForResult(settingsIntent, REQUEST_SETTINGS);
                    }
                }
                break;
            case REQUEST_ENABLE_BT:
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    startConnecting();
                } else {
                    // User did not enable Bluetooth or an error occurred
//                    Toast toast = Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_LONG);
//                    toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
//                    toast.show();
                    //mainContainer.setVisibility(View.VISIBLE);
                    showManualEntryForm();
                }
                break;
            case REQUEST_PHOTO:
                SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
                resultPhotoPath = preferences.getString("photoPath", "");
                break;
        }
    }

    AlertDialog internetAlert = null;

    private void showInternetAlert() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        if (internetAlert != null && internetAlert.isShowing()) {
            return;
        }
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("An internet connection is required.\n\nPlease check settings...")
                .setCancelable(true)
                .setPositiveButton("Goto Settings",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //dialog.dismiss();
                                Intent intent = new Intent(
                                        Settings.ACTION_WIRELESS_SETTINGS);
                                startActivityForResult(intent, REQUEST_INTERNET);

                            }
                        })
                .setNegativeButton("Cancel", null);
        internetAlert = alertDialogBuilder.create();
        internetAlert.show();
    }

    AlertDialog gpsAlert = null;

    private void showGPSDisabledAlertToUser() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        if (gpsAlert != null && gpsAlert.isShowing()) {
            return;
        }
        String message = "GPS is disabled.";

        if (locationProvider == LocationManager.NETWORK_PROVIDER) {
            message = "Location service is disabled.";
        }

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(message + "\n\nPlease enable in settings to continue...")
                .setCancelable(true)
                .setPositiveButton("Goto Settings",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //dialog.dismiss();
                                Intent intent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivityForResult(intent, REQUEST_LOCATION);

                            }
                        })
                .setNegativeButton("Cancel", null);
        gpsAlert = alertDialogBuilder.create();
        gpsAlert.show();

    }

    private Boolean isLocationServiceEnabled() {
        return locationManager.isProviderEnabled(locationProvider);
    }

    public void onPhoto(View view) {
        Intent serverIntent = new Intent(this, PhotoIntentActivity.class);
        startActivityForResult(serverIntent, REQUEST_PHOTO);
    }

    public void onReset(View view) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setMessage("Delete this test result?")
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        resetCache(false);
                        gotoStart();
                    }
                })
                .setNegativeButton("No", null);

        AlertDialog alertBox = alertBuilder.create();
        alertBox.show();
    }

    public void onSend(View view) {
        sendResultToMaps();
    }

    public void onEdit(View view) {
        showEditForm();
    }

    public static String ellipsise(String input, int maxLen) {
        if (input == null)
            return null;
        if ((input.length() < maxLen) || (maxLen < 3))
            return input;
        return input.substring(0, maxLen - 3) + "...";
    }

    public void showInfo() {

        String sourceType = "";

        if (resultSource > 0) {
            String[] sourceArray = getResources().getStringArray(R.array.source_types);
            sourceType = sourceArray[resultSource - 1];
        }

        String location = "";
        if (gpsLocation != null) {
            location = "lat: " + String.valueOf(gpsLocation.getLatitude()) + "\n" +
                    "lon: " + String.valueOf(gpsLocation.getLongitude()) + "\n" +
                    "accuracy: " + String.valueOf(gpsLocation.getAccuracy()) + "mts" + "\n";

        }

        showAlert("Information",
                "date: " + resultDisplayDate + "\n" +
                        "type: " + getTestType(resultTestType) + "\n" +
                        "result: " + resultValue + "\n" +
                        "source: " + ellipsise(sourceType, 20) + "\n" +
                        "place: " + resultPlace + "\n" +
                        "gps place: " + gpsPlace + "\n" +
                        location +
                        "device id: " + resultDeviceId
                , null);

    }

    private final String getValue(String text, String key) {
        String[] keyValues = text.split(",");
        for (String keyValue : keyValues) {
            String[] kv = keyValue.split(":");
            String k = kv[0];
            String value = kv[1];
            if (k.equalsIgnoreCase(key)) {
                return value;
            }
        }
        return "";
    }

    private String getTestType(String type) {
        if (type.equals("1")) {
            return "FLUORIDE";
        } else if (type.equals("2")) {
            return "NITRATE";
        } else if (type.equals("3")) {
            return "TURBIDITY";
        } else if (type.equals("4")) {
            return "ARSENIC";
        } else if (type.equals("5")) {
            return "E.COLI";
        } else {
            return "";
        }
    }

    public void displayResults() {
        if (resultValue.isEmpty()) {
            return;
        }
        container1.setVisibility(View.VISIBLE);
        detailsLayout.setVisibility(View.VISIBLE);
        container2.setVisibility(View.VISIBLE);
        testDate.setVisibility(View.VISIBLE);

        //topContainer.setBackgroundColor(Color.BLACK);

        if (gpsLocation == null) {
            if (!isLocationListenerEnabled) {
                if (progressDialog == null || !progressDialog.isShowing()) {
                    progressDialog = new ProgressDialog(this);
                    progressDialog.setMessage("Result Received\n\nWaiting for location...");
                    progressDialog.setCancelable(false);
                    progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (locationManager != null) {
                                locationManager.removeUpdates(MainActivity.this);
                            }
                        }
                    });
                    progressDialog.show();

                }
                setupLocationListener();
            }
        } else {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
        }

        //testResult.setText(resultValue);
        String displayResult = resultValue + "ppm";
        if (resultTestType.equals("3")) {
            displayResult = resultValue + "ntu";
        }
        Spannable span = new SpannableString(displayResult);
        int start = resultValue.length();
        int end = start + 3;
        span.setSpan(new RelativeSizeSpan(0.2f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new ForegroundColorSpan(Color.GRAY), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        testResult.setText(span);


        //testResult.setText(Html.fromHtml("<small>" + resultValue + "</small><small><font size=\"1dp\">ppm</font></small>"));
        testDate.setText(resultDisplayDate);

        String details = "";

        if (resultSource > 0) {
            String[] sourceArray = getResources().getStringArray(R.array.source_types);
            details = sourceArray[resultSource - 1];
        }

        if (!resultPlace.isEmpty()) {
            if (!details.isEmpty()) {
                details += ", ";
            }

            details += resultPlace;
        }

        if (!gpsPlace.isEmpty()) {
            if (!details.isEmpty()) {
                details += ", ";
            }

            details += "\n" + gpsPlace;
        }

        testDetails.setText(details);


        testType.setText(getTestType(resultTestType));

        progressLayout.setVisibility(View.GONE);
    }


    String mNewBuf = "";
    String resultMsg = "";


    // The Handler that gets information back from the BluetoothService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            port = -1;
                            //setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            progressText.setText("Connected.\nWaiting for Result");
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            //setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            //setStatus(R.string.title_not_connected);
                            break;
                        case BluetoothChatService.STATE_CONNECT_FAILED:
                            deviceFound = false;
                            startConnecting();
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case MESSAGE_READ:
                    if (D) Log.i(TAG, "bt read start");
                    if (resultMsg.isEmpty()) {
                        byte[] readBuf = (byte[]) msg.obj;
                        // construct a string from the valid bytes in the buffer
                        String readMessage = new String(readBuf, 0, msg.arg1);

                        Pattern p = Pattern.compile("<[^>^<]*?>");
                        Matcher m = p.matcher(readMessage.trim());

                        if (m.find()) {
                            mNewBuf = m.group(0);
                        }

                        if (!mNewBuf.startsWith("<")) {
                            mNewBuf = "";
                        }

                        if (mNewBuf.endsWith(">") && mNewBuf.startsWith("<")) {
                            resultMsg = mNewBuf.substring(1, mNewBuf.length() - 1);
                            if (resultMsg.contains("<") || resultMsg.contains(">")) {
                                mNewBuf = "";
                                resultMsg = "";

                            } else {
                                disconnectBluetooth();
                                mNewBuf = "";

                                resultValue = getValue(resultMsg, "v");
                                if (resultValue.startsWith(".")) {
                                    resultValue = "0" + resultValue;
                                }
                                if (resultValue.endsWith(".")) {
                                    resultValue += "0";
                                }

                                resultDeviceId = getValue(resultMsg, "d");
                                resultTestType = getValue(resultMsg, "t");
                                SimpleDateFormat ts = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                                resultDate = ts.format(new Date());

                                SimpleDateFormat ts1 = new SimpleDateFormat("dd MMM yyyy, hh:mm aa");
                                resultDisplayDate = ts1.format(new Date());

                                resultTestId = java.util.UUID.randomUUID().toString();

                                showAlert("Success", "Result Received\n\n" + getTestType(resultTestType) + ": " + resultValue, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        showEditForm();
                                        displayResults();
                                    }
                                });
                            }
                        }
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void setupChat() {
        Log.d(TAG, "bt Setup chat");
        if (mBluetoothAdapter != null && mChatService == null) {
            // Initialize the BluetoothChatService to perform bluetooth connections
            Log.d(TAG, "bt service created");
            mChatService = new BluetoothChatService(this, mHandler);
            // Initialize the buffer for outgoing messages
            //mOutStringBuffer = new StringBuffer("");
        }
    }

    int port = -1;

    private void connectDevice(String address, boolean secure) {
        port++;
        if (mBluetoothAdapter != null) {
            if (!address.isEmpty()) {
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                if (port > 1) {
                    port = 0;
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            doDiscovery();
                        }
                    }, 20);

                } else {
                    mChatService.connect(device, secure, port);
                }
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        isLocationListenerEnabled = false;
        showGPSDisabledAlertToUser();
    }

    @Override
    public void onProviderEnabled(String provider) {
        displayResults();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onLocationChanged(Location location) {
        gpsLocation = location;

        gpsPlace = getCity(getApplicationContext(), location);

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putFloat("lat", (float) gpsLocation.getLatitude());
        editor.putFloat("lon", (float) gpsLocation.getLongitude());
        editor.putFloat("accuracy", (float) gpsLocation.getAccuracy());
        editor.putString("gpsPlace", gpsPlace);
        editor.commit();


        locationManager.removeUpdates(this);
        isLocationListenerEnabled = false;
        if (progressDialog != null) {
            progressDialog.dismiss();
        }

        displayResults();

        if (isSendingToMap) {
            sendResultToMaps();
        }
    }


    public String getCity(Context context, Location location) {

        if (location == null) {
            return null;
        }

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        int maxResults = 1;

        List<Address> addresses = null;
        try {
            Geocoder gc = new Geocoder(context, Locale.getDefault());
            addresses = gc.getFromLocation(latitude, longitude, maxResults);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (addresses != null && addresses.size() > 0) {
            return addresses.get(0).getLocality() + ", " + addresses.get(0).getCountryName();
        } else {
            return "";
        }

    }

    Boolean isLocationListenerEnabled = false;

    public void setupLocationListener() {

        if (locationProvider == null) {
            useCustomCoordinates();
            return;
        }
        if (!isLocationListenerEnabled) {

            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) {
                Toast toast = Toast.makeText(MainActivity.this, "Location service is not available", Toast.LENGTH_LONG);
                toast.show();

                return;
            }

            try {
                locationManager.requestLocationUpdates(locationProvider, 0, 0, this);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Location service is not available", Toast.LENGTH_LONG).show();
                return;
            }

            isLocationListenerEnabled = true;


            int timeOut = 30000;

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    locationManager.removeUpdates(MainActivity.this);
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                    isLocationListenerEnabled = false;
                }
            }, timeOut);

            if (!isLocationServiceEnabled()) {
                showGPSDisabledAlertToUser();
            }
        }
    }


    private void useCustomCoordinates() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        gpsLocation = new Location("custom");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        gpsPlace = prefs.getString("pref_place", "").trim();
        String value = prefs.getString("pref_latitude", "").trim();
        gpsLocation.setLatitude(value.isEmpty() ? 0 : Float.valueOf(value));
        value = prefs.getString("pref_longitude", "").trim();
        gpsLocation.setLongitude(value.isEmpty() ? 0 : Float.valueOf(value));
        gpsLocation.setAccuracy(40);

        if (gpsLocation.getLatitude() == 0 || gpsLocation.getLongitude() == 0) {
            gpsLocation = null;
            if (isSendingToMap) {
                Toast.makeText(this, "Cannot send. No location data", Toast.LENGTH_LONG).show();
                isSendingToMap = false;
            }

        } else {
            SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = preferences.edit();

            editor.putFloat("lat", (float) gpsLocation.getLatitude());
            editor.putFloat("lon", (float) gpsLocation.getLongitude());
            editor.putFloat("accuracy", (float) gpsLocation.getAccuracy());
            editor.commit();
        }
    }

    private void gotoStart() {
        if (resultValue.isEmpty()) {
            if (webView != null) {
                webView.setVisibility(View.GONE);
            }
            startHelp.setVisibility(View.VISIBLE);
            topContainer.setVisibility(View.VISIBLE);
            startButton.setVisibility(View.VISIBLE);
            container1.setVisibility(View.GONE);
            detailsLayout.setVisibility(View.GONE);
            container2.setVisibility(View.GONE);
            testDate.setVisibility(View.GONE);

            progressLayout.setVisibility(View.GONE);
            //checkStartOrResume();
        } else {
            startHelp.setVisibility(View.GONE);
            startButton.setVisibility(View.GONE);
            container1.setVisibility(View.VISIBLE);
            detailsLayout.setVisibility(View.VISIBLE);
            container2.setVisibility(View.VISIBLE);
            testDate.setVisibility(View.VISIBLE);
        }
        //topContainer.setBackgroundResource(R.drawable.water);
    }

    private void showAlert(String title, String message, DialogInterface.OnClickListener okMethod) {

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", okMethod);

        AlertDialog alertBox = alertBuilder.create();
        alertBox.show();

    }

    Boolean webError = false;

    private class OverrideWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            webError = false;
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(final WebView view, String url) {
            if (view.getUrl().equalsIgnoreCase("about:blank")) {
                webView.setVisibility(View.GONE);
                return;
            }

            if (!webError) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        progressLayout.setVisibility(View.GONE);
                        topContainer.setVisibility(View.GONE);
                        view.clearView();
                        view.clearHistory();
                        view.setVisibility(View.VISIBLE);
                        hideActionBar();
                    }
                }, 2000);
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            webError = true;
            progressLayout.setVisibility(View.GONE);
            view.stopLoading();
            topContainer.setVisibility(View.VISIBLE);
            view.clearView();
            view.clearHistory();
            view.setVisibility(View.GONE);

            showInternetAlert();

//            mWebView.setWebViewClient(new WebViewClient() {
//                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
//                    webView.loadUrl("file:///android_asset/myerrorpage.html");
//
//                }
//            });
        }
    }

    private void hideActionBar() {
        getSupportActionBar().hide();
    }

    private void showActionBar() {
        getSupportActionBar().show();
    }

    WebView webView;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (webView != null && webView.getVisibility() == View.VISIBLE) {
            if ((keyCode == KeyEvent.KEYCODE_BACK)) {
                if (webView.canGoBack()) {
                    webView.goBack();
                    return true;
                } else {
                    if (resultValue.isEmpty()) {
                        webView.clearHistory();
                        gotoStart();
                    } else {
                        webView.setVisibility(View.GONE);
                        topContainer.setVisibility(View.VISIBLE);
                        showActionBar();
                    }
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void gotoMap() {
        webError = false;

        //WebView webView = new WebView(getActivity(), null, android.R.attr.webViewStyle);
        webView = (WebView) findViewById(R.id.webView);
        webView.setBackgroundColor(0x00000000);

//        WebSettings webSettings = webView.getSettings();
//        webSettings.setJavaScriptEnabled(true);
//        webSettings.setBuiltInZoomControls(true);
        //webView.getSettings().setUseWideViewPort(true);
        webView.requestFocus(View.FOCUS_DOWN);
        //webView.requestFocusFromTouch();
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_UP:
                        if (!v.hasFocus()) {
                            v.requestFocus();
                        }
                        break;
                }
                return false;
            }
        });
        webView.setWebViewClient(new OverrideWebViewClient());
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.getSettings().setJavaScriptEnabled(true);
        String url = "http://ec2-54-251-25-70.ap-southeast-1.compute.amazonaws.com/?q=" + resultPlace;

        if (webView.getUrl() != null && !webView.getUrl().isEmpty() && webView.getUrl().equalsIgnoreCase(url)) {
            webView.setVisibility(View.VISIBLE);
            topContainer.setVisibility(View.GONE);
            hideActionBar();
        } else {
            webView.clearHistory();
            progressLayout.setVisibility(View.VISIBLE);
            webView.loadUrl(url);
        }

    }

    private void showSentAlert() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setMessage("Result sent successfully!")
                .setCancelable(false)
                .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity.this.finish();
                    }
                })
                .setPositiveButton("Goto Map", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        gotoMap();
                    }
                });

        AlertDialog alertBox = alertBuilder.create();
        alertBox.show();
    }

    private void resetCache(Boolean locationOnly) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = preferences.edit();
        resultPhotoPath = preferences.getString("photoPath", "");
        resultMsg = "";

        if (!locationOnly) {
            resultDisplayDate = "";
            resultDate = "";
            resultValue = "";
            resultDeviceId = "";
            resultSource = 0;
            resultTestType = "";
            editor.remove("date");
            editor.remove("sourceType");
            editor.remove("testType");
            editor.remove("result");
            editor.remove("deviceId");
            editor.remove("place");
            if (webView != null) {
                webView.clearHistory();
            }

            if (!resultPhotoPath.isEmpty()) {
                File file = new File(resultPhotoPath);
                file.delete();
            }

            editor.remove("photoPath");
        }
        gpsLocation = null;
        gpsPlace = "";
        editor.remove("lat");
        editor.remove("lon");
        editor.remove("accuracy");
        editor.remove("gpsPlace");
        editor.commit();
    }

    public static boolean isNetworkAvailable(Context context) {
        return ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo() != null;
    }

    boolean isSendingToMap = false;

    private void sendResultToMaps() {

        if (gpsLocation == null) {
            isSendingToMap = true;
            progressDialog = ProgressDialog.show(this, "", "Waiting for location...");
            setupLocationListener();
            if (gpsLocation == null) {
                return;
            }
        }

        if (resultSource < 1 || resultPlace.isEmpty()) {
            showEditForm();
            return;
        }

        if (!isNetworkAvailable(this)) {
            showInternetAlert();
            return;
        }

        progressDialog = ProgressDialog.show(this, "", "Sending to\n\nCaddisfly Maps ...");

        final Random rnd = new Random();

        if (resultDeviceId.isEmpty()) {
            resultDeviceId = String.valueOf(rnd.nextInt(99999));
        }
        RequestParams params = new RequestParams();
        params.put("deviceId", resultDeviceId);
        params.put("testId", resultTestId);
        params.put("place", resultPlace);
        params.put("test", resultTestType);
        params.put("source", String.valueOf(resultSource - 1));
        params.put("test_time", resultDate);
        params.put("value", resultValue);
        params.put("lat", String.valueOf(gpsLocation.getLatitude()));
        params.put("lon", String.valueOf(gpsLocation.getLongitude()));
        params.put("accuracy", String.valueOf(gpsLocation.getAccuracy()));


        if (!resultPhotoPath.isEmpty()) {
            File myFile = new File(resultPhotoPath);
            if (myFile.exists()) {
                try {
                    params.put("photo", myFile);
                } catch (Exception e) {

                    return;
                }
            }
        }

        WebClient.post("result", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "success: " + response);

                resetCache(false);
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {

                        testType.setText("");
                        testResult.setText("");

                        container1.setVisibility(View.GONE);
                        detailsLayout.setVisibility(View.GONE);
                        container2.setVisibility(View.GONE);
                        testDate.setVisibility(View.GONE);
                        if (progressDialog != null) {
                            progressDialog.dismiss();
                        }
                        showSentAlert();
                    }
                });
            }

            @Override
            public void onFailure(final Throwable e, final String response) {
                Log.d(TAG, "fail: " + response);

                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {

                        Toast.makeText(MainActivity.this, e.getMessage() + ", " + response, Toast.LENGTH_LONG).show();

                        if (progressDialog != null) {
                            progressDialog.dismiss();
                        }

                        //showInternetAlert();
                    }
                });

            }
        });
    }

    private void setLayout() {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);

        if (metrics.heightPixels > 480) {
            //LayoutParams params = topContainer.getLayoutParams();
            //params.height = 400;
            container1.getLayoutParams().height = 220;
            detailsLayout.getLayoutParams().height = 200;
            //topContainer.setLayoutParams(params);
            //this.getWindow().setLayout(LayoutParams.WRAP_CONTENT, 600);
        }
    }


    private void disconnectBluetooth() {
        if (mChatService != null) {
            try {
                mChatService.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Exception on disconnect", e);
            }
        }

        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            mBluetoothAdapter = null;
        }

        if (mReceiver != null) {
            try {
                this.unregisterReceiver(mReceiver);
                mReceiver = null;
            } catch (Exception e) {
                Log.e(TAG, "Exception on disconnect", e);
            }
        }
    }

    @Override
    public synchronized void onPause() {

        //disconnectBluetooth();

        if (!resultValue.isEmpty()) {
            SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("date", resultDate);
            editor.putString("displayDate", resultDisplayDate);

            editor.putString("testType", resultTestType);
            editor.putString("result", resultValue);
            editor.putString("place", resultPlace);
            editor.putString("gpsPlace", gpsPlace);
            editor.putInt("sourceType", resultSource);
            editor.putString("deviceId", resultDeviceId);

            editor.commit();
        }

        //locationManager.removeUpdates(this);
        super.onPause();

        if (D) Log.i(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (D) Log.i(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {

        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (Exception e) {
                Log.e(TAG, "", e);
            }
        }

        disconnectBluetooth();
        super.onDestroy();

        if (D) Log.i(TAG, "--- ON DESTROY ---");

    }
}