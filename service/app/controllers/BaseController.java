package controllers;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.Application;
import org.sunbird.BaseException;
import org.sunbird.message.Localizer;
import org.sunbird.request.Request;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;
import utils.JsonKey;
import utils.RequestMapper;
import utils.RequestValidatorFunction;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * This controller we can use for writing some common method to handel api request.
 * CompletableFuture: A Future that may be explicitly completed (setting its value and status), and
 * may be used as a CompletionStage, supporting dependent functions and actions that trigger upon
 * its completion. CompletionStage: A stage of a possibly asynchronous computation, that performs an
 * action or computes a value when another CompletionStage completes
 *
 * @author Anmol
 */
public class BaseController extends Controller {

    protected static ObjectMapper mapper = new ObjectMapper();
    protected static final String dummyResponse =
            "{\"id\":\"api.200ok\",\"ver\":\"v1\",\"ts\":\"2019-01-17 16:53:26:286+0530\",\"params\":{\"resmsgid\":null,\"msgid\":\"8e27cbf5-e299-43b0-bca7-8347f7ejk5abcf\",\"err\":null,\"status\":\"success\",\"errmsg\":null},\"responseCode\":\"OK\",\"result\":{\"response\":{\"response\":\"SUCCESS\",\"errors\":[]}}}";

    /**
     * We injected HttpExecutionContext to decrease the response time of APIs.
     */
    @Inject
    private HttpExecutionContext httpExecutionContext;
    protected static Localizer localizerObject = Localizer.getInstance();

    /**
     * This method will return the current timestamp.
     *
     * @return long
     */
    public long getTimeStamp() {
        return System.currentTimeMillis();
    }

    protected ActorRef getActorRef(String operation) throws BaseException {
        return Application.getInstance().getActorRef(operation);
    }

    /**
     * this method will take org.sunbird.Request and a validation function and lastly operation(Actor operation)
     * this method is validating the request and ,
     * this method is used to handle all the request type which has requestBody
     *
     * @param request
     * @param validatorFunction
     * @param operation
     * @return
     */
    public CompletionStage<Result> handleRequest(play.mvc.Http.Request req,Request request, RequestValidatorFunction validatorFunction, String operation) {
        try {
            if (validatorFunction != null) {
                validatorFunction.apply(request);
            }
            return new RequestHandler().handleRequest(request, httpExecutionContext, operation,req);
        } catch (BaseException ex) {
            return CompletableFuture.supplyAsync(() -> StringUtils.EMPTY)
                    .thenApply(result -> badRequest(Json.toJson(RequestHandler.prepareFailureMessage(ex,req))));
        } catch (Exception ex) {
            return CompletableFuture.supplyAsync(() -> StringUtils.EMPTY)
                    .thenApply(result -> internalServerError(Json.toJson(RequestHandler.prepareFailureMessage(ex,req))));        }
    }

    /**
     * this method will take play.mv.http request and a validation function and lastly operation(Actor operation)
     * this method is validating the request and ,
     * it will map the request to our sunbird Request class and make a call to requestHandler which is internally calling ask to actor
     * this method is used to handle all the request type which has requestBody
     *
     * @param req
     * @param validatorFunction
     * @param operation
     * @return
     */
    public CompletionStage<Result> handleRequest(play.mvc.Http.Request req, RequestValidatorFunction validatorFunction, String operation) {
        try {
            Request request = (Request) RequestMapper.mapRequest(req, Request.class);
            request.setTimeout(5);
            if (validatorFunction != null) {
                validatorFunction.apply(request);
            }
            return new RequestHandler().handleRequest(request, httpExecutionContext, operation,req);
        } catch (BaseException ex) {
            return CompletableFuture.supplyAsync(() -> StringUtils.EMPTY)
                    .thenApply(result -> badRequest(Json.toJson(RequestHandler.prepareFailureMessage(ex,req))));
        } catch (Exception ex) {
            return CompletableFuture.supplyAsync(() -> StringUtils.EMPTY)
                    .thenApply(result -> internalServerError(Json.toJson(RequestHandler.prepareFailureMessage(ex,req))));
        }
    }


    public CompletionStage<Result> handleRequest() {
        CompletableFuture<String> cf = new CompletableFuture<>();
        cf.complete(dummyResponse);
        return cf.thenApplyAsync(Results::ok);
    }

    /**
     * This method is used specifically to handel Log Apis request this will set log levels and then
     * return the CompletionStage of Result
     *
     * @return
     */
    public CompletionStage<Result> handleLogRequest() {
        CompletableFuture<String> cf = new CompletableFuture<>();
        cf.complete(dummyResponse);
        return cf.thenApplyAsync(Results::ok);
    }

    /**
     * This method is responsible to convert Response object into json
     *
     * @param response
     * @return string
     */
    public static String jsonify(Object response) {
        try {
            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            return JsonKey.EMPTY_STRING;
        }
    }
}