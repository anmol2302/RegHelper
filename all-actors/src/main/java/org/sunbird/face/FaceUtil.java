package org.sunbird.face;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.log4j.Logger;
import org.sunbird.cassandra.CassandraConnection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class FaceUtil {
    private static Logger logger = Logger.getLogger(FaceUtil.class);
    private static Map<String,String> headers = new HashMap<>();
    private static Map<String,String> personToUserIdMapper = new ConcurrentHashMap<>();
    private static Map<String,String> userToPersonIdMapper = new ConcurrentHashMap<>();
    private static CassandraConnection connection = CassandraConnection.getInstance();;
    static {
        headers.put("Content-Type","application/json");
        headers.put("Ocp-Apim-Subscription-Key",System.getenv("face_api_key"));
    }

    public static void initInMemoryMap() {
        logger.info("updating in memory map.");
        ResultSet rs = connection.getSession().execute("select * from sunbird.user_person;");
        List<Row> rsList = rs.all();
        logger.info("fetched total "+rsList.size() + " from cassandra.");
        rsList.parallelStream().forEach(row -> {
            userToPersonIdMapper.put(row.getString("osid"),row.getString("personid"));
            personToUserIdMapper.put(row.getString("osid"),row.getString("personid"));
        } );
        logger.info("In memory map updated.");
    }

    public static void updateCassandra(String osid, String personid){
        connection.getSession().execute("INSERT INTO sunbird.user_person (osid, personid) VALUES ('"+osid +"','"+ personid.toString()+"');");
    }

    public static Map<String, String> getPersonToUserIdMapper() {
        return personToUserIdMapper;
    }

    public static Map<String, String> getUserToPersonIdMapper() {
        return userToPersonIdMapper;
    }

    public static HttpResponse<JsonNode> makeSyncPostCall(String apiToCall, String requestBody) throws UnirestException {
        logger.info("logger:makeSyncPostCall:get request to make post call for API:"+apiToCall+":"+requestBody);
        return makeSyncPostCall(apiToCall,requestBody,headers);
    }

    public static HttpResponse<JsonNode> makeSyncPostCall(String apiToCall, String requestBody, Map<String,String> headers) throws UnirestException {
        logger.info("logger:makeSyncPostCall:get request to make post call for API:"+apiToCall+":"+requestBody);
        HttpResponse<JsonNode>jsonResponse
                = Unirest.post(apiToCall)
                .headers(headers)
                .body(requestBody)
                .asJson();
        return jsonResponse;
    }

    public static HttpResponse<JsonNode> makeSyncGetCall(String apiToCall, Map<String,String> headers) throws UnirestException {
        logger.info("logger:makeSyncPostCall:get request to make get call for API:"+apiToCall);
        HttpResponse<JsonNode>jsonResponse
                = Unirest.get(apiToCall)
                .headers(headers)
                .asJson();
        return jsonResponse;
    }

    public static Future<HttpResponse<JsonNode>> makeAsyncPostCall(String apiToCall, String requestBody) throws UnirestException {
        logger.info("logger:makeSyncPostCall:get request to make post call for API:"+apiToCall+":"+requestBody);
        Future<HttpResponse<JsonNode>> jsonResponse
                = Unirest.post(apiToCall)
                .headers(headers)
                .body(requestBody)
                .asJsonAsync();
        return jsonResponse;
    }

}
