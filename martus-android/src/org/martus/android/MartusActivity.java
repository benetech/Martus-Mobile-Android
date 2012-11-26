package org.martus.android;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.clientside.ClientSideNetworkHandlerUsingXmlRpcForNonSSL;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.network.NetworkResponse;
import org.martus.common.network.NonSSLNetworkAPI;
import org.martus.util.StreamableBase64;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MartusActivity extends Activity {

	public static final String defaultServerIP = "54.245.101.104"; //public QA server
    public static final String defaultServerPublicCode = "8714.7632.8884.7614.8217";
    public static final String defaultMagicWord = "spam";
    private String serverPublicKey;

    private MartusSecurity martusCrypto;
    private static Activity myActivity;
    private ClientSideNetworkGateway gateway = null;
    private String serverIP;
    private String serverPublicCode;

    DialogFragment newAccountDialog;

    final int ACTIVITY_DESKTOP_KEY = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        myActivity = this;
        updateSettings();

        martusCrypto = AppConfig.getInstance().getCrypto();
        if (!martusCrypto.hasKeyPair()) {
            if (isAccountCreated()) {
                showLoginDialog();
            } else {
                showCreateAccountDialog();
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        if (martusCrypto.hasKeyPair()) {
            checkDesktopKey();
            confirmServerPublicKey();
        }
        updateSettings();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         if (requestCode == ACTIVITY_DESKTOP_KEY) {
            confirmServerPublicKey();

             //todo: will need to happen whenever server ip changes (putting here for now)
             try {
                 final AsyncTask<Object, Void, NetworkResponse> rightsTask = new UploadRightsTask().execute(gateway, martusCrypto, defaultMagicWord);
                 final NetworkResponse response = rightsTask.get();
                 if (!response.getResultCode().equals("ok")) {
                     showMessage(myActivity, "Don't have upload rights!", "Error");
                 }
             } catch (Exception e) {
                 Log.e(AppConfig.LOG_LABEL, "Problem verifying upload rights");
             }
         }
    }

    public void verifyServer(View view) {
        try {
            //Network calls must be made in background task
            final AsyncTask<ClientSideNetworkGateway, Void, NetworkResponse> infoTask = new ServerInfoTask().execute(gateway);
            NetworkResponse response1 = infoTask.get();

            Object[] resultArray = response1.getResultArray();
            final TextView responseView = (TextView)findViewById(R.id.response_server);
            responseView.setText("ServerInfo: " + response1.getResultCode() + ", " + resultArray[0]);
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "Failed getting server info", e);
            e.printStackTrace();
        }
    }

    public void sendBulletin(View view) {
        Intent intent = new Intent(MartusActivity.this, BulletinActivity.class);
        startActivity(intent);
    }

    public void getBulletinCount(View view) {
        try {
            final AsyncTask<Object, Void, String> getIdsTask = new GetDraftBulletinsTask().execute(gateway, martusCrypto, martusCrypto.getPublicKeyString());
            String response = getIdsTask.get();

            final TextView responseView = (TextView)findViewById(R.id.check_bulletins_text);
            responseView.setText(response);
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "Failed getting bulletin count", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;

        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings_menu_item:
                intent = new Intent(MartusActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.ping_menu_item:
                intent = new Intent(MartusActivity.this, PingServer.class);
                startActivity(intent);
                return true;
            case R.id.quit_menu_item:
                martusCrypto.clearKeyPair();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {

        String filePath = intent.getStringExtra(BulletinActivity.EXTRA_ATTACHMENT);
        if (null != filePath) {
            Intent bulletinIntent = new Intent(MartusActivity.this, BulletinActivity.class);
            bulletinIntent.putExtra(BulletinActivity.EXTRA_ATTACHMENT, filePath);
            startActivity(bulletinIntent);
        }
    }

    private void confirmServerPublicKey() {
        //Not sure if this is the best place to get/set Server Public Key
        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(this);
        serverPublicKey = mySettings.getString(SettingsActivity.KEY_SERVER_PUBLIC_KEY, "");
        if (serverPublicKey.length() < 1) {
            //Network calls must be made in background task
            NonSSLNetworkAPI server = new ClientSideNetworkHandlerUsingXmlRpcForNonSSL(serverIP);
            final AsyncTask<Object, Void, String> keyTask = new PublicKeyTask().execute(server, martusCrypto);
            try {
                serverPublicKey = keyTask.get();
                SharedPreferences.Editor editor = mySettings.edit();
                editor.putString(SettingsActivity.KEY_SERVER_PUBLIC_KEY, serverPublicKey);
                editor.commit();
            } catch (Exception e) {
                Log.e(AppConfig.LOG_LABEL, "Problem getting server public key", e);
                showMessage(this, "Problem getting server public key", "Error");
                return;
            }
        }

        //confirm serverPublicKey is correct
        final String normalizedPublicCode = MartusCrypto.removeNonDigits(serverPublicCode);
        final String computedCode;
        try {
            computedCode = MartusCrypto.computePublicCode(serverPublicKey);
            if (! normalizedPublicCode.equals(computedCode)) {
                showMessage(myActivity, "Invalid server public code! Please fix in Settings screen.", "Error");
            }
        } catch (StreamableBase64.InvalidBase64Exception e) {
            Log.e(AppConfig.LOG_LABEL,"problem computing public code", e);
            showMessage(myActivity, "Problem computing public code", "Error");
            return;
        }

        gateway = ClientSideNetworkGateway.buildGateway(serverIP, serverPublicKey);
    }

    private void checkDesktopKey() {
        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(this);
        String desktopPublicKeyString = mySettings.getString(SettingsActivity.KEY_DESKTOP_PUBLIC_KEY, "");
        if (desktopPublicKeyString.length() < 1) {
            Intent intent = new Intent(MartusActivity.this, DesktopKeyActivity.class);
            startActivityForResult(intent, ACTIVITY_DESKTOP_KEY);
        }
    }

    private boolean isAccountCreated() {
        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(MartusActivity.this);

        // attempt to read keypair from prefs
        String keyPairString = mySettings.getString(SettingsActivity.KEY_KEY_PAIR, "");
        return keyPairString.length() > 1;
    }

    private boolean confirmAccount(char[] password)  {

        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(MartusActivity.this);
        String keyPairString = mySettings.getString(SettingsActivity.KEY_KEY_PAIR, "");

        // construct keypair from value read from prefs
        byte[] decodedKeyPair = Base64.decode(keyPairString, Base64.NO_WRAP);
        InputStream is = new ByteArrayInputStream(decodedKeyPair);
        try {
            martusCrypto.readKeyPair(is, password);
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "Problem confirming password", e);
            return false;
        }
        return true;
    }

    private void createAccount(char[] password)  {
        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(MartusActivity.this);
        // create new keypair and store in prefs
        martusCrypto.createKeyPair();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            martusCrypto.writeKeyPair(out, password);
            out.close();
            byte[] keyPairData = out.toByteArray();

            // write keypair to prefs
            // need to first base64 encode so we can write to prefs
            String encodedKeyPair = Base64.encodeToString(keyPairData, Base64.NO_WRAP);

            // write to prefs
            SharedPreferences.Editor editor = mySettings.edit();
            editor.putString(SettingsActivity.KEY_KEY_PAIR, encodedKeyPair);
            editor.commit();
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "Problem creating account", e);
            showMessage(MartusActivity.this, "Problem creating account", "Error");
        }
    }

    private void updateSettings() {
        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(this);
        serverIP = mySettings.getString(SettingsActivity.KEY_SERVER_IP, defaultServerIP);
        serverPublicCode = mySettings.getString(SettingsActivity.KEY_SERVER_PUBLIC_CODE, defaultServerPublicCode);
    }

    public static void showMessage(Context context, String msg, String title){
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setIcon(android.R.drawable.ic_dialog_alert)
             .setTitle(title)
             .setMessage(msg)
             .show();
    }

    void showLoginDialog() {
        DialogFragment loginDialog = LoginDialogFragment.newInstance();
        loginDialog.show(getFragmentManager(), "login");
    }

    public void doLoginPositiveClick(EditText passwordText) {
        char[] password = passwordText.getText().toString().trim().toCharArray();
        boolean confirmed = confirmAccount(password);
        if (!confirmed) {
            MartusActivity.this.finish();
        }

        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(MartusActivity.this);
        serverPublicKey = mySettings.getString(SettingsActivity.KEY_SERVER_PUBLIC_KEY, "");
        gateway = ClientSideNetworkGateway.buildGateway(serverIP, serverPublicKey);
    }

    public void doLoginNegativeClick() {
        this.finish();
    }

    public static class LoginDialogFragment extends DialogFragment {

        public static LoginDialogFragment newInstance() {
            LoginDialogFragment frag = new LoginDialogFragment();
            Bundle args = new Bundle();
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater factory = LayoutInflater.from(myActivity);
            final View passwordEntryView = factory.inflate(R.layout.password_dialog, null);
            final EditText passwordText = (EditText) passwordEntryView.findViewById(R.id.password_edit);
            return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.password_dialog_title)
                .setView(passwordEntryView)
                .setPositiveButton(R.string.alert_dialog_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ((MartusActivity) getActivity()).doLoginPositiveClick(passwordText);
                            }
                        }
                )
                .setNegativeButton(R.string.password_dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ((MartusActivity) getActivity()).doLoginNegativeClick();
                            }
                        }
                )
                .create();
        }
    }

    void showCreateAccountDialog() {
        newAccountDialog = CreateAccountDialogFragment.newInstance();
        newAccountDialog.show(getFragmentManager(), "create");
    }

    public void doCreateAccountPositiveClick(EditText passwordText) {
        char[] password = passwordText.getText().toString().trim().toCharArray();
        createAccount(password);
        checkDesktopKey();
        newAccountDialog.dismiss();
    }

    public void doCreateAccountNegativeClick() {
        this.finish();
    }

    public static class CreateAccountDialogFragment extends DialogFragment {

            public static CreateAccountDialogFragment newInstance() {
                CreateAccountDialogFragment frag = new CreateAccountDialogFragment();
                Bundle args = new Bundle();
                frag.setArguments(args);
                return frag;
            }

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                LayoutInflater factory = LayoutInflater.from(myActivity);
                final View createAccountDialog = factory.inflate(R.layout.create_account, null);
                final EditText newPasswordText = (EditText) createAccountDialog.findViewById(R.id.new_password_field);
                return new AlertDialog.Builder(getActivity())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.create_account_dialog_title)
                    .setView(createAccountDialog)
                    .setPositiveButton(R.string.alert_dialog_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ((MartusActivity) getActivity()).doCreateAccountPositiveClick(newPasswordText);
                                }
                            }
                    )
                    .setNegativeButton(R.string.password_dialog_cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ((MartusActivity) getActivity()).doCreateAccountNegativeClick();
                                }
                            }
                    )
                    .create();
            }
        }
    
}