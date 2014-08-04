package org.martus.andriod.vitalVoices.activities;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import org.martus.andriod.vitalVoices.R;
import org.martus.android.vitalVoices.application.Constants;
import org.martus.android.vitalVoices.application.MainApplication;
import org.odk.collect.android.provider.FormsProviderAPI;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
    }

    public void startNewForm(View view) {
        System.out.println("boom");

        try {
            Intent intent = new Intent(this, MainFormEntryActivity.class);
            Uri formUri = ContentUris.withAppendedId(FormsProviderAPI.FormsColumns.CONTENT_URI, 1);

            intent.setData(formUri);

            Cursor cursor = getContentResolver().query(FormsProviderAPI.FormsColumns.CONTENT_URI, null, null, null, null);
            printContentResolver(cursor);
            startActivity(intent);

        } catch (Exception e) {
            Log.e(Constants.LOG_LABEL, "problem finding form file", e);
        }
    }

    private void printContentResolver(Cursor cursor) {
        for(cursor.moveToFirst();!cursor.isAfterLast();cursor.moveToNext())
        {
            System.out.println("===========================================");
            String[] columnNames = cursor.getColumnNames();
            for (int index = 0; index < columnNames.length; ++index) {
                System.out.println("---------------------------COLUMN name = " + columnNames[index]);

                int nameColumn = cursor.getColumnIndex(columnNames[index]);

                String name = cursor.getString(nameColumn);
                System.out.println("============================ COL Value = " + name);
            }
            System.out.println("===========================================");
        }
    }
}
