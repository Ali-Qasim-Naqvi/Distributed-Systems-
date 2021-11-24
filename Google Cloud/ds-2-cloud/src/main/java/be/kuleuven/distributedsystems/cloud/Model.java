package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import com.google.api.core.ApiFuture;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.*;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;

@Component
public class Model {
    private List<Booking> bookings = new ArrayList<>();
    String API_KEY = "wCIoTqec6vGJijW2meeqSokanZuqOL";
    String baseURL = "https://reliabletheatrecompany.com/";
    @Autowired String projectId;
    String topicId = "confirm-quote";
    String subscriptionId = "confirm-quote-subscription";
//    String pushEndPoint = "http://localhost:8080/worker/confirmQuote";
    String pushEndPoint = "http://localhost:3000/messages";
    @Autowired WebClient.Builder webClientBuilder;

    public List<Show> getShows() {
        var shows = webClientBuilder
                                    .baseUrl(baseURL)
                                    .build()
                                    .get()
                                    .uri(uriBuilder -> uriBuilder
                                            .pathSegment("shows")
                                            .queryParam("key",API_KEY)
                                            .build())
                                    .retrieve()
                                    .bodyToMono(new ParameterizedTypeReference<CollectionModel<Show>>() {})
                                    .block()
                                    .getContent();
        return new ArrayList<>(shows);
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
                .block();
        return ticket;
    }

    public List<Booking> getBookings(String customer) {
        List<Booking> bookingList = bookings.stream().filter(booking -> booking.getCustomer().equals(customer)).collect(Collectors.toList());
        return bookingList;
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

        String hostport = "localhost:8083";
        ManagedChannel channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build();
        TransportChannelProvider channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
        CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

        TopicName topicName = TopicName.of(projectId, topicId);

        Publisher publisher = null;
        try(TopicAdminClient topicAdminClient = TopicAdminClient.create(TopicAdminSettings.newBuilder().setTransportChannelProvider(channelProvider).setCredentialsProvider(credentialsProvider).build())) {
            Topic topic = topicAdminClient.createTopic(topicName);
            // Create a publisher instance with default settings bound to the topic
            publisher = Publisher.newBuilder(topicName).setChannelProvider(channelProvider).setCredentialsProvider(credentialsProvider).build();
            try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(SubscriptionAdminSettings.newBuilder().setTransportChannelProvider(channelProvider).setCredentialsProvider(credentialsProvider).build())) {
                ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);
                PushConfig pushConfig = PushConfig.newBuilder().setPushEndpoint(pushEndPoint).build();

                // Create a push subscription with default acknowledgement deadline of 10 seconds.
                // Messages not successfully acknowledged within 10 seconds will get resent by the server.
                Subscription subscription = subscriptionAdminClient.createSubscription(subscriptionName, topicName, pushConfig, 60);
                System.out.println("Created push subscription: " + subscription.getName());
            }
            QuotesWrapper quotesWrapper = new QuotesWrapper(quotes,customer);
            String quotesWrapperString = quotesWrapper.toString(quotesWrapper);
            ByteString data = ByteString.copyFromUtf8(quotesWrapperString);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

            // Once published, returns a server-assigned message id (unique within the topic)
            ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);

            String messageId = messageIdFuture.get();
            System.out.println("Published message ID: " + messageId);
        } finally {
            if (publisher != null) {
                // When finished with the publisher, shutdown to free up resources.
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        }


    }
}
