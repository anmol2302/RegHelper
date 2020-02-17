package org.sunbird.face;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.sunbird.BaseActor;
import org.sunbird.Constants;
import org.sunbird.actor.core.ActorConfig;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import scala.concurrent.Future;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

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
        Response response = new Response();
        response.put("result", Constants.SUCCESS);
        sender().tell(response,self());
        register(request);
    }

    private void register(Request request) {
        Map<String,Object> req = request.getRequest();
        Future<String> f = future(new Callable<String>() {
            public String call() {
                return "Hello" + "World";
            }
        }, getContext().dispatcher());
    }


}
