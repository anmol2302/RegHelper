package org.sunbird.face;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FaceUtil {
    private static Logger logger = Logger.getLogger(FaceUtil.class);
    private static Map<String,String> headers = new HashMap<>();
    private static Map<String,String> personAndUserIdMapper = new ConcurrentHashMap<>();

    static {
        headers.put("Content-Type","application/json");
        headers.put("Ocp-Apim-Subscription-Key",System.getenv("face_api_key"));
    }
    public static Map<String, String> getPersonAndUserIdMapper() {
        return personAndUserIdMapper;
    }

    public static HttpResponse<JsonNode> makeSyncPostCall(String apiToCall, String requestBody) throws UnirestException {
        logger.info("logger:makeSyncPostCall:get request to make post call for API:"+apiToCall+":"+requestBody);
        HttpResponse<JsonNode>jsonResponse
                = Unirest.post(apiToCall)
                .headers(headers)
                .body(requestBody)
                .asJson();
        return jsonResponse;
    }
}
