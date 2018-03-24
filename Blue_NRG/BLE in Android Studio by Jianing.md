###ECSE426 Final Project Tutorial — BLE with Android Studio

#### by  Jianing Sun    Winter 2018   



- Peripherals, centrals
- GATT 
- services, characteristics
- read, write, notify
- UUIDs



### Part 1:

- **BLE Permissions**

  In order to use Bluetooth features in your application, you must declare the Bluetooth permission [BLUETOOTH](https://developer.android.com/reference/android/Manifest.permission.html#BLUETOOTH). You need this permission to perfom any Bluetooth communication, such as requesting a connection, accepting a connection, and transferring data.

  If you want your app to initiate device discovery or manipulate Bluetooth settings, you must **also** declare the [BLUETOOTH_ADMIN](https://developer.android.com/reference/android/Manifest.permission.html#BLUETOOTH_ADMIN) permission.

  Declare the Bluetooth permissions in your Android project **manifest** file:

  ```Java
  <uses-permission android:name="android.permission.BLUETOOTH"/>
  <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
  ```

  If you want to make your app available to devices that don't support BLE, you can determine whether BLE is supported on the device by using [PackageManager.hasSystemFeature()](https://developer.android.com/reference/android/content/pm/PackageManager.html#hasSystemFeature(java.lang.String)) once you start running.

  for example, including following code in the onCreate() method of your MainActivity file:

  ```Java
  if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
      Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
      finish();
  }
  ```

- **Setting Up BLE**

  Before your application can communicate over BLE, you need to verify that BLE is supported on the device, and if so, ensure that it is enabled.

  If BLE is supported, but disabled, then you can request that the user enable Bluetooth without leaving your application (pop out a request dialog). This setup is accomplished in two steps by using the [BluetoothAdapter](https://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html).

  - **Initialize [BluetoothAdapter](https://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html) and [BluetoothManager](https://developer.android.com/reference/android/bluetooth/BluetoothManager.html) **

  The BluetoothAdapter is **required** for any and all Bluetooth activity. It represents the device's own 			Bluetooth adapter. There's one Bluetooth adapter for the entire system, and your application can interact with it using this object. The snippet below shows how to get the adapter.

  > Note that this approach uses getSystemService() to return an instance of BluetoothManager, which is then used to get the adapter.

  ```Java
  // define this at the very beginning of your class
  private BluetoothAdapter mBluetoothAdapter;
  ...
  // Initializes Bluetooth adapter
  final BluetoothManager bluetoothManager =
          (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
  mBluetoothAdapter = bluetoothManager.getAdapter();
  ```

  - **Enable Bluetooth**

  Next, you need to ensure that Bluetooth is enabled. Call [isEnabled()](https://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html#isEnabled()) to check whether Bluetooth is currently enabled. If this method returns false, then Bluetooth is disabled. The following snippet checks whether Bluetooth is enabled. If it isn't, the snippet displays an error prompting the user to go to Settings to enable Bluetooth:	

  ```Java
  if (!mBluetoothAdapter.isEnabled()) {
              Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
              startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
          }
  ```

  > Note: you need to define the REQUEST_ENABLE_BT constant in your class (which must be greater than 0). It passes to your `startActivityForResult` and passes back to you in your `onActivityResult(int, int, android.content.Intent)` as the `requestCode` parameter.

  Below is an example about how to implement onActivityResult which pairs with you startActivityForResult method:

  ```Java
  @Override
      public void onActivityResult(int requestCode, int resultCode, Intent data) {
          super.onActivityResult(requestCode, resultCode, data);
          if (requestCode == ScanFragment.REQUEST_ENABLE_BT) {
              if (resultCode == Activity.RESULT_CANCELED) {
                  Toast.makeText(getActivity(), "Bluetooth Disabled", Toast.LENGTH_SHORT).show();
                  getActivity().finish();
              }
          }
      }
  ```

- **Finding BLE Devices**

  [ScanCallback](Bluetooth LE scan callbacks. Scan results are reported using these callbacks.) used for Bluetooth LE scan callbacks. Scan results are reported using these callbacks. Initialize the right ScanCallback based on your current API level:

  ```java
  private ScanCallback mScanCallback;
  ...
  if (Build.VERSION.SDK_INT >= 21) {
              mScanCallback = new ScanCallback() {
                  @Override
                  public void onScanResult(int callbackType, ScanResult result) {
                      super.onScanResult(callbackType, result);
                      Log.d("addDevice", "add!");
                 		// if you implement a RecyclerView inside your fragment or Activity for scanning, write an addDevice method in its corresponding Adapter class and call that method as following. Otherwise  no need to include this statement.
                      mRecyclerViewAdapter.addDevice(result.getDevice().getAddress(), result.getDevice().getName());

                  }
              };
          } else {
              mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                  @Override
                  public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
                      // same as above explanation
                      mRecyclerViewAdapter.addDevice(bluetoothDevice.getAddress(), bluetoothDevice.getName());
                  }
              };
          }
  ```

  > Note: do not forget to initialize your bluetooth manager & adapter following above snippet like we mentioned before.

- **Start Scanning**

  Even though Java is object-oriented programming, for Android development that are same principles under the code. Some functions will be called once you start your activity or fragment, like onCreate(), and onCreateView() will be called once you phone display a View. In practice if you are not sure which phase you program currently running on you can use [Log.d(String tag, String msg)](https://developer.android.com/reference/android/util/Log.html#d(java.lang.String,%20java.lang.String)) to debug, and check you log information in Logcat window at the bottom of Android Studio. 

  - Activity-lifecycle concepts

    To navigate transitions between stages of the activity lifecycle, the Activity class provides a core set of six callbacks:`onCreate()`, `onStart()`, `onResume()`, `onPause()`, `onStop()`, and `onDestroy()`. The system invokes each of these callbacks as an activity enters a new state. Below is a simplified illustration of the activity lifecycle:

    <img src="https://ws4.sinaimg.cn/large/006tNc79gy1fpj2y3yd4zj30s810sqb0.jpg" width="320px"/>

  - On the other hand, if you use [Fragment](https://developer.android.com/guide/components/fragments.html), keep its lifecycle execution order below in mind:

    A fragment is a **reusable** class implementing a portion of an activity. A Fragment typically defines a part of a user interface. Fragments must be embedded in activities; they cannot run independently of activities. **Activities are for navigation and fragments are for views and logic.**

    <img src="https://ws4.sinaimg.cn/large/006tNc79gy1fp58tats7jj30ry0p078g.jpg" width="400px"/>

  - After figuring out the lifecycle of you fragment and activity, put you startScan method in onResume(). Below is a snippet about how did I implement the startScan() method:

    ```java
    private void startScan() {
            Log.d("startScan", "scan!");
            if (mRecyclerViewAdapter.getSize() == 0) mListener.onShowProgress();
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                // request BluetoothLeScanner if it hasn't been initialized yet
                if (mLeScanner == null) mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                // start scan in low latency mode
                mLeScanner.startScan(new ArrayList<ScanFilter>(),
                        new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
                        mScanCallback);
            }
        }
    ```

    In contrast, put you stopScan() method in onPause().

### Part 2: Communicating with BLE and receiving data

- **GATT Connection**

  Create a GATT connection to the given (selected) device based on its address. That means for every device you need to obtain its address. To get a BluetoothDevice, use BluetoothAdapter.getRemoteDevice(String address) to create one representing a device of a known MAC address. Run connectDevice() method in onResume():

  ```java
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
  ```

- **Implement BluetoothGatt callbacks**

  Upon connection, Android is not aware of any services or characteristics on the device. Hence, you must implement [discoverServices()](https://developer.android.com/reference/android/bluetooth/BluetoothGatt.html#discoverServices()) inside onConnectionStateChange() which is inside your  [BluetoothGattCallback](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback.html#BluetoothGattCallback()) callback.

  ```java
  private BluetoothGattCallback mCallback = new BluetoothGattCallback() {
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
  ```

- **Once service discovery is completed, the [onServicesDiscovered()](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback.html#onServicesDiscovered(android.bluetooth.BluetoothGatt,%20int)) is triggered. **

  ```Java
  public void onServicesDiscovered(BluetoothGatt gatt, int status) {
              super.onServicesDiscovered(gatt, status);
              // as soon as services are discovered, acquire characteristic and try enabling
              mMovService = mGatt.getService(UUID.fromString("02366E80-CF3A-11E1-9AB4-0002A5D5C51B"));
              mEnable = mMovService.getCharacteristic(UUID.fromString("340A1B80-CF4B-11E1-AC36-0002A5D5C51B"));
              if (mEnable == null) {
                  Toast.makeText(getActivity(), R.string.service_not_found, Toast.LENGTH_LONG).show();
                  getActivity().finish();
              }
              mGatt.readCharacteristic(mEnable);
              deviceConnected();
          }
  ```

- Once you device has been fully connected, call the deviceConnected() method. Note this is not an official Android method for BLE and it needs to be implemented by yourself. This is highly associated with the functions you want to achieve in your app.

- **Receive Data**

  In the above onServicesdiscovered() method, note I called [readCharacteristic()](https://developer.android.com/reference/android/bluetooth/BluetoothGatt.html#readCharacteristic(android.bluetooth.BluetoothGattCharacteristic)), which reads the requested characteristic from the associated remote device. This is an asynchronous operation. The result of the read operation is reported by the [onCharacteristicRead()](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback.html#onCharacteristicRead(android.bluetooth.BluetoothGatt,%20android.bluetooth.BluetoothGattCharacteristic,%20int)) callback.

  Inside the onCharacteristicRead method, you can finally get your data by using `characteristic.getValue()`

  > Note the value you get through characteristic.getValue() is byte[] type, if you want to display that in your app you need to implement an utility to transfer byte[] to String. If you just want to process the data inside your code and don't want to display the data, you can just do the calculation you want based on the byte[]*.

### Part 3: Some links

If you have no experience in Android, I recommend you start with a simple app and play with Android Studio first. Once you get familiar with Android Studio, for this project, I used following components to build up the overall layout and functions for the app.

- RecyclerView:

  https://developer.android.com/guide/topics/ui/layout/recyclerview.html

- Fragment:

  https://developer.android.com/training/basics/fragments/creating.html

- Some others about views and layouts: [Button](https://developer.android.com/guide/topics/ui/controls/button.html), TextView, EditText, [Styles](https://developer.android.com/guide/topics/resources/style-resource.html) and [Themes](https://developer.android.com/guide/topics/ui/look-and-feel/themes.html), [Animation](https://github.com/codepath/android_guides/wiki/Animations), [SharedPreference](https://developer.android.com/reference/android/content/SharedPreferences.html), [Toast](https://developer.android.com/reference/android/widget/Toast.html), [Listener](https://developer.android.com/guide/topics/ui/ui-events.html)

- Some convenient tools you can use and make your app better but not necessary for everyone: [SwipeRefreshLayout](https://developer.android.com/reference/android/support/v4/widget/SwipeRefreshLayout.html), [SharedPreference ](https://developer.android.com/reference/android/content/SharedPreferences.html), some external libraries. 

- The most useful link (offical API for Android): https://developer.android.com/index.html . 

  You can find everything about Android by using the **Search** on top right corner,

  ![](https://ws1.sinaimg.cn/large/006tNc79gy1fpj5iy7lyrj31be0oy77u.jpg)

- Some useful websites about UI:

  Material Palette: https://www.materialpalette.com

  Flat UI Colors: https://flatuicolors.com

  ​

From my side, I created one MainActivity (used to contain fragments), and two fragments: one for scanning with a RecyclerView (and its RecyclerViewAdapter), one for the connected Device (display the data, etc). But you can have you own design as long as it works.

Make sure you understand your code and figure out their relations (which method would be called first and which one would be executed after which one, what kind of object I need to declare, such as BluetoothGatt, BluetoothAdapter, BluetoothGattService, BluetoothGattCharacteristic, BluetoothLeScanner, ScanCallback, etc.). I have to say they are pretty confusing and kind of similar. Do not simply copy and paste otherwise it probably won't work. Be patient with your code and make fully use of [StackOverflow](https://stackoverflow.com) and Logcat in Android Studio when you are debugging.

