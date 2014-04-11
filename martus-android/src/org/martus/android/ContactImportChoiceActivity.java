package org.martus.android;

import android.os.Bundle;
import android.view.View;

/**
 * Created by nimaa on 4/11/14.
 */
public class ContactImportChoiceActivity extends BaseActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.contact_import_choice);
    }

    @Override
    public void onResume() {
        super.onResume();

        System.out.println("ON RESUME");
    }

    public void addContactFromFile(View view){
        System.out.println("ADD CONTACT FROM FILE");
    }
}
