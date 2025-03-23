package com.erasmos.assignments.teya

import org.springframework.stereotype.Component
import java.util.*

interface IdGenerator {
    fun generate(): UUID
}

@Component
class DefaultIdGenerator : IdGenerator {
    override fun generate(): UUID = UUID.randomUUID()

}