package com.erasmos.assignments.teya

import com.erasmos.assignments.teya.Account.Companion.Transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.test.BeforeTest

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class LedgerApplicationTests(
    @Autowired val restTemplate: TestRestTemplate,
    @LocalServerPort val localServerPort: Int,
    @Autowired val accountRepository: AccountRepository
) {

    @MockitoBean
    private lateinit var idGenerator: IdGenerator

    @MockitoSpyBean
    private lateinit var clock: Clock

    @BeforeTest
    fun setUp() {
        // ENHANCEMENT: I'd advance the time within the test scenarios.
        givenCurrentDateTime(LocalDateTime.of(2024, 10, 4, 18, 0, 0).toInstant(ZoneOffset.UTC))
    }

    @Test
    fun createAnAccount() {

        val generateAccountId = UUID.randomUUID()
        givenNextGeneratedId(generateAccountId)

        val response = restTemplate.postForEntity(toFullUrl("/accounts"), null, String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertJson(
            """
            {
              "id": "$generateAccountId",
              "currentBalanceInMinorUnits": 0
            }  
            """.trimIndent(),
            response.body
        )
    }

    @Test
    fun getAccountByIdWhenItExists() {

        val newAccount = givenAnAccount()

        val response = restTemplate.getForEntity<String>(toFullUrl("/accounts/${newAccount.id}"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertJson(
            """
            {
              "id": "${newAccount.id}",
              "currentBalanceInMinorUnits": 0
            }  
            """.trimIndent(),
            response.body
        )
    }

    @Test
    fun getAccountByIdWhenItDoesNotExist() {
        val idOfANonExistentAccount = UUID.randomUUID()

        val response = restTemplate.getForEntity<String>(toFullUrl("/accounts/$idOfANonExistentAccount"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertJson(
            """
            {
              "message": "Unknown account."
            } 
            """.trimIndent(),
            response.body
        )
    }


    @Test
    fun makeSingleDepositWithNegativeAmount() {
        val account = givenAnAccount()

        val request = generateRequest(
            """
            {
              "amountInMinorUnits": -10000
            }    
            """.trimIndent()
        )

        val generatedDepositId = UUID.randomUUID()
        givenNextGeneratedId(generatedDepositId)
        val response = restTemplate.postForEntity(
            toFullUrl("/accounts/${account.id}/deposits"),
            request,
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertJson(
            """
            {
              "message": "Attempted to deposit with a negative amount."
            } 
            """.trimIndent(),
            response.body
        )

    }

    @Test
    fun makeSingleDepositWithAmountOfZero() {
        val account = givenAnAccount()

        val request = generateRequest(
            """
            {
              "amountInMinorUnits": 0.00
            }    
            """.trimIndent()
        )

        val generatedDepositId = UUID.randomUUID()
        givenNextGeneratedId(generatedDepositId)
        val response = restTemplate.postForEntity(
            toFullUrl("/accounts/${account.id}/deposits"),
            request,
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertJson(
            """
            {
              "message": "Attempted to deposit with an amount of zero."
            } 
            """.trimIndent(),
            response.body
        )
    }


    @Test
    fun makeSingleDepositIntoAccount() {
        val account = givenAnAccount()

        val request = generateRequest(
            """
            {
              "amountInMinorUnits": 1042
            }    
            """.trimIndent()
        )

        val generatedDepositId = UUID.fromString("576f5b80-74b9-4821-af6b-0dc59829a04f")
        givenNextGeneratedId(generatedDepositId)
        val response = restTemplate.postForEntity(
            toFullUrl("/accounts/${account.id}/deposits"),
            request,
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertJson(
            """
            {
              "id": "$generatedDepositId",
              "accountId": "${account.id}",
              "type": "DEPOSIT",
              "amountInMinorUnits": 1042,
              "date": "2024-10-04T18:00:00"
            } 
            """.trimIndent(),
            response.body
        )

        assertThat(getCurrentBalance(account)).isEqualTo(1042)
    }

    @Test
    fun makeMultipleDepositsIntoAccount() {
        val account = givenAnAccount()

        val requestForFirstDeposit = generateRequest(
            """
            {
              "amountInMinorUnits": 1042
            }    
            """.trimIndent()
        )

        val generateIdForFirstDeposit = UUID.randomUUID()
        givenNextGeneratedId(generateIdForFirstDeposit)
        val firstResponse = restTemplate.postForEntity(
            toFullUrl("/accounts/${account.id}/deposits"),
            requestForFirstDeposit,
            String::class.java
        )
        assertThat(firstResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        assertJson(
            """
            {
              "id": "$generateIdForFirstDeposit",
              "accountId": "${account.id}",
              "type": "DEPOSIT",
              "amountInMinorUnits": 1042,
              "date": "2024-10-04T18:00:00"
            } 
            """.trimIndent(),
            firstResponse.body
        )

        assertThat(getCurrentBalance(account)).isEqualTo(1042)

        val requestForSecondDeposit = generateRequest(
            """
            {
              "amountInMinorUnits": 958
            }    
            """.trimIndent()
        )

        val generatedSecondDepositId = UUID.randomUUID()
        givenNextGeneratedId(generatedSecondDepositId)
        val secondResponse = restTemplate.postForEntity(
            toFullUrl("/accounts/${account.id}/deposits"),
            requestForSecondDeposit,
            String::class.java
        )
        assertThat(secondResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        assertJson(
            """
            {
              "id": "$generatedSecondDepositId",
              "accountId": "${account.id}",
              "type": "DEPOSIT",
              "amountInMinorUnits": 958,
              "date": "2024-10-04T18:00:00"
            } 
            """.trimIndent(),
            secondResponse.body
        )

        assertThat(getCurrentBalance(account)).isEqualTo(2000)
    }

    @Test
    fun makeSingleWithdrawalNegativeAmount() {
        val account = createAccountWithInitialDeposit(1042)

        val requestForWithdrawal = generateRequest(
            """
            {
              "amountInMinorUnits": -2.00
            }    
            """.trimIndent()
        )

        val generatedWithdrawalId = UUID.randomUUID()
        givenNextGeneratedId(generatedWithdrawalId)
        val response = restTemplate.postForEntity(
            toFullUrl("/accounts/${account.id}/withdrawals"),
            requestForWithdrawal,
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertJson(
            """
            {
              "message": "Attempted to withdraw with a negative amount."
            } 
            """.trimIndent(),
            response.body
        )
    }

    @Test
    fun makeSingleWithdrawalWithAmountOfZero() {
        val account = createAccountWithInitialDeposit(1042)

        val requestForWithdrawal = generateRequest(
            """
            {
              "amountInMinorUnits": 0.00
            }    
            """.trimIndent()
        )

        val generatedWithdrawalId = UUID.randomUUID()
        givenNextGeneratedId(generatedWithdrawalId)
        val response = restTemplate.postForEntity(
            toFullUrl("/accounts/${account.id}/withdrawals"),
            requestForWithdrawal,
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertJson(
            """
            {
              "message": "Attempted to withdraw with an amount of zero."
            } 
            """.trimIndent(),
            response.body
        )
    }

    @Test
    fun makeSingleWithdrawalWithInsufficientFunds() {
        val account = createAccountWithInitialDeposit(4264)

        val requestForWithdrawal = generateRequest(
            """
            {
              "amountInMinorUnits": 6442
            }    
            """.trimIndent()
        )

        val generatedWithdrawalId = UUID.randomUUID()
        givenNextGeneratedId(generatedWithdrawalId)
        val response = restTemplate.postForEntity(
            toFullUrl("/accounts/${account.id}/withdrawals"),
            requestForWithdrawal,
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertJson(
            """
            {
              "message": "Attempted to withdraw but had insufficient funds."
            } 
            """.trimIndent(),
            response.body
        )
    }

    @Test
    fun makeSingleWithdrawalWithSufficientFunds() {
        val account = createAccountWithInitialDeposit(2000)

        val request = generateRequest(
            """
            {
              "amountInMinorUnits": 1400
            }    
            """.trimIndent()
        )

        val generatedWithdrawalId = UUID.randomUUID()
        givenNextGeneratedId(generatedWithdrawalId)
        val response = restTemplate.postForEntity(
            toFullUrl("/accounts/${account.id}/withdrawals"),
            request,
            String::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertJson(
            """
            {
              "id": "$generatedWithdrawalId",
              "accountId": "${account.id}",
              "type": "WITHDRAWAL",
              "amountInMinorUnits": 1400,
              "date": "2024-10-04T18:00:00"
            } 
            """.trimIndent(),
            response.body
        )

        assertThat(getCurrentBalance(account)).isEqualTo(600)
    }

    @Test
    fun makeMultipleWithdrawalsWithSufficientFunds() {
        val account = createAccountWithInitialDeposit(1000)

        val requestForFirstWithdrawal = generateRequest(
            """
            {
              "amountInMinorUnits": 622
            }    
            """.trimIndent()
        )

        val generateIdForFirstWithdrawal = UUID.randomUUID()
        givenNextGeneratedId(generateIdForFirstWithdrawal)
        val responseForFirstWithdrawal = restTemplate.postForEntity(
            toFullUrl("/accounts/${account.id}/withdrawals"),
            requestForFirstWithdrawal,
            String::class.java
        )
        assertThat(responseForFirstWithdrawal.statusCode).isEqualTo(HttpStatus.CREATED)
        assertJson(
            """
            {
              "id": "$generateIdForFirstWithdrawal",
              "accountId": "${account.id}",
              "type": "WITHDRAWAL",
              "amountInMinorUnits": 622,
              "date": "2024-10-04T18:00:00"
            } 
            """.trimIndent(),
            responseForFirstWithdrawal.body
        )

        assertThat(getCurrentBalance(account)).isEqualTo(378)

        val requestForSecondWithdrawal = generateRequest(
            """
            {
              "amountInMinorUnits": 378
            }    
            """.trimIndent()
        )

        val generateIdForSecondWithdrawal = UUID.randomUUID()
        givenNextGeneratedId(generateIdForSecondWithdrawal)
        val responseForSecondWithdrawal = restTemplate.postForEntity(
            toFullUrl("/accounts/${account.id}/withdrawals"),
            requestForSecondWithdrawal,
            String::class.java
        )
        assertThat(responseForSecondWithdrawal.statusCode).isEqualTo(HttpStatus.CREATED)
        assertJson(
            """
            {
              "id": "$generateIdForSecondWithdrawal",
              "accountId": "${account.id}",
              "type": "WITHDRAWAL",
              "amountInMinorUnits": 378,
              "date": "2024-10-04T18:00:00"
            } 
            """.trimIndent(),
            responseForSecondWithdrawal.body
        )

        assertThat(getCurrentBalance(account)).isEqualTo(0)
    }

    @Test
    fun viewTransactionsWhenThereAreNone() {
        val account = givenAnAccount()

        val response = restTemplate.getForEntity<String>(toFullUrl("/accounts/${account.id}/transactions"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertJson(
            """
            {
              "accountId": "${account.id}",
              "transactions": []
            } 
            """.trimIndent(),
            response.body
        )
    }

    @Test
    fun viewTransactionsWhenThereAreSomeOfBothTypes() {
        val account = givenAnAccount()
        val firstDeposit = deposit(account, 2000)
        val firstWithdrawal = withdraw(account, 1000)
        val secondDeposit = deposit(account, 2200)
        val secondWithdrawal = withdraw(account, 222)

        val response = restTemplate.getForEntity<String>(toFullUrl("/accounts/${account.id}/transactions"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertJson(
            """
            {
              "accountId": "${account.id}",
              "transactions": [
                {
                  "id": "${firstDeposit.id}",
                  "accountId": "${account.id}",
                  "type": "DEPOSIT",
                  "amountInMinorUnits": 2000,
                  "date": "2024-10-04T18:00:00"
                },
                {
                  "id": "${firstWithdrawal.id}",
                  "accountId": "${account.id}",
                  "type": "WITHDRAWAL",
                  "amountInMinorUnits": 1000,
                  "date": "2024-10-04T18:00:00"
                },
                {
                  "id": "${secondDeposit.id}",
                  "accountId": "${account.id}",
                  "type": "DEPOSIT",
                  "amountInMinorUnits": 2200,
                  "date": "2024-10-04T18:00:00"
                },
                {
                  "id": "${secondWithdrawal.id}",
                  "accountId": "${account.id}",
                  "type": "WITHDRAWAL",
                  "amountInMinorUnits": 222,
                  "date": "2024-10-04T18:00:00"
                }
              ]
            } 
            """.trimIndent(),
            response.body
        )
    }

    private fun assertJson(expectedRawJson: String, actualRawJson: String?) {
        assertThat(actualRawJson).isNotNull()
        JSONAssert.assertEquals(expectedRawJson, actualRawJson, JSONCompareMode.STRICT)
    }

    private fun toFullUrl(path: String): String =
        "http://localhost:$localServerPort/api/$path"

    private fun givenNextGeneratedId(returnedId: UUID) {
        Mockito.reset(idGenerator)
        Mockito.`when`(idGenerator.generate()).thenReturn(returnedId)
    }

    private fun deposit(account: Account, amountInMinorUnits: Long): Transaction {
        givenNextGeneratedId(UUID.randomUUID())
        return accountRepository.makeDeposit(account, amountInMinorUnits)
    }

    private fun withdraw(account: Account, amountInMinorUnits: Long): Transaction {
        givenNextGeneratedId(UUID.randomUUID())
        return accountRepository.makeWithdrawal(account, amountInMinorUnits)
    }

    private fun getCurrentBalance(account: Account): Long = accountRepository.getCurrentBalance(account)

    private fun givenAnAccount(): Account {
        givenNextGeneratedId(UUID.randomUUID())
        return accountRepository.createAccount()
    }

    private fun createAccountWithInitialDeposit(amountInMinorUnits: Long): Account {
        givenNextGeneratedId(UUID.randomUUID())
        val account = accountRepository.createAccount()
        deposit(account, amountInMinorUnits)
        assertThat(getCurrentBalance(account)).isEqualTo(amountInMinorUnits)
        return account
    }

    private fun generateRequest(rawJsonBody: String): HttpEntity<String> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        return HttpEntity(rawJsonBody, headers)
    }

    private fun givenCurrentDateTime(dateTime: Instant) {
        Mockito.reset(clock)
        Mockito.`when`(clock.instant()).thenReturn(dateTime)
    }

}

