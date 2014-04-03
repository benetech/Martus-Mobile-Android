package org.martus.android;

import android.os.AsyncTask;
import android.util.Log;

import org.martus.clientside.MobileClientSideNetworkGateway;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.network.NetworkResponse;

/**
 * Created by nimaa on 4/3/14.
 */
abstract public class AbstractServerActivity extends BaseActivity{
    public static final int MIN_SERVER_CODE = 20;
    public static final String SERVER_INFO_FILENAME = "Server.mmsi";
    public static final String IP_ADDRESS_PATTERN =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    protected static final int MIN_SERVER_IP = 7;


    abstract protected void processMagicWordResponse(NetworkResponse result);

    protected class UploadRightsTask extends AsyncTask<Object, Void, NetworkResponse> {
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
}
