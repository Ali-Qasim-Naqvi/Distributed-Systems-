package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.Quote;

import java.io.*;
import java.util.Base64;
import java.util.List;

@SuppressWarnings("serial")
public class QuotesWrapper implements Serializable{

        private List<Quote> quotes;
        private String customer;

        public QuotesWrapper(List<Quote> quotes, String customer) {
            setQuotes(quotes);
            setCustomer(customer);
        }

        public List<Quote> getQuotes() {
            return quotes;
        }

        private void setQuotes(List<Quote> quotes) {
            this.quotes = quotes;
        }

        public String getCustomer() {
            return customer;
        }

        private void setCustomer(String customer) {
            this.customer = customer;
        }

        public String toString( QuotesWrapper quotesWrapper ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( baos );
        oos.writeObject( quotesWrapper );
        oos.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
        }

        public Object fromString( String message ) throws IOException , ClassNotFoundException {
            byte [] data = Base64.getDecoder().decode( message);
            ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( data ) );
            Object o  = ois.readObject();
            ois.close();
            return o;
        }
}
