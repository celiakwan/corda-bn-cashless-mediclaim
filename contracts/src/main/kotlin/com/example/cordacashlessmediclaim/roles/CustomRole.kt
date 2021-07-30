package com.example.cordacashlessmediclaim.roles

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class CustomRole {
    INSURER_CLAIMS_OFFICER,
    INSURER_FINANCE_CLERK,
    HOSPITAL_REGISTRAR,
    HOSPITAL_FINANCE_CLERK,
    PATIENT
}