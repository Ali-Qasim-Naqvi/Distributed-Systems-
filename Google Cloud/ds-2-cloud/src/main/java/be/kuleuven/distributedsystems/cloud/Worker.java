package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.Seat;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
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
import org.springframework.context.annotation.Bean;
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
import java.util.*;
import java.util.concurrent.ExecutionException;
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
    @Autowired Firestore db;
    @Autowired WebClient.Builder webClientBuilder;
    private final Gson gson = new Gson();
    private final JsonParser jsonParser = new JsonParser();
    private List<Booking> bookings = new ArrayList<>();

    @Bean
    public List<Booking> returnbookings(){
        return bookings;
    }

    @PostMapping("/confirmQuote")
    public ResponseEntity<Void> confirmQuote(@RequestBody String body) throws IOException, ClassNotFoundException, NullPointerException, ExecutionException, InterruptedException {
        JsonElement jsonRoot = jsonParser.parse(body);
        String messageStr = jsonRoot.getAsJsonObject().get("message").toString();
        Object obj = JSONValue.parse(messageStr);
        JSONObject jsonObject = (JSONObject) obj;
        String messageStr1 = jsonObject.get("attributes").toString();
        Object obj2 = JSONValue.parse(messageStr1);
        JSONObject jsonObject2 = (JSONObject) obj2;
        String messageStr2 = jsonObject2.get("quoteWrapper").toString();
        byte [] data = Base64.getDecoder().decode(messageStr2);
        ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( data ) );
        Object o  = ois.readObject();
        ois.close();

        QuotesWrapper quotesWrapper = (QuotesWrapper) o;
        String customer = quotesWrapper.getCustomer();
        System.out.println("[createQuote:pushSubscriber] Customer is : " + customer);

        List<Ticket> tickets = new ArrayList<>();
        boolean null_ticket = false;
        for(var quote:quotesWrapper.getQuotes()){
            if (quote.getCompany().equals( "Local Company")) {
                ApiFuture<DocumentSnapshot> future = db.collection("shows").document(quote.getShowId().toString()).get();
                DocumentSnapshot document = future.get();
                ShowWrapperUpload tempShowWrapper = document.toObject(ShowWrapperUpload.class);
                List<SeatWrapperExtended> tempSeatWrapper = tempShowWrapper.getSeats();

                int i = 0;
                for(var entry : tempSeatWrapper){
                    if(entry.getSeatId().equals(quote.getSeatId().toString()) && entry.getAvailability()){
                        tickets.add(new Ticket(quote.getCompany(),quote.getShowId(),UUID.fromString(entry.getSeatId()), UUID.randomUUID(), customer));
                        Map<String,Object> updates = new HashMap<>();
                        updates.put("availability",true);
                        updates.put("name",entry.getName());
                        updates.put("price",entry.getPrice());
                        updates.put("seatId",entry.getSeatId());
                        updates.put("time",entry.getTime());
                        updates.put("type",entry.getType());
                        DocumentReference docRef = db.collection("shows").document(quote.getShowId().toString());
                        ApiFuture<WriteResult> arrayRm = docRef.update("seats",FieldValue.arrayRemove(updates));
                        System.out.println("Update time : " + arrayRm.get());
                        updates.put("availability",false);
                        ApiFuture<WriteResult> arrayUnion = docRef.update("seats",FieldValue.arrayUnion(updates));
                        System.out.println("Update time : " + arrayUnion.get());
                    }
                    else if (entry.getSeatId().equals(quote.getSeatId().toString()) && !entry.getAvailability()){
                        tickets.add(null);
                    }
                    i++;
                }

            }
            else{
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
                        .retry(3)
                        .block());
            }
            System.out.println("[createQuote:pushSubscriber] Quote show ID is : " + quote.getShowId());
        }

        for (var entry:tickets){
            if (entry.getTicketId() == null) {
                null_ticket = true;
                tickets.remove(entry);
            }
        }
        if(null_ticket){
            for(var entry:tickets) {
                webClientBuilder
                        .baseUrl("https://" + entry.getCompany())
                        .build()
                        .delete()
                        .uri(uriBuilder -> uriBuilder
                                .pathSegment("shows", entry.getShowId().toString(), "seats", entry.getSeatId().toString(), "ticket", entry.getTicketId().toString())
                                .queryParam("key", API_KEY)
                                .build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Ticket>() {})
                        .retry(3)
                        .block();
            }
            return ResponseEntity.status(201).build();
        }
        Booking tempBooking = new Booking(UUID.randomUUID(), LocalDateTime.now(),tickets,customer);
        BookingWrapper tempBooking2 = new BookingWrapper(UUID.randomUUID(),LocalDateTime.now(),tickets,customer);
        DocumentReference docRef = db.collection(tempBooking2.getCustomer()).document(tempBooking2.getId().toString());
        Map<String, Object> tempMap = new HashMap<>();
        tempMap.put("Booking",tempBooking2.toString(tempBooking2) );
        //asynchronously write data
        ApiFuture<WriteResult> result = docRef.set(tempMap);
        // ...
        // result.get() blocks on response
        System.out.println("Update time : " + result.get().getUpdateTime());

        bookings.add(tempBooking);
        return ResponseEntity.status(200).build();
    }
}
