package org.sunbird.face;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.sunbird.BaseActor;
import org.sunbird.Constants;
import org.sunbird.actor.core.ActorConfig;
import org.sunbird.request.Request;
import org.sunbird.response.Response;

import java.util.concurrent.CompletableFuture;

@ActorConfig(
        tasks = {"identify"},
        dispatcher = "",
        asyncTasks = {}
)
public class Identify extends BaseActor {

    private static Logger logger = Logger.getLogger(Identify.class);

    @Override
    public void onReceive(Request request) throws Throwable {
        Response response = new Response();
        response.put("userId", "123456789");
        sender().tell(response,self());
        identify(request);
    }

    private void identify(Request request) {

        CompletableFuture.supplyAsync(()->{

            return null;
        });
    }
}
