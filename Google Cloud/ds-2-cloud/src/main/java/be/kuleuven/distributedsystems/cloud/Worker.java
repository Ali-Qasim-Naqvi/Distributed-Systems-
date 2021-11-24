package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController

@RequestMapping("/worker")
public class Worker {
    String API_KEY = "wCIoTqec6vGJijW2meeqSokanZuqOL";
    @Autowired String projectId;
    String topicId = "confirm-quote";
    String subscriptionId = "worker-confirm-quote";
    @Autowired WebClient.Builder webClientBuilder;

    @PostMapping("/confirmQuote")
    public ResponseEntity confirmQuote(@RequestBody JSONObject body) throws IOException, ClassNotFoundException {
        System.out.println("This is the confirmQuote message" + body);
        JSONObject messageBody = (JSONObject) body.get("messages");
        JSONObject messageData = (JSONObject) messageBody.get("message");
        String message = String.valueOf(Base64.getDecoder().decode(String.valueOf(messageData.get("data"))));
        QuotesWrapper quotesWrapper = null;
        quotesWrapper = (QuotesWrapper) quotesWrapper.fromString(message);
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
        return (ResponseEntity) ResponseEntity.status(200);
    }
}
