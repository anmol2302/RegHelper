package controllers.face;


import controllers.BaseController;
import org.sunbird.BaseException;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class IdentifyMultiple extends BaseController {

    public CompletionStage<Result> identifyMultiple() throws BaseException {
        return handleRequest(request(),null,"multiple");
    }
}
