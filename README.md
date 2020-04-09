# corda-samples
Sample code for development components of a Corda App: contracts, flows and rpc-clients, based on JDK 11

## Pre-reqs
- JDK 11 (such as from [https://adoptopenjdk.net/](https://gradle.org/install/))
- gradle ([https://gradle.org/install/](https://gradle.org/install/))

## Set up the target Corda network
The easiest way to get a local Corda network running, according to the way Kaleido is building them, is by checking out the repository `photic-cordamanager` and run the integration test.
```
cd photic-cordamanager
mocha integration-test/generate-full-network.js |bunyan
```

Then follow the [configuration steps](https://github.com/kaleido-io/photic-cordamanager/blob/master/integration-test/generate-full-network.js#L13) to launch the network based on the generated configurations.

## Build
The 3 components can be built together or separately.

To build everything:
```
./gradlew buildAll
```
The output are in these folders:
- `iou-contract/build/libs/iou-contract-1.0.jar`
- `iou-flow/build/libs/iou-flow-1.0.jar`
- `rpc-client/build/install/bin/rpc-client`

To build each component separately:
```
./gradlew :iou-contract:build
./gradlew :iou-flow:build
./gradlew :rpc-client:build
./gradlew :rpc-client:installDist
```

You then must sign the jar with a trusted key in the target network. For instance the network map key (_integtration-test-network_/notary/corda/ca/netmapkeystore.jks, alias _cordanetworkmap_), or an identity key from one of the Corda nodes (_integration-test-network_/node1/certificates/nodekeystore.jks, alias _identity-private-key_):
```
jarsigner -keystore ~/Documents/tmp/cordatest/node1/corda/certificates/nodekeystore.jks -storepass $PASS ../iou-contract/build/libs/iou-contract.jar identity-private-key
```

## Run to Test Issuance of an IOU
To create a new IoU from the lender and have the borrower sign it and for the notary to notarize it:
```
$ rpc-client/build/install/rpc-client/bin/rpc-client -url localhost:10011 -username user1 -password test -newIoU
[main] INFO net.corda.client.rpc.internal.RPCClient - Startup took 1201 msec
[main] INFO io.kaleido.ClientRpcExample - Calling node for current time...
[main] INFO io.kaleido.ClientRpcExample - 2020-04-06T14:21:46.773364Z
[main] INFO io.kaleido.ClientRpcExample - Calling node for node info...
[main] INFO io.kaleido.ClientRpcExample - NodeInfo(addresses=[zzmrepgprk.zzdrbkw7k1.kaleido.network:10000], legalIdentitiesAndCerts=[CN=Node of zzmrepgprk for zzdrbkw7k1, O=Kaleido, L=Raleigh, C=US], platformVersion=6, serial=1586061213623)
[main] INFO io.kaleido.ClientRpcExample - Calling node for notary information...
[main] INFO io.kaleido.ClientRpcExample - [CN=Node of zzmreppmqy for zzdrbkw7k1, O=Kaleido, L=Raleigh, C=US]
[main] INFO io.kaleido.ClientRpcExample - Initiating the IoU flow...
[main] INFO io.kaleido.ClientRpcExample - Size of parties: 1
[main] INFO io.kaleido.ClientRpcExample - Borrower party: CN=Node of zzabcdefgh for zzdrbkw7k1, O=Kaleido, L=Raleigh, C=US
[main] INFO io.kaleido.ClientRpcExample - Started flow, handle: FlowHandleImpl(id=[b2db26eb-0a8a-4f71-bd79-d42e8b3ca072], returnValue=net.corda.core.internal.concurrent.CordaFutureImpl@12010fd1)
[main] INFO io.kaleido.ClientRpcExample - Signed tx: SignedTransaction(id=311E4723F5B1C647BCD3472BC6097E708487331100A36ACAC821C22D7DC46D22)
[main] INFO io.kaleido.ClientRpcExample - IOUState(value=100, lender=CN=Node of zzmrepgprk for zzdrbkw7k1, O=Kaleido, L=Raleigh, C=US, borrower=CN=Node of zzabcdefgh for zzdrbkw7k1, O=Kaleido, L=Raleigh, C=US, linearId=aa6a3f4d-a5eb-45de-9412-3df76b634638)
```

To query the completed transaction, copy the transaction ID from the output above and use it as the value of `-txId`:
```
$ rpc-client/build/install/rpc-client/bin/rpc-client -url localhost:10011 -username user1 -password test -query -txId 311E4723F5B1C647BCD3472BC6097E708487331100A36ACAC821C22D7DC46D22
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