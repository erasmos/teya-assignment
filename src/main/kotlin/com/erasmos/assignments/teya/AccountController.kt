package com.erasmos.assignments.teya

import com.erasmos.assignments.teya.Account.Companion.Transaction
import com.erasmos.assignments.teya.Account.Companion.Transactions
import com.erasmos.assignments.teya.AccountRepository.Companion.AttemptToDepositNegativeAmountException
import com.erasmos.assignments.teya.AccountRepository.Companion.AttemptToDepositWithAnAmountOfZeroException
import com.erasmos.assignments.teya.AccountRepository.Companion.AttemptToWithdrawNegativeAmountException
import com.erasmos.assignments.teya.AccountRepository.Companion.AttemptToWithdrawWithAnAmountOfZeroException
import com.erasmos.assignments.teya.AccountRepository.Companion.AttemptToWithdrawWithInsufficientFundsException
import com.erasmos.assignments.teya.AccountRepository.Companion.UnknownAccountException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*


@RestController
@RequestMapping(path = ["/api/accounts"], produces = ["application/json"])
class AccountController(@Autowired val accountRepository: AccountRepository) {

    @PostMapping(value = [""], produces = ["application/json"])
    fun createAccount(): ResponseEntity<AccountResponse?> {
        val account = accountRepository.createAccount()
        return ResponseEntity(
            AccountResponse(account.id, accountRepository.getCurrentBalance(account)),
            HttpStatus.CREATED
        )
    }

    @GetMapping(value = ["/{id}"])
    fun getAccount(@PathVariable id: UUID): ResponseEntity<AccountResponse?> {
        val account = accountRepository.findAccount(id)
        return ResponseEntity(AccountResponse(account.id, accountRepository.getCurrentBalance(account)), HttpStatus.OK)
    }

    @PostMapping(value = ["/{id}/deposits"], consumes = ["application/json"])
    fun deposit(@PathVariable id: UUID, @RequestBody depositRequest: DepositRequest): ResponseEntity<Transaction> {
        val account = accountRepository.findAccount(id)
        val transaction = accountRepository.makeDeposit(account, depositRequest.amountInMinorUnits)
        /// ENHANCEMENT: I would change to this returning a location header, and add the corresponding GET endpoint
        return ResponseEntity(transaction, HttpStatus.CREATED)
    }

    @PostMapping(value = ["/{id}/withdrawals"], consumes = ["application/json"])
    fun withdraw(
        @PathVariable id: UUID,
        @RequestBody withdrawalRequest: WithdrawalRequest
    ): ResponseEntity<Transaction> {
        val account = accountRepository.findAccount(id)
        val transaction = accountRepository.makeWithdrawal(account, withdrawalRequest.amountInMinorUnits)
        // ENHANCEMENT: I would change to this returning a location header, and add the corresponding GET endpoint
        return ResponseEntity(transaction, HttpStatus.CREATED)
    }

    @GetMapping(value = ["/{id}/transactions"])
    fun getAccountTransactions(@PathVariable id: UUID): ResponseEntity<Transactions> {
        val accounts = accountRepository.findAccount(id)
        return ResponseEntity(accountRepository.getAllTransactions(accounts), HttpStatus.OK)
    }

    companion object {

        data class AccountResponse(val id: UUID, val currentBalanceInMinorUnits: Long)
        data class DepositRequest(val amountInMinorUnits: Long)
        data class WithdrawalRequest(val amountInMinorUnits: Long)

        data class Error(val message: String)

        @ControllerAdvice
        class ControllerExceptionHandler {

            @ExceptionHandler
            fun handle(e: UnknownAccountException): ResponseEntity<Error> =
                ResponseEntity(Error("Unknown account."), HttpStatus.NOT_FOUND)

            @ExceptionHandler
            fun handle(e: AttemptToWithdrawNegativeAmountException): ResponseEntity<Error> =
                ResponseEntity(Error("Attempted to withdraw with a negative amount."), HttpStatus.BAD_REQUEST)

            @ExceptionHandler
            fun handle(e: AttemptToWithdrawWithAnAmountOfZeroException): ResponseEntity<Error> =
                ResponseEntity(Error("Attempted to withdraw with an amount of zero."), HttpStatus.BAD_REQUEST)

            @ExceptionHandler
            fun handle(e: AttemptToWithdrawWithInsufficientFundsException): ResponseEntity<Error> =
                ResponseEntity(Error("Attempted to withdraw but had insufficient funds."), HttpStatus.BAD_REQUEST)

            @ExceptionHandler
            fun handle(e: AttemptToDepositNegativeAmountException): ResponseEntity<Error> =
                ResponseEntity(Error("Attempted to deposit with a negative amount."), HttpStatus.BAD_REQUEST)

            @ExceptionHandler
            fun handle(e: AttemptToDepositWithAnAmountOfZeroException): ResponseEntity<Error> =
                ResponseEntity(Error("Attempted to deposit with an amount of zero."), HttpStatus.BAD_REQUEST)

        }
    }

}