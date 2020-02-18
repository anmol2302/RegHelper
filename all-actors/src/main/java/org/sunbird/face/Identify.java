package org.sunbird.face;

import akka.actor.ActorRef;
import akka.dispatch.OnComplete;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.sunbird.BaseActor;
import org.sunbird.actor.core.ActorConfig;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import scala.concurrent.Future;

import java.io.IOException;
import java.util.*;

import static akka.dispatch.Futures.future;

@ActorConfig(
        tasks = {"identify"},
        dispatcher = "",
        asyncTasks = {}
)
public class Identify extends BaseActor {

    private static Logger logger = Logger.getLogger(Identify.class);

    @Override
    public void onReceive(Request request) throws Throwable {
        if (MapUtils.isEmpty(FaceUtil.getPersonToUserIdMapper()) && MapUtils.isEmpty(FaceUtil.getUserToPersonIdMapper())) {
            logger.info("Identify:onReceive: updating inner map.");
            FaceUtil.initInMemoryMap();
        }
        identify(request, sender());
    }

    private void identify(Request request, ActorRef sender) {
        String imageUrl = (String) request.get("photo");
        logger.info("Identify face called for image url :: " + imageUrl);
        logger.info("calling detect api.");
        detectFace(imageUrl).andThen(new OnComplete<HttpResponse<JsonNode>>() {
            public void onComplete(Throwable failure, HttpResponse<JsonNode> result) throws IOException {
                if (failure != null) {
                    logger.error("Identify:identify : Exception occurred while detecting face to person : " + failure.getLocalizedMessage(), failure);
                } else {
                    logger.info("face detected for image : " + imageUrl);
                    if (result != null && result.getStatus() == HttpStatus.SC_OK) {
                        String json = result.getBody().getArray().toString();
                        List<Map<String, Object>> resultList = requestMapper.readValue(json, List.class);
                        List<String> faceIds = getFaceIds(resultList);
                        logger.info("calling indentify api.");
                        identifyFace(faceIds).andThen(new OnComplete<HttpResponse<JsonNode>>() {
                            public void onComplete(Throwable failure, HttpResponse<JsonNode> result) throws IOException {
                                if (failure != null) {
                                    logger.error("Identify:identify : Exception occurred while identifying face to person : " + failure.getLocalizedMessage(), failure);
                                } else {
                                    if (result != null && result.getStatus() == HttpStatus.SC_OK) {
                                        String json = result.getBody().getArray().toString();
                                        List<Map<String, Object>> resultList = requestMapper.readValue(json, List.class);
                                        String personId = getUserID(resultList);
                                        logger.info("sending response for identify endpoint.");
                                        String userId = FaceUtil.getPersonToUserIdMapper().get(personId);
                                        Response response = new Response();
                                        response.put("osid", userId);
                                        sender.tell(response, self());
                                    } else {
                                        logger.error("Register:register:exception occurred:");
                                    }
                                }
                            }
                        }, getContext().dispatcher());

                    } else {
                        logger.error("Register:register:exception occurred:");
                    }
                }
            }
        }, getContext().dispatcher());

    }

    private String getUserID(List<Map<String, Object>> resultList) {
        List<Map<String, Object>> candidates = new ArrayList<>();
        resultList.stream().forEach(result -> {
            if (CollectionUtils.isNotEmpty((List<Map<String, Object>>) result.get("candidates"))) {
                candidates.addAll((List<Map<String, Object>>) result.get("candidates"));
            }
        });
        List<Double> confidenceList = new ArrayList<>();
        candidates.stream().forEach(candidate -> {

            confidenceList.add(Double.valueOf(candidate.get("confidence") + ""));
        });
        Collections.sort(confidenceList, Collections.reverseOrder());
        double highestConfidenceLevel = confidenceList.get(0);
        String personId = "";

        for (Map<String, Object> candidate : candidates) {
            if (highestConfidenceLevel == (Double.valueOf(candidate.get("confidence") + ""))) {
                personId = (String) candidate.get("personId");
                break;
            }
        }
        return personId;
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
