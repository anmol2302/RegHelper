package org.sunbird;

import org.apache.log4j.Logger;

public class Constants {

    private static Logger logger = Logger.getLogger(Constants.class);
    public static final String REQUEST = "request";
    public static final String RESPONSE = "response";
    public static final String RESULT = "result";
    public static final String RECIPIENT_TYPE="recipientType";
    public static final String RELATED = "related";
    public static final String RECIPIENT = "recipient";
    public static final String INDIVIDUAL = "individual";
    public static final String TYPE ="type";
    public static final String RECIPIENT_NAME="recipientName";
    public static final String SUCCESS = "success";
    public static final String NAME = "name";
    public static final String JSON_DATA = "jsonData";
    public static final String DEVCON2020 = "DEVCON2020";


    public static String getCertGenApi() {
        String api= String.format("%s/v1/certs/generate","http://cert-service:9000");
        logger.info("Constants:getCertGenApi:api call to generate certificate"+api);
        return api;
    }

    public static String getCertAddApi() {
        String api= String.format("%s/certs/v1/registry/add","http://cert-registry-service:9000");
        logger.info("Constants:getCertGenApi:api call to deposit certificate"+api);
        return api;
    }
}
