package org.martus.android;

import java.security.SignatureException;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import info.guardianproject.onionkit.ui.OrbotHelper;

/**
 * @author roms
 *         Date: 10/8/13
 */
public class TorIntroActivity extends BaseActivity implements OrbotHandler
{
	private CheckBox torCheckbox;

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tor_during_setup);
		torCheckbox = (CheckBox)findViewById(R.id.checkBox_use_tor);
	}

	public void nextScreen(View view) {
		Intent intent = new Intent(TorIntroActivity.this, ChooseConnectionActivity.class);
        startActivityForResult(intent, EXIT_REQUEST_CODE);
		finish();
	}

	@Override
    public void onResume() {
        super.onResume();

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

	@Override
    public void onOrbotInstallCanceled() {
        torCheckbox.setChecked(false);
    }

    @Override
    public void onOrbotStartCanceled() {
        torCheckbox.setChecked(false);
    }

}
