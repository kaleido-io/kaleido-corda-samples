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

Go to the next step to build the IoU contract and flow jars.

## Build
The 3 components can be built together or separately.

To build everything:
```
./gradlew buildAll
```

The output are in these folders:
- `iou-contract/build/libs/foo-contract-1.0.jar`
- `iou-flow/build/libs/foo-flow-1.0.jar`
- `rpc-client/build/install/bin/rpc-client`

To build each component separately:

```
./gradlew :foo-contract:build
./gradlew :foo-flow:build
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
jarsigner -keystore /corda/certificates/nodekeystore.jks -storepass $PASS ./iou-contract/build/libs/iou-contract.jar identity-private-key
```

## Run
The program supports the following commands and switches:

| Command | Parameter | Usage |
|---------|-----------|-------|
| start | | Start a new flow|
|       | `-u`, `--url`| URL of the target Corda node or the local Kaleido bridge endpoint |
|       | `-n`, `--username`| username for authentiation |
|       | `-p`, `--password`| password for authentiation |
|       | `-c`, `--counter-party`| Name of the counterparty to start flow with, can be a partial search string |
|       | `-v`, `--value`| Value of passed for contract |
|       | `-w`, `--workers`| Number of concurrent workers, default 1 |
|       | `-l`, `--loops`| Loops each worker executes before exiting, default 1 (0=infinite) |
| query | | Query a past transaction |
|       | `-u`, `--url`| URL of the target Corda node or the local Kaleido bridge endpoint |
|       | `-n`, `--username`| username for authentiation |
|       | `-p`, `--password`| password for authentiation |
|       | `-i`, `--tx-id`| Transaction id of an existing transaction |


### Test Issuance of an IOU
To create a new IoU from the lender and have the borrower sign it and for the notary to notarize it:
```
$ rpc-client/build/install/rpc-client/bin/rpc-client issue -u localhost:10011 -n user1 -p test
```

The client will discover all the participants in the network and prompt you for a node as the counterprise (borrower), make sure to pick the other node you created in the network, not the node you are connected to or the notary.

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

