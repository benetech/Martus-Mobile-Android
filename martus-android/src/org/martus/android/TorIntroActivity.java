package org.martus.android;

import java.security.SignatureException;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

import info.guardianproject.onionkit.ui.OrbotHelper;

/**
 * @author roms
 *         Date: 10/8/13
 */
public class TorIntroActivity extends AbstractTorActivity implements OrbotHandler
{
    @Override
    protected int getLayoutName() {
        return R.layout.tor_during_setup;
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
                turnOffTorToggle();
            }
        } catch (SignatureException e)
        {
            turnOffTorToggle();
        }
	}

    @Override
    public void onOrbotInstallCanceled() {
        turnOffTorToggle();
    }

    @Override
    public void onOrbotStartCanceled() {
        turnOffTorToggle();
    }

}
