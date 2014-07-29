package org.martus.andriod.vitalVoices.activities;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import org.martus.android.vitalVoices.application.Constants;
import org.martus.android.vitalVoices.application.MainApplication;
import org.odk.collect.android.provider.FormsProviderAPI;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Intent intent = new Intent(this, MainFormEntryActivity.class);
            Uri formUri = ContentUris.withAppendedId(FormsProviderAPI.FormsColumns.CONTENT_URI, 1);
            intent.setData(formUri);

            Cursor cursor = getContentResolver().query(FormsProviderAPI.FormsColumns.CONTENT_URI, null, null, null, null);
            startActivity(intent);

        } catch (Exception e) {
            Log.e(Constants.LOG_LABEL, "problem finding form file", e);
        }
    }
}
