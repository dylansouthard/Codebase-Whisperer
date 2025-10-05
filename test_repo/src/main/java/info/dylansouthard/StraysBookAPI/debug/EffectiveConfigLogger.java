//package info.dylansouthard.StraysBookAPI.debug;
//
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.context.annotation.Profile;
//import org.springframework.core.env.Environment;
//import org.springframework.stereotype.Component;
//
//@Component
//@Profile("!test") // optional: keep it off in tests
//public class EffectiveConfigLogger implements ApplicationRunner {
//
//    private final Environment env;
//
//    public EffectiveConfigLogger(Environment env) {
//        this.env = env;
//    }
//
//    @Override
//    public void run(ApplicationArguments args) {
//        System.out.println(">>> ACTIVE PROFILES : " + String.join(",", env.getActiveProfiles()));
//        System.out.println(">>> DS URL          : " + env.getProperty("spring.datasource.url"));
//        System.out.println(">>> DS USERNAME     : " + env.getProperty("spring.datasource.username"));
//        System.out.println(">>> HIBERNATE DDL   : " + env.getProperty("spring.jpa.hibernate.ddl-auto"));
//    }
//}