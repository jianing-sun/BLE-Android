package com.jianingsun.mysensortag2;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by jianingsun on 2018-03-09.
 */

public class DeviceRecyclerViewAdapter extends RecyclerView.Adapter<DeviceRecyclerViewAdapter.ViewHolder> {

    private ArrayList<String> mAddresses;
    private OnStatusListener mListener;
    private ArrayList<String> mNames;

    public DeviceRecyclerViewAdapter(OnStatusListener listener) {
        mAddresses = new ArrayList<>();
        mListener = listener;
        mNames = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device_item, parent, false);
        return new ViewHolder(view);
    }


    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.mAddress.setText(mAddresses.get(position));
        holder.mName.setText(mNames.get(position));

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    mListener.onListFragmentInteraction(mAddresses.get(position));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mAddresses.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mAddress;
        public final TextView mName;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mAddress = (TextView) view.findViewById(R.id.tvAddress);
            mName = (TextView) view.findViewById(R.id.tvName);
        }
    }

    /**
     * Inserts the address into the list if it doesn't yet exist.
     *
     * @param address the MAC address of the new device
     */
    public void addDevice(String address, String name) {
        // after the first device has been discovered, disable the spinning
        // status indicator that is partly hiding the element
        if (mAddresses.size() == 0) mListener.onHideProgress();

        // add the device to list if it doesn't exist
        if (!mAddresses.contains(address)) {
            mAddresses.add(address);
            mNames.add(name);
            notifyItemInserted(mAddresses.indexOf(address));
        }
    }

    /**
     * Returns the number of devices shown.
     *
     * @return the number of devices
     */
    public int getSize() {
        return mAddresses.size();
    }
}
