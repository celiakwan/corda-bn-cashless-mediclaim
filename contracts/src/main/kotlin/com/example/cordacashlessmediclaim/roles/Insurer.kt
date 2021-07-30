package com.example.cordacashlessmediclaim.roles

import net.corda.bn.states.BNPermission
import net.corda.bn.states.BNRole
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
class InsurerClaimsOfficerRole: BNRole(CustomRole.INSURER_CLAIMS_OFFICER.name, setOf(InsurerPermissions.CAN_APPROVE_PRE_AUTHORIZATION))

@CordaSerializable
class InsurerFinanceClerkRole: BNRole(CustomRole.HOSPITAL_FINANCE_CLERK.name, setOf(InsurerPermissions.CAN_CREATE_PAYMENT_RECEIPT))

@CordaSerializable
enum class InsurerPermissions: BNPermission {
    CAN_APPROVE_PRE_AUTHORIZATION, CAN_CREATE_PAYMENT_RECEIPT
}