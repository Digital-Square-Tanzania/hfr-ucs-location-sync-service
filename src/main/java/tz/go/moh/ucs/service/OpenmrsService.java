package tz.go.moh.ucs.service;


import java.text.SimpleDateFormat;
import java.util.Date;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;


public abstract class OpenmrsService {

    public static final SimpleDateFormat OPENMRS_DATE = new SimpleDateFormat("yyyy-MM-dd");
    public static final String CUSTOM_UUID_PARAM = "v=custom:(uuid)";

    public static String OPENMRS_BASE_URL;
    public static String OPENMRS_USER;
    public static String OPENMRS_PWD;
    protected String OPENMRS_VERSION = "2.4.1";

    public OpenmrsService() {
        Config config = ConfigFactory.load();
        OPENMRS_BASE_URL = config.getString("openmrs.base_url");
        OPENMRS_USER = config.getString("openmrs.user");
        OPENMRS_PWD = config.getString("openmrs.password");
    }

    public OpenmrsService(String openmrsUrl, String user, String password) {
        OPENMRS_BASE_URL = openmrsUrl;
        OPENMRS_USER = user;
        OPENMRS_PWD = password;
    }

    public OpenmrsService(String openmrsUrl, String user, String password, String openmrsVersion) {
        OPENMRS_BASE_URL = openmrsUrl;
        OPENMRS_USER = user;
        OPENMRS_PWD = password;
        OPENMRS_VERSION = openmrsVersion;
    }

    public static void main(String[] args) {
        System.out.println(OPENMRS_DATE.format(new Date()));
    }


    void setURL(String url) {
        OPENMRS_BASE_URL = url;
    }

}
