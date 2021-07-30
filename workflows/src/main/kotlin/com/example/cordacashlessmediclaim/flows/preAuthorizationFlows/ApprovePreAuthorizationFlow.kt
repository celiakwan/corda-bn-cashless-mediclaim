package com.example.cordacashlessmediclaim.flows.preAuthorizationFlows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import com.example.cordacashlessmediclaim.contracts.PreAuthorizationContract
import com.example.cordacashlessmediclaim.flows.BusinessNetworkIntegration
import com.example.cordacashlessmediclaim.roles.InsurerPermissions
import com.example.cordacashlessmediclaim.states.PreAuthorizationState
import com.example.cordacashlessmediclaim.states.PreAuthorizationStatus
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria

@InitiatingFlow
@StartableByRPC
class ApprovePreAuthorizationFlow(
        private val linearId: UniqueIdentifier
): BusinessNetworkIntegration<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val inputStateAndRef = serviceHub.vaultService.queryBy<PreAuthorizationState>(queryCriteria).states
                .singleOrNull() ?: throw FlowException("PreAuthorizationState with linear ID $linearId not found")
        val inputState = inputStateAndRef.state.data

        verifyMembership(inputState.networkId, ourIdentity, InsurerPermissions.CAN_APPROVE_PRE_AUTHORIZATION)

        inputState.apply {
            if (policyIssuer != ourIdentity) {
                throw FlowException("policyIssuer doesn't match our identity")
            }
        }

        val outputState = inputState.copy(status = PreAuthorizationStatus.APPROVED)
        val builder = TransactionBuilder(inputStateAndRef.state.notary)
                .addInputState(inputStateAndRef)
                .addOutputState(outputState)
                .addCommand(PreAuthorizationContract.Commands.Approve(), ourIdentity.owningKey)

        builder.verify(serviceHub)

        val selfSignedTransaction = serviceHub.signInitialTransaction(builder)
        val sessions = (inputState.participants - ourIdentity).map { initiateFlow(it) }
        return subFlow(FinalityFlow(selfSignedTransaction, sessions))
    }
}

@InitiatedBy(ApprovePreAuthorizationFlow::class)
class ApprovePreAuthorizationResponderFlow(private val session: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction = subFlow(ReceiveFinalityFlow(session))
}