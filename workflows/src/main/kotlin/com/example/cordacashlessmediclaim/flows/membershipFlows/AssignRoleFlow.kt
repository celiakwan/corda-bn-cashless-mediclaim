package com.example.cordacashlessmediclaim.flows.membershipFlows

import co.paralleluniverse.fibers.Suspendable
import com.example.cordacashlessmediclaim.roles.CustomRoles
import com.example.cordacashlessmediclaim.roles.HospitalFinanceClerkRole
import com.example.cordacashlessmediclaim.roles.HospitalRegistrarRole
import com.example.cordacashlessmediclaim.roles.InsurerClaimsOfficerRole
import com.example.cordacashlessmediclaim.roles.InsurerFinanceClerkRole
import com.example.cordacashlessmediclaim.roles.PatientRole
import net.corda.bn.flows.BNService
import net.corda.bn.flows.MembershipNotFoundException
import net.corda.bn.flows.ModifyRolesFlow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import java.lang.IllegalArgumentException

@StartableByRPC
class AssignRoleFlow (
        private val networkId: UniqueIdentifier,
        private val membershipId: UniqueIdentifier,
        private val roleName: String
): FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val bnService = serviceHub.cordaService(BNService::class.java)
        val membershipState = bnService.getMembership(membershipId)?.state?.data ?: throw
        MembershipNotFoundException("$ourIdentity is not a member of Business Network with ID $networkId")
        val role = when (roleName) {
            CustomRoles.INSURER_CLAIMS_OFFICER.name -> InsurerClaimsOfficerRole()
            CustomRoles.INSURER_FINANCE_CLERK.name -> InsurerFinanceClerkRole()
            CustomRoles.HOSPITAL_REGISTRAR.name -> HospitalRegistrarRole()
            CustomRoles.HOSPITAL_FINANCE_CLERK.name -> HospitalFinanceClerkRole()
            CustomRoles.PATIENT.name -> PatientRole()
            else -> throw IllegalArgumentException("Illegal roleName $roleName")
        }
        subFlow(ModifyRolesFlow(membershipId, membershipState.roles + role, notary))
    }
}