package org.martus.android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.martus.common.MartusUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.network.NetworkInterfaceXmlRpcConstants;
import org.martus.util.StreamableBase64;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

/**
 * Created by nimaa on 4/29/14.
 */
abstract public class AbstractMainActivityWithMainMenuHandler extends AbstractTorActivity {

    public static final String ACCOUNT_ID_FILENAME = "Mobile_Public_Account_ID.mpi";
    private static final String SERVER_COMMAND_PREFIX = "MartusServer.";
    private final static String pingPath = "/RPC2";

    protected String serverPublicKey;
    protected String serverIP;

    @Override
    public void onResume() {
        super.onResume();

        synchronizeTorSwitchWithCurrentSystemProperties();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.main, menu);
        setTorToggleButton((CompoundButton)  menu.findItem(R.id.tor_button).getActionView());
        getTorToggleButton().setOnCheckedChangeListener(new AbstractTorActivity.TorToggleChangeHandler());
        getTorToggleButton().setText(R.string.tor_label);
        synchronizeTorSwitchWithCurrentSystemProperties();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;

        int id = item.getItemId();
        if (id == R.id.settings_menu_item) {
            intent = new Intent(AbstractMainActivityWithMainMenuHandler.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.quit_menu_item) {
            if (!MartusApplication.isIgnoreInactivity()) {
                logout();
                finish();
            } else {
                showMessage(this, getString(R.string.logout_while_sending_message),
                        getString(R.string.logout_while_sending_title));
            }
            return true;
        } else if (id == R.id.ping_server_menu_item) {
            pingServer();
            return true;
        } else if (id == R.id.resend_menu_item) {
            resendFailedBulletins();
            return true;
        } else if (id == R.id.view_public_code_menu_item) {
            showPublicKeyDialog();
            return true;
        } else if (id == R.id.view_access_token_menu_item) {
            showAccessToken();
        } else if (id == R.id.reset_install_menu_item) {
            if (!MartusApplication.isIgnoreInactivity()) {
                showConfirmationDialog();
            } else {
                showMessage(this, getString(R.string.logout_while_sending_message),
                        getString(R.string.reset_while_sending_title));
            }
            return true;
        } else if (id == R.id.show_version_menu_item) {
            showVersionNumberAsToast();
            return true;
        } else if (id == R.id.export_mpi_menu_item) {
            File mpiFile = getMpiFile();
            showMessage(this, mpiFile.getAbsolutePath(), getString(R.string.exported_account_id_file_confirmation));
            return true;
        } else if (id == R.id.email_mpi_menu_item) {
            sendAccountIDAsEmail();
            return true;
        } else if (id == R.id.send_mpi_menu_item_via_bulletin) {
            sendAccountIDAsBulletin();
            return true;
        } else if (id == R.id.feedback_menu_item) {
            showContactUs();
            return true;
        } else if (id == R.id.view_docs_menu_item) {
            showViewDocs();
            return true;
        } else if (id == R.id.view_tor_message_menu_item) {
            showTorMessage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAccessToken() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_DESKTOP_KEY, MODE_PRIVATE);
        String accessToken = sharedPreferences.getString(SettingsActivity.KEY_ACCESS_TOKEN, "");
        showMessage(this, accessToken, getString(R.string.access_token));
    }

