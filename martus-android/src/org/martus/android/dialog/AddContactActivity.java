package org.martus.android.dialog;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.martus.android.AppConfig;
import org.martus.android.BaseActivity;
import org.martus.android.R;
import org.martus.android.SettingsActivity;
import org.martus.clientside.MobileClientSideNetworkGateway;
import org.martus.common.Exceptions;
import org.martus.common.MartusAccountAccessToken;
import org.martus.common.MartusUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.network.NetworkResponse;

import java.io.File;
import java.util.Vector;

/**
 * Created by nimaa on 4/3/14.
 */
public class AddContactActivity extends BaseActivity {

    private EditText accessTokenTextField;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.add_contact);

        accessTokenTextField = (EditText) findViewById(R.id.access_token_text_field);
    }

    public void addContactFromServer(View view){

        String code = accessTokenTextField.getText().toString().trim();
        if (code.isEmpty()) {
            accessTokenTextField.requestFocus();
            showMessage(this, getString(R.string.public_code_validation_empty), getString(R.string.error_message));

            return;
        }

        showProgressDialog("Retrieving contact information.");
        getPublicKeyFromServer(code);
    }

    public String getPublicKeyFromServer(String code) {

        final AsyncTask <Object, Void, NetworkResponse> keyTask = new RetrieveAccountTask();
        keyTask.execute(code);

        return null;
    }

    private void processResult(NetworkResponse response) {
        try
        {
            dismissProgressDialog();
            if(!response.getResultCode().equals(NetworkInterfaceConstants.OK))
            {
                if(response.getResultCode().equals(NetworkInterfaceConstants.NO_TOKEN_AVAILABLE))
                    throw new MartusAccountAccessToken.TokenNotFoundException();

                throw new Exceptions.ServerNotAvailableException();
            }

            Vector<String> singleAccountId = response.getResultVector();
            if (singleAccountId == null || singleAccountId.isEmpty()){
                Log.e(AppConfig.LOG_LABEL, "Server response was empty");
            }

            String accountId = singleAccountId.get(0);
            setPublicKey(accountId);

        } catch (Exceptions.ServerNotAvailableException e) {
            Log.e(AppConfig.LOG_LABEL, "Server Not Available", e);
            showErrorMessage(getString(R.string.error_getting_server_key), getString(R.string.error_message));
        }
        catch (MartusAccountAccessToken.TokenNotFoundException e){
            Log.e(AppConfig.LOG_LABEL, "Token not found.", e);
            showErrorMessage(getString(R.string.error_getting_server_key), getString(R.string.error_message));
        }
        catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "Exception retrieving account", e);
            showErrorMessage(getString(R.string.error_retrieving_contact), getString(R.string.error_message));
        }
    }

    private void setPublicKey(String publicKey) throws Exception {
        SharedPreferences HQSettings = getSharedPreferences(PREFS_DESKTOP_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = HQSettings.edit();

        editor.putString(SettingsActivity.KEY_DESKTOP_PUBLIC_KEY, publicKey);
        editor.commit();

        File desktopKeyFile = getPrefsFile(PREFS_DESKTOP_KEY);
        MartusUtilities.createSignatureFileFromFile(desktopKeyFile, getSecurity());
        Toast.makeText(this, getString(R.string.success_import_hq_key), Toast.LENGTH_LONG).show();
        finish();
    }

    protected class RetrieveAccountTask extends AsyncTask<Object, Void, NetworkResponse> {
        @Override
        protected NetworkResponse doInBackground(Object... params) {

            try
            {
                String userEnteredAccessToken = accessTokenTextField.getText().toString().trim();
                SharedPreferences serverSettings = getSharedPreferences(PREFS_SERVER_IP, MODE_PRIVATE);
                String serverIP = serverSettings.getString(SettingsActivity.KEY_SERVER_IP, "");
                MobileClientSideNetworkGateway gateway = getNetworkGateway();
                MartusSecurity martusCrypto = AppConfig.getInstance().getCrypto();
                MartusAccountAccessToken accessToken = new MartusAccountAccessToken(userEnteredAccessToken);
                NetworkResponse response = gateway.getMartusAccountIdFromAccessToken(martusCrypto, accessToken);

                return response;
            }
            catch (Exception e){
                Log.e(AppConfig.LOG_LABEL, "Server connection failed!", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(NetworkResponse result) {
            super.onPostExecute(result);

            processResult(result);
        }
    }
}
