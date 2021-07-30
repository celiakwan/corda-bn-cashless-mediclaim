package com.example.cordacashlessmediclaim.flows.membershipFlows

import co.paralleluniverse.fibers.Suspendable
import net.corda.bn.flows.CreateBusinessNetworkFlow
import net.corda.bn.flows.composite.BatchOnboardMembershipFlow
import net.corda.bn.flows.composite.OnboardingInfo
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party

@StartableByRPC
class CreateBNAndOnboardMembersFlow (
        private val networkId: UniqueIdentifier,
        private val groupId: UniqueIdentifier,
        private val members: List<Party>
): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // Create a business network.
        subFlow(CreateBusinessNetworkFlow(networkId = networkId, groupId = groupId, notary = notary))

        val onboardedParties = members.map {
            OnboardingInfo(party = it, groupId = groupId)
        }.toSet()

        // Onboard and activate members.
        subFlow(BatchOnboardMembershipFlow(networkId.toString(), onboardedParties, groupId, notary))
    }
}