    private void showPublicKeyDialog() {
        try {

            String keyPairString = mySettings.getString(SettingsActivity.KEY_KEY_PAIR, "");
            String publicCode40Digit = MartusCrypto.computeFormattedPublicCode40(keyPairString);
            String publicCode = MartusCrypto.getFormattedPublicCode(keyPairString);

            String newPublicCodeMessageSection = getString(R.string.view_new_public_code_message, publicCode40Digit);
            String oldPublicCodeMessageSection = getString(R.string.view_old_public_code_message, publicCode);
            String entireMessage = newPublicCodeMessageSection + "\n\n" + oldPublicCodeMessageSection;

            showMessage(this, entireMessage, getString(R.string.view_public_code_dialog_title));
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "couldn't get public code", e);
            showMessage(this, getString(R.string.view_public_code_dialog_error), getString(R.string.view_public_code_dialog_title));
        }
    }

    private void showVersionNumberAsToast() {
        PackageInfo pInfo;
        String versionLabel;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionNameLabel = getString(R.string.version_name_label, pInfo.versionName);
            String versionCodeLabel = getString(R.string.version_code_label, pInfo.versionCode);
            versionLabel = versionNameLabel + "\n" + versionCodeLabel;
        } catch (PackageManager.NameNotFoundException e) {
            versionLabel = "?";
        }
        Toast.makeText(this, versionLabel, Toast.LENGTH_LONG).show();
    }

    public static void logout() {
        AppConfig.getInstance().getCrypto().clearKeyPair();
    }

    private void sendAccountIDAsEmail()
    {
        File mpiFile = getMpiFile();
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/plain");
        Uri uri = Uri.parse("file://" + mpiFile.getAbsolutePath());
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(emailIntent, "Send email..."));
    }

    private void sendAccountIDAsBulletin()
    {
        File mpiFile = getMpiFile();
        String filePath = mpiFile.getPath();
        Intent bulletinIntent = new Intent(this, BulletinActivity.class);
        bulletinIntent.putExtra(BulletinActivity.EXTRA_ATTACHMENT, filePath);
        startActivity(bulletinIntent);
    }

    protected File getMpiFile()
    {
        File externalDir;
        File mpiFile;
        externalDir = Environment.getExternalStorageDirectory();
        mpiFile = new File(externalDir, ACCOUNT_ID_FILENAME);
        try {
            exportPublicInfo(mpiFile);
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "couldn't export public id", e);
            showMessage(this, getString(R.string.export_public_account_id_dialog_error),
                    getString(R.string.export_public_account_id_dialog_title));
        }
        return mpiFile;
    }

    private void showTorMessage() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.view_tor_help_message, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.tor_label)
                .setView(view)
                .setPositiveButton(R.string.alert_dialog_ok, new SimpleOkayButtonListener())
                .show();
    }

    private void showContactUs()
    {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.contact_us, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setIcon(android.R.drawable.ic_dialog_email)
                .setTitle(R.string.feedback_dialog_title)
                .setView(view)
                .setPositiveButton(R.string.alert_dialog_ok, new SimpleOkayButtonListener())
                .show();
    }

    private void showViewDocs()
    {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.view_docs, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.view_docs_menu)
                .setView(view)
                .setPositiveButton(R.string.alert_dialog_ok, new SimpleOkayButtonListener())
                .show();
    }

    private void exportPublicInfo(File exportFile) throws IOException,
            StreamableBase64.InvalidBase64Exception,
            MartusCrypto.MartusSignatureException {
        MartusUtilities.exportClientPublicKey(getSecurity(), exportFile);
    }

    private void pingServer() {
        if (! NetworkUtilities.isNetworkAvailable(this)) {
            Toast.makeText(this, getString(R.string.no_network_connection), Toast.LENGTH_LONG).show();
            return;
        }
        showProgressDialog(getString(R.string.progress_connecting_to_server));
        try {
            String pingUrl = "http://" + serverIP + pingPath;
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(pingUrl));
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);

            final AsyncTask<XmlRpcClient, Void, String> pingTask = new PingTask();
            pingTask.execute(client);
        } catch (MalformedURLException e) {
            // do nothing
        }
    }

    private void resendFailedBulletins()
    {
        int count = getNumberOfUnsentBulletins();
        if (count < 1) {
            Toast.makeText(this, getString(R.string.resending_no_bulletins), Toast.LENGTH_LONG).show();
            return;
        }
        if (!NetworkUtilities.isNetworkAvailable(this)) {
            Toast.makeText(this, getString(R.string.resending_no_network), Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, getString(R.string.resending), Toast.LENGTH_LONG).show();
        Intent resendService = new Intent(AbstractMainActivityWithMainMenuHandler.this, ResendService.class);
        resendService.putExtra(SettingsActivity.KEY_SERVER_IP, serverIP);
        resendService.putExtra(SettingsActivity.KEY_SERVER_PUBLIC_KEY, serverPublicKey);
        startService(resendService);
    }

    protected int getNumberOfUnsentBulletins() {
        int pendingBulletins;
        final File unsentBulletinsDir = getAppDir();
        final String[] sendingBulletinNames = unsentBulletinsDir.list(new ZipFileFilter());
        pendingBulletins = sendingBulletinNames.length;

        File failedDir = new File (unsentBulletinsDir, UploadBulletinTask.FAILED_BULLETINS_DIR);
        if (failedDir.exists()) {
            final String[] failedBulletins = failedDir.list(new ZipFileFilter());
            pendingBulletins += failedBulletins.length;
        }
        return pendingBulletins;
    }

    private void processPingResult(String result) {
        dismissProgressDialog();
        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
    }

    public class CancelSendButtonListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            //do nothing
        }
    }

    private class PingTask extends AsyncTask<XmlRpcClient, Void, String> {
        @Override
        protected String doInBackground(XmlRpcClient... clients) {

            final Vector params = new Vector();
            final XmlRpcClient client = clients[0];
            String result = getString(R.string.ping_result_ok);
            try {
                client.execute(SERVER_COMMAND_PREFIX + NetworkInterfaceXmlRpcConstants.CMD_PING, params);
            } catch (XmlRpcException e) {
                Log.e(AppConfig.LOG_LABEL, "Ping failed", e);
                result = getString(R.string.ping_result_down);
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            processPingResult(result);
        }
    }
}
