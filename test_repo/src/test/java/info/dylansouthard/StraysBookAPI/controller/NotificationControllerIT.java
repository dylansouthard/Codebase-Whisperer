package info.dylansouthard.StraysBookAPI.controller;

import info.dylansouthard.StraysBookAPI.common.BaseNotificationTest;
import info.dylansouthard.StraysBookAPI.constants.ApiRoutes;
import info.dylansouthard.StraysBookAPI.errors.ErrorFactory;
import info.dylansouthard.StraysBookAPI.testutils.ExceptionAssertionRunner;
import info.dylansouthard.StraysBookAPI.testutils.TestSecurityUtil;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
public class NotificationControllerIT extends BaseNotificationTest {
    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void When_SyncingNotifications_Expect_NotificationsSynced(boolean provideLastChecked) throws Exception {
        int numDaysAgo = 21;
        LocalDateTime lastChecked = LocalDateTime.now().minusDays(numDaysAgo);

        TestSecurityUtil.authenticateTestUser(queryUser);

        mockMvc.perform(
                get(ApiRoutes.NOTIFICATIONS.SYNC)
                        .with(TestSecurityUtil.testUser(queryUser))
                        .param("lastChecked", provideLastChecked ? lastChecked.toString() : null)
                )
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void When_SyncingNotificationsWithNoUser_Expect_AuthError () throws Exception {
        ResultActions result = mockMvc.perform(get(ApiRoutes.NOTIFICATIONS.SYNC))
                .andDo(print());

        ExceptionAssertionRunner.assertThrowsExceptionOfType(result, ErrorFactory.auth());

    }
}
