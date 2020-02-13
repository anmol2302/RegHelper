package org.sunbird;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.sunbird.actor.core.ActorConfig;
import org.sunbird.request.Request;
import org.sunbird.response.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@ActorConfig(
        tasks = {"process"},
        dispatcher = "",
        asyncTasks = {}
)
public class CertProcessorActor extends BaseActor {
    private static Logger logger = Logger.getLogger(CertProcessorActor.class);
    private static ObjectMapper requestMapper = new ObjectMapper();
    static Map<String, String> headerMap = new HashMap<>();
    static {
        headerMap.put("Content-Type", "application/json");
    }


    @Override
    public void onReceive(Request request) throws Throwable {
        Response response = new Response();
        response.put("result",Constants.SUCCESS);
        sender().tell(response,self());
       CompletableFuture.supplyAsync(()->{
           process(request);
           return null;
       });

    }


    private void process(Request request) {
        try {
            Map<String, Object> certMap = generateCertificate(request.getRequest());
            add(getCertAddReq(certMap));
        }catch (Exception e){
            logger.error("CertsServiceImpl:process:exception occurred:" + e);
        }
    }

    private Map<String,Object> getCertAddReq(Map<String,Object>certMap){
        Map<String,String>relatedMap=new HashMap<>();
        relatedMap.put(Constants.TYPE,Constants.DEVCON2020);
        certMap.put(Constants.RECIPIENT_TYPE,Constants.INDIVIDUAL);
        certMap.put(Constants.RELATED,relatedMap);
        certMap.put(Constants.RECIPIENT_NAME,((Map)((Map)certMap.get(Constants.JSON_DATA)).get(Constants.RECIPIENT)).get(Constants.NAME));
        return certMap;
    }


    private Map<String, Object> generateCertificate(Map<String, Object> reqMap) throws BaseException, IOException, UnirestException {
        List<Map<String, Object>> genApiResp = new ArrayList<>();
        Map<String, Object> certReqMap = new HashMap<>();
        certReqMap.put(Constants.REQUEST, reqMap);
        String requestBody = requestMapper.writeValueAsString(certReqMap);
        logger.info("CertProcessorActor:generateCertificate:request body found:" + requestBody);
        String apiToCall = Constants.getCertGenApi();
        logger.info("CertProcessorActor:generateCertificate:complete api found:" + apiToCall);
        HttpResponse<JsonNode> jsonResponse = makeSyncPostCall(apiToCall, requestBody, headerMap);
        if (jsonResponse != null && jsonResponse.getStatus() == HttpStatus.SC_OK) {
            String stringifyResponse = jsonResponse.getBody().getObject().getJSONObject(Constants.RESULT).get(Constants.RESPONSE).toString();
            genApiResp = requestMapper.readValue(stringifyResponse, List.class);
        } else {
            logger.error("CertProcessorActor:generateCertificate:exception occurred:" + jsonResponse.getBody().toString());
        }
        return genApiResp.get(0);
    }



    public static HttpResponse<JsonNode> makeSyncPostCall(String apiToCall, String requestBody, Map<String,String>headerMap) throws UnirestException {
        logger.info("CertProcessorActor:makeSyncPostCall:get request to make post call for API:"+apiToCall+":"+requestBody);
        HttpResponse<JsonNode>jsonResponse
                = Unirest.post(apiToCall)
                .headers(headerMap)
                .body(requestBody)
                .asJson();
        return jsonResponse;
    }


    public void add(Map<String,Object> request) throws JsonProcessingException, UnirestException {
            Map<String, Object> certAddReqMap = new HashMap<>();
            certAddReqMap.put(Constants.REQUEST,request);
            String requestBody = requestMapper.writeValueAsString(certAddReqMap);
            logger.info("CertProcessorActor:add:request body found:" + requestBody);
            String apiToCall = Constants.getCertAddApi();
            logger.info("CertProcessorActor:add:complete url found:" + apiToCall);
            HttpResponse<JsonNode> jsonResponse=makeSyncPostCall(apiToCall,requestBody,headerMap);
            if (jsonResponse != null && jsonResponse.getStatus() == HttpStatus.SC_OK) {
                String id=jsonResponse.getBody().getObject().getJSONObject(Constants.RESULT).getString("id");
                logger.info("CertProcessorActor:add:cert id got for req:"+id);
            } else {
                logger.error("CertProcessorActor:add:request error occurred:" +jsonResponse.getStatusText());
            }}


    }







