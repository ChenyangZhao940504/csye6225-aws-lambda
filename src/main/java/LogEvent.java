import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.UUID;

public class LogEvent implements RequestHandler<SNSEvent, Object> {

  public Object handleRequest(SNSEvent request, Context context) {

    String email = request.getRecords().get(0).getSNS().getMessage();

    String uuid = UUID.randomUUID().toString();
    String token = null;

    long ttl = System.currentTimeMillis() / 1000L + 1200;

    AmazonDynamoDB amazonDynamoDB = new AmazonDynamoDBClient();
    DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
    Table table = dynamoDB.getTable("csye6225");

    QuerySpec spec = new QuerySpec().withKeyConditionExpression("email = :username")
            .withValueMap(new ValueMap().withString(":username", email));

    ItemCollection<QueryOutcome> items = table.query(spec);

    Iterator<Item> iterator = items.iterator();
    Item item = null;
    while (iterator.hasNext()) {
      item = iterator.next();
      token = item.getString("token");
      System.out.println(item.toJSONPretty());
    }
    if(item == null){
      try {
        PutItemOutcome outcome = table.putItem(new Item().
                withPrimaryKey("email", email).withString("token", uuid).withNumber("ttl",ttl));
        token = uuid;
      }
      catch (AmazonServiceException e) {
        System.err.println("Unable to add item: " + email + "with token " + uuid);
        System.err.println(e.getMessage());
      }
    }

    final String FROM = "assignment8@csye6225-fall2017-lijin3.me";
    final String SUBJECT = "Password Reset Email";
    final String HTMLBODY = "<h1>Amazon SES Application for Password Reset</h1>"
            + "<p>The password reset link: " + "<a href='https://aws.amazon.com/ses/'>" + "http://csye6225-fall2017-lijin3.me/reset?email="
            + email + "&token=" + token + "</a>";
    final String TEXTBODY = "This email was sent through Amazon SES "
            + "using the AWS SDK for Java.";

    try {
      AmazonSimpleEmailService client =
              AmazonSimpleEmailServiceClientBuilder.standard()
                      .withRegion(Regions.US_EAST_1).build();
      SendEmailRequest sendEmailRequest = new SendEmailRequest()
              .withDestination(
                      new Destination().withToAddresses(email))
              .withMessage(new Message()
                      .withBody(new Body()
                              .withHtml(new Content()
                                      .withCharset("UTF-8").withData(HTMLBODY))
                              .withText(new Content()
                                      .withCharset("UTF-8").withData(TEXTBODY)))
                      .withSubject(new Content()
                              .withCharset("UTF-8").withData(SUBJECT)))
              .withSource(FROM);
      client.sendEmail(sendEmailRequest);
      System.out.println("Email sent successfully!");
    } catch (Exception ex) {
      System.out.println("The email was not sent. Error message: "
              + ex.getMessage());
    }

    return null;
  }

}

