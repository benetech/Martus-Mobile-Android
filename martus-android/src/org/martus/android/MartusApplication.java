package org.martus.android;

import java.io.File;

import org.martus.common.FieldSpecCollection;
import org.martus.common.network.TorTransportWrapper;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.database.ActivityLogger;
import org.odk.collect.android.logic.FormController;
import org.odk.collect.android.logic.PropertyManager;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * @author roms
 *         Date: 10/24/12
 */
public class MartusApplication extends Application {

    public static boolean ignoreInactivity = false;

	public static final String DEFAULT_FONTSIZE = "21";
	private FieldSpecCollection customTopSectionSpecs;
	private FieldSpecCollection customBottomSectionSpecs;
	private static MartusApplication singleton = null;

    public static MartusApplication getInstance() {
        return singleton;
    }

    public void setIgnoreInactivity(boolean ignore) {
        ignoreInactivity = ignore;
    }

    public static boolean isIgnoreInactivity() {
        return ignoreInactivity;
    }

    @Override
    public void onCreate()
    {
	    singleton = this;

	    PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        super.onCreate();

	    PRNGFixes.apply();
        initSingletons();
    }

    protected void initSingletons()
    {
        AppConfig.initInstance(this.getCacheDir().getParentFile(), this.getApplicationContext());
	    Collect.initInstance(this.getApplicationContext());
    }

	public TorTransportWrapper getTransport()
	{
		return AppConfig.getInstance().getTransport();
	}

	public FieldSpecCollection getCustomTopSectionSpecs()
	{
		return customTopSectionSpecs;
	}

	public void setCustomTopSectionSpecs(FieldSpecCollection customTopSectionSpecs)
	{
		this.customTopSectionSpecs = customTopSectionSpecs;
	}

	public FieldSpecCollection getCustomBottomSectionSpecs()
	{
		return customBottomSectionSpecs;
	}

	public void setCustomBottomSectionSpecs(FieldSpecCollection customBottomSectionSpecs)
	{
		this.customBottomSectionSpecs = customBottomSectionSpecs;
	}

	public static int getQuestionFontsize() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MartusApplication
		        .getInstance());
        String question_font = settings.getString(SettingsActivity.KEY_FONT_SIZE,
		        MartusApplication.DEFAULT_FONTSIZE);
        int questionFontsize = Integer.valueOf(question_font);
        return questionFontsize;
    }

}
