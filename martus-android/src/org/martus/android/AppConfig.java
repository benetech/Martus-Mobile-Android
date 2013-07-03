package org.martus.android;

import java.io.File;

import org.martus.client.bulletinstore.MobileClientBulletinStore;
import org.martus.clientside.ClientSideNetworkHandlerUsingXmlRpc;
import org.martus.clientside.MobileClientSideNetworkGateway;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.crypto.MobileMartusSecurity;
import org.martus.common.fieldspec.StandardFieldSpecs;
import org.martus.common.network.ClientSideNetworkInterface;
import org.martus.common.network.TorTransportWrapper;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * @author roms
 *         Date: 10/24/12
 */
public class AppConfig {

    public static final String LOG_LABEL = "martus";

    private static AppConfig instance;
    private MobileClientBulletinStore store;
    private MartusSecurity martusCrypto;
	private Context context;
	private TorTransportWrapper transport;
	private String serverPublicKey;
    private String serverIP;
	private ClientSideNetworkInterface currentNetworkInterfaceHandler;
	private MobileClientSideNetworkGateway currentNetworkInterfaceGateway;

    public static void initInstance(File cacheDir, Context context ) {
        if (instance == null) {
            instance = new AppConfig(cacheDir, context);
        }
    }

    public static AppConfig getInstance() {
        return instance;
    }

    private AppConfig(File cacheDir, Context context) {
        // Constructor hidden because this is a singleton

	    this.context = context;

        transport = TorTransportWrapper.create();
        File torDirectory = getOrchidDirectory();
        torDirectory.mkdirs();
        transport.setTorDataDirectory(torDirectory);

        try {
            martusCrypto = new MobileMartusSecurity();
        } catch (Exception e) {
            Log.e(LOG_LABEL, "unable to initialize crypto", e);
        }

        store = new MobileClientBulletinStore(martusCrypto);
        try {
            store.doAfterSigninInitialization(cacheDir);
        } catch (Exception e) {
            Log.e(LOG_LABEL, "unable to initialize store", e);
        }

        //store = new MobileBulletinStore(martusCrypto);
        store.setTopSectionFieldSpecs(StandardFieldSpecs.getDefaultTopSetionFieldSpecs());
        store.setBottomSectionFieldSpecs(StandardFieldSpecs.getDefaultBottomSectionFieldSpecs());


    }


    public MartusSecurity getCrypto() {
        return martusCrypto;
    }

    public MobileClientBulletinStore getStore() {
        return store;
    }

	public File getOrchidDirectory() {
		return new File(context.getCacheDir().getParent(), BaseActivity.PREFS_DIR);
	}

	public TorTransportWrapper getTransport() {
		return transport;
	}

	public MobileClientSideNetworkGateway getCurrentNetworkInterfaceGateway()
	{

		if(currentNetworkInterfaceGateway == null)
		{
			currentNetworkInterfaceGateway = new MobileClientSideNetworkGateway(getCurrentNetworkInterfaceHandler());
		}

		return currentNetworkInterfaceGateway;
	}

	private ClientSideNetworkInterface getCurrentNetworkInterfaceHandler()
	{
		updateSettings();
		if(currentNetworkInterfaceHandler == null) {
			currentNetworkInterfaceHandler = createXmlRpcNetworkInterfaceHandler();
		}

		return currentNetworkInterfaceHandler;
	}

	private ClientSideNetworkInterface createXmlRpcNetworkInterfaceHandler()
	{
		return MobileClientSideNetworkGateway.buildNetworkInterface(serverIP , serverPublicKey, transport);
	}

	public void invalidateCurrentHandlerAndGateway()
	{
		currentNetworkInterfaceHandler = null;
		currentNetworkInterfaceGateway = null;
	}

	private void updateSettings() {
        SharedPreferences serverSettings = context.getSharedPreferences(BaseActivity.PREFS_SERVER_IP, Context.MODE_PRIVATE);
        serverIP = serverSettings.getString(SettingsActivity.KEY_SERVER_IP, "");
        serverPublicKey = serverSettings.getString(SettingsActivity.KEY_SERVER_PUBLIC_KEY, "");
    }

}
