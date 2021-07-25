package com.example.cordacashlessmediclaim.flows.preAuthorizationFlows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import com.example.cordacashlessmediclaim.contracts.PreAuthorizationContract
import com.example.cordacashlessmediclaim.flows.BusinessNetworkIntegration
import com.example.cordacashlessmediclaim.roles.HospitalPermissions
import com.example.cordacashlessmediclaim.states.PreAuthorizationState
import com.example.cordacashlessmediclaim.states.PreAuthorizationStatus
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
import java.math.BigDecimal
import java.time.Instant
import java.util.Currency

@InitiatingFlow
@StartableByRPC
class CreatePreAuthorizationFlow(
        private val networkId: UniqueIdentifier,
        private val policyHolder: Party,
        private val membershipNumber: String,
        private val diagnosisDescription: String,
        private val currency: Currency,
        private val amount: BigDecimal,
        private val policyIssuer: Party
): BusinessNetworkIntegration<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        verifyMembership(networkId, ourIdentity, HospitalPermissions.CAN_CREATE_PRE_AUTHORIZATION)

        val outputState = PreAuthorizationState(
                networkId = networkId,
                policyHolder = policyHolder,
                membershipNumber = membershipNumber,
                provider = ourIdentity,
                diagnosisDescription = diagnosisDescription,
                currency = currency,
                amount = amount,
                policyIssuer = policyIssuer,
                submissionTime = Instant.now(),
                status = PreAuthorizationStatus.CREATED
        )
        val builder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.first())
                .addOutputState(outputState)
                .addCommand(PreAuthorizationContract.Commands.Create(), ourIdentity.owningKey, policyHolder.owningKey)

        builder.verify(serviceHub)

        val selfSignedTransaction = serviceHub.signInitialTransaction(builder)
        val fullSignedTransaction = subFlow(CollectSignaturesInitiatingFlow(selfSignedTransaction, listOf(policyHolder)))
        val sessions = (outputState.participants - ourIdentity).map { initiateFlow(it) }
        return subFlow(FinalityFlow(fullSignedTransaction, sessions))
    }
}

@InitiatingFlow
class CollectSignaturesInitiatingFlow(
        private val transaction: SignedTransaction,
        private val signers: List<Party>
): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val sessions = signers.map { initiateFlow(it) }
        return subFlow(CollectSignaturesFlow(transaction, sessions))
    }
}

@InitiatedBy(CollectSignaturesInitiatingFlow::class)
class CollectSignaturesResponderFlow(private val session: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return subFlow(object: SignTransactionFlow(session) {
            override fun checkTransaction(stx: SignedTransaction) {
                val command = stx.tx.commands.single()
                if (command.value !is PreAuthorizationContract.Commands.Create) {
                    throw FlowException("Only PreAuthorizationStateContract.Commands.Create command is allowed")
                }

                val outputState = stx.tx.outputStates.single() as PreAuthorizationState
                outputState.apply {
                    if (policyHolder != ourIdentity) {
                        throw FlowException("policyHolder doesn't match our identity")
                    }
                    if (provider != session.counterparty) {
                        throw FlowException("provider doesn't match sender's identity")
                    }
                }
            }
        })
    }
}

@InitiatedBy(CreatePreAuthorizationFlow::class)
class CreatePreAuthorizationResponderFlow(private val session: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction = subFlow(ReceiveFinalityFlow(session))
}