package com.jianingsun.mysensortag;

import android.app.DialogFragment;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements OnStatusListener {

    private Fragment mCurrentFragment;
    private FragmentManager mFragmentManager;
    private SwipeRefreshLayout mSwipeContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSwipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        mSwipeContainer.setEnabled(false);
        mSwipeContainer.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccent));

        // exit if the device doesn't have BLE
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.no_ble, Toast.LENGTH_SHORT).show();
            finish();
        }

        // load ScanFragment
        mFragmentManager = getSupportFragmentManager();
        mCurrentFragment = ScanFragment.newInstance();
        mFragmentManager.beginTransaction().replace(R.id.container, mCurrentFragment).commit();
    }

    @Override
    public void onListFragmentInteraction(String address) {
        mCurrentFragment = DeviceFragment.newInstance(address);
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
//        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left);
        transaction.replace(R.id.container, mCurrentFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onShowProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSwipeContainer.setRefreshing(true);
            }
        });
    }

    @Override
    public void onHideProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSwipeContainer.setRefreshing(false);
            }
        });
    }

    public boolean optionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getFragmentManager().popBackStackImmediate();
                break;
            case R.id.menuGoal:
                DialogFragment goalDialog = GoalDialogFragment.newInstance();
                goalDialog.show(getFragmentManager(), "Goal");
                break;
        }
        return true;
    }
}
