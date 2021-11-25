package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.protobuf.ByteString;
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


    // Convert String to ByteString and make corresponding changes
    @PostMapping("/confirmQuote")
    public ResponseEntity<Void> confirmQuote(@RequestBody String body) throws IOException, ClassNotFoundException, NullPointerException {
        Object obj = JSONValue.parse(body);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject messageData = (JSONObject) jsonObject.get("message");
        String message = (String) messageData.get("data");
        System.out.println("[createQuote:pushSubscriber] Data is : " + message);
        byte [] data = Base64.getDecoder().decode( message);
        System.out.println("[createQuote:pushSubscriber] After Byte " );
        if(data == null){
            return ResponseEntity.ok().build();
        }
        ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( data ) );
//        ObjectInputStream ois = new ObjectInputStream(  message.chars()  );
        System.out.println("[createQuote:pushSubscriber] After OIS " );
        Object o  = ois.readObject();
        System.out.println("[createQuote:pushSubscriber] After ReadObject " );
        ois.close();
        System.out.println("[createQuote:pushSubscriber] After OIS " );
        QuotesWrapper quotesWrapper = (QuotesWrapper) o;
        System.out.println("[createQuote:pushSubscriber] After Wrapper " );
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
        System.out.println("worker-end ");
        return ResponseEntity.ok().build();
    }
}
