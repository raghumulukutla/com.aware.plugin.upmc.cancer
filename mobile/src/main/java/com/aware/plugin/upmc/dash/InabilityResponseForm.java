package com.aware.plugin.upmc.dash;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ui.PermissionsHandler;

public class InabilityResponseForm extends AppCompatActivity {

    private int checkCount = 0;
    private EditText editText;
    private Button submitButton;
    private boolean isOtherChecked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        sendBroadcast(intent);
        final CharSequence[] items = {"Busy", "Pain", "Nausea", "Other"};
        LayoutInflater factory = LayoutInflater.from(this);
        View inabilityForm = factory.inflate(R.layout.content_inability_response_form,null);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Why are you unable to walk?")
                .setCancelable(false)
                .setView(inabilityForm)
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .create();

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
        editText = inabilityForm.findViewById(R.id.reason_field);
        submitButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        submitButton.setEnabled(false);
        editText.setVisibility(View.INVISIBLE);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                submitButton.setEnabled(true);
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(editText.getText().length() == 0 && isOtherChecked()) {
                    Toast.makeText(InabilityResponseForm.this, "Please specify a reason for other", Toast.LENGTH_SHORT).show();
                }
                else {
                    finish();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    public boolean isOtherChecked() {
        return isOtherChecked;
    }

    public void setOtherChecked(boolean otherChecked) {
        isOtherChecked = otherChecked;
        if(otherChecked)
            editText.setVisibility(View.VISIBLE);
        else
            editText.setVisibility(View.GONE);
    }

    public void onCheckBoxClocked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        if(checked)
            checkCount++;
        else
            checkCount--;

        if(checkCount>0)
            submitButton.setEnabled(true);
        else
            submitButton.setEnabled(false);

        switch (view.getId()) {
            case R.id.other_checkbox:
                setOtherChecked(checked);
                break;
        }
    }

}