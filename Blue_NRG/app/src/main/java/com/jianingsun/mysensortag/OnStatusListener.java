package com.jianingsun.mysensortag;

/**
 * Created by jianingsun on 2018-03-08.
 */

public interface OnStatusListener {
    void onListFragmentInteraction(String address);

    void onShowProgress();

    void onHideProgress();
}
