package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;

import com.google.protobuf.Message;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
public class Worker {
    String API_KEY = "wCIoTqec6vGJijW2meeqSokanZuqOL";
    @Autowired String projectId;
    String topicId = "confirm-quote";
    @Autowired public Worker (){
        System.out.println("Just a message" );
    }
    @Autowired WebClient.Builder webClientBuilder;
    private final Gson gson = new Gson();
    private final JsonParser jsonParser = new JsonParser();


    @PostMapping("/confirmQuote")
    public ResponseEntity<Void> confirmQuote(@RequestBody String body) throws IOException, ClassNotFoundException, NullPointerException {
        System.out.println("[createQuote:pushSubscriber] Body is : " + body);
        JsonElement jsonRoot = jsonParser.parse(body);
        String messageStr = jsonRoot.getAsJsonObject().get("message").toString();
        System.out.println("[createQuote:pushSubscriber] jsonRoot is : " + messageStr);
        Object obj = JSONValue.parse(messageStr);
        JSONObject jsonObject = (JSONObject) obj;
        String messageStr1 = jsonObject.get("attributes").toString();
        Object obj2 = JSONValue.parse(messageStr1);
        JSONObject jsonObject2 = (JSONObject) obj2;
        String messageStr2 = jsonObject2.get("quoteWrapper").toString();
        System.out.println("[createQuote:pushSubscriber] Decoded is  " + messageStr2 );
        byte [] data = Base64.getDecoder().decode(messageStr2);
        System.out.println("[createQuote:pushSubscriber] After Byte " );
        ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( data ) );
        Object o  = ois.readObject();
        ois.close();

        System.out.println("[createQuote:pushSubscriber] After Byte " );

        QuotesWrapper quotesWrapper = (QuotesWrapper) o;
        String customer = quotesWrapper.getCustomer();
        System.out.println("[createQuote:pushSubscriber] Customer is : " + customer);

        List<Ticket> tickets = new ArrayList<>();
        for(var quote:quotesWrapper.getQuotes()){
            System.out.println("[createQuote:pushSubscriber] Quote show ID is : " + quote.getShowId());
            tickets.add(webClientBuilder
                    .baseUrl("https://" + quote.getCompany())
                    .build()
                    .put()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("shows",quote.getShowId().toString(),"seats", quote.getSeatId().toString(), "ticket")
                            .queryParam("customer",customer)
                            .queryParam("key",API_KEY)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Ticket>() {})
                    .block());
        }
        Booking tempBooking = new Booking(UUID.randomUUID(), LocalDateTime.now(),tickets,customer);
        //bookings.add(tempBooking);
        return ResponseEntity.ok().build();
    }
}
