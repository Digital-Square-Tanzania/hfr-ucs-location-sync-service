package tz.go.moh.ucs.service;


import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.text.SimpleDateFormat;
import java.util.Date;


public abstract class OpenmrsService {

    public static final SimpleDateFormat OPENMRS_DATE = new SimpleDateFormat("yyyy-MM-dd");

    public static String OPENMRS_BASE_URL;
    public static String OPENMRS_USER;
    public static String OPENMRS_PWD;

    public OpenmrsService() {
        Config config = ConfigFactory.load();
        OPENMRS_BASE_URL = config.getString("openmrs.base_url");
        OPENMRS_USER = config.getString("openmrs.user");
        OPENMRS_PWD = config.getString("openmrs.password");
    }

    public static void main(String[] args) {
        System.out.println(OPENMRS_DATE.format(new Date()));
    }

}
