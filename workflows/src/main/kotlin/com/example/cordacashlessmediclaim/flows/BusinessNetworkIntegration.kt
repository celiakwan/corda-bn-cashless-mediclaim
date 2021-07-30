package com.example.cordacashlessmediclaim.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.bn.flows.BNService
import net.corda.bn.flows.IllegalMembershipStatusException
import net.corda.bn.flows.MembershipAuthorisationException
import net.corda.bn.flows.MembershipNotFoundException
import net.corda.bn.states.BNPermission
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.Party
import java.lang.IllegalStateException

abstract class BusinessNetworkIntegration<T>: FlowLogic<T>() {
    @Suppress("ComplexMethod", "ThrowsCount")
    @Suspendable
    protected fun verifyMembership(networkId: UniqueIdentifier, party: Party, customPermission: BNPermission) {
        val bnService = serviceHub.cordaService(BNService::class.java)

        try {
            bnService.getMembership(networkId.id.toString(), party)?.state?.data?.apply {
                if (!isActive()) {
                    throw IllegalMembershipStatusException("$party is not an active member of Business Network with ID $networkId")
                }
                if (roles.find { customPermission in it.permissions } == null) {
                    throw MembershipAuthorisationException("$party does not have the required permission $customPermission in Business Network with ID $networkId")
                }
            } ?: throw MembershipNotFoundException("$party is not a member of Business Network with ID $networkId")
        } catch (e: IllegalStateException) {
            throw MembershipNotFoundException("$party is not a member of Business Network with ID $networkId")
        }
    }
}