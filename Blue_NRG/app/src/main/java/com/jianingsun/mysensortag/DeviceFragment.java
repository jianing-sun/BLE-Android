package com.jianingsun.mysensortag;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.eazegraph.lib.models.PieModel;
import org.eazegraph.lib.charts.PieChart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.UUID;

/**
 * Fragment showing data for a connected device.
 */
public class DeviceFragment extends Fragment  implements View.OnClickListener {

    private static final String ARG_ADDRESS = "address";
    private String mAddress;
    private boolean mIsRecording = false;
    private LinkedList<Measurement> mRecording;
    private OnStatusListener mListener;
    private Calendar previousRead;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mGatt;
    private BluetoothGattService mMovService;
    private BluetoothGattCharacteristic mRead, mEnable, mPeriod;

    private TextView maAxis, mbAxis, mcAxis;
    private Button mStart, mStop, mExport;
    private LineChart mChart;
    private TextView mSteps;

    // used for piechart display of our step
    private PieChart pc;
    private PieModel Current, Goal;
    private TextView stepsView;
    final int DEFAULT_GOAL = 100;
    private int goal;

    // used to store accelerator data and calculate the step
    private final int ACCELE_ARRAY_SIZE = 50;
    private final int VEL_ARRAY_SIZE = 10;
    private final float STEP_THRESHOLD = 2f;
    private final int TIME_THRESHOLD = 700;
    private int accel_counter = 0;
    private int vel_counter = 0;
    private double[] accelX = new double[ACCELE_ARRAY_SIZE];
    private double[] accelY = new double[ACCELE_ARRAY_SIZE];
    private double[] accelZ = new double[ACCELE_ARRAY_SIZE];
    private double[] velArray = new double[VEL_ARRAY_SIZE];
    private float last_vel = 0;
    private long lastTime = 0;
    private long curTime = 0;
    private int steps = 0;

    /**
     * Mandatory empty constructor.
     */
    public DeviceFragment() {
    }

    /**
     * Returns a new instance of this Fragment.
     *
     * @param address the MAC address of the device to connect
     * @return A new instance of {@link DeviceFragment}
     */
    public static DeviceFragment newInstance(String address) {
        DeviceFragment fragment = new DeviceFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ADDRESS, address);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            mAddress = getArguments().getString(ARG_ADDRESS);
        }

        // initialize bluetooth manager & adapter
        BluetoothManager manager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("BLE", "onResume");
        connectDevice(mAddress);
        SharedPreferences prefs = getActivity()
                .getSharedPreferences("sensortag", Context.MODE_PRIVATE);
        goal = prefs.getInt("goal", DEFAULT_GOAL);
