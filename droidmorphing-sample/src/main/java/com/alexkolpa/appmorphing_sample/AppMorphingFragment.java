package com.alexkolpa.appmorphing_sample;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alexkolpa.appmorphing.AppMorph;

import java.io.File;

public class AppMorphingFragment extends Fragment {

    private static final int IMAGE_REQUEST_CODE = 1343;

    private static final int STEP_COUNT = AppMorph.ProgressStep.values().length;

    AppMorph mAppMorph;

    private Uri mIcon = null;
    private String mLabel = null;

    private View mMorphButton;
    private View mAsciiError;

    private View mShareResult;

    private ProgressBar mProgressBar;

    private TextView mLabelView;
    private ImageView mIconView;

    private File mMorphedApk;

    public AppMorphingFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mAppMorph = new AppMorph(getActivity());

        return inflater.inflate(R.layout.fragment_app_morphing_sample, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupViews(view);
    }

    private void setupViews(View rootView){
        rootView.findViewById(R.id.select_icon_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Icon"), IMAGE_REQUEST_CODE);
            }
        });

        ((TextView)rootView.findViewById(R.id.select_label_field)).addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                if(editable.length() > 0) {
                    mLabel = editable.toString();
                    activateMorphButton();
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
        });

        mMorphButton = rootView.findViewById(R.id.morph_button);

        mMorphButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startMorph();
            }
        });

        mAsciiError = rootView.findViewById(R.id.non_ascii_error);

        mShareResult = rootView.findViewById(R.id.share_result);

        mIconView = (ImageView) rootView.findViewById(R.id.icon);
        mLabelView = (TextView) rootView.findViewById(R.id.label);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progress);
        mProgressBar.setMax(STEP_COUNT * 100);
    }

    private void startMorph() {
        MorphTask task = new MorphTask();
        mAppMorph.setMorphProgressListener(task);
        task.execute();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == IMAGE_REQUEST_CODE && data != null && data.getData() != null) {
            mIcon = data.getData();
            activateMorphButton();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void activateMorphButton() {
        boolean isNonAscii = mLabel != null && !mLabel.matches("\\A\\p{ASCII}*\\z");
        if(mIcon != null && mLabel != null && !isNonAscii) {
            mMorphButton.setEnabled(true);
        }
        else {
            mMorphButton.setEnabled(false);
        }

        if(isNonAscii) {
            mAsciiError.setVisibility(View.VISIBLE);
        }
        else {
            mAsciiError.setVisibility(View.GONE);
        }
    }



    /**
     * This task runs the actual morphing, done async as not to block the UI thread.
     */
    private class MorphTask extends AsyncTask<Void, Integer, Void> implements AppMorph.MorphProgressListener {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mMorphButton.setEnabled(false);
            mProgressBar.setProgress(0);
            mProgressBar.setVisibility(View.VISIBLE);
            mShareResult.setVisibility(View.GONE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            mAppMorph.morphApp(mLabel, mIcon);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            mMorphButton.setEnabled(true);
            mProgressBar.setVisibility(View.GONE);
            mShareResult.setVisibility(View.VISIBLE);

            //We failed. We're sorry
            if(isCancelled() || mMorphedApk == null) {
                Toast.makeText(getActivity(), R.string.failed_app_morph, Toast.LENGTH_SHORT).show();
                return;
            }


            mIconView.setImageURI(mIcon);
            mLabelView.setText(mLabel);

            mShareResult.findViewById(R.id.share_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mMorphedApk));
                    //Since it's technically a zip, we treat it as one, since Android refuses to share 'application/vnd.android.package-archive'
                    sendIntent.setType("application/zip");
                    startActivity(sendIntent);
                }
            });
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            updateSmoothProgress(values[0]);
        }

        private void updateSmoothProgress(int progress) {
            if(android.os.Build.VERSION.SDK_INT >= 11){
                // will update the "progress" propriety of seekbar until it reaches progress
                ObjectAnimator animation = ObjectAnimator.ofInt(mProgressBar, "progress", progress);
                animation.setDuration(500); // 0.5 second
                animation.setInterpolator(new DecelerateInterpolator());
                animation.start();
            }
            else
                mProgressBar.setProgress(progress);
        }

        @Override
        public void onProgress(AppMorph.ProgressStep progress) {
            publishProgress(progress.id * 100);
        }

        @Override
        public void onMorphFailed(AppMorph.ProgressStep atPoint, Exception failure) {
            cancel(true);
        }

        @Override
        public void onFinished(File newApk) {
            mMorphedApk = newApk;
            publishProgress(STEP_COUNT * 100);
        }
    }
}