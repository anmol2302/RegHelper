package org.sunbird;

public class Envs {
    public static final String CERT_GEN_HOST="sunbird_cert_host";
    public static final String CERT_GEN_PORT="sunbird_cert_port";
    public static final String CERT_REGISTRY_HOST="sunbird_cert_reg_host";
    public static final String CERT_REGISTRY_PORT = "sunbird_cert_reg_port";


//    public static String getCertGenHost() {
//        return System.getenv(CERT_GEN_HOST);
//    }
//
//    public static String getCertGenPort() {
//        return System.getenv(CERT_GEN_PORT);
//    }
//
//    public static String getCertRegistryHost() {
//        return System.getenv(CERT_REGISTRY_HOST);
//    }
//
//    public static String getCertRegistryPort() {
//        return System.getenv(CERT_REGISTRY_PORT);
//    }
    public static String getCertGenHost() {
        return "localhost";
    }

    public static String getCertGenPort() {
        return "9100";
    }

    public static String getCertRegistryHost() {
        return "localhost";
    }

    public static String getCertRegistryPort() {
        return "8900";
    }
}
