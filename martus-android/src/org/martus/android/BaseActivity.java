package org.martus.android;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import org.martus.android.dialog.ConfirmationDialog;
import org.martus.android.dialog.InstallExplorerDialog;
import org.martus.android.dialog.LoginDialog;
import org.martus.android.dialog.LoginRequiredDialog;
import org.martus.common.MartusUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusKeyPair;
import org.martus.common.crypto.MartusSecurity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.util.Base64;
import android.util.Log;
import com.actionbarsherlock.app.SherlockFragmentActivity;
//import com.bugsense.trace.BugSenseHandler;

/**
 * @author roms
 *         Date: 12/19/12
 */
public class BaseActivity extends SherlockFragmentActivity implements ConfirmationDialog.ConfirmationDialogListener,
        LoginRequiredDialog.LoginRequiredDialogListener {

    private static final long MINUTE_MILLIS = 60000;


    public static final int EXIT_RESULT_CODE = 10;
    public static final int EXIT_REQUEST_CODE = 10;
    public static final String PREFS_DESKTOP_KEY = "desktopHQ";
    public static final String PREFS_SERVER_IP = "serverIP";
    protected static final String PREFS_DIR = "shared_prefs";
	private static final String LOGIN_DIALOG_TAG = "dlg_login";
	static final int MIN_PASSWORD_SIZE = 8;

    protected MartusApplication parentApp;
    private String confirmationDialogTitle;
    protected ProgressDialog dialog;
    SharedPreferences mySettings;

    private static Handler inactivityHandler;
    private LogOutProcess inactivityCallback;
    private static long inactivityTimeout;
	MartusSecurity martusCrypto;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Comment out for production build
        //BugSenseHandler.initAndStartSession(this, ExternalKeys.BUGSENSE_KEY);
        parentApp = (MartusApplication) this.getApplication();
        confirmationDialogTitle = getString(R.string.confirm_default);
        mySettings = PreferenceManager.getDefaultSharedPreferences(this);
	    initInactivityHandler();
	    martusCrypto = AppConfig.getInstance().getCrypto();
    }

	private void initInactivityHandler() {
		if (inactivityHandler == null)
			inactivityHandler = new EmptyHandler();
		if (inactivityCallback == null) {
			inactivityCallback = new LogOutProcess(this);
		}
		int timeoutSetting = Integer.valueOf(mySettings.getString(SettingsActivity.KEY_TIMEOUT_MINUTES, SettingsActivity.DEFAULT_TIMEOUT_MINUTES));
        setTimeout(timeoutSetting);
	}

    public void resetInactivityTimer(){
	    stopInactivityTimer();
        if (!MartusApplication.isIgnoreInactivity()) {
            inactivityHandler.postDelayed(inactivityCallback, inactivityTimeout);
        } else {
            Log.i(AppConfig.LOG_LABEL, "is ignore in resetInactivityTimer");
        }
    }

    public void stopInactivityTimer(){
        Log.d(AppConfig.LOG_LABEL, "start stopInactivityTimer");
        inactivityHandler.removeCallbacksAndMessages(null);
    }

    public void showLoginRequiredDialog() {
        LoginRequiredDialog loginRequiredDialog = LoginRequiredDialog.newInstance();
        loginRequiredDialog.show(getSupportFragmentManager(), "dlg_login");
    }

	void showLoginDialog() {
        DialogFragment dialogFragment = (DialogFragment)getSupportFragmentManager().findFragmentByTag(LOGIN_DIALOG_TAG);
        if (dialogFragment != null) {
            dialogFragment.dismiss();
        }

        LoginDialog loginDialog = LoginDialog.newInstance();
        loginDialog.show(getSupportFragmentManager(), LOGIN_DIALOG_TAG);
    }

	boolean confirmAccount(char[] password)  {

        String keyPairString = mySettings.getString(SettingsActivity.KEY_KEY_PAIR, "");

        // construct keypair from value read from prefs
        byte[] decodedKeyPair = Base64.decode(keyPairString, Base64.NO_WRAP);
        InputStream is = new ByteArrayInputStream(decodedKeyPair);
        try {
            martusCrypto.readKeyPair(is, password);
        } catch (Exception e) {
            //Log.e(AppConfig.LOG_LABEL, "Problem confirming password", e);
            return false;
        }

        martusCrypto.setShouldWriteAuthorDecryptableData(false);
        return true;
    }

	public void onCancelPasswordDialog() {
        this.finish();
    }

    public void onFinishLoginRequiredDialog() {
        BaseActivity.this.finish();
        Intent intent = new Intent(BaseActivity.this, MartusActivity.class);
        intent.putExtras(getIntent());
        intent.putExtra(MartusActivity.RETURN_TO, MartusActivity.ACTIVITY_BULLETIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    /**
     *
     * @param timeoutSettingInMinutes number of minutes before timeout
     */
    public static void setTimeout(int timeoutSettingInMinutes) {
        BaseActivity.inactivityTimeout = timeoutSettingInMinutes * MINUTE_MILLIS;
    }

    public void showInstallExplorerDialog() {
        InstallExplorerDialog explorerDialog = InstallExplorerDialog.newInstance();
        explorerDialog.show(getSupportFragmentManager(), "dlg_install");
    }

    public void showConfirmationDialog() {
        ConfirmationDialog confirmationDialog = ConfirmationDialog.newInstance();
        confirmationDialog.show(getSupportFragmentManager(), "dlg_confirmation");
    }

    public void onConfirmationAccepted() {
        //do nothing
    }

    public void onConfirmationCancelled() {
        //do nothing
    }

    @Override
    public String getConfirmationTitle() {
        return confirmationDialogTitle;
    }

    @Override
    public String getConfirmationMessage() {
        return "";
    }

    @Override
    public void onUserInteraction(){
        resetInactivityTimer();
    }

    @Override
    public void onResume() {
        super.onResume();
        resetInactivityTimer();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Comment out for production build
        //BugSenseHandler.closeSession(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }

    protected void showMessage(Context context, String msg, String title){
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setIcon(android.R.drawable.ic_dialog_alert)
             .setTitle(title)
             .setMessage(msg)
             .setPositiveButton(R.string.alert_dialog_ok, new SimpleOkayButtonListener())
             .show();
    }

    public class SimpleOkayButtonListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            //do nothing
        }
    }

    protected boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    public void close() {
        setResult(EXIT_RESULT_CODE);
        finish();
    }

    protected MartusCrypto getSecurity()
    {
        return AppConfig.getInstance().getCrypto();
    }

    protected void verifySignedPrefsFile(String fileName) throws MartusUtilities.FileVerificationException {
        File prefsFile = getPrefsFile(fileName);
        if (prefsFile.exists()) {
            File sigFile = new File(prefsFile.getParent(), prefsFile.getName() + ".sig");
            MartusUtilities.verifyFileAndSignature(prefsFile, sigFile, getSecurity(), getSecurity().getPublicKeyString());
        }
    }

    protected void verifySavedDesktopKeyFile() throws MartusUtilities.FileVerificationException {
        verifySignedPrefsFile(PREFS_DESKTOP_KEY);
    }

    protected void verifyServerIPFile() throws MartusUtilities.FileVerificationException {
        verifySignedPrefsFile(PREFS_SERVER_IP);
    }

    protected File getPrefsFile(String fileName) {
        File prefsDir = new File(getCacheDir().getParent(), PREFS_DIR);
        return new File(prefsDir, fileName + ".xml");
    }

    public static MartusSecurity cloneSecurity(MartusSecurity original) {
        MartusSecurity cryptoCopy = null;
        try {
            MartusKeyPair keyPair = original.getKeyPair();
            byte[] data = keyPair.getKeyPairData();
            cryptoCopy = new MartusSecurity();
            cryptoCopy.setKeyPairFromData(data);
            cryptoCopy.setShouldWriteAuthorDecryptableData(false);
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "Problem copying crypto", e);
        }
        return cryptoCopy;
    }

    protected void showProgressDialog(String title) {
        dialog = new ProgressDialog(this);
        dialog.setTitle(title);
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

	public long getInactivityTimeout()
	{
		return inactivityTimeout;
	}
}