package org.martus.android;

import java.net.URL;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class PingServer extends Activity {

	//String serverIPNew = "http://50.112.118.184/RPC2";
	//String serverIPNew = "http://66.201.46.82:988/RPC2";
	private final static String pingPath = "/RPC2";
	TextView textview;
    private String serverIP;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ping);

        textview = new TextView(this); 
    	textview=(TextView)findViewById(R.id.response);

        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(this);
        serverIP = mySettings.getString(SettingsActivity.KEY_SERVER_IP, MartusActivity.defaultServerIP + pingPath);
	    
        final Button button = (Button) findViewById(R.id.buttonPing);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	try {
            		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            		config.setServerURL(new URL(serverIP));
            		XmlRpcClient client = new XmlRpcClient();
            		client.setConfig(config);

                    //Network calls must be made in background task
                    final AsyncTask<XmlRpcClient, Void, String> pingTask = new PingTask().execute(client);
                    String result = pingTask.get();

                  	textview.setText("response: " +result);
				} catch (Exception e) {
					Log.e("martus-xmlrpc", "xmlrpc call failed", e);
					e.printStackTrace();
				}
            }
        });

    }
    
}