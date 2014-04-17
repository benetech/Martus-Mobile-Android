package org.martus.android;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

import org.martus.android.dialog.AddContactActivity;

/**
 * Created by nimaa on 4/11/14.
 */
public class ContactImportChoiceActivity extends BaseActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.contact_import_choice);
        TextView addContactFromFileLinkTextView = (TextView) findViewById(R.id.addContactFromFileLink);
        addContactFromFileLinkTextView.setMovementMethod(LinkMovementMethod.getInstance());
        makeTextViewHyperlink(addContactFromFileLinkTextView);
        addContactFromFileLinkTextView.setOnClickListener(new TextViewClickHandler());
    }

    public void addContactUsingAccessToken(View view){
        Intent intent = new Intent(ContactImportChoiceActivity.this, AddContactActivity.class);
        startActivityForResult(intent, EXIT_REQUEST_CODE);
        finish();
    }

    private void makeTextViewHyperlink(TextView textView) {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        stringBuilder.append(textView.getText());
        stringBuilder.setSpan(new URLSpan("#"), 0, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(stringBuilder, TextView.BufferType.SPANNABLE);
    }

    private class TextViewClickHandler implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(ContactImportChoiceActivity.this, DesktopKeyActivity.class);
            startActivityForResult(intent, EXIT_REQUEST_CODE);
            finish();
        }
    }
}
