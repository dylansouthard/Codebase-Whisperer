//package info.dylansouthard.StraysBookAPI.debug;
//
//import jakarta.annotation.PostConstruct;
//import org.springframework.boot.context.event.ApplicationReadyEvent;
//import org.springframework.context.annotation.Profile;
//import org.springframework.context.event.EventListener;
//import org.springframework.stereotype.Component;
//
//import javax.sql.DataSource;
//
//@Component
//@Profile("!test") // don't spam during tests
//public class DataSourceLogger {
//
//    private final DataSource ds;
//
//    public DataSourceLogger(DataSource ds) {
//        this.ds = ds;
//    }
//
//    // Fires when the app is fully started (controller mappings ready, etc.)
//    @EventListener(ApplicationReadyEvent.class)
//    public void logAtReady() throws Exception {
//        try (var c = ds.getConnection()) {
//            System.out.println(">>> DS URL  : " + c.getMetaData().getURL());
//            System.out.println(">>> DS User : " + c.getMetaData().getUserName());
//        }
//    }
//
//    // (optional) also fire right after bean construction
//    @PostConstruct
//    void logAtConstruct() throws Exception {
//        try (var c = ds.getConnection()) {
//            System.out.println(">>> (construct) DS URL  : " + c.getMetaData().getURL());
//        }
//    }
//}