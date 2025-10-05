package info.dylansouthard.StraysBookAPI.testutils;

import info.dylansouthard.StraysBookAPI.errors.AppException;
import org.junit.jupiter.api.function.Executable;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ExceptionAssertionRunner {

    /**
     * Asserts that the given executable throws an AppException matching the expected exception.
     * This method wraps all assertions (error code, message, and status) into a single assertAll
     * block to ensure all checks are run and reported together.
     *
     * @param executable the block of code expected to throw an AppException
     * @param expectedException the expected exception instance containing the expected code, message, and status
     * @param batchAssertionMessage a label for the group of assertions in the test report
     */
    public static void assertThrowsExceptionOfType(Executable executable, AppException expectedException, String batchAssertionMessage) {
        AppException ex = assertThrows(AppException.class, executable);
        assertAll(batchAssertionMessage,
                () -> assertEquals(expectedException.getErrorCode(), ex.getErrorCode(), "Error code should be " + expectedException.getErrorCode()),
                () -> assertEquals(expectedException.getErrorMessage(), ex.getErrorMessage(), "Error message should match"),
                () -> assertEquals(expectedException.getStatus(), ex.getStatus(), "Status should be " + expectedException.getStatus())
        );
    }

    public static void assertThrowsExceptionOfType(ResultActions result, AppException expectedException) throws Exception {
        result
                .andExpect(status().is(expectedException.getStatus()))
                .andExpect(jsonPath("$.code").value(expectedException.getErrorCode()))
                .andExpect(jsonPath("$.message").value(expectedException.getErrorMessage()));
    }


}
