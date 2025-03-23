package com.erasmos.assignments.teya

import com.erasmos.assignments.teya.Account.Companion.Transaction
import com.erasmos.assignments.teya.Account.Companion.Transaction.Companion.TransactionType
import com.erasmos.assignments.teya.Account.Companion.Transaction.Companion.TransactionType.DEPOSIT
import com.erasmos.assignments.teya.Account.Companion.Transaction.Companion.TransactionType.WITHDRAWAL
import com.erasmos.assignments.teya.Account.Companion.Transactions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime
import java.util.*
import kotlin.math.sign

@Service
class AccountRepository(
    @Autowired val clock: Clock,
    @Autowired val idGenerator: IdGenerator
) {

    val accounts = mutableMapOf<UUID, Account>()

    val transactionsByAccountId = mutableMapOf<UUID, MutableList<Transaction>>()

    fun createAccount(): Account {
        val newAccount = Account(idGenerator.generate())
        accounts[newAccount.id] = newAccount
        return newAccount
    }

    fun findAccount(id: UUID): Account = accounts[id] ?: throw UnknownAccountException()

    fun getCurrentBalance(account: Account): Long {
        val allTransactionsForAccount = transactionsByAccountId.getOrDefault(account.id, emptyList())
        return allTransactionsForAccount.fold(0L) { sum, transaction ->
            when (transaction.type) {
                DEPOSIT -> sum + transaction.amountInMinorUnits
                WITHDRAWAL -> sum - transaction.amountInMinorUnits
            }
        }
    }


    fun makeDeposit(account: Account, amountInMinorUnits: Long): Transaction {
        if (amountInMinorUnits.sign == 0) throw AttemptToDepositWithAnAmountOfZeroException()
        if (amountInMinorUnits.sign == -1) throw AttemptToDepositNegativeAmountException()

        return recordTransaction(account, DEPOSIT, amountInMinorUnits)
    }

    fun makeWithdrawal(account: Account, amountInMinorUnits: Long): Transaction {
        if (amountInMinorUnits.sign == 0) throw AttemptToWithdrawWithAnAmountOfZeroException()
        if (amountInMinorUnits.sign == -1) throw AttemptToWithdrawNegativeAmountException()
        if (getCurrentBalance(account) < amountInMinorUnits) throw AttemptToWithdrawWithInsufficientFundsException()

        return recordTransaction(account, WITHDRAWAL, amountInMinorUnits)
    }

    fun getAllTransactions(account: Account): Transactions =
        Transactions(
            accountId = account.id,
            transactions = transactionsByAccountId.getOrDefault(account.id, emptyList())
        )

    private fun recordTransaction(account: Account, type: TransactionType, amountInMinorUnits: Long): Transaction {
        val transaction = Transaction(
            id = idGenerator.generate(),
            accountId = account.id,
            type = type,
            amountInMinorUnits = amountInMinorUnits,
            date = LocalDateTime.now(clock)
        )

        val currentTransactionsForAccount = transactionsByAccountId.getOrDefault(account.id, mutableListOf())
        currentTransactionsForAccount.add(transaction)
        transactionsByAccountId[account.id] = currentTransactionsForAccount

        return transaction
    }


    companion object {
        class UnknownAccountException : Exception()
        class AttemptToWithdrawNegativeAmountException : Exception()
        class AttemptToWithdrawWithAnAmountOfZeroException : Exception()
        class AttemptToWithdrawWithInsufficientFundsException : Exception()
        class AttemptToDepositNegativeAmountException : Exception()
        class AttemptToDepositWithAnAmountOfZeroException : Exception()
    }
}