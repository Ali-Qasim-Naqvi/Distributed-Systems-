package be.kuleuven.distributedsystems.cloud;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.pubsub.v1.*;
import com.google.pubsub.v1.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.HypermediaWebClientConfigurer;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import com.google.cloud.firestore.FirestoreOptions.EmulatorCredentials;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.util.Objects;


@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@SpringBootApplication
public class Application {

    @Autowired String projectId;
    private String topicId = "DS-Cloud-2-Worker";
    private String subscriptionId = "confirm-quote-subscription";
    private String pushEndPoint = "http://localhost:8080/confirmQuote";
//    private String pushEndPoint = "http://localhost:3000/messages";
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        System.setProperty("server.port", System.getenv().getOrDefault("PORT", "8080"));

        // Apache JSP scans by default all JARs, which is not necessary, so disable it
        System.setProperty(org.apache.tomcat.util.scan.Constants.SKIP_JARS_PROPERTY, "*.jar");
        System.setProperty(org.apache.tomcat.util.scan.Constants.SCAN_JARS_PROPERTY, "taglibs-standard-spec-*.jar,taglibs-standard-impl-*.jar");

        // Start Spring Boot application
        ApplicationContext context = SpringApplication.run(Application.class, args);
    }

    @Bean
    public boolean isProduction() {
        return Objects.equals(System.getenv("GAE_ENV"), "standard");
    }

    @Bean
    public String projectId() {
        return "demo-distributed-systems-kul";
    }

    /*
     * You can use this builder to create a Spring WebClient instance which can be used to make REST-calls.
     */
    @Bean
    WebClient.Builder webClientBuilder(HypermediaWebClientConfigurer configurer) {
        return configurer.registerHypermediaTypes(WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
                .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs().maxInMemorySize(100 * 1024 * 1024)));
    }

    @Bean
    HttpFirewall httpFirewall() {
        DefaultHttpFirewall firewall = new DefaultHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        return firewall;
    }

    @Bean
    public Publisher pubsubPublisher() throws IOException {
        String hostport = "localhost:8083";
        ManagedChannel channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build();

        TransportChannelProvider channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
        CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

        TopicName topicName = TopicName.of(projectId, topicId);
        Publisher publisher = null;
        TopicAdminClient topicAdminClient = TopicAdminClient.create(TopicAdminSettings.newBuilder().setTransportChannelProvider(channelProvider).setCredentialsProvider(credentialsProvider).build());
        Topic topic = topicAdminClient.createTopic(topicName);
        // Create a publisher instance with default settings bound to the topic
        /*SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(SubscriptionAdminSettings.newBuilder().setTransportChannelProvider(channelProvider).setCredentialsProvider(credentialsProvider).build());
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);
        PushConfig pushConfig = PushConfig.newBuilder().setPushEndpoint(pushEndPoint).build();

        // Create a push subscription with default acknowledgement deadline of 10 seconds.
        // Messages not successfully acknowledged within 10 seconds will get resent by the server.
        Subscription subscription = subscriptionAdminClient.createSubscription(subscriptionName, topicName, pushConfig, 60);
        System.out.println("Created push subscription: " + subscription.getName());*/
        return Publisher.newBuilder(topicName).setChannelProvider(channelProvider).setCredentialsProvider(credentialsProvider).build();
    }

    @Bean
    public Firestore firestoreInitializer() throws IOException {
        CredentialsProvider credentialsProvider = NoCredentialsProvider.create();
        FirestoreOptions firestoreOptions =
                FirestoreOptions.getDefaultInstance().toBuilder()
                        .setProjectId(projectId)
                        .setEmulatorHost("localhost:8084")
                        .setCredentials(new EmulatorCredentials())
                        .setCredentialsProvider(FixedCredentialsProvider.create(new EmulatorCredentials()))
                        .build();
        Firestore db = firestoreOptions.getService();
        return db;
    }
}
