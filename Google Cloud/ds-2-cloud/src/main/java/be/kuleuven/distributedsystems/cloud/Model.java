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
import com.google.cloud.firestore.*;
import com.google.cloud.pubsub.v1.*;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
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
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    @Autowired Firestore db;

    public boolean checkDataBase() throws Exception {
        //ApiFuture<QuerySnapshot> future = firestore.collection("shows").get();
        //List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        Iterable<CollectionReference> collections = db.listCollections();
        boolean isCreated = false;
        for (CollectionReference collRef : collections) {
            if (collRef.getId().equals("shows")) {
                isCreated = true;
            }
        }
        if (!isCreated) {
            String payload = readFileAsString();
            Object obj= JSONValue.parse(payload);
            JSONObject jsonObject = (JSONObject) obj;
            JSONArray jsonArray = (JSONArray) jsonObject.get("shows");

            for(int i = 0; i < jsonArray.size(); i++) {
                ShowWrapper showWrapper = convertFromJSONToMyClass((JSONObject) jsonArray.get(i));
                ShowWrapperUpload showWrapperUpload = new ShowWrapperUpload(showWrapper.getName(), showWrapper.getLocation(),showWrapper.getImage(),showWrapper.getSeats());
                db.collection("shows").document(UUID.randomUUID().toString()).set(showWrapperUpload);
            }
            return false;
        }
        return true;
    }
    public static ShowWrapper convertFromJSONToMyClass(JSONObject json) {
        if (json == null) {
            return null;
        }
        Gson gson = new Gson();
        return gson.fromJson(json.toString(), ShowWrapper.class);
    }

    public String readFileAsString()throws Exception
    {
        return new String(Files.readAllBytes(Paths.get("src/main/resources/data.json")));
    }

    public UUID stringToUUID (String uuid){
        return UUID.fromString(uuid);
    }

    public List<Show> getShows() throws Exception {
        checkDataBase();
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

        ApiFuture<QuerySnapshot> future =
                db.collection("shows").get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<Show> shows3 = new ArrayList<>();
        ShowWrapperUpload tempShowWrapper = null;
        for ( var  document:documents){
            tempShowWrapper = document.toObject(ShowWrapperUpload.class);
            shows3.add(new Show("Local Company", UUID.fromString(document.getId()),(String)document.get("name"), (String) document.get("location"), (String) document.get("image")));
        }

        List<Show> showList1 = new ArrayList<>(shows1);
        List<Show> showList2 = new ArrayList<>(shows2);
        List<Show> showList3 = new ArrayList<>(shows3);
        List<Show> finalList = new ArrayList<>();
        finalList.addAll(showList1);
        finalList.addAll(showList2);
        finalList.addAll(showList3);

        return finalList;
    }

    public Show getShow(String company, UUID showId) throws ExecutionException, InterruptedException {
        if (company.equals( "Local Company")){
            ApiFuture<DocumentSnapshot> future =
                    db.collection("shows").document(showId.toString()).get();
            DocumentSnapshot document = future.get();
            return new Show("Local Company", UUID.fromString(document.getId()), (String)document.get("name"), (String) document.get("location"), (String) document.get("image"));
        }
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

    public List<LocalDateTime> getShowTimes(String company, UUID showId) throws ExecutionException, InterruptedException {
        if (company.equals( "Local Company")){
            ApiFuture<DocumentSnapshot> future = db.collection("shows").document(showId.toString()).get();
            DocumentSnapshot document = future.get();
            ShowWrapperUpload tempShowWrapper = document.toObject(ShowWrapperUpload.class);
            List<SeatWrapperExtended> tempSeatWrapper = tempShowWrapper.getSeats();
            Set<LocalDateTime> setTime = new HashSet<>();
            for(var entry : tempSeatWrapper){
                String times = entry.getTime();
                setTime.add(LocalDateTime.parse(times));
            }
            /*Map<String,Object> updates = new HashMap<>();
            for(int tempIndex = 0;tempIndex<tempSeatWrapper.size();tempIndex++){
                updates.put("seats."+String.valueOf(tempIndex)+".seatID",UUID.randomUUID().toString());
            }
            DocumentReference docRef = db.collection("shows").document(showId.toString());
            ApiFuture<WriteResult> writeResultApiFuture = docRef.update(updates);
           */ return new LinkedList<LocalDateTime>(setTime);
        }
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

    public List<Seat> getAvailableSeats(String company, UUID showId, LocalDateTime time) throws ExecutionException, InterruptedException {
        if (company.equals( "Local Company")) {
            ApiFuture<DocumentSnapshot> future = db.collection("shows").document(showId.toString()).get();
            DocumentSnapshot document = future.get();
            ShowWrapperUpload tempShowWrapper = document.toObject(ShowWrapperUpload.class);
            List<SeatWrapperExtended> tempSeatWrapper = tempShowWrapper.getSeats();
            List<Seat> tempSeats = new ArrayList<>();
            for(var entry : tempSeatWrapper){
//                System.out.println("Availability : " + entry.getAvailability());
                if(entry.getTime().substring(0,entry.getTime().length()-3).equals(time.toString()) && entry.getAvailability()){
                    tempSeats.add(new Seat(company,showId,UUID.fromString(entry.getSeatId()), time, entry.getType(), entry.getName(), entry.getPrice()));
//                    System.out.println("The seat is : " + entry.getName() );
                }
            }

            return tempSeats;
        }

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

    public Seat getSeat(String company, UUID showId, UUID seatId) throws ExecutionException, InterruptedException {
        if (company.equals( "Local Company")) {
            ApiFuture<DocumentSnapshot> future = db.collection("shows").document(showId.toString()).get();
            DocumentSnapshot document = future.get();
            ShowWrapperUpload tempShowWrapper = document.toObject(ShowWrapperUpload.class);
            List<SeatWrapperExtended> tempSeatWrapper = tempShowWrapper.getSeats();

            for(var entry : tempSeatWrapper){
                if(entry.getSeatId().equals(seatId.toString())){
                    return new Seat(company,showId,UUID.fromString(entry.getSeatId()), LocalDateTime.parse(entry.getTime()), entry.getType(), entry.getName(), entry.getPrice());
                }
            }

        }
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

    public List<Booking> getBookings(String customer) throws ExecutionException, InterruptedException, IOException, ClassNotFoundException {
        List<Booking> bookings = new ArrayList<>();
        ApiFuture<QuerySnapshot> query = db.collection(customer).get();

        QuerySnapshot querySnapshot = query.get();
        List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
        for (QueryDocumentSnapshot document : documents) {
            byte [] data = Base64.getDecoder().decode( document.getString("Booking"));
            ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( data ) );
            Object o  = ois.readObject();
            ois.close();
            BookingWrapper bookingWrapper = (BookingWrapper) o;
            bookings.add(new Booking(bookingWrapper.getId(),bookingWrapper.getTime(),bookingWrapper.getTickets(),bookingWrapper.getCustomer()));
        }
        return bookings;
    }

    public List<Booking> getAllBookings() throws ExecutionException, InterruptedException, IOException, ClassNotFoundException {
        List<Booking> bookings = new ArrayList<>();
        Iterable<CollectionReference> collections = db.listCollections();
        for (CollectionReference collRef : collections) {
            if (!collRef.getId().equals("shows")) {
                Iterable<DocumentReference> documents = collRef.listDocuments();
                for (DocumentReference document : documents) {
                    ApiFuture<DocumentSnapshot> query = document.get();
                    DocumentSnapshot querySnapshot = query.get();
                    byte[] data = Base64.getDecoder().decode(querySnapshot.getString("Booking"));
                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
                    Object o = ois.readObject();
                    ois.close();
                    BookingWrapper bookingWrapper = (BookingWrapper) o;
                    bookings.add(new Booking(bookingWrapper.getId(), bookingWrapper.getTime(), bookingWrapper.getTickets(), bookingWrapper.getCustomer()));
                }
            }
        }
        return bookings;
    }

    public Set<String> getBestCustomers() throws IOException, ExecutionException, InterruptedException, ClassNotFoundException {
        List<Booking> bookings = getAllBookings();

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
