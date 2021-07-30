# corda-bn-cashless-mediclaim
A Corda project written in Kotlin to demonstrate the simplified processes of cashless medical insurance claims and creating payment receipts. It shows how to create business networks, onboard, activate and assign different custom roles to network members, and verify their memberships in the flows.

[corda-ac-cashless-mediclaim](https://github.com/celiakwan/corda-ac-cashless-mediclaim) is another cashless mediclaim project implementing Corda accounts instead.

### Version
- [Kotlin](https://kotlinlang.org/): 1.2.71
- [Corda](https://www.corda.net/): 4.8
- [Corda Business Network](https://github.com/corda/bn-extension): 1.1
- [Gradle](https://gradle.org/): 5.0.12

### Installation
Install Java 8 JDK.
```
brew tap adoptopenjdk/openjdk
brew install --cask adoptopenjdk8
```

### Configuration
`build.gradle` contains the configuration for 5 Corda nodes that grants the RPC user of each node permissions to run specific RPC operations and flows.
```
node {
    name "O=Notary,L=London,C=GB"
    notary = [validating : false]
    p2pPort 10002
    rpcSettings {
        address("localhost:10003")
        adminAddress("localhost:10043")
    }
}
node {
    name "O=NetworkOperator,L=London,C=GB"
    p2pPort 10005
    rpcSettings {
        address("localhost:10006")
        adminAddress("localhost:10046")
    }
    rpcUsers = [
            [
                    "username": "user1",
                    "password": "test",
                    "permissions": [
                            "InvokeRpc.nodeInfo",
                            "InvokeRpc.networkMapSnapshot",
                            "InvokeRpc.currentNodeTime",
                            "InvokeRpc.wellKnownPartyFromX500Name",
                            "InvokeRpc.vaultQuery",
                            "InvokeRpc.vaultQueryBy",
                            "InvokeRpc.stateMachinesSnapshot",
                            "InvokeRpc.nodeDiagnosticInfo",
                            "InvokeRpc.notaryIdentities",
                            "InvokeRpc.attachmentExists",
                            "InvokeRpc.partyFromKey",
                            "InvokeRpc.notaryPartyFromX500Name",
                            "InvokeRpc.partiesFromName",
                            "InvokeRpc.registeredFlows",
                            "StartFlow.com.example.cordacashlessmediclaim.flows.membershipFlows.CreateBNAndOnboardMembersFlow",
                            "StartFlow.com.example.cordacashlessmediclaim.flows.membershipFlows.AssignRoleFlow"
                    ]
            ]
    ]
}
node {
    name "O=Insurer,L=New York,C=US"
    p2pPort 10008
    rpcSettings {
        address("localhost:10009")
        adminAddress("localhost:10049")
    }
    rpcUsers = [
            [
                    "username": "user1",
                    "password": "test",
                    "permissions": [
                            "InvokeRpc.nodeInfo",
                            "InvokeRpc.networkMapSnapshot",
                            "InvokeRpc.currentNodeTime",
                            "InvokeRpc.wellKnownPartyFromX500Name",
                            "InvokeRpc.vaultQuery",
                            "InvokeRpc.vaultQueryBy",
                            "InvokeRpc.stateMachinesSnapshot",
                            "InvokeRpc.nodeDiagnosticInfo",
                            "InvokeRpc.notaryIdentities",
                            "InvokeRpc.attachmentExists",
                            "InvokeRpc.partyFromKey",
                            "InvokeRpc.notaryPartyFromX500Name",
                            "InvokeRpc.partiesFromName",
                            "InvokeRpc.registeredFlows",
                            "StartFlow.com.example.cordacashlessmediclaim.flows.preAuthorizationFlows.ApprovePreAuthorizationFlow",
                            "StartFlow.com.example.cordacashlessmediclaim.flows.paymentReceiptFlows.CreatePaymentReceiptFlow"
                    ]
            ]
    ]
}
node {
    name "O=Hospital,L=New York,C=US"
    p2pPort 10011
    rpcSettings {
        address("localhost:10012")
        adminAddress("localhost:10052")
    }
    rpcUsers = [
            [
                    "username": "user1",
                    "password": "test",
                    "permissions": [
                            "InvokeRpc.nodeInfo",
                            "InvokeRpc.networkMapSnapshot",
                            "InvokeRpc.currentNodeTime",
                            "InvokeRpc.wellKnownPartyFromX500Name",
                            "InvokeRpc.vaultQuery",
                            "InvokeRpc.vaultQueryBy",
                            "InvokeRpc.stateMachinesSnapshot",
                            "InvokeRpc.nodeDiagnosticInfo",
                            "InvokeRpc.notaryIdentities",
                            "InvokeRpc.attachmentExists",
                            "InvokeRpc.partyFromKey",
                            "InvokeRpc.notaryPartyFromX500Name",
                            "InvokeRpc.partiesFromName",
                            "InvokeRpc.registeredFlows",
                            "StartFlow.com.example.cordacashlessmediclaim.flows.preAuthorizationFlows.CreatePreAuthorizationFlow"
                    ]
            ]
    ]
}
node {
    name "O=Patient,L=New York,C=US"
    p2pPort 10014
    rpcSettings {
        address("localhost:10015")
        adminAddress("localhost:10055")
    }
    rpcUsers = [
            [
                    "username": "user1",
                    "password": "test",
                    "permissions": [
                            "InvokeRpc.nodeInfo",
                            "InvokeRpc.networkMapSnapshot",
                            "InvokeRpc.currentNodeTime",
                            "InvokeRpc.wellKnownPartyFromX500Name",
                            "InvokeRpc.vaultQuery",
                            "InvokeRpc.vaultQueryBy",
                            "InvokeRpc.stateMachinesSnapshot",
                            "InvokeRpc.nodeDiagnosticInfo",
                            "InvokeRpc.notaryIdentities",
                            "InvokeRpc.attachmentExists",
                            "InvokeRpc.partyFromKey",
                            "InvokeRpc.notaryPartyFromX500Name",
                            "InvokeRpc.partiesFromName",
                            "InvokeRpc.registeredFlows"
                    ]
            ]
    ]
}
```

### Build
Run the following Gradle task to build the nodes for testing. In the `build/nodes` directory, there will be files generated for the nodes such as CorDapp JARs, configuration files, certificates, etc.
```
./gradlew deployNodes
```

### Get Started
1. Open 5 terminal tabs and for each one set `JAVA_HOME` to the path where Java 8 JDK is located.
    ```
    export JAVA_HOME=`/usr/libexec/java_home -v 1.8`
    ```

2. Manually start each node in each terminal tab.
    ##### Notary
    ```
    cd build/nodes/Notary
    java -jar corda.jar run-migration-scripts --core-schemas --app-schemas
    java -jar corda.jar
    ```

    ##### Network Operator
    ```
    cd build/nodes/NetworkOperator
    java -jar corda.jar run-migration-scripts --core-schemas --app-schemas
    java -jar corda.jar
    ```

    ##### Insurer
    ```
    cd build/nodes/Insurer
    java -jar corda.jar run-migration-scripts --core-schemas --app-schemas
    java -jar corda.jar
    ```

    ##### Hospital
    ```
    cd build/nodes/Hospital
    java -jar corda.jar run-migration-scripts --core-schemas --app-schemas
    java -jar corda.jar
    ```

    ##### Patient
    ```
    cd build/nodes/Patient
    java -jar corda.jar run-migration-scripts --core-schemas --app-schemas
    java -jar corda.jar
    ```

### Testing
##### Network Operator
1. Create the first business network and a network sub-group. Add Insurer, Hospital and Patient to the network and the sub-group, and activate their memberships in batches.
    ```
    flow start CreateBNAndOnboardMembersFlow networkId: aeb486a7-309e-4b9f-8b09-91c1b7ba5849, groupId: c11790fa-c197-4fa5-a057-8cb0cc571c11, members: ["O=Insurer,L=New York,C=US", "O=Hospital,L=New York,C=US", "O=Patient,L=New York,C=US"]
    ```

2. Query the `MembershipState` from the vault. Get the `linearId.id` from the states which the `networkId` is `aeb486a7-309e-4b9f-8b09-91c1b7ba5849` and the `identity.cordaIdentity` belongs to Insurer, Hospital and Patient respectively.
    ```
    run vaultQuery contractStateType: net.corda.bn.states.MembershipState
    ```

3. Query the `GroupState` from the vault.
    ```
    run vaultQuery contractStateType: net.corda.bn.states.GroupState
    ```

4. Assign custom roles to Insurer, Hospital and Patient in the first network.
    ```
    flow start AssignRoleFlow networkId: aeb486a7-309e-4b9f-8b09-91c1b7ba5849, membershipId: 8acb891a-c1d2-4039-b4f1-ea59e8c5844a, roleName: INSURER_CLAIMS_OFFICER
    flow start AssignRoleFlow networkId: aeb486a7-309e-4b9f-8b09-91c1b7ba5849, membershipId: 598c5e12-004e-4c70-be59-bf4d3211d351, roleName: HOSPITAL_REGISTRAR
    flow start AssignRoleFlow networkId: aeb486a7-309e-4b9f-8b09-91c1b7ba5849, membershipId: 38dc6472-46c2-44d9-b9d9-b4325f96fcaa, roleName: PATIENT
    ```

##### Hospital
5. Submit a pre-authorization.
    ```
    flow start CreatePreAuthorizationFlow networkId: aeb486a7-309e-4b9f-8b09-91c1b7ba5849, policyHolder: "O=Patient,L=New York,C=US", membershipNumber: 00001, diagnosisDescription: Stroke, currency: USD, amount: 300, policyIssuer: "O=Insurer,L=New York,C=US"
    ```

6. Query the `PreAuthorizationState` from the vault and get the `linearId.id`.
    ```
    run vaultQuery contractStateType: com.example.cordacashlessmediclaim.states.PreAuthorizationState
    ```

##### Insurer
7. Approve the pre-authorization.
    ```
    flow start ApprovePreAuthorizationFlow linearId: 04e8240c-6709-4a70-a7c8-2a8143fef84b
    ```

##### Patient
8. Query the `PreAuthorizationState` from the vault.
    ```
    run vaultQuery contractStateType: com.example.cordacashlessmediclaim.states.PreAuthorizationState
    ```

##### Network Operator
9. Create the second business network and a network sub-group. Add Insurer and Hospital to the network and the sub-group, and activate their memberships in batches.
    ```
    flow start CreateBNAndOnboardMembersFlow networkId: e546df03-ae17-420e-bf56-7ccc9277428d, groupId: 5d5a3f3a-9ebd-48d7-a057-7b48a2ec7009, members: ["O=Insurer,L=New York,C=US", "O=Hospital,L=New York,C=US"]
    ```

10. Query the `MembershipState` from the vault. Get the `linearId.id` from the states which the `networkId` is `e546df03-ae17-420e-bf56-7ccc9277428d` and the `identity.cordaIdentity` belongs to Insurer and Hospital respectively.
    ```
    run vaultQuery contractStateType: net.corda.bn.states.MembershipState
    ```

11. Query the `GroupState` from the vault.
    ```
    run vaultQuery contractStateType: net.corda.bn.states.GroupState
    ```

12. Assign custom roles to Insurer and Hospital in the second network.
    ```
    flow start AssignRoleFlow networkId: e546df03-ae17-420e-bf56-7ccc9277428d, membershipId: c06f8435-17b0-486d-8a1b-169688293663, roleName: INSURER_FINANCE_CLERK
    flow start AssignRoleFlow networkId: e546df03-ae17-420e-bf56-7ccc9277428d, membershipId: f5d4ffaf-1ad6-4850-bf3f-b36f99f54185, roleName: HOSPITAL_FINANCE_CLERK
    ```

##### Insurer
13. Send a payment receipt.
    ```
    flow start CreatePaymentReceiptFlow networkId: e546df03-ae17-420e-bf56-7ccc9277428d, payer: "O=Insurer,L=New York,C=US", payee: "O=Hospital,L=New York,C=US", currency: USD, amount: 300, preAuthorizationLinearId: 04e8240c-6709-4a70-a7c8-2a8143fef84b
    ```

##### Hospital
14. Query the `PaymentReceiptState` from the vault.
    ```
    run vaultQuery contractStateType: com.example.cordacashlessmediclaim.states.PaymentReceiptState
    ```