//        Log.d("goal", String.valueOf(goal));
    }

    @Override
    public void onPause() {
//        deviceDisconnected();
        super.onPause();
    }

    /**
     * Creates a GATT connection to the given device.
     *
     * @param address String containing the address of the device
     */
    private void connectDevice(String address) {
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getActivity(), R.string.ble_disable, Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
        mListener.onShowProgress();
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mGatt = device.connectGatt(getActivity(), false, mCallback);
        Log.d("BLE", "connectDevice");
    }

    private BluetoothGattCallback mCallback = new BluetoothGattCallback() {
        double result[];
        String str;
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault());
        Calendar currentTime;

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            switch (newState) {
                case BluetoothGatt.STATE_CONNECTED:
                    // as soon as we're connected, discover services
                    mGatt.discoverServices();
                    Log.d("BLE", "onConnectionStateChange");
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            // as soon as services are discovered, acquire characteristic and try enabling
            mMovService = mGatt.getService(UUID.fromString("02366E80-CF3A-11E1-9AB4-0002A5D5C51B"));
            mEnable = mMovService.getCharacteristic(UUID.fromString("340A1B80-CF4B-11E1-AC36-0002A5D5C51B"));
            if (mEnable == null) {
                Toast.makeText(getActivity(), R.string.service_not_found, Toast.LENGTH_LONG).show();
                getActivity().finish();
            }
            Log.d("BLE", "onServiceDiscovered");
            previousRead = Calendar.getInstance();
            mGatt.readCharacteristic(mEnable);
            deviceConnected();
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            // convert raw byte array to G unit values for xyz axes
            Log.d("BLE", "called!");
            str = Util.toHexString(characteristic.getValue(), characteristic.getValue().length);
            Log.d("BLE", str);
            result = Util.convertAccel(characteristic.getValue());
            Log.d("Calculation", "result[0]: " + result[0] + " result[1]+ " + result[1] + " result[2]+ " + result[2]);

            if (mIsRecording) {
                Measurement measurement = new Measurement(result[0]*10, result[1]*10, result[2]*10, formatter.format(Calendar.getInstance().getTime()));
                mRecording.add(measurement);
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isAdded()) {
                        // update current acceleration readings
                        stepsView.setText(String.valueOf(str));
                        maAxis.setText(String.format(getString(R.string.xAxis), Math.abs(result[0]*10)));
                        mbAxis.setText(String.format(getString(R.string.yAxis), Math.abs(result[1]*10)));
                        mcAxis.setText(String.format(getString(R.string.zAxis), Math.abs(result[2]*10)));
                        maAxis.setTextColor(ContextCompat.getColor(getActivity(), result[0] < 0 ? R.color.red : R.color.green));
                        mbAxis.setTextColor(ContextCompat.getColor(getActivity(), result[1] < 0 ? R.color.red : R.color.green));
                        mcAxis.setTextColor(ContextCompat.getColor(getActivity(), result[2] < 0 ? R.color.red : R.color.green));
                    }
                }
            });
            // poll for next values
            currentTime = Calendar.getInstance();
            long diff = currentTime.getTimeInMillis() - previousRead.getTimeInMillis();
            if (diff < 100) {
                try {
                    Thread.sleep(100 - diff);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            previousRead = Calendar.getInstance();
            mGatt.readCharacteristic(mRead);
        }
    };

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bStart:
                startRecording();
                break;
            case R.id.bStop:
                stopRecording();
                break;
            case R.id.bExport:
                try {
                    // create and write output file in cache directory
                    File outputFile = new File(getActivity().getCacheDir(), "recording.csv");
                    OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputFile));
                    writer.write(Util.recordingToCSV(mRecording));
                    writer.close();

                    // get Uri from FileProvider
                    Uri contentUri = FileProvider.getUriForFile(getActivity(), "com.jianingsun.mysensortag.fileprovider", outputFile);

                    // create sharing intent
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    // temp permission for receiving app to read this file
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    shareIntent.setType("text/csv");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    startActivity(Intent.createChooser(shareIntent, "Choose an app"));
                } catch (IOException e) {
                    Toast.makeText(getActivity(), R.string.error_file, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /**
     * Called when the device has been fully connected.
     */
    private void deviceConnected() {
        mListener.onHideProgress();
        Log.d("BLE", "deviceConnected");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStart.setEnabled(true);
                Log.d("BLE", "runOnUiThread");
            }
        });

        // start connection watcher thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean hasConnection = true;
                while (hasConnection) {
                    long diff = Calendar.getInstance().getTimeInMillis() - previousRead.getTimeInMillis();
                    mGatt.readCharacteristic(mEnable);
                    if (diff > 500) {
                        hasConnection = false;
                        mStart.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getContext(), R.string.connection_lost, Toast.LENGTH_LONG).show();
                                deviceDisconnected();
                            }
                        });
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * Called when the device should be disconnected.
     */
    private void deviceDisconnected() {
        stopRecording();
        mStart.setEnabled(false);
        if (mGatt != null) mGatt.disconnect();
    }

    /**
     * Starts the recording and updates the UI to reflect that.
     */
    private void startRecording() {
        // update UI
        mStart.setEnabled(false);
        mExport.setEnabled(false);
        mStop.setEnabled(true);
        mIsRecording = true;

        mRecording = new LinkedList<>();
        Log.d("startRecording: ", " start recording!");
    }

    /**
     * Stops the recording and updates the UI to reflect that.
     */
    private void stopRecording() {
        if (mIsRecording) {
            // update UI
            mStop.setEnabled(false);
            mStart.setEnabled(true);
            mExport.setEnabled(true);
            mIsRecording = false;

            if (mRecording.size() > 0) {
                ArrayList<Entry> combined = new ArrayList<>(mRecording.size());
                ArrayList<Entry> x = new ArrayList<>(mRecording.size());
                ArrayList<Entry> y = new ArrayList<>(mRecording.size());
                ArrayList<Entry> z = new ArrayList<>(mRecording.size());
                int i = 0;
                for (Measurement m : mRecording) {
                    combined.add(new Entry(i, (float) m.getCombined()));
                    x.add(new Entry(i, (float) m.getX()));
                    y.add(new Entry(i, (float) m.getY()));
                    z.add(new Entry(i++, (float) m.getZ()));
                }
                LineDataSet sCombined = new LineDataSet(combined, getString(R.string.combined));
                LineDataSet sX = new LineDataSet(x, getString(R.string.x));
                LineDataSet sY = new LineDataSet(y, getString(R.string.y));
                LineDataSet sZ = new LineDataSet(z, getString(R.string.z));
                sCombined.setDrawCircles(false);
                sX.setDrawCircles(false);
                sY.setDrawCircles(false);
                sZ.setDrawCircles(false);
                sCombined.setColor(ContextCompat.getColor(getActivity(), R.color.purple));
                sX.setColor(ContextCompat.getColor(getActivity(), R.color.red));
                sY.setColor(ContextCompat.getColor(getActivity(), R.color.green));
                sZ.setColor(ContextCompat.getColor(getActivity(), R.color.blue));
                LineData lineData = new LineData(sCombined, sX, sY, sZ);
                mChart.setData(lineData);
                mChart.invalidate();

            } else {
                Toast.makeText(getContext(), R.string.no_data, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("BLE", "onCreateView");
        View layout = inflater.inflate(R.layout.fragment_device, null);
        stepsView = (TextView) layout.findViewById(R.id.tvOverall);
        mStart = (Button) layout.findViewById(R.id.bStart);
        mStop = (Button) layout.findViewById(R.id.bStop);
        mExport = (Button) layout.findViewById(R.id.bExport);
        mChart = (LineChart) layout.findViewById(R.id.chart);
        maAxis = (TextView) layout.findViewById(R.id.tvaAxis);
        mbAxis = (TextView) layout.findViewById(R.id.tvbAxis);
        mcAxis = (TextView) layout.findViewById(R.id.tvcAxis);

        mChart.setDescription(null);
        mChart.setHighlightPerDragEnabled(false);
        mChart.setHighlightPerTapEnabled(false);
        mChart.setPinchZoom(true);
        mChart.getLegend().setDrawInside(true);
        mChart.setExtraTopOffset(10);

        mStart.setOnClickListener(this);
        mStop.setOnClickListener(this);
        mExport.setOnClickListener(this);

        return layout;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnStatusListener) {
            mListener = (OnStatusListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnStatusListener");
        }
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.device_menu, menu);
        // TODO: pause and resume?? (i think it's meanlingless to add this function, maybe find sth different)

    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {

            default:
                return ((MainActivity) getActivity()).optionsItemSelected(item);
        }
    }
}
