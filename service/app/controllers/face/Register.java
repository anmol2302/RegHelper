package controllers.face;

import controllers.BaseController;
import org.sunbird.BaseException;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class Register extends BaseController {

    public CompletionStage<Result> register() throws BaseException {
        return handleRequest(request(),null,"register");
    }
}
