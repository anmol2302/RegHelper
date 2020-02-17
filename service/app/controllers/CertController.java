package controllers;

import org.sunbird.BaseException;
import org.sunbird.request.Request;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

/**
 * this controller will help you in understanding the process of passing request to Actors with operation.
 */
public class CertController extends BaseController {

    public CompletionStage<Result> certProcess() throws BaseException {
        return handleRequest(request(),null,"process");
    }

}
