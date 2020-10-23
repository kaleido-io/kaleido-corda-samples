package com.foo.examples.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;

// ******************
// * Responder flow *
// ******************


// Replace Responder's definition with:
@InitiatedBy(MyFlowInitiator.class)
public class MyFlowResponder extends FlowLogic<Void> {
    private final FlowSession otherPartySession;

    public MyFlowResponder(FlowSession otherPartySession) {
        this.otherPartySession = otherPartySession;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
	// See below commented out re. how we might do "last licks" checks on
	// the *signed* transaction before we *really* commit to it.
	// In this simple example, no such checks; just move on:
        subFlow(new ReceiveFinalityFlow(otherPartySession));
        return null;
    }
}


/*
@InitiatedBy(Initiator.class)  // Ooooo!
public class Responder extends FlowLogic<Void> {

    //private variable                                                                                                                      
    private FlowSession counterpartySession;

    //Constructor                                                                                                                           
    public Responder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Suspendable   // grrrrr....
    @Override
    public Void call() throws FlowException {
        SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
            @Suspendable
            @Override
            protected void checkTransaction(SignedTransaction stx) throws FlowException {

                 // * However, just because a transaction is contractually valid doesn’t mean we necessarily want to sign.                     
                 // * What if we don’t want to deal with the counterparty in question, or the value is too high,                               
                 // * or we’re not happy with the transaction’s structure? checkTransaction                                                    
                 // * allows us to define these additional checks. If any of these conditions are not met,                                     
                 // * we will not sign the transaction - even if the transaction and its signatures are contractually valid.                   
                 // * ----------                                                                                                               
                 // * For this hello-world cordapp, we will not implement any aditional checks.                                                
            }
        });
        //Stored the transaction into data base.                                                                                            
        subFlow(new ReceiveFinalityFlow(counterpartySession, signedTransaction.getId()));
        return null;
    }
}
*/

