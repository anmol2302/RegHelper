package org.sunbird.face;

import akka.actor.ActorRef;
import akka.dispatch.OnComplete;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.sunbird.BaseActor;
import org.sunbird.BaseException;
import org.sunbird.Constants;
import org.sunbird.actor.core.ActorConfig;
import org.sunbird.message.ResponseCode;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import scala.concurrent.Future;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import static akka.dispatch.Futures.future;


@ActorConfig(
        tasks = {"register"},
        dispatcher = "",
        asyncTasks = {}
)
public class Register extends BaseActor {

    private static Logger logger = Logger.getLogger(Register.class);

    @Override
    public void onReceive(Request request) throws Throwable {
        if (MapUtils.isEmpty(FaceUtil.getPersonToUserIdMapper()) && MapUtils.isEmpty(FaceUtil.getUserToPersonIdMapper())) {
            logger.info("Identify:onReceive: updating inner map.");
            FaceUtil.initInMemoryMap();
        }
        register(request, sender());
    }

    private void register(Request request, ActorRef sender) {
        logger.info("register method called.");
        Map<String, Object> req = request.getRequest();
        String userId = (String) req.get("osid");
        String imageUrl = (String) req.get("photo");
        String personId = getPersonId(userId);
        if (StringUtils.isBlank(personId)) {
            logger.info("osid does not exist in our record.");
            //user does not exist
            // create person and add face to it
            logger.info("create person in azure for osid :" + userId);
            Future<HttpResponse<JsonNode>> createUserFuture = createPerson(userId);
            createUserFuture.andThen(
                    new OnComplete<HttpResponse<JsonNode>>() {
                        public void onComplete(Throwable failure, HttpResponse<JsonNode> result) throws BaseException, IOException {
                            if (failure != null) {
                                logger.error("Register:register : Exception occurred while creating person : "
                                        + failure.getLocalizedMessage(), failure);
                                Exception ex =  new BaseException("BAD REQUEST",failure.getLocalizedMessage(), ResponseCode.BAD_REQUEST.getCode());
                                sender.tell(ex,self());
                            } else {
                                if (result != null && result.getStatus() == HttpStatus.SC_OK) {
                                    String personId = result.getBody().getObject().get("personId").toString();
                                    logger.info("person created for osid :: " + userId + " and personId is :: " + personId);
                                    logger.info("adding face for personId ::" + personId);
                                    addFaceToPersonId(personId, imageUrl).andThen(new OnComplete<HttpResponse<JsonNode>>() {
                                        public void onComplete(Throwable failure, HttpResponse<JsonNode> result) throws BaseException, IOException {
                                            if (failure != null) {
                                                logger.error("Register:register : Exception occurred while adding face to person : "
                                                        + failure.getLocalizedMessage(), failure);
                                                Exception ex =  new BaseException("BAD REQUEST",failure.getLocalizedMessage(), ResponseCode.BAD_REQUEST.getCode());
                                                sender.tell(ex,self());
                                            } else {
                                                if (result != null && result.getStatus() == HttpStatus.SC_OK) {
                                                    Response response = new Response();
                                                    response.put("result", Constants.SUCCESS);
                                                    sender.tell(response, self());

                                                    logger.info("face add success for personId ::" + personId);
                                                    updateUserPersonMap(userId, personId);
                                                    logger.info("call to train user group");
                                                    trainUserGroup();
                                                } else {
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
                                    Map<String,String> errorMap =requestMapper.readValue(result.getBody().getObject().get("error").toString(), Map.class);
                                    String code = errorMap.get("code");
                                    String message = errorMap.get("message");
                                    Exception ex = new BaseException(code,message, ResponseCode.BAD_REQUEST.getCode());
                                    sender.tell(ex,self());
                                    logger.error("Register:register:exception occurred:");
                                }
                            }
                        }
                    }, getContext().dispatcher());
        } else {
            logger.info("user exist for osid :" + userId);
            // user exits , just add face to personId
            logger.info("call to update face for osid " + userId + " , personId : " + personId);
            addFaceToPersonId(personId, imageUrl).andThen(new OnComplete<HttpResponse<JsonNode>>() {
                public void onComplete(Throwable failure, HttpResponse<JsonNode> result) throws IOException {
                    if (failure != null) {
                        logger.error("Register:register : Exception occurred while adding face to person : "
                                + failure.getLocalizedMessage(), failure);
                        Exception ex =  new BaseException("BAD REQUEST",failure.getLocalizedMessage(), ResponseCode.BAD_REQUEST.getCode());
                        sender.tell(ex,self());
                    } else {
                        if (result != null && result.getStatus() == HttpStatus.SC_OK) {
                            Response response = new Response();
                            response.put("result", Constants.SUCCESS);
                            sender.tell(response, self());

                            logger.info("Updating user for userId:" + userId);
                            updateUserPersonMap(userId, personId);
                            trainUserGroup();
                        } else {
                            Map<String,String> errorMap =requestMapper.readValue(result.getBody().getObject().get("error").toString(), Map.class);
                            String code = errorMap.get("code");
                            String message = errorMap.get("message");
                            Exception ex = new BaseException(code,message, ResponseCode.BAD_REQUEST.getCode());
                            sender.tell(ex,self());
                            logger.error("Register:register:exception occurred:");
                        }
                    }
                }
            }, getContext().dispatcher());
        }
    }

    private void updateUserPersonMap(String userId, String personId) {
        FaceUtil.getPersonToUserIdMapper().put(personId+"", userId);
        FaceUtil.getUserToPersonIdMapper().put(userId, personId+"");
        FaceUtil.updateCassandra(userId, personId.toString());
    }

    private Future<HttpResponse<JsonNode>> createPerson(String userId) {
        String uri = "https://devcon-face.cognitiveservices.azure.com/face/v1.0/persongroups/" + System.getenv("person_group") + "/persons";
        Future<HttpResponse<JsonNode>> f = future(
                () -> {
                    Map<String, String> req = new WeakHashMap<>();
                    req.put("name", userId);
                    return FaceUtil.makeSyncPostCall(uri, requestMapper.writeValueAsString(req));
                }, getContext().dispatcher());
        return f;
    }

    private void trainUserGroup() {
        String uri = "https://devcon-face.cognitiveservices.azure.com/face/v1.0/persongroups/" + System.getenv("person_group") + "/train";
        Future<HttpResponse<JsonNode>> f = future(
                () -> {
                    Map<String, String> req = new WeakHashMap<>();
                    return FaceUtil.makeSyncPostCall(uri, requestMapper.writeValueAsString(req));
                }, getContext().dispatcher());
    }

    private Future<HttpResponse<JsonNode>> addFaceToPersonId(String personId, String imageUrl) {
        String uri = "https://devcon-face.cognitiveservices.azure.com/face/v1.0/persongroups/" + System.getenv("person_group") + "/persons/" +
                personId + "/persistedFaces";
        Future<HttpResponse<JsonNode>> f = future(
                () -> {
                    Map<String, String> req = new WeakHashMap<>();
                    req.put("url", imageUrl);
                    return FaceUtil.makeSyncPostCall(uri, requestMapper.writeValueAsString(req));
                }, getContext().dispatcher());
        return f;
    }

    private String getPersonId(String userId) {
        return FaceUtil.getUserToPersonIdMapper().get(userId);
    }


}
