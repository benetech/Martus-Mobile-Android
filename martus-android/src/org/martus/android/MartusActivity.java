package org.martus.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SignatureException;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.martus.android.dialog.LoginDialog;
import org.martus.android.dialog.ModalConfirmationDialog;
import org.martus.common.FieldCollection;
import org.martus.common.FieldSpecCollection;
import org.martus.common.HeadquartersKey;
import org.martus.common.MartusUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.fieldspec.CustomFieldTemplate;
import org.martus.common.fieldspec.FieldSpec;
import org.martus.common.network.NetworkInterfaceXmlRpcConstants;
import org.martus.util.StreamableBase64;
import org.odk.collect.android.activities.FormEntryActivity;
import org.odk.collect.android.application.Collect;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import info.guardianproject.onionkit.ui.OrbotHelper;

public class MartusActivity extends BaseActivity implements LoginDialog.LoginDialogListener,
        OrbotHandler {

	public static final String ACCOUNT_ID_FILENAME = "Mobile_Public_Account_ID.mpi";

    private static final String PACKETS_DIR = "packets";
    private static final String SERVER_COMMAND_PREFIX = "MartusServer.";
    private static final int CONFIRMATION_TYPE_RESET = 0;
    private static final int CONFIRMATION_TYPE_TAMPERED_DESKTOP_FILE = 1;

    public static final int MAX_LOGIN_ATTEMPTS = 3;
    private String serverPublicKey;

    private String serverIP;
    private int invalidLogins;
    private CheckBox torCheckbox;

    static final int ACTIVITY_DESKTOP_KEY = 2;
    public static final int ACTIVITY_BULLETIN = 3;
	final static int ACTIVITY_CHOOSE_FORM = 4;
    public static final String RETURN_TO = "return_to";
    public static final String FORM_NAME= "formName";
    public static final String HAVE_FORM= "haveForm";
    private final static String pingPath = "/RPC2";
    private int confirmationType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);


        torCheckbox = (CheckBox)findViewById(R.id.checkBox_use_tor);
        updateSettings();
        confirmationType = CONFIRMATION_TYPE_RESET;

    }

    protected void onStart() {
        super.onStart();
        invalidLogins = 0;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (martusCrypto.hasKeyPair()) {
	        boolean canUpload = mySettings.getBoolean(SettingsActivity.KEY_HAVE_UPLOAD_RIGHTS, false);
            if (!confirmServerPublicKey() || !canUpload) {
                Intent intent = new Intent(MartusActivity.this, TorIntroActivity.class);
                startActivity(intent);
                return;
            }
	        if (!checkDesktopKey()) {
                return;
            }

            OrbotHelper oc = new OrbotHelper(this);
	        try
	        {
		        if (!oc.isOrbotInstalled() || !oc.isOrbotRunning()) {
		            torCheckbox.setChecked(false);
		        }
	        } catch (SignatureException e)
	        {
		        torCheckbox.setChecked(false);
	        }

	        verifySetupInfo();
        } else {
            if (isAccountCreated()) {
                showLoginDialog();
            } else {
	            Intent intent = new Intent(MartusActivity.this, CreateAccountActivity.class);
                startActivityForResult(intent, EXIT_REQUEST_CODE);
                return;
            }
        }
        updateSettings();

    }

    @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EXIT_REQUEST_CODE && resultCode == EXIT_RESULT_CODE) {
            AppConfig.getInstance().getCrypto().clearKeyPair();
            finish();
        }  else if (requestCode == ACTIVITY_CHOOSE_FORM) {
	        if (resultCode == RESULT_OK){
		        SharedPreferences HQSettings = getSharedPreferences(PREFS_DESKTOP_KEY, MODE_PRIVATE);
                HeadquartersKey hqKey = new HeadquartersKey(HQSettings.getString(SettingsActivity.KEY_DESKTOP_PUBLIC_KEY, ""));

                Uri uri = data.getData();
                String filePath = uri.getPath();

                try {
	                CustomFieldTemplate template = new CustomFieldTemplate();
	                Vector authorizedKeys = new Vector<String>();
                    authorizedKeys.add(hqKey.getPublicKey());
	                File customTemplate = new File(filePath);
	                if(template.importTemplate(martusCrypto, customTemplate, authorizedKeys))
                    {
	                    String topSectionXML = template.getImportedTopSectionText();
	                    String bottomSectionXML = template.getImportedBottomSectionText();

                        FieldSpecCollection topFields = FieldCollection.parseXml(topSectionXML);
                        FieldSpecCollection bottomFields = FieldCollection.parseXml(bottomSectionXML);
                        MartusApplication.getInstance().setCustomTopSectionSpecs(topFields);
                        MartusApplication.getInstance().setCustomBottomSectionSpecs(bottomFields);

	                    FieldSpecCollection allFields = mergeIntoOneSpecCollection(topFields, bottomFields);

                        ODKUtils.writeXml(this, allFields);
                        Intent intent = new Intent(MartusActivity.this, FormEntryActivity.class);
                        intent.putExtra(MartusActivity.FORM_NAME, ODKUtils.MARTUS_CUSTOM_ODK_FORM);
                        startActivity(intent);
                    }

	                deleteExistingTemplate();
	                copyFile(customTemplate, new File(Collect.MARTUS_TEMPLATE_PATH, ODKUtils.MARTUS_CUSTOM_TEMPLATE));
                } catch (Exception e) {
                    showMessage(this, "Invalid form file", getString(R.string.error_message));
                    Log.e(AppConfig.LOG_LABEL, "problem getting form file", e);
                }
            } else if (resultCode == RESULT_CANCELED) {
                //shouldShowInstallExplorer = true;
            }
        }
    }

	private FieldSpecCollection mergeIntoOneSpecCollection(FieldSpecCollection topFields, FieldSpecCollection bottomFields)
	{
		FieldSpecCollection allFields = new FieldSpecCollection(topFields.asArray());
		allFields.addAllReusableChoicesLists(topFields.getAllReusableChoiceLists());
		FieldSpec[] bottomSpecs = bottomFields.asArray();
		for (FieldSpec spec: bottomSpecs) {
			allFields.add(spec);
		}
		allFields.addAllReusableChoicesLists(bottomFields.getAllReusableChoiceLists());
		return allFields;
	}

	private void deleteExistingTemplate()
	{
		File dir = new File(Collect.MARTUS_TEMPLATE_PATH);
		File file = new File(dir, ODKUtils.MARTUS_CUSTOM_TEMPLATE);
		file.delete();
	}


	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
	    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
	        if (event.getAction() == KeyEvent.ACTION_UP &&
	            keyCode == KeyEvent.KEYCODE_MENU) {
	            openOptionsMenu();
	            return true;
	        }
	    }
	    return super.onKeyUp(keyCode, event);
	}

    public void sendBulletin(View view) {
	    File dir = new File(Collect.FORMS_PATH);
        File file = new File(dir, ODKUtils.MARTUS_CUSTOM_ODK_FORM);

	    if (file.exists()) {
		    Intent intent = new Intent(MartusActivity.this, FormEntryActivity.class);
            intent.putExtra(MartusActivity.FORM_NAME, ODKUtils.MARTUS_CUSTOM_ODK_FORM);
            startActivity(intent);
	    } else {
	        Intent intent = new Intent(MartusActivity.this, BulletinActivity.class);
	        startActivityForResult(intent, EXIT_REQUEST_CODE) ;
	    }
    }

	public void loadForm() {
		try {
            Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
            chooseFile.setType("file/*");
            Intent intent = Intent.createChooser(chooseFile, getString(R.string.select_file_picker));
            startActivityForResult(intent, ACTIVITY_CHOOSE_FORM);
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "Failed choosing file", e);
            e.printStackTrace();
        }
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	    Intent intent;

        int id = item.getItemId();
        if (id == R.id.settings_menu_item) {
	        intent = new Intent(MartusActivity.this, SettingsActivity.class);
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
        } else if (id == R.id.server_menu_item) {
            intent = new Intent(MartusActivity.this, ServerActivity.class);
            startActivityForResult(intent, EXIT_REQUEST_CODE);
            return true;
        } else if (id == R.id.ping_server_menu_item) {
            pingServer();
            return true;
        } else if (id == R.id.resend_menu_item) {
	        resendFailedBulletins();
            return true;
        } else if (id == R.id.view_public_code_menu_item) {
	        try {
	            String publicCode = MartusCrypto.getFormattedPublicCode(martusCrypto.getPublicKeyString());
	            showMessage(this, publicCode, getString(R.string.view_public_code_dialog_title));
	        } catch (Exception e) {
	            Log.e(AppConfig.LOG_LABEL, "couldn't get public code", e);
	            showMessage(this, getString(R.string.view_public_code_dialog_error),
	                    getString(R.string.view_public_code_dialog_title));
	        }
	        return true;
	    } else if (id == R.id.reset_install_menu_item) {
	        if (!MartusApplication.isIgnoreInactivity()) {
	            showConfirmationDialog();
	        } else {
	            showMessage(this, getString(R.string.logout_while_sending_message),
	                    getString(R.string.reset_while_sending_title));
	        }
	        return true;
	    } else if (id == R.id.show_version_menu_item) {
            PackageInfo pInfo;
            String version;
            try {
                pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                version = pInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                version = "?";
            }
            Toast.makeText(this, version, Toast.LENGTH_LONG).show();
            return true;
        } else if (id == R.id.export_mpi_menu_item) {
            File mpiFile = getMpiFile();
            showMessage(this, mpiFile.getAbsolutePath(), getString(R.string.exported_account_id_file_confirmation));
             return true;
        } else if (id == R.id.email_mpi_menu_item) {
            showHowToSendDialog(this, getString(R.string.send_dialog_title));
            return true;
        } else if (id == R.id.feedback_menu_item) {
            showContactUs();
            return true;
        } else if (id == R.id.view_docs_menu_item) {
            showViewDocs();
            return true;
/*        } else if (id == R.id.load_form_menu_item) {
            loadForm();
            return true;*/
        }
        return super.onOptionsItemSelected(item);
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

	private File getMpiFile()
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

	private void clearCacheDir() {
		clearDirectory(getCacheDir());
	}

    private void clearPrefsDir() {
        File prefsDirFile = new File(getAppDir(), PREFS_DIR);
        clearDirectory(prefsDirFile);
    }

    private void clearFailedBulletinsDir() {
            File prefsDirFile = new File(getAppDir(), UploadBulletinTask.FAILED_BULLETINS_DIR);
            clearDirectory(prefsDirFile);
            prefsDirFile.delete();
        }

    private void removePacketsDir() {
        File packetsDirFile = new File(getAppDir(), PACKETS_DIR);
        clearDirectory(packetsDirFile);
        packetsDirFile.delete();
    }

	private void removeFormsDir() {
        File formsDirFile = new File(getAppDir(), Collect.FORMS_DIR_NAME);
        clearDirectory(formsDirFile);
		formsDirFile.delete();
    }

    public static void logout() {
        AppConfig.getInstance().getCrypto().clearKeyPair();
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

    public void onTorChecked(View view) {
        boolean checked = ((CheckBox) view).isChecked();

        if  (checked) {
            System.setProperty("proxyHost", PROXY_HOST);
            System.setProperty("proxyPort", String.valueOf(PROXY_HTTP_PORT));

            System.setProperty("socksProxyHost", PROXY_HOST);
            System.setProperty("socksProxyPort", String.valueOf(PROXY_SOCKS_PORT));

            try {

                OrbotHelper oc = new OrbotHelper(this);

                if (!oc.isOrbotInstalled())
                {
                    oc.promptToInstall(this);
                }
                else if (!oc.isOrbotRunning())
                {
                    oc.requestOrbotStart(this);
                }
            } catch (Exception e) {
                Log.e(AppConfig.LOG_LABEL, "Tor check failed", e);
	            showMessage(this, getString(R.string.invalid_orbot_message), getString(R.string.invalid_orbot_title));
	            torCheckbox.setChecked(false);
            }

        } else {
            System.clearProperty("proxyHost");
            System.clearProperty("proxyPort");

            System.clearProperty("socksProxyHost");
            System.clearProperty("socksProxyPort");
        }
    }

    private void verifySetupInfo() {
        try {
            verifySavedDesktopKeyFile();
            verifyServerIPFile();
        } catch (MartusUtilities.FileVerificationException e) {
            Log.e(AppConfig.LOG_LABEL, "Desktop key file corrupted in checkDesktopKey");
            confirmationType = CONFIRMATION_TYPE_TAMPERED_DESKTOP_FILE;
            showModalConfirmationDialog();
        }
    }

    private void showModalConfirmationDialog() {
        ModalConfirmationDialog confirmationDialog = ModalConfirmationDialog.newInstance();
        confirmationDialog.show(getSupportFragmentManager(), "dlg_confirmation");
    }

    private boolean checkDesktopKey() {
        SharedPreferences HQSettings = getSharedPreferences(PREFS_DESKTOP_KEY, MODE_PRIVATE);
        String desktopPublicKeyString = HQSettings.getString(SettingsActivity.KEY_DESKTOP_PUBLIC_KEY, "");

        if (desktopPublicKeyString.length() < 1) {
            Intent intent = new Intent(MartusActivity.this, DesktopKeyActivity.class);
            startActivityForResult(intent, ACTIVITY_DESKTOP_KEY);
            return false;
        }
        return true;
    }

    private boolean isAccountCreated() {
        String keyPairString = mySettings.getString(SettingsActivity.KEY_KEY_PAIR, "");
        return keyPairString.length() > 1;
    }

    private void updateSettings() {
        SharedPreferences serverSettings = getSharedPreferences(PREFS_SERVER_IP, MODE_PRIVATE);
        serverPublicKey = serverSettings.getString(SettingsActivity.KEY_SERVER_PUBLIC_KEY, "");
        serverIP = serverSettings.getString(SettingsActivity.KEY_SERVER_IP, "");
    }

    @Override
    public void onFinishPasswordDialog(TextView passwordText) {
        char[] password = passwordText.getText().toString().trim().toCharArray();
        boolean confirmed = (password.length >= MIN_PASSWORD_SIZE) && confirmAccount(password);
        if (!confirmed) {
            if (++invalidLogins == MAX_LOGIN_ATTEMPTS) {
                finish();
            }
            Toast.makeText(this, getString(R.string.incorrect_password), Toast.LENGTH_SHORT).show();
            showLoginDialog();
            return;
        }

        SharedPreferences serverSettings = getSharedPreferences(PREFS_SERVER_IP, MODE_PRIVATE);
        serverPublicKey = serverSettings.getString(SettingsActivity.KEY_SERVER_PUBLIC_KEY, "");
	    AppConfig.getInstance().invalidateCurrentHandlerAndGateway();

	    int count = getNumberOfUnsentBulletins();
        if (count != 0) {
	        Resources res = getResources();
	        showMessage(this, res.getQuantityString(R.plurals.show_unsent_count, count, count), getString(R.string.show_unsent_title));
        }

	    Intent intent = getIntent();
        int returnTo = intent.getIntExtra(RETURN_TO, 0);
        if (returnTo == ACTIVITY_BULLETIN) {
            Intent destination = new Intent(MartusActivity.this, BulletinActivity.class);
            destination.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            destination.putExtras(intent);
            startActivity(destination);
        }
        onResume();
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
	    Intent resendService = new Intent(MartusActivity.this, ResendService.class);
	    resendService.putExtra(SettingsActivity.KEY_SERVER_IP, serverIP);
	    resendService.putExtra(SettingsActivity.KEY_SERVER_PUBLIC_KEY, serverPublicKey);
	    startService(resendService);
	}


	private static void clearDirectory(final File dir) {
        if (dir!= null && dir.isDirectory()) {
            try {
                for (File child:dir.listFiles()) {
                    if (child.isDirectory()) {
                        clearDirectory(child);
                    }
                    child.delete();
                }
            }
            catch(Exception e) {
                Log.e(AppConfig.LOG_LABEL, String.format("Failed to clean the cache, error %s", e.getMessage()));
            }
        }
    }

    @Override
    public void onOrbotInstallCanceled() {
        torCheckbox.setChecked(false);
    }

    @Override
    public void onOrbotStartCanceled() {
        torCheckbox.setChecked(false);
    }

    @Override
    public String getConfirmationTitle() {
        if (confirmationType == CONFIRMATION_TYPE_TAMPERED_DESKTOP_FILE) {
            return getString(R.string.confirm_tamper_reset_title);
        } else
            return getString(R.string.confirm_reset_install);
    }

    @Override
    public String getConfirmationMessage() {
        if (confirmationType == CONFIRMATION_TYPE_TAMPERED_DESKTOP_FILE) {
            return getString(R.string.confirm_tamper_reset_message);
        } else {
            int count = getNumberOfUnsentBulletins();
            if (count == 0) {
              return getString(R.string.confirm_reset_install_extra_no_pending);
            } else {
                Resources res = getResources();
                return res.getQuantityString(R.plurals.confirm_reset_install_extra, count, count);
            }
        }
    }

    private int getNumberOfUnsentBulletins() {
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

    @Override
    public void onConfirmationAccepted() {
        removePacketsDir();
	    removeFormsDir();
        clearPreferences(mySettings.edit());
        clearPreferences(getSharedPreferences(PREFS_DESKTOP_KEY, MODE_PRIVATE).edit());
        clearPreferences(getSharedPreferences(PREFS_SERVER_IP, MODE_PRIVATE).edit());
        logout();
        clearPrefsDir();
        clearFailedBulletinsDir();
	    clearCacheDir();
        final File unsentBulletinsDir = getAppDir();
        final String[] names = unsentBulletinsDir.list(new ZipFileFilter());
        for (String name : names) {
            File zipFile = new File(unsentBulletinsDir, name);
            zipFile.delete();
        }
        finish();
    }

    private void removePrefsFile(String prefName) {
        File serverIpFile = getPrefsFile(prefName);
        serverIpFile.delete();
    }

    private void clearPreferences(SharedPreferences.Editor editor) {
        editor.clear();
        editor.commit();
    }

    @Override
    public void onConfirmationCancelled() {
        if (confirmationType == CONFIRMATION_TYPE_TAMPERED_DESKTOP_FILE) {
            martusCrypto.clearKeyPair();
            finish();
        }
    }

    private void processPingResult(String result) {
        dialog.dismiss();
        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
    }

	protected void showHowToSendDialog(Context context, String title) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setIcon(android.R.drawable.ic_dialog_info)
             .setTitle(title)
             .setPositiveButton(R.string.send_dialog_email, new SendEmailButtonListener())
             .setNegativeButton(R.string.password_dialog_cancel, new CancelSendButtonListener())
             .setNeutralButton(R.string.send_dialog_bulletin, new SendBulletinButtonListener())
             .show();
    }

    public class SendEmailButtonListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
	        File mpiFile = getMpiFile();
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            Uri uri = Uri.parse("file://" + mpiFile.getAbsolutePath());
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(emailIntent, "Send email..."));
        }
    }

	public class SendBulletinButtonListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
	        File mpiFile = getMpiFile();
            String filePath = mpiFile.getPath();
            Intent bulletinIntent = new Intent(MartusActivity.this, BulletinActivity.class);
            bulletinIntent.putExtra(BulletinActivity.EXTRA_ATTACHMENT, filePath);
            startActivity(bulletinIntent);
        }
    }

	public class CancelSendButtonListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            //do nothing
        }
    }

	public void refreshView()
	{
		setContentView(R.layout.main);
	}

	public void copyFile(File src, File dst) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		try {
		    in = new FileInputStream(src);
		    out = new FileOutputStream(dst);

		    // Transfer bytes from in to out
		    byte[] buf = new byte[1024];
		    int len;
		    while ((len = in.read(buf)) > 0) {
		        out.write(buf, 0, len);
		    }
		} catch (Exception e) {
			Log.e(AppConfig.LOG_LABEL, "problem copying template ", e);
		} finally {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
		}

	}

    class PingTask extends AsyncTask<XmlRpcClient, Void, String> {
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