package info.dylansouthard.StraysBookAPI;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import info.dylansouthard.StraysBookAPI.model.shared.GeoSchema;
import info.dylansouthard.StraysBookAPI.serializers.GeoSchemaSerializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(properties = "spring.profiles.active=test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class BaseTestContainer {

    //ENVIRONMENT
    @Autowired
    protected Environment environment;


    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:15-3.4").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @BeforeAll
    static void startContainer() {
        postgres.start();
    }

    @BeforeEach
    public void registerGeoSchema() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(GeoSchema.class, new GeoSchemaSerializer());
        mapper.registerModule(module);
    }

    @PersistenceContext
    private EntityManager entityManager;

    @DynamicPropertySource
    static void configureTestDatabase(DynamicPropertyRegistry registry) {
        System.out.println("🌱 Registering dynamic DB props: " + postgres.getJdbcUrl());
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
//
//    @Test
//    void checkActiveProfile() {
//        String[] activeProfiles = environment.getActiveProfiles();
//        System.out.println("🚀 Active Profiles: " + String.join(", ", activeProfiles)); // Debug output
//        assertTrue(activeProfiles.length > 0 && activeProfiles[0].equals("test"),
//                "Active profile should be 'test'");
//    }

}
