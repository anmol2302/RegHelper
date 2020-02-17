package controllers.face;

import controllers.BaseController;
import org.sunbird.BaseException;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class Identify extends BaseController {

    public CompletionStage<Result> identify() throws BaseException {
        return handleRequest(request(),null,"identify");
    }
}
