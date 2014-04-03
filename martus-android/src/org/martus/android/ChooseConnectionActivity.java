package org.martus.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.martus.clientside.MobileClientSideNetworkGateway;
import org.martus.clientside.MobileClientSideNetworkHandlerUsingXmlRpcForNonSSL;
import org.martus.common.MartusUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.network.NetworkResponse;
import org.martus.common.network.NonSSLNetworkAPI;
import org.martus.util.StreamableBase64;

import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by nimaa on 3/31/14.
 */
public class ChooseConnectionActivity extends AbstractServerActivity {

    private Button useDefaultServer;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_connection);
    }

    public void useAdvancedServerOptions(View view) {
        Intent intent = new Intent(ChooseConnectionActivity.this, ServerActivity.class);
        startActivityForResult(intent, EXIT_REQUEST_CODE);
        finish();
    }

    public void useDefaultServer(View view) throws Exception {
        if ((getServerIp().length() < 7) || (! validate(getServerIp()))) {
            System.out.println(R.string.invalid_server_ip);
            return;
        }

        String serverCode = getServerPublicCode();
        if (serverCode.length() < ServerActivity.MIN_SERVER_CODE) {
            System.out.println(R.string.invalid_server_code);
            return;
        }

        if (getMagicWord().isEmpty()) {
            System.out.println(R.string.error_message);
            System.out.println(R.string.invalid_magic_word);
            return;
        }

        showProgressDialog(getString(R.string.progress_connecting_to_server));

        NonSSLNetworkAPI server = null;
        try
        {
            server = new MobileClientSideNetworkHandlerUsingXmlRpcForNonSSL(getServerIp(), ((MartusApplication)getApplication()).getTransport());
        } catch (Exception e)
        {
            Log.e(AppConfig.LOG_LABEL, "problem creating client side network handler using xml for non ssl", e);
            return;
        }
        MartusSecurity martusCrypto = AppConfig.getInstance().getCrypto();

        final AsyncTask <Object, Void, Vector> keyTask = new PublicKeyTask();
        keyTask.execute(server, martusCrypto);
    }

    public static boolean validate(final String ip) {
        Pattern pattern = Pattern.compile(ServerActivity.IP_ADDRESS_PATTERN);
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }

    @Override
    public void onResume() {
        super.onResume();

       //NOTE need to do anything here?
    }

    private String getServerIp(){
        return "54.213.152.140";
    }

    private String getServerPublicCode() {
        return "23364357724534822212";
    }

    private void processResult(Vector serverInformation) {
        dialog.dismiss();
        if (! NetworkUtilities.isNetworkAvailable(this)) {
            System.out.println(getString(R.string.no_network_connection));
            System.out.println(getString(R.string.error_message));
            return;
        }
        try {
            if (null == serverInformation || serverInformation.isEmpty()) {
                System.out.println(getString(R.string.invalid_server_info));
                System.out.println(getString(R.string.error_message));
                return;
            }
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "Problem getting server public key", e);
            System.out.println(getString(R.string.error_getting_server_key));
            System.out.println(getString(R.string.error_message));

            return;
        }

        String serverPublicKey = (String)serverInformation.get(1);
        try {
            if (confirmServerPublicKey(getServerPublicCode(), serverPublicKey)) {
                SharedPreferences serverSettings = getSharedPreferences(PREFS_SERVER_IP, MODE_PRIVATE);
                SharedPreferences.Editor editor = serverSettings.edit();
                editor.putString(SettingsActivity.KEY_SERVER_IP, getServerIp());
                editor.putString(SettingsActivity.KEY_SERVER_PUBLIC_KEY, serverPublicKey);
                editor.commit();

                SharedPreferences.Editor magicWordEditor = mySettings.edit();
                magicWordEditor.putBoolean(SettingsActivity.KEY_HAVE_UPLOAD_RIGHTS, false);
                magicWordEditor.commit();
                Toast.makeText(this, getString(R.string.successful_server_choice), Toast.LENGTH_SHORT).show();

                File serverIpFile = getPrefsFile(PREFS_SERVER_IP);
                MartusUtilities.createSignatureFileFromFile(serverIpFile, getSecurity());

                showProgressDialog(getString(R.string.progress_confirming_magic_word));
                final AsyncTask<Object, Void, NetworkResponse> rightsTask = new UploadRightsTask();
                rightsTask.execute(getNetworkGateway(), martusCrypto, getMagicWord());

            } else {
                System.out.println(getString(R.string.invalid_server_code));
                System.out.println(getString(R.string.error_message));
            }
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL,"problem processing server IP", e);
        }
    }

    private String getMagicWord() {
        return "martus";
    }

    private boolean confirmServerPublicKey(String serverCode, String serverPublicKey) throws StreamableBase64.InvalidBase64Exception {
        final String normalizedPublicCode = MartusCrypto.removeNonDigits(serverCode);
        final String computedCode;
        computedCode = MartusCrypto.computePublicCode(serverPublicKey);
        return normalizedPublicCode.equals(computedCode);
    }

    private void saveServerConnectionData() {
        SharedPreferences serverSettings = getSharedPreferences(PREFS_SERVER_IP, MODE_PRIVATE);
        SharedPreferences.Editor editor = serverSettings.edit();
        editor.putString(SettingsActivity.KEY_SERVER_IP, getServerIp());
        editor.putString(SettingsActivity.KEY_SERVER_PUBLIC_KEY, getServerPublicCode());
        editor.commit();
    }

    private void processMagicWordResponse(NetworkResponse response) {
        dialog.dismiss();
        try {
            if (!response.getResultCode().equals(NetworkInterfaceConstants.OK)) {
                Toast.makeText(this, getString(R.string.no_upload_rights), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.success_magic_word), Toast.LENGTH_LONG).show();
                SharedPreferences.Editor editor = mySettings.edit();
                editor.putBoolean(SettingsActivity.KEY_HAVE_UPLOAD_RIGHTS, true);
                editor.commit();
                this.finish();
            }
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "Problem verifying upload rights", e);
            Toast.makeText(this, getString(R.string.problem_confirming_magic_word), Toast.LENGTH_SHORT).show();
        }
    }

    private class UploadRightsTask extends AsyncTask<Object, Void, NetworkResponse> {
        @Override
        protected NetworkResponse doInBackground(Object... params) {

            final MobileClientSideNetworkGateway gateway = (MobileClientSideNetworkGateway)params[0];
            final MartusSecurity signer = (MartusSecurity)params[1];
            final String magicWord = (String)params[2];

            NetworkResponse result = null;

            try {
                result = gateway.getUploadRights(signer, magicWord);
            } catch (MartusCrypto.MartusSignatureException e) {
                Log.e(AppConfig.LOG_LABEL, "problem getting upload rights", e);
            }

            return result;
        }

        @Override
        protected void onPostExecute(NetworkResponse result) {
            super.onPostExecute(result);
            processMagicWordResponse(result);
        }
    }

    private class PublicKeyTask extends AsyncTask<Object, Void, Vector> {
        @Override
        protected Vector doInBackground(Object... params) {

            final NonSSLNetworkAPI server = (NonSSLNetworkAPI)params[0];
            final MartusSecurity security = (MartusSecurity)params[1];
            Vector result = null;

            result = server.getServerInformation();

            return result;
        }

        @Override
        protected void onPostExecute(Vector result) {
            super.onPostExecute(result);
            processResult(result);
        }
    }

}
