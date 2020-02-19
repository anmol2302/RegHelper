package org.sunbird.face;


import akka.actor.ActorRef;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.sunbird.BaseActor;
import org.sunbird.BaseException;
import org.sunbird.actor.core.ActorConfig;
import org.sunbird.message.ResponseCode;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import scala.concurrent.Future;

import java.io.IOException;
import java.util.*;

import static akka.dispatch.Futures.future;

@ActorConfig(
        tasks = {"multiple"},
        dispatcher = "",
        asyncTasks = {}
)
public class IdentifyMultiple extends BaseActor {

    private static Logger logger = Logger.getLogger(IdentifyMultiple.class);

    @Override
    public void onReceive(Request request) throws Throwable {
        if (MapUtils.isEmpty(FaceUtil.getPersonToUserIdMapper()) && MapUtils.isEmpty(FaceUtil.getUserToPersonIdMapper())) {
            logger.info("IdentifyMultiple:onReceive: updating inner map.");
            FaceUtil.initInMemoryMap();
        }
        identify(request, sender());
    }

    private void identify(Request request, ActorRef sender) {
        String imageUrl = (String) request.get("photo");
        logger.info("IdentifyMultiple:Identify face called for image url :: " + imageUrl);
        logger.info("IdentifyMultiple:calling detect api.");
        detectFace(imageUrl).andThen(new OnComplete<HttpResponse<JsonNode>>() {
            public void onComplete(Throwable failure, HttpResponse<JsonNode> result) throws IOException, BaseException {
                if (failure != null) {
                    logger.error("IdentifyMultiple::identify : Exception occurred while detecting face to person : " + failure.getLocalizedMessage(), failure);
                    Exception ex = new BaseException("BAD REQUEST",failure.getLocalizedMessage(), ResponseCode.BAD_REQUEST.getCode());
                    sender.tell(ex,self());
                } else {
                    logger.info("IdentifyMultiple:face detected for image : " + imageUrl);
                    if (result != null && result.getStatus() == HttpStatus.SC_OK) {
                        String json = result.getBody().getArray().toString();
                        List<Map<String, Object>> resultList = requestMapper.readValue(json, List.class);
                        List<String> faceIds = getFaceIds(resultList);
                        logger.info("IdentifyMultiple:calling indentify api.");
                        identifyFace(faceIds).andThen(new OnComplete<HttpResponse<JsonNode>>() {
                            public void onComplete(Throwable failure, HttpResponse<JsonNode> result) throws IOException, BaseException {
                                if (failure != null) {
                                    logger.error("IdentifyMultiple::identify : Exception occurred while identifying face to person : " + failure.getLocalizedMessage(), failure);
                                    Exception ex = new BaseException("BAD REQUEST",failure.getLocalizedMessage(), ResponseCode.BAD_REQUEST.getCode());
                                    sender.tell(ex,self());
                                } else {
                                    Set<String> osids = new HashSet<>();
                                    if (result != null && result.getStatus() == HttpStatus.SC_OK) {
                                        try {
                                            String json = result.getBody().getArray().toString();
                                            List<Map<String, Object>> resultList = requestMapper.readValue(json, List.class);
                                            List<String> personIds = getPersonIds(resultList);
                                            personIds.stream().forEach(
                                                    personId -> {
                                                        if (StringUtils.isNotBlank(personId)) {
                                                            String osid = FaceUtil.getPersonToUserIdMapper().get(personId);
                                                            if (StringUtils.isNotBlank(osid)) {
                                                                osids.add(osid);
                                                            }
                                                        }
                                                    }
                                            );
                                        } catch (Exception ex) {
                                            logger.error("Exception occurred while fetching personids",ex);
                                        }
                                        Response response = new Response();
                                        if (CollectionUtils.isNotEmpty(osids)) {
                                            response.put("osids" , osids);
                                        } else {
                                            response.put("osids" , new ArrayList<>());
                                        }
                                        sender.tell(response,self());
                                    } else {
                                        logger.error("IdentifyMultiple::identify:exception occurred:");
                                        Map<String,String> errorMap =requestMapper.readValue(result.getBody().getObject().get("error").toString(), Map.class);
                                        String code = errorMap.get("code");
                                        String message = errorMap.get("message");
                                        Exception ex = new BaseException(code,message, ResponseCode.BAD_REQUEST.getCode());
                                        sender.tell(ex,self());
                                    }
                                }
                            }
                        }, getContext().dispatcher());

                    } else {
                        logger.error("IdentifyMultiple:identify:exception occurred:");
                        Map<String,String> errorMap =requestMapper.readValue(result.getBody().getObject().get("error").toString(), Map.class);
                        String code = errorMap.get("code");
                        String message = errorMap.get("message");
                        Exception ex = new BaseException(code,message, ResponseCode.BAD_REQUEST.getCode());
                        sender.tell(ex,self());
                    }
                }
            }
        }, getContext().dispatcher());

    }

    private List<String> getPersonIds(List<Map<String, Object>> resultList) {
        List<String> personIdList = new ArrayList<>();
        try {
            List<Map<String, Object>> candidates = new ArrayList<>();
            resultList.stream().forEach(result -> {
                if (CollectionUtils.isNotEmpty((List<Map<String, Object>>) result.get("candidates"))) {
                    candidates.addAll((List<Map<String, Object>>) result.get("candidates"));
                }
            });
            candidates.stream().forEach(candidate -> {
                personIdList.add((String)candidate.get("personId"));
            });
        } catch (Exception ex) {
            logger.error("Exception occurred while fetching personIds ",ex);
        }
        return personIdList;
    }

    private List<String> getFaceIds(List<Map<String, Object>> resultList) {
        List<String> faceIds = new ArrayList<>();
        resultList.stream().forEach(result -> {
            faceIds.add((String) result.get("faceId"));
        });
        return faceIds;
    }

    private Future<HttpResponse<JsonNode>> identifyFace(List<String> faceIds) {
        String uri = "https://devcon-face.cognitiveservices.azure.com/face/v1.0/identify?recognitionModel=recognition_02";
        Future<HttpResponse<JsonNode>> f = future(
                () -> {
                    Map<String, Object> req = new WeakHashMap<>();
                    req.put("personGroupId", System.getenv("person_group"));
                    req.put("faceIds", faceIds);
                    return FaceUtil.makeSyncPostCall(uri, requestMapper.writeValueAsString(req));
                }, getContext().dispatcher());
        return f;
    }

    private Future<HttpResponse<JsonNode>> detectFace(String imageUrl) {
        String uri = "https://devcon-face.cognitiveservices.azure.com/face/v1.0/detect?recognitionModel=recognition_02";
        Future<HttpResponse<JsonNode>> f = future(
                () -> {
                    Map<String, String> req = new WeakHashMap<>();
                    req.put("url", imageUrl);
                    return FaceUtil.makeSyncPostCall(uri, requestMapper.writeValueAsString(req));
                }, getContext().dispatcher());
        return f;
    }
}
