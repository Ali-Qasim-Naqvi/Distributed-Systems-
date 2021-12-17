package be.kuleuven.distributedsystems.cloud.auth;

import be.kuleuven.distributedsystems.cloud.entities.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

@Component
public class SecurityFilter extends OncePerRequestFilter {
    @Autowired WebClient.Builder webClientBuilder;
    @Autowired Boolean isProduction;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var session = WebUtils.getCookie(request, "session");
        if (session != null) {
            // TODO: (level 2) verify Identity Token
//            System.out.println("HTTP Request is : " + request.getScheme());
           /* String email = null;
            String role = null;
            try {
                String[] parts = session.getValue().split("\\.");
                Base64.Decoder decoder = Base64.getDecoder();
                String header = new String(decoder.decode(parts[0]));
                String payload = new String(decoder.decode(parts[1]));
                Object obj= JSONValue.parse(payload);
                JSONObject jsonObject = (JSONObject) obj;
                email = (String) jsonObject.get("email");
                role = (String) jsonObject.get("role");
//                System.out.println("Email is : " + email);
//                System.out.println("Role is : " + role);

            } catch (JWTDecodeException exception){
                System.out.println("Invlaid Token");
            }*/
            String email = null;
            String role = null;
            if(!isProduction){

                try {
                    String[] parts = session.getValue().split("\\.");
                    Base64.Decoder decoder = Base64.getDecoder();
                    String header = new String(decoder.decode(parts[0]));
                    String payload = new String(decoder.decode(parts[1]));
                    Object obj= JSONValue.parse(payload);
                    JSONObject jsonObject = (JSONObject) obj;
                    email = (String) jsonObject.get("email");
                    role = (String) jsonObject.get("role");
//                System.out.println("Email is : " + email);
//                System.out.println("Role is : " + role);

                } catch (JWTDecodeException exception){
                    System.out.println("Invlaid Token");
                }
            }
            else{
                try {
                    String[] parts = session.getValue().split("\\.");
                    Base64.Decoder decoder = Base64.getDecoder();
                    String header = new String(decoder.decode(parts[0]));
                    String payload = new String(decoder.decode(parts[1]));
                    String signature = parts[2];
                    Object objHeader= JSONValue.parse(header);
                    JSONObject jsonObjectHeader = (JSONObject) objHeader;
                    Object obj= JSONValue.parse(payload);
                    JSONObject jsonObject = (JSONObject) obj;
                    email = (String) jsonObject.get("email");
                    role = (String) jsonObject.get("role");
                    String kid = (String) jsonObjectHeader.get("kid");

                    System.out.println("kid is : " + kid);
                    System.out.println("Signarute is : " + signature);
                    System.out.println("Token is : " + header+"."+payload + "." + signature  );

                    var keys = webClientBuilder
                            .baseUrl("https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com")
                            .build()
                            .get()
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<>() {})
                            .retry(3)
                            .block();

                    final LinkedHashMap<String,String> keyMap = (LinkedHashMap<String, String>) keys;
                    JSONObject jsonObject1 = new JSONObject();
                    for (Map.Entry<String,String> entry:keyMap.entrySet()){
                        jsonObject1.put(entry.getKey(),entry.getValue());
                    }

                    String publicKeyPEM = (String) jsonObject1.get(kid);


                    byte[] encoded = publicKeyPEM.getBytes();
                    KeyFactory kf = KeyFactory.getInstance("RSA");
                    RSAPublicKey pubKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(encoded));


                    try {
                        Algorithm algorithm = Algorithm.RSA256(pubKey,null);
                        JWTVerifier verifier = JWT.require(algorithm)
                                .withIssuer("https://securetoken.google.com/ds-booking-system")
                                .build(); //Reusable verifier instance
                        verifier.verify(header+"."+payload + "." + signature);
                        System.out.println("Signature is correct");
                    } catch (JWTVerificationException exception){
                        System.out.println("Signature is incorrect");
                    }
//                System.out.println("Email is : " + email);
//                System.out.println("Role is : " + role);

                } catch (JWTDecodeException | NoSuchAlgorithmException | InvalidKeySpecException exception){
                    System.out.println("Invalid Token");
                }
            }
            var user = new User(email, role);

            SecurityContext context = SecurityContextHolder.getContext();
            context.setAuthentication(new FirebaseAuthentication(user));
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/authenticate") || path.endsWith(".html") || path.endsWith(".js") || path.endsWith(".css") || path.endsWith("/confirmQuote");
    }

    private static class FirebaseAuthentication implements Authentication {
        private final User user;

        FirebaseAuthentication(User user) {
            this.user = user;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            if (user.isManager()) {
                return List.of(new SimpleGrantedAuthority("manager"));
            } else{
                return new ArrayList<>();
            }
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getDetails() {
            return null;
        }

        @Override
        public User getPrincipal() {
            return this.user;
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public void setAuthenticated(boolean b) throws IllegalArgumentException {

        }

        @Override
        public String getName() {
            return null;
        }
    }
}

