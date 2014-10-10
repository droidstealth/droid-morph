package com.alexkolpa.appmorphing_sample;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.alexkolpa.appmorphing.AppMorph;

/**
 * Created by Alex on 26-9-2014.
 */
public class ProgressFragment extends DialogFragment {
    private ProgressDialog mProgressDialog;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setTitle("Morphing App");
        mProgressDialog.setMessage("Starting morphing...");
        return mProgressDialog;
    }

    public void setProgress(AppMorph.ProgressStep progress) {
        if(mProgressDialog != null) {
            mProgressDialog.setProgress(progress.id / AppMorph.ProgressStep.values().length * 100);
        }
    }
}
