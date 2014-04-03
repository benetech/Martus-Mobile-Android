package org.martus.android;

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
}
