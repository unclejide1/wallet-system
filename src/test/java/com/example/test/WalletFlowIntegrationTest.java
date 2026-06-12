package com.example.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WalletFlowIntegrationTest {

    private static final AtomicLong PHONE_SEQUENCE = new AtomicLong(8111111111L);
    private static final String DEFAULT_PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void userCanFundWalletAndTransferToAnotherUser() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        String firstUserEmail = "ada." + suffix + "@wallet.test";
        String secondUserEmail = "bola." + suffix + "@wallet.test";

        registerUser("Ada", "Lovelace", firstUserEmail);
        registerUser("Bola", "Adewale", secondUserEmail);

        String firstUserToken = login(firstUserEmail);
        String secondUserToken = login(secondUserEmail);

        String firstUserWallet = createWallet(firstUserToken, "SAVINGS");
        String secondUserWallet = createWallet(secondUserToken, "SAVINGS");

        JsonNode fundingData = responseData(fundWallet(firstUserToken, firstUserWallet, "5000.00", "Card top-up", "PAY-" + suffix));
        assertThat(fundingData.get("availableBalance").decimalValue()).isEqualByComparingTo("5000.00");
        assertThat(fundingData.get("paymentReference").asText()).isEqualTo("PAY-" + suffix);

        JsonNode transferData = responseData(transferFunds(firstUserToken, firstUserWallet, secondUserWallet, "1250.00", "June allowance"));
        assertThat(transferData.get("sourceAvailableBalance").decimalValue()).isEqualByComparingTo("3750.00");
        assertThat(transferData.get("destinationAvailableBalance").decimalValue()).isEqualByComparingTo("1250.00");

        JsonNode firstUserStatement = responseData(getStatement(firstUserToken, firstUserWallet));
        JsonNode secondUserStatement = responseData(getStatement(secondUserToken, secondUserWallet));

        assertThat(firstUserStatement.get("page").get("totalElements").asInt()).isEqualTo(2);
        assertThat(secondUserStatement.get("page").get("totalElements").asInt()).isEqualTo(1);
    }

    @Test
    void userCannotFundAnotherUsersWallet() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        String firstUserEmail = "owner." + suffix + "@wallet.test";
        String secondUserEmail = "intruder." + suffix + "@wallet.test";

        registerUser("Owner", "One", firstUserEmail);
        registerUser("Intruder", "Two", secondUserEmail);

        String ownerToken = login(firstUserEmail);
        String intruderToken = login(secondUserEmail);

        String ownerWallet = createWallet(ownerToken, "SAVINGS");

        mockMvc.perform(post("/api/v1/wallets/fund")
                        .header("Authorization", bearer(intruderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountNumber": "%s",
                                  "amount": 2500.00,
                                  "narration": "Unauthorized top-up attempt"
                                }
                                """.formatted(ownerWallet)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only operate on wallet accounts that belong to you"));
    }

    private void registerUser(String firstName, String lastName, String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/onboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "lastName": "%s",
                                  "otherName": "Test",
                                  "gender": "FEMALE",
                                  "address": "12 Broad Street, Lagos",
                                  "stateOfOrigin": "Lagos",
                                  "email": "%s",
                                  "password": "%s",
                                  "phoneNumber": "%s",
                                  "alternativePhoneNumber": "%s"
                                }
                                """.formatted(firstName, lastName, email, DEFAULT_PASSWORD, nextPhoneNumber(), nextPhoneNumber())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, DEFAULT_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        return responseData(result).get("token").asText();
    }

    private String createWallet(String token, String walletType) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/wallets")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "walletType": "%s"
                                }
                                """.formatted(walletType)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accountNumber").isNotEmpty())
                .andReturn();

        return responseData(result).get("accountNumber").asText();
    }

    private MvcResult fundWallet(String token, String accountNumber, String amount, String narration, String paymentReference) throws Exception {
        return mockMvc.perform(post("/api/v1/wallets/fund")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountNumber": "%s",
                                  "amount": %s,
                                  "narration": "%s",
                                  "paymentReference": "%s"
                                }
                                """.formatted(accountNumber, amount, narration, paymentReference)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountNumber").value(accountNumber))
                .andReturn();
    }

    private MvcResult transferFunds(String token, String sourceAccount, String destinationAccount, String amount, String narration) throws Exception {
        return mockMvc.perform(post("/api/v1/wallets/transfer")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceAccountNumber": "%s",
                                  "destinationAccountNumber": "%s",
                                  "amount": %s,
                                  "narration": "%s"
                                }
                                """.formatted(sourceAccount, destinationAccount, amount, narration)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceAccountNumber").value(sourceAccount))
                .andExpect(jsonPath("$.data.destinationAccountNumber").value(destinationAccount))
                .andReturn();
    }

    private MvcResult getStatement(String token, String accountNumber) throws Exception {
        return mockMvc.perform(get("/api/v1/wallets/{accountNumber}/statement", accountNumber)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String nextPhoneNumber() {
        return "0" + PHONE_SEQUENCE.getAndIncrement();
    }
}
