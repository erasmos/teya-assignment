package com.erasmos.assignments.teya

import java.time.LocalDateTime
import java.util.*

data class Account(val id: UUID) {

    companion object {
        data class Transactions(val accountId: UUID, val transactions: List<Transaction>)

        data class Transaction(
            val id: UUID,
            val accountId: UUID,
            val type: TransactionType,
            val amountInMinorUnits: Long,
            val date: LocalDateTime
        ) {
            companion object {
                enum class TransactionType {
                    DEPOSIT, WITHDRAWAL
                }
            }
        }
    }
}