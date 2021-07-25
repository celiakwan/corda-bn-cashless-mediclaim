package com.example.cordacashlessmediclaim.flows.paymentReceiptFlows

import co.paralleluniverse.fibers.Suspendable
import com.example.cordacashlessmediclaim.contracts.PaymentReceiptContract
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import com.example.cordacashlessmediclaim.flows.BusinessNetworkIntegration
import com.example.cordacashlessmediclaim.roles.InsurerPermissions
import com.example.cordacashlessmediclaim.states.PaymentReceiptState
import com.example.cordacashlessmediclaim.states.PaymentReceiptStatus
import com.example.cordacashlessmediclaim.states.PreAuthorizationState
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import java.math.BigDecimal
import java.time.Instant
import java.util.Currency

@InitiatingFlow
@StartableByRPC
class CreatePaymentReceiptFlow(
        private val networkId: UniqueIdentifier,
        private val payer: Party,
        private val payee: Party,
        private val currency: Currency,
        private val amount: BigDecimal,
        private val preAuthorizationLinearId: UniqueIdentifier
): BusinessNetworkIntegration<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        verifyMembership(networkId, ourIdentity, InsurerPermissions.CAN_CREATE_PAYMENT_RECEIPT)

        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(preAuthorizationLinearId))
        val preAuthorizationStateAndRef = serviceHub.vaultService.queryBy<PreAuthorizationState>(queryCriteria).states
                .singleOrNull() ?: throw FlowException("PreAuthorizationState with linear ID $preAuthorizationLinearId not found")
        val outputState = PaymentReceiptState(
                networkId = networkId,
                payer = payer,
                payee = payee,
                currency = currency,
                amount = amount,
                submissionTime = Instant.now(),
                preAuthorizationLinearId = preAuthorizationLinearId,
                status = PaymentReceiptStatus.CREATED
        )
        val builder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.first())
                .addOutputState(outputState)
                .addReferenceState(ReferencedStateAndRef(preAuthorizationStateAndRef))
                .addCommand(PaymentReceiptContract.Commands.Create(), ourIdentity.owningKey, payee.owningKey)

        builder.verify(serviceHub)

        val selfSignedTransaction = serviceHub.signInitialTransaction(builder)
        val sessions = (outputState.participants - ourIdentity).map { initiateFlow(it) }
        val fullSignedTransaction = subFlow(CollectSignaturesFlow(selfSignedTransaction, sessions))
        return subFlow(FinalityFlow(fullSignedTransaction, sessions))
    }
}

@InitiatedBy(CreatePaymentReceiptFlow::class)
class CreatePaymentReceiptResponderFlow(private val session: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        subFlow(object : SignTransactionFlow(session) {
            override fun checkTransaction(stx: SignedTransaction) {
                val command = stx.tx.commands.single()
                if (command.value !is PaymentReceiptContract.Commands.Create) {
                    throw FlowException("Only PaymentReceiptContract.Commands.Create command is allowed")
                }

                val outputState = stx.tx.outputStates.single() as PaymentReceiptState
                outputState.apply {
                    if (payee != ourIdentity) {
                        throw FlowException("payee doesn't match our identity")
                    }
                    if (payer != session.counterparty) {
                        throw FlowException("payer doesn't match sender's identity")
                    }
                }
            }
        })

        return subFlow(ReceiveFinalityFlow(session))
    }
}