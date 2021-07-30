package com.example.cordacashlessmediclaim.roles

import net.corda.bn.states.BNPermission
import net.corda.bn.states.BNRole
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
class HospitalRegistrarRole: BNRole(CustomRole.HOSPITAL_REGISTRAR.name, setOf(HospitalPermissions.CAN_CREATE_PRE_AUTHORIZATION))

@CordaSerializable
class HospitalFinanceClerkRole: BNRole(CustomRole.HOSPITAL_FINANCE_CLERK.name, setOf())

@CordaSerializable
enum class HospitalPermissions: BNPermission {
    CAN_CREATE_PRE_AUTHORIZATION
}