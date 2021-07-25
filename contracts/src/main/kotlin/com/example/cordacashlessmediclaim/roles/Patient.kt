package com.example.cordacashlessmediclaim.roles

import net.corda.bn.states.BNRole
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
class PatientRole: BNRole(CustomRoles.PATIENT.name, setOf())