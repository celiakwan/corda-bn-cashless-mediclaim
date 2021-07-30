package com.example.cordacashlessmediclaim.roles

import net.corda.bn.states.BNRole
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
class PatientRole: BNRole(CustomRole.PATIENT.name, setOf())