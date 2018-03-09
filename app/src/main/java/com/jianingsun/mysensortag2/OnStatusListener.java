package com.jianingsun.mysensortag2;

/**
 * Created by jianingsun on 2018-03-09.
 */

public interface OnStatusListener {

    void onListFragmentInteraction(String address);

    void onShowProgress();

    void onHideProgress();
}
