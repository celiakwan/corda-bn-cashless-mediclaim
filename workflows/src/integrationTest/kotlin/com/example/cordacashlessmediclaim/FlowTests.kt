package com.example.cordacashlessmediclaim

import com.example.cordacashlessmediclaim.flows.membershipFlows.AssignRoleFlow
import com.example.cordacashlessmediclaim.flows.membershipFlows.CreateBNAndOnboardMembersFlow
import com.example.cordacashlessmediclaim.flows.paymentReceiptFlows.CreatePaymentReceiptFlow
import com.example.cordacashlessmediclaim.flows.preAuthorizationFlows.ApprovePreAuthorizationFlow
import com.example.cordacashlessmediclaim.flows.preAuthorizationFlows.CreatePreAuthorizationFlow
import com.example.cordacashlessmediclaim.roles.CustomRoles
import com.example.cordacashlessmediclaim.roles.HospitalFinanceClerkRole
import com.example.cordacashlessmediclaim.roles.HospitalRegistrarRole
import com.example.cordacashlessmediclaim.roles.InsurerClaimsOfficerRole
import com.example.cordacashlessmediclaim.roles.InsurerFinanceClerkRole
import com.example.cordacashlessmediclaim.roles.PatientRole
import com.example.cordacashlessmediclaim.states.PaymentReceiptState
import com.example.cordacashlessmediclaim.states.PaymentReceiptStatus
import com.example.cordacashlessmediclaim.states.PreAuthorizationState
import com.example.cordacashlessmediclaim.states.PreAuthorizationStatus
import net.corda.bn.schemas.MembershipStateSchemaV1
import net.corda.bn.states.MembershipState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.NetworkParameters
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.Currency
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlowTests {
    private lateinit var network: MockNetwork
    private lateinit var networkOperatorNode: StartedMockNode
    private lateinit var insurerNode: StartedMockNode
    private lateinit var hospitalNode: StartedMockNode
    private lateinit var patientNode: StartedMockNode
    private lateinit var insurer: Party
    private lateinit var hospital: Party
    private lateinit var patient: Party

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters().withCordappsForAllNodes(listOf(
                TestCordapp.findCordapp("com.example.cordacashlessmediclaim.contracts"),
                TestCordapp.findCordapp("com.example.cordacashlessmediclaim.flows"),
                TestCordapp.findCordapp("net.corda.bn.flows"),
                TestCordapp.findCordapp("net.corda.bn.states")
        )).withNetworkParameters(testNetworkParameters(minimumPlatformVersion = 4)))
        networkOperatorNode = network.createPartyNode(CordaX500Name(organisation = "NetworkOperator", locality = "London", country = "GB"))
        insurerNode = network.createPartyNode(CordaX500Name(organisation = "Insurer", locality = "New York", country = "US"))
        hospitalNode = network.createPartyNode(CordaX500Name(organisation = "Hospital", locality = "New York", country = "US"))
        patientNode = network.createPartyNode(CordaX500Name(organisation = "Patient", locality = "New York", country = "US"))
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun integrationTest() {
        insurer = insurerNode.info.legalIdentities.single()
        hospital = hospitalNode.info.legalIdentities.single()
        patient = patientNode.info.legalIdentities.single()

        val preAuthorizationLinearId = preAuthorizationTest()
        paymentReceiptTest(preAuthorizationLinearId)
    }
    
    private fun preAuthorizationTest(): UniqueIdentifier {
        // Create the first business network, onboard and activate members.
        val networkId = UniqueIdentifier()
        val groupId = UniqueIdentifier()
        val networkMembers = listOf(insurer, hospital, patient)
        networkOperatorNode.startFlow(CreateBNAndOnboardMembersFlow(networkId, groupId, networkMembers))
        network.runNetwork()

        val queryCriteria = QueryCriteria.VaultCustomQueryCriteria(builder {
            MembershipStateSchemaV1.PersistentMembershipState::networkId.equal(networkId.id.toString())
        })
        var membershipStateAndRef = networkOperatorNode.services.vaultService.queryBy<MembershipState>(queryCriteria).states
        assertEquals(4, membershipStateAndRef.size)

        // Assign roles to different parties.
        val membershipIds = membershipStateAndRef.associateBy({ it.state.data.identity.cordaIdentity }, { it.state.data.linearId })
        networkOperatorNode.startFlow(AssignRoleFlow(networkId, membershipIds[insurer]!!, CustomRoles.INSURER_CLAIMS_OFFICER.name))
        networkOperatorNode.startFlow(AssignRoleFlow(networkId, membershipIds[hospital]!!, CustomRoles.HOSPITAL_REGISTRAR.name))
        networkOperatorNode.startFlow(AssignRoleFlow(networkId, membershipIds[patient]!!, CustomRoles.PATIENT.name))
        network.runNetwork()

        membershipStateAndRef = networkOperatorNode.services.vaultService.queryBy<MembershipState>(queryCriteria).states
        val roles = membershipStateAndRef.associateBy({ it.state.data.identity.cordaIdentity }, { it.state.data.roles })
        assertTrue { roles[insurer]!!.contains(InsurerClaimsOfficerRole()) }
        assertTrue { roles[hospital]!!.contains(HospitalRegistrarRole()) }
        assertTrue { roles[patient]!!.contains(PatientRole()) }

        // Create a pre-authorization.
        hospitalNode.startFlow(CreatePreAuthorizationFlow(
                networkId,
                patient,
                "00001",
                "Stroke",
                Currency.getInstance("USD"),
                BigDecimal(300),
                insurer
        ))
        network.runNetwork()

        var insurerPreAuthorizationState = insurerNode.services.vaultService
                .queryBy(PreAuthorizationState::class.java).states.single().state.data
        var hospitalPreAuthorizationState = hospitalNode.services.vaultService
                .queryBy(PreAuthorizationState::class.java).states.single().state.data
        var patientPreAuthorizationState = patientNode.services.vaultService
                .queryBy(PreAuthorizationState::class.java).states.single().state.data
        assertEquals(PreAuthorizationStatus.CREATED, insurerPreAuthorizationState.status)
        assertEquals(PreAuthorizationStatus.CREATED, hospitalPreAuthorizationState.status)
        assertEquals(PreAuthorizationStatus.CREATED, patientPreAuthorizationState.status)

        // Approve the pre-authorization.
        insurerNode.startFlow(ApprovePreAuthorizationFlow(insurerPreAuthorizationState.linearId))
        network.runNetwork()

        insurerPreAuthorizationState = insurerNode.services.vaultService
                .queryBy(PreAuthorizationState::class.java).states.single().state.data
        hospitalPreAuthorizationState = hospitalNode.services.vaultService
                .queryBy(PreAuthorizationState::class.java).states.single().state.data
        patientPreAuthorizationState = patientNode.services.vaultService
                .queryBy(PreAuthorizationState::class.java).states.single().state.data
        assertEquals(PreAuthorizationStatus.APPROVED, insurerPreAuthorizationState.status)
        assertEquals(PreAuthorizationStatus.APPROVED, hospitalPreAuthorizationState.status)
        assertEquals(PreAuthorizationStatus.APPROVED, patientPreAuthorizationState.status)

        return insurerPreAuthorizationState.linearId
    }

    private fun paymentReceiptTest(preAuthorizationLinearId: UniqueIdentifier) {
        // Create the second business network, onboard and activate members.
        val networkId = UniqueIdentifier()
        val groupId = UniqueIdentifier()
        val networkMembers = listOf(insurer, hospital)
        networkOperatorNode.startFlow(CreateBNAndOnboardMembersFlow(networkId, groupId, networkMembers))
        network.runNetwork()

        val queryCriteria = QueryCriteria.VaultCustomQueryCriteria(builder {
            MembershipStateSchemaV1.PersistentMembershipState::networkId.equal(networkId.id.toString())
        })
        var membershipStateAndRef = networkOperatorNode.services.vaultService.queryBy<MembershipState>(queryCriteria).states
        assertEquals(3, membershipStateAndRef.size)

        // Assign roles to different parties.
        val membershipIds = membershipStateAndRef.associateBy({ it.state.data.identity.cordaIdentity }, { it.state.data.linearId })
        networkOperatorNode.startFlow(AssignRoleFlow(networkId, membershipIds[insurer]!!, CustomRoles.INSURER_FINANCE_CLERK.name))
        networkOperatorNode.startFlow(AssignRoleFlow(networkId, membershipIds[hospital]!!, CustomRoles.HOSPITAL_FINANCE_CLERK.name))
        network.runNetwork()

        membershipStateAndRef = networkOperatorNode.services.vaultService.queryBy<MembershipState>(queryCriteria).states
        val roles = membershipStateAndRef.associateBy({ it.state.data.identity.cordaIdentity }, { it.state.data.roles })
        assertTrue { roles[insurer]!!.contains(InsurerFinanceClerkRole()) }
        assertTrue { roles[hospital]!!.contains(HospitalFinanceClerkRole()) }

        // Create a payment receipt.
        insurerNode.startFlow(CreatePaymentReceiptFlow(
                networkId,
                insurer,
                hospital,
                Currency.getInstance("USD"),
                BigDecimal(300),
                preAuthorizationLinearId
        ))
        network.runNetwork()

        val insurerPaymentReceiptState = insurerNode.services.vaultService
                .queryBy(PaymentReceiptState::class.java).states.single().state.data
        val hospitalPaymentReceiptState = hospitalNode.services.vaultService
                .queryBy(PaymentReceiptState::class.java).states.single().state.data
        assertEquals(PaymentReceiptStatus.CREATED, insurerPaymentReceiptState.status)
        assertEquals(PaymentReceiptStatus.CREATED, hospitalPaymentReceiptState.status)
    }
}