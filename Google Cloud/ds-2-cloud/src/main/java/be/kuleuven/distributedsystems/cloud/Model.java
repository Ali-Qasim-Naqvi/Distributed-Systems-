package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.controller.AuthController;
import be.kuleuven.distributedsystems.cloud.entities.*;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.pubsub.v1.*;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.awt.print.Book;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;

@Component
public class Model {

    String API_KEY = "wCIoTqec6vGJijW2meeqSokanZuqOL";
    String baseURL = "https://reliabletheatrecompany.com/";
    String baseURL2 = "https://unreliabletheatrecompany.com/";
    @Autowired String projectId;
    @Autowired WebClient.Builder webClientBuilder;
    @Autowired private Publisher pubSubPublisher;
    @Autowired List<Booking> bookings;
    @Autowired Firestore db;

    public List<Show> getShows() {
        var shows1 = webClientBuilder
                                    .baseUrl(baseURL)
                                    .build()
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .pathSegment("shows")
                                            .queryParam("key",API_KEY)
                                            .build())
                                    .retrieve()
                                    .bodyToMono(new ParameterizedTypeReference<CollectionModel<Show>>() {})
                                    .retry(3)
                                    .block()
                                    .getContent();
        var shows2 = webClientBuilder
                .baseUrl(baseURL2)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows")
                        .queryParam("key",API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<Show>>() {})
                .retry(3)
                .block()
                .getContent();
        List<Show> showList1 = new ArrayList<>(shows1);
        List<Show> showList2 = new ArrayList<>(shows2);
        List<Show> finalList = new ArrayList<>();
        finalList.addAll(showList1);
        finalList.addAll(showList2);
        return finalList;
    }

    public Show getShow(String company, UUID showId) {
        var show = webClientBuilder
                .baseUrl("https://"+company)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows",showId.toString())
                        .queryParam("key",API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Show>() {})
                .retry(3)
                .block();
        return show;
    }

    public List<LocalDateTime> getShowTimes(String company, UUID showId) {
        var showTimes = webClientBuilder
                .baseUrl("https://"+company)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows",showId.toString(),"times")
                        .queryParam("key",API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<LocalDateTime>>() {})
                .retry(3)
                .block()
                .getContent();
        return new ArrayList<>(showTimes);
    }

    public List<Seat> getAvailableSeats(String company, UUID showId, LocalDateTime time) {
        var seats = webClientBuilder
                .baseUrl("https://"+company)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows",showId.toString(),"seats")
                        .queryParam("time",time)
                        .queryParam("available","true")
                        .queryParam("key",API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<Seat>>() {})
                .retry(3)
                .block()
                .getContent();
        return new ArrayList<>(seats);
    }

    public Seat getSeat(String company, UUID showId, UUID seatId) {
        var seat = webClientBuilder
                .baseUrl("https://"+company)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows",showId.toString(),"seats", seatId.toString())
                        .queryParam("key",API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Seat>() {})
                .retry(3)
                .block();
        return seat;
    }

    public Ticket getTicket(String company, UUID showId, UUID seatId) {
        var ticket = webClientBuilder
                .baseUrl("https://"+company)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows",showId.toString(),"seats", seatId.toString(), "ticket")
                        .queryParam("key",API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Ticket>() {})
                .retry(3)
                .block();
        return ticket;
    }

    public List<Booking> getBookings(String customer) throws ExecutionException, InterruptedException {
        // asynchronously retrieve all users
        List<Booking> bookings2 = new ArrayList<>();
        ApiFuture<QuerySnapshot> query = db.collection(customer).get();
        // ...
        // query.get() blocks on response
        QuerySnapshot querySnapshot = query.get();
        List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
        for (QueryDocumentSnapshot document : documents) {
            //Booking tempBooking = document.toObject(Booking.class);

            //Map<String, Object> tempMap = document.getData();
//            Map<String,Object> tempId= (Map<String, Object>) document.get("id");
//            UUID Id = new UUID((long)tempId.get("mostSignificantBits"),(long)tempId.get("leastSignificantBits"));
//            Map<String,Object> tempTime= (Map<String, Object>) document.get("time");
//            UUID Id = new UUID((long)tempId.get("mostSignificantBits"),(long)tempId.get("mostSignificantBits"));

//            bookings2.add(new Booking((UUID) tempMap.get("Id"),(LocalDateTime) tempMap.get("Time"),(List<Ticket>) tempMap.get("Tickets"),(String) tempMap.get("Customer")));
        }
        return bookings2.stream().filter(booking -> booking.getCustomer().equals(customer)).collect(Collectors.toList());
    }

    public List<Booking> getAllBookings() {

        return bookings;
    }

    public Set<String> getBestCustomers() {
        Map<String, Integer> customerBookings = new HashMap<String, Integer>();
        for(var entry:bookings){
            customerBookings.put(entry.getCustomer(),0);
        }
        for(var entry:bookings){
            customerBookings.put(entry.getCustomer(),customerBookings.get(entry.getCustomer()) + entry.getTickets().size());
        }
        Integer largestVal = null;
        Set<String> customers = new HashSet<>();
        for (Map.Entry<String, Integer> i : customerBookings.entrySet()){
            if (largestVal == null || largestVal  < i.getValue()){
                largestVal = i.getValue();
                customers.clear();
                customers.add(i.getKey());
            }else if (largestVal == i.getValue()){
                customers.add(i.getKey());
            }
        }
//        Stream<Map.Entry<String, Integer>> customerBookings2 = customerBookings.entrySet().stream().filter(customer -> customer.getValue().equals(Collections.max(customerBookings.values())));
//        Set<String> customers = customerBookings2.keySet();
        return customers;
    }

    public void confirmQuotes(List<Quote> quotes, String customer) throws InterruptedException, IOException, ExecutionException {
        Publisher publisher = this.pubSubPublisher;
        QuotesWrapper quotesWrapper = new QuotesWrapper(quotes,customer);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream( baos );
        outputStream.writeObject( quotesWrapper );
        outputStream.close();
        String quotesWrapperString = Base64.getEncoder().encodeToString(baos.toByteArray());
        System.out.println("Quote Wrapper String is " + quotesWrapperString);
        ByteString data = ByteString.copyFromUtf8(quotesWrapperString);
        System.out.println("Byte String is " + data.toString());
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder().putAttributes("quoteWrapper",quotesWrapperString).build();
        // Once published, returns a server-assigned message id (unique within the topic)
        ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
        String messageId = messageIdFuture.get();
        System.out.println("Published message ID: " + messageId);

        try {
            // Add an asynchronous callback to handle success / failure
            ApiFutures.addCallback(messageIdFuture, new ApiFutureCallback<String>() {
                        @Override
                        public void onFailure(Throwable throwable) {
                            if (throwable instanceof ApiException) {
                                ApiException apiException = ((ApiException) throwable);
                                // details on the API exception
                                System.out.println(apiException.getStatusCode().getCode());
                                System.out.println(apiException.isRetryable());
                            }
                            System.out.println("Error publishing message : " + messageId);
                            System.out.println("Error publishing error : " + throwable.getMessage());
                            System.out.println("Error publishing cause : " + throwable.getCause());
                        }

                        @Override
                        public void onSuccess(String messageId) {
                            // Once published, returns server-assigned message ids (unique within the topic)
                            System.out.println("Successfully Executed: " + messageId);
                        }
                    },
                    MoreExecutors.directExecutor());
        } catch (Exception e) {
            e.printStackTrace();
        }
//        if (publisher != null) {
//            // When finished with the publisher, shutdown to free up resources.
////            publisher.awaitTermination(1, TimeUnit.MINUTES);
//        }
    }
}
