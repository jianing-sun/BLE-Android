package com.jianingsun.mysensortag;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

/**
 * Created by jianingsun on 2018-03-10.
 */

public class GoalDialogFragment extends DialogFragment {

    private EditText mEditText;
    final int DEFAULT_GOAL = 100;


    public GoalDialogFragment() {
    }

    public static GoalDialogFragment newInstance() {
        GoalDialogFragment frag = new GoalDialogFragment();
        Bundle args = new Bundle();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder;
        View v;
        final SharedPreferences prefs = getActivity()
                .getSharedPreferences("sensortag", Context.MODE_PRIVATE);

        // initial attempt with number picker, but not very practical as for goal modify
        /*builder = new AlertDialog.Builder(getActivity());
        final NumberPicker np = new NumberPicker(getActivity());
        np.setMinValue(1);
        np.setMaxValue(10000);
        np.setValue(100);
        builder.setView(np);
        builder.setTitle("Set Goal");

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                np.clearFocus();
                prefs.edit().putInt("goal", np.getValue()).commit();
                dialog.dismiss();

            }
        });*/

        // try to use simple edittext
        builder = new AlertDialog.Builder(getActivity());
        v = getActivity().getLayoutInflater().inflate(R.layout.goal, null);
        final EditText new_goal = (EditText) v.findViewById(R.id.etGoal);
        new_goal.setText(String.valueOf(prefs.getInt("goal", DEFAULT_GOAL)));
        builder.setView(v);
        builder.setTitle("Input new goal");
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    prefs.edit().putInt("goal", Integer.valueOf(new_goal.getText().toString()))
                            .apply();
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });


        return builder.create();
    }

}

















