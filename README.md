# Kaleido sample code for Corda

Sample code for development components of a Corda App: contracts, flows and rpc-clients, based on JDK 11

© Copyright 2020 Kaleido

## Pre-reqs
- JDK 11 (such as from [https://adoptopenjdk.net/](https://gradle.org/install/))
- gradle ([https://gradle.org/install/](https://gradle.org/install/))

## Set up the target Corda network
Go to [kaleido.io](https://console.kaleido.io), sign up and create a Corda blockchain environment with at least two user Corda nodes (the platform with create a 3rd node as the notary for you, as a fully managed setup).

Define users and role for each node you want to send transactions to.

Set up the Kaleido bridge and running it locally on your laptop, so that your RPC client program can communicate with your Corda node running in Kaleido.

Go to the next step to build the IoU contracts and workflow jars.

## Build
The 3 components can be built together or separately.

To build everything:
```
./gradlew buildAll
```

The output are in these folders:
- `contracts/build/libs/contracts-1.0.jar`
- `flows/build/libs/flows-1.0.jar`
- `rpc-client/build/install/bin/rpc-client`

To build each component separately:

```
./gradlew :contracts:build
./gradlew :flows:build
./gradlew :rpc-client:build
./gradlew :rpc-client:installDist
```

## Deploy
Kaleido CorDapp management will automatically sign the contract and flow jars when you use the platform to deploy them to your Corda environment in Kaleido. If you are using this sample against a locally set up Corda network, you must sign the jars yourself in order to deploy them.

### Deploying to Corda networks in Kaleido
Using the Kaleido Console UI or APIs, you can easily upload the unsigned jars into Kaleido's contract management and promote them to your Corda environment. Then you can decide when to accept them into your Corda nodes, and will be prompted to restart the Corda server to pick up the new jars. Kaleido will make sure the jars get properly signed.


### Deploying to a local Corda network
You will need to sign the jars yourself with the node identity key. Below is a sample command to sign:

```
jarsigner -keystore /corda/certificates/nodekeystore.jks -storepass $PASS ./contracts/build/libs/contracts.jar identity-private-key
```

## Run
The program supports the following commands and switches:

| Command | Parameter | Usage |
|---------|-----------|-------|
| issue | | Issue a new IoU |
|       | `-u`, `--url`| URL of the target Corda node or the local Kaleido bridge endpoint |
|       | `-n`, `--username`| username for authentiation |
|       | `-p`, `--password`| password for authentiation |
|       | `-b`, `--borrower-id`| X.500 name of the borrower's node to issue the IoU to|
|       | `--lender-id` | X.500 name  of the your account name to lend|
|       | `--borrower-account`| Name of the borrower's account to issue the IoU to|
|       | `--lender-account` | Name of the your account name to lend|
|       | `-v`, `--value`| Value of the issued IoU contract |
|       | `-w`, `--workers`| Number of concurrent workers, default 1 |
|       | `-l`, `--loops`| Loops each worker executes before exiting, default 1 (0=infinite) |
| query | | Query a past IoU issuance transaction |
|       | `-u`, `--url`| URL of the target Corda node or the local Kaleido bridge endpoint |
|       | `-n`, `--username`| username for authentiation |
|       | `-p`, `--password`| password for authentiation |
|       | `-i`, `--tx-id`| Transaction id of an existing transaction |
| account-utils | | Command for invoking accounts related utility function |
|       | `--create-account` | Sub command for creating a new account |
|       | `--account-name` | Name of the account to be created or share account info |
|       | `--share-account-info` | Sub command to share your account's info to other node |
|       | `--create-account-key` | Sub command to create a new key for the given account uuid |
|       | `--account-uuid` | UUID of account to generate new key pair for |
|       | `--party-share-to` | id of the party to share account info with, can be a partial string |


### Test Issuance of an IOU
To create a new IoU from the lender and have the borrower sign it and for the notary to notarize it:
```
$ rpc-client/build/install/rpc-client/bin/rpc-client issue -u localhost:10011 -n user1 -p test
```

The client will discover all the participants in the network and prompt you for a node as the counterprise (borrower), make sure to pick the other node you created in the network, not the node you are connected to or the notary.

### Account Utilities
This example also works with corda [accounts](https://github.com/corda/accounts). To use accounts, nodes must have required accounts cordapps already installed i.e., `accounts-contracts-1.0.jar`, `accounts-workflows-1.0.jar`, `ci-workflows-1.0.jar` .
Introduction to accounts is available [here](https://github.com/corda/accounts/blob/master/docs.md). Accounts allow Cordapp developers to divide node's vault into logical sub-vaults, i.e., account is collection of states on a node which are controlled by keys belonging to account.
Nodes store `AccountInfo` state, which has basic information about an `account` which is. 
    * Account host, `Party` object, i.e., corda node which this accounts belong to.
    * Account name, `String`, human readable string to identify account, it doesn't have to be unique at network level. (name, host) tuple is guaranteed to unique at network level.
    * Account id, `UUID`, 128-bit random ID that should be unique at Network level.

Unlike corda nodes, which have a default key pair associated with them. A newly created account doesn't have a key pair associated with it. A new key pair can be requested for an account using RequestKey flow whenever you want to transact with account.
Nodes don't know about the accounts of other nodes unless the information is explicitly shared with them. This is done via ShareAccountInfoFlow. 

In order to use IOU issue example with accounts, account utilities can be used to create accounts and share the account info with other nodes.

#### Create new account
To create a new account with name `partyA` on a node:
```
$ rpc-client/build/install/rpc-client/bin/rpc-client account-utils --create-account --account-name partyA -u localhost:10011 -n user1 -p test
```
#### Generate a key pair for a given account
To create a new key pair for a given account on a node:
```
$ rpc-client/build/install/rpc-client/bin/rpc-client account-utils --create-account-key --account-uuid 85b8121c-2bfb-4bec-af36-41a33d87b897 -u localhost:10011 -n user1 -p test
```
#### Share account's information with other node
To share account info with other node so, that they can use it in a flow:
```
$ rpc-client/build/install/rpc-client/bin/rpc-client account-utils --share-account-info --account-name partyA  --party-share-to zzgqil3zm1 -u localhost:10011 -n user1 -p test
```

### Test Issuance of an IOU
To create a new IoU from the lender and have the borrower sign it and for the notary to notarize it using `account <> account`:
```
$ rpc-client/build/install/rpc-client/bin/rpc-client issue --lender-account partyA --borrower-account partyB --value 98 -u localhost:10011 -n user1 -p test
```
or To using `node <> node`
```
$ rpc-client/build/install/rpc-client/bin/rpc-client issue  --value 98 -u localhost:10011 -n user1 -p test
```
The client will discover all the participants in the network and prompt you for a node as the lender and  a counterparty (borrower), make sure to pick the other node you created in the network, not the node you are connected to or the notary.

or To using `node <> account`
```
$ rpc-client/build/install/rpc-client/bin/rpc-client issue  --borrower-account partyB --value 98 -u localhost:10011 -n user1 -p test
```
The client will discover all the participants in the network and prompt you for a node as the lender and  a counterparty (borrower), make sure to pick the other node you created in the network, not the node you are connected to or the notary.

### Query Past Transactions
To query a completed transaction, copy the transaction ID from the output above and use it as the value of `-i`:
```
$ rpc-client/build/install/rpc-client/bin/rpc-client query -u localhost:10011 -n user1 -p test -i 311E4723F5B1C647BCD3472BC6097E708487331100A36ACAC821C22D7DC46D22

[main] INFO net.corda.client.rpc.internal.RPCClient - Startup took 1197 msec
[main] INFO io.kaleido.ClientRpcExample - Calling node for current time...
[main] INFO io.kaleido.ClientRpcExample - 2020-04-06T15:48:07.444985Z
[main] INFO io.kaleido.ClientRpcExample - Calling node for node info...
[main] INFO io.kaleido.ClientRpcExample - NodeInfo(addresses=[zzmrepgprk.zzdrbkw7k1.kaleido.network:10000], legalIdentitiesAndCerts=[CN=Node of zzmrepgprk for zzdrbkw7k1, O=Kaleido, L=Raleigh, C=US], platformVersion=6, serial=1586061213623)
[main] INFO io.kaleido.ClientRpcExample - Calling node for notary information...
[main] INFO io.kaleido.ClientRpcExample - [CN=Node of zzmreppmqy for zzdrbkw7k1, O=Kaleido, L=Raleigh, C=US]
[main] INFO io.kaleido.ClientRpcExample - Number of IOU states returned from query: 3
[main] INFO io.kaleido.ClientRpcExample - Found state by transaction hash:
[main] INFO io.kaleido.ClientRpcExample -   Notary: CN=Node of zzmreppmqy for zzdrbkw7k1, O=Kaleido, L=Raleigh, C=US
[main] INFO io.kaleido.ClientRpcExample -   Value: 100
[main] INFO io.kaleido.ClientRpcExample -   IoU lender: CN=Node of zzmrepgprk for zzdrbkw7k1, O=Kaleido, L=Raleigh, C=US
[main] INFO io.kaleido.ClientRpcExample -   IoU borrower: CN=Node of zzabcdefgh for zzdrbkw7k1, O=Kaleido, L=Raleigh, C=US
```

## Reference

Here's a print out of the saved State objects inside a Corda node's state database (called "Vault"):

```
Page(
  states=[
    StateAndRef(
      state=TransactionState(
        data=IOUState(
          value=100,
          lender=CN=Node of zzmrepgprk for zzdrbkw7k1, O=Kaleido, L=Raleigh, C=US,
          borrower=CN=Node of zzabcdefgh for zzdrbkw7k1, O=Kaleido, L=Raleigh, C=US,
          linearId=093ac716-8594-4650-a893-419d6e6893b2
        ),
        contract=io.kaleido.contract.IOUContract,
        notary=CN=Node of zzmreppmqy for zzdrbkw7k1, O=Kaleido, L=Raleigh, C=US,
        encumbrance=null,
        constraint=SignatureAttachmentConstraint(
          key=(Public key: DLCTymWiNxvGWE117QUKSjeW71yaaGc5tNL2i8Zp8dFUvE, weight: 1, Public key: DLEoyzE6Y6Jyw5rRP2yqzGdJpBGCqGsZWD19u6BJdVgVr4, weight: 1)
        )
      ),
      ref=DA263D8AB3225C0F4BC81FB7512F3D9A416BFAC89832CCB8D054AEE0CC91BF59(0)
    ),
    StateAndRef(
      state=TransactionState(
        data=IOUState(
          value=100,
          lender=CN=Node of zzmrepgprk for zzdrbkw7k1, O=Kaleido, L=Raleigh, C=US,
          borrower=CN=Node of zzabcdefgh for zzdrbkw7k1, O=Kaleido, L=Raleigh, C=US,
          linearId=aa6a3f4d-a5eb-45de-9412-3df76b634638
        ),
        contract=io.kaleido.contract.IOUContract,
        notary=CN=Node of zzmreppmqy for zzdrbkw7k1, O=Kaleido, L=Raleigh, C=US,
        encumbrance=null,
        constraint=SignatureAttachmentConstraint(
          key=(Public key: DLCTymWiNxvGWE117QUKSjeW71yaaGc5tNL2i8Zp8dFUvE, weight: 1, Public key: DLEoyzE6Y6Jyw5rRP2yqzGdJpBGCqGsZWD19u6BJdVgVr4, weight: 1)
        )
      ),
      ref=311E4723F5B1C647BCD3472BC6097E708487331100A36ACAC821C22D7DC46D22(0)
    )
  ],
  statesMetadata=[
    StateMetadata(
      ref=DA263D8AB3225C0F4BC81FB7512F3D9A416BFAC89832CCB8D054AEE0CC91BF59(0),
      contractStateClassName=io.kaleido.state.IOUState,
      recordedTime=2020-04-05T15:41:35.881766Z,
      consumedTime=null,
      status=UNCONSUMED,
      notary=CN=Node of zzmreppmqy for zzdrbkw7k1, O=Kaleido, L=Raleigh, C=US,
      lockId=null,
      lockUpdateTime=null,
      relevancyStatus=RELEVANT,
      constraintInfo=ConstraintInfo(
        constraint=SignatureAttachmentConstraint(
          key=(Public key: DLCTymWiNxvGWE117QUKSjeW71yaaGc5tNL2i8Zp8dFUvE, weight: 1, Public key: DLEoyzE6Y6Jyw5rRP2yqzGdJpBGCqGsZWD19u6BJdVgVr4, weight: 1)
        )
      )
    ),
    StateMetadata(
      ref=311E4723F5B1C647BCD3472BC6097E708487331100A36ACAC821C22D7DC46D22(0),
      contractStateClassName=io.kaleido.state.IOUState,
      recordedTime=2020-04-06T14:21:51.889793Z,
      consumedTime=null,
      status=UNCONSUMED,
      notary=CN=Node of zzmreppmqy for zzdrbkw7k1, O=Kaleido, L=Raleigh, C=US,
      lockId=null,
      lockUpdateTime=null,
      relevancyStatus=RELEVANT,
      constraintInfo=ConstraintInfo(
        constraint=SignatureAttachmentConstraint(
          key=(Public key: DLCTymWiNxvGWE117QUKSjeW71yaaGc5tNL2i8Zp8dFUvE, weight: 1, Public key: DLEoyzE6Y6Jyw5rRP2yqzGdJpBGCqGsZWD19u6BJdVgVr4, weight: 1)
        )
      )
    )
  ],
  totalStatesAvailable=-1,
  stateTypes=UNCONSUMED,
  otherResults=[]
)
```
