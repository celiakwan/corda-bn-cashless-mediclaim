package com.example.cordacashlessmediclaim.states

import com.example.cordacashlessmediclaim.contracts.PaymentReceiptContract
import com.example.cordacashlessmediclaim.schemas.PaymentReceiptSchemaV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.time.Instant
import java.util.Currency

@BelongsToContract(PaymentReceiptContract::class)
data class PaymentReceiptState(
        val networkId: UniqueIdentifier,
        val payer: Party,
        val payee: Party,
        val currency: Currency,
        val amount: BigDecimal,
        val submissionTime: Instant,
        val preAuthorizationLinearId: UniqueIdentifier,
        override val status: PaymentReceiptStatus,
        override val participants: List<AbstractParty> = listOf(payer, payee),
        override val linearId: UniqueIdentifier = UniqueIdentifier()
): VerifiableState<PaymentReceiptStatus>, QueryableState {
    override fun partyRoleToParty(partyRole: PartyRole): Party =
            when (partyRole) {
                PartyRole.INSURER -> payer
                PartyRole.HOSPITAL -> payee
                else -> throw IllegalArgumentException("Illegal PartyRole $partyRole")
            }

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is PaymentReceiptSchemaV1 -> PaymentReceiptSchemaV1.PersistentPaymentReceipt(
                    networkId = networkId.id,
                    payer = payer.name.toString(),
                    payee = payee.name.toString(),
                    currency = currency,
                    amount = amount,
                    submissionTime = submissionTime,
                    preAuthorizationLinearId = preAuthorizationLinearId.id,
                    status = status,
                    linearId = linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(PaymentReceiptSchemaV1)
}

@CordaSerializable
enum class PaymentReceiptStatus {
    CREATED
}