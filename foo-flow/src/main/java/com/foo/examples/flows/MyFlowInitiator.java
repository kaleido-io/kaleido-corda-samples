package com.foo.examples.flows;

import co.paralleluniverse.fibers.Suspendable; // see below...

import com.foo.examples.contracts.MyContract;
import com.foo.examples.contracts.MyState1;

import net.corda.core.utilities.ProgressTracker;

import net.corda.core.flows.*;

import net.corda.core.contracts.Command;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;


// Replace Initiator's definition with:
@InitiatingFlow
@StartableByRPC
public class MyFlowInitiator extends FlowLogic<Void> {
    private final Integer iouValue;
    private final Party otherParty;

    private final String xx = "foo";

    /**
     * The progress tracker provides checkpoints indicating the progress of
     the flow to observers.
     */
    private final ProgressTracker progressTracker = new ProgressTracker();

    public MyFlowInitiator(Integer iouValue, Party otherParty) {
        this.iouValue = iouValue;
        this.otherParty = otherParty;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    /**
     * The flow logic is encapsulated within the call() method.
     *
     * FlowLogic.call is annotated @Suspendable - this allows the flow to be check-pointed and serialised to disk when it encounters a long-running operation, allowing your node to move on to running other flows. 
     *   --> Forgetting this annotation out will lead to some very weird error messages!
     * What a Drag that it comes from co.paralleluniverse ... yikes
     */
    @Suspendable
    @Override
    public Void call() throws FlowException {
        // We retrieve the notary identity from the network map.
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // We create the transaction components.
        MyState1 outputState = new MyState1(iouValue, "Hello-World", getOurIdentity(), otherParty);

	// Template had MyContract.Commands.Send not Action so... hmm...
	Command command = new Command<>(new MyContract.Commands.Send(), getOurIdentity().getOwningKey());

        // We create a transaction builder and add the components.
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(outputState, MyContract.ID)
                .addCommand(command);

        // Signing the transaction.
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Creating a session with the other party.
        FlowSession otherPartySession = initiateFlow(otherParty);

        // We finalise the transaction and then send it to the counterparty.
        subFlow(new FinalityFlow(signedTx, otherPartySession));

        return null;
    }
}

