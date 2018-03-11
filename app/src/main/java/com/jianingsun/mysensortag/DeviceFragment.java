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

    private TextView mXAxis, mYAxis, mZAxis, mMax;
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
        connectDevice(mAddress);
        SharedPreferences prefs = getActivity()
                .getSharedPreferences("sensortag", Context.MODE_PRIVATE);
        goal = prefs.getInt("goal", DEFAULT_GOAL);
        Log.d("goal", String.valueOf(goal));
    }

    @Override
    public void onPause() {
        deviceDisconnected();
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
    }

    private BluetoothGattCallback mCallback = new BluetoothGattCallback() {
        double result[];
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault());
        Calendar currentTime;

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            switch (newState) {
                case BluetoothGatt.STATE_CONNECTED:
                    // as soon as we're connected, discover services
                    mGatt.discoverServices();
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            // as soon as services are discovered, acquire characteristic and try enabling
            mMovService = mGatt.getService(UUID.fromString("F000AA80-0451-4000-B000-000000000000"));
            mEnable = mMovService.getCharacteristic(UUID.fromString("F000AA82-0451-4000-B000-000000000000"));
            if (mEnable == null) {
                Toast.makeText(getActivity(), R.string.service_not_found, Toast.LENGTH_LONG).show();
                getActivity().finish();
            }
            /**
             * Bits starting with the least significant bit (the rightmost one)
             * 0       Gyroscope z axis enable
             * 1       Gyroscope y axis enable
             * 2       Gyroscope x axis enable
             * 3       Accelerometer z axis enable
             * 4       Accelerometer y axis enable
             * 5       Accelerometer x axis enable
             * 6       Magnetometer enable (all axes)
             * 7       Wake-On-Motion Enable
             * 8:9	    Accelerometer range (0=2G, 1=4G, 2=8G, 3=16G)
             * 10:15   Not used
             */
            mEnable.setValue(0b1000111000, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
            mGatt.writeCharacteristic(mEnable);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (characteristic == mEnable) {
                // if enable was successful, set the sensor period to the lowest value
                mPeriod = mMovService.getCharacteristic(UUID.fromString("F000AA83-0451-4000-B000-000000000000"));
                if (mPeriod == null) {
                    Toast.makeText(getActivity(), R.string.service_not_found, Toast.LENGTH_LONG).show();
                    getActivity().finish();
                }
                mPeriod.setValue(0x0A, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                mGatt.writeCharacteristic(mPeriod);
            } else if (characteristic == mPeriod) {
                // if setting sensor period was successful, start polling for sensor values
                mRead = mMovService.getCharacteristic(UUID.fromString("F000AA81-0451-4000-B000-000000000000"));
                if (mRead == null) {
                    Toast.makeText(getActivity(), R.string.characteristic_not_found, Toast.LENGTH_LONG).show();
                    getActivity().finish();
                }
                previousRead = Calendar.getInstance();
                mGatt.readCharacteristic(mRead);
                deviceConnected();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            // convert raw byte array to G unit values for xyz axes
//            Log.d("onChara", "called!");
            result = Util.convertAccel(characteristic.getValue());
            if (mIsRecording) {
                Measurement measurement = new Measurement(result[0], result[1], result[2], formatter.format(Calendar.getInstance().getTime()));
//                Log.d("onChara", "new measurement!");
                mRecording.add(measurement);

                double[] curAccel = new double[3];
                curAccel[0] = result[0];
                curAccel[1] = result[1];
                curAccel[2] = result[2];
//                Log.d("result", "result[0]: " + result[0] + " result[1]+ " + result[1] + " result[2]+ " + result[2]);

                accel_counter++;
                accelX[accel_counter % ACCELE_ARRAY_SIZE] = curAccel[0];
                accelY[accel_counter % ACCELE_ARRAY_SIZE] = curAccel[1];
                accelZ[accel_counter % ACCELE_ARRAY_SIZE] = curAccel[2];

                double[] realZ = new double[3];
                realZ[0] = DataProcessUtil.sum(accelX) / Math.min(accel_counter, ACCELE_ARRAY_SIZE);
                realZ[1] = DataProcessUtil.sum(accelY) / Math.min(accel_counter, ACCELE_ARRAY_SIZE);
                realZ[2] = DataProcessUtil.sum(accelZ) / Math.min(accel_counter, ACCELE_ARRAY_SIZE);

                double norm_denominator = DataProcessUtil.norm(realZ);
                realZ[0] /= norm_denominator;
                realZ[1] /= norm_denominator;
                realZ[2] /= norm_denominator;

                double finalZ = DataProcessUtil.dot3(realZ, curAccel) - norm_denominator;
                vel_counter++;
                velArray[vel_counter % VEL_ARRAY_SIZE] = finalZ;
                float velocity = (float)DataProcessUtil.sum(velArray) * 10;
//                Log.d("velocity", String.valueOf(velocity));
                curTime = Calendar.getInstance().getTimeInMillis();
//                Log.d("curTime", String.valueOf(curTime));

                if (Math.abs(velocity) > STEP_THRESHOLD && last_vel <= STEP_THRESHOLD
                        && (curTime - lastTime) > TIME_THRESHOLD) {
                    lastTime = curTime;
//                    Log.d("lastTime", String.valueOf(lastTime));
                    steps++;
                    Log.d("steps", String.valueOf(steps));

                }
                last_vel = velocity;
//                Log.d("last_vel", String.valueOf(last_vel));


            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isAdded()) {
                        // update current acceleration readings
//                        mSteps.setText(String.valueOf(steps));
                        stepsView.setText(String.valueOf(steps));
                        updatePie();
                        mXAxis.setText(String.format(getString(R.string.xAxis), Math.abs(result[0])));
                        mYAxis.setText(String.format(getString(R.string.yAxis), Math.abs(result[1])));
                        mZAxis.setText(String.format(getString(R.string.zAxis), Math.abs(result[2])));
                        mXAxis.setTextColor(ContextCompat.getColor(getActivity(), result[0] < 0 ? R.color.red : R.color.green));
                        mYAxis.setTextColor(ContextCompat.getColor(getActivity(), result[1] < 0 ? R.color.red : R.color.green));
                        mZAxis.setTextColor(ContextCompat.getColor(getActivity(), result[2] < 0 ? R.color.red : R.color.green));
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

    private void updatePie() {
        Current.setValue(steps);
        SharedPreferences prefs = getActivity().getSharedPreferences("sensortag", Context.MODE_PRIVATE);
        goal = prefs.getInt("goal", DEFAULT_GOAL);
        Log.d("goal", String.valueOf(goal));
        if (goal - steps > 0) {
            if (pc.getData().size() == 1) {
                pc.addPieSlice(Goal);
            }
            Goal.setValue(goal - steps);
        } else {
            pc.clearChart();
            pc.addPieSlice(Current);
        }
        pc.update();

    }

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
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStart.setEnabled(true);
            }
        });

        // start connection watcher thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean hasConnection = true;
                while (hasConnection) {
                    long diff = Calendar.getInstance().getTimeInMillis() - previousRead.getTimeInMillis();
                    if (diff > 2000) {
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
                        Thread.sleep(2000);
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
        mMax.setVisibility(View.INVISIBLE);

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
                double max = 0;
                for (Measurement m : mRecording) {
                    combined.add(new Entry(i, (float) m.getCombined()));
                    if (m.getCombined() > max) max = m.getCombined();
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

                mMax.setText(String.format(getString(R.string.max), max));
                mMax.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getContext(), R.string.no_data, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_device, null);
        mXAxis = (TextView) layout.findViewById(R.id.tvXAxis);
        mYAxis = (TextView) layout.findViewById(R.id.tvYAxis);
        mZAxis = (TextView) layout.findViewById(R.id.tvZAxis);
        mMax = (TextView) layout.findViewById(R.id.tvMax);
        mStart = (Button) layout.findViewById(R.id.bStart);
        mStop = (Button) layout.findViewById(R.id.bStop);
        mExport = (Button) layout.findViewById(R.id.bExport);
        mChart = (LineChart) layout.findViewById(R.id.chart);

        mChart.setDescription(null);
        mChart.setHighlightPerDragEnabled(false);
        mChart.setHighlightPerTapEnabled(false);
        mChart.setPinchZoom(true);
        mChart.getLegend().setDrawInside(true);
        mChart.setExtraTopOffset(10);

        mStart.setOnClickListener(this);
        mStop.setOnClickListener(this);
        mExport.setOnClickListener(this);

        // initial implement of step display
//        mSteps = (TextView)layout.findViewById(R.id.numSteps);

        stepsView = (TextView) layout.findViewById(R.id.steps);
        pc = (PieChart) layout.findViewById(R.id.piechart);

        Current = new PieModel("", 0, ContextCompat.getColor(getActivity(), R.color.wildgreen));
        pc.addPieSlice(Current);
        Goal = new PieModel("", DEFAULT_GOAL, ContextCompat.getColor(getActivity(), R.color.lightbuleballerina));
        pc.addPieSlice(Goal);

        // display and animation
        pc.setDrawValueInPie(false);
        pc.setUsePieRotation(true);
        pc.startAnimation();

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
