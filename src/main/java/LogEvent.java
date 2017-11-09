import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class LogEvent implements RequestHandler<SNSEvent, Object> {

  public Object handleRequest(SNSEvent request, Context context) {

    String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());

    context.getLogger().log("Invocation started: " + timeStamp);

    context.getLogger().log("Request is NULL: " + (request == null));

    context.getLogger().log("Number of Records: " + (request.getRecords().size()));

    context.getLogger().log("Record Message:" + request.getRecords().get(0).getSNS().getMessage());

    timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());

    context.getLogger().log("Invocation completed: " + timeStamp);

    return null;
  }

}

