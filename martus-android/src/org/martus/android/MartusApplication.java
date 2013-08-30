package org.martus.android;

import org.martus.common.network.TorTransportWrapper;

import android.app.Application;
import android.util.Log;

/**
 * @author roms
 *         Date: 10/24/12
 */
public class MartusApplication extends Application {

    public static boolean ignoreInactivity = false;

    public void setIgnoreInactivity(boolean ignore) {
        ignoreInactivity = ignore;
    }

    public static boolean isIgnoreInactivity() {
        return ignoreInactivity;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

	    PRNGFixes.apply();
        initSingletons();
    }

    protected void initSingletons()
    {
        AppConfig.initInstance(this.getCacheDir().getParentFile(), this.getApplicationContext());
    }

	public TorTransportWrapper getTransport()
	{
		return AppConfig.getInstance().getTransport();
	}
}
