package com.foo.examples.contracts;

import java.util.Arrays;
import java.util.List;

//import com.google.common.collect.ImmutableList;

import net.corda.core.serialization.CordaSerializable;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;

/**
 * The state object recording IOU agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 */

//public class state1 implements LinearState, QueryableState {

// Do not omit this @BelongsToContract annotation!
@BelongsToContract(com.foo.examples.contracts.MyContract.class)
public class MyState1 implements ContractState {
    private Integer value;
    private String msg;
    private Party sender;
    private Party receiver;

    private int x;

    //private final UniqueIdentifier linearId;

    /**
     * @param value the value of the IOU.
     * @param sender the party issuing the IOU.
     * @param receiver the party receiving and approving the IOU.
     */
    public MyState1(Integer value,
		    String msg,
                    Party sender,
                    Party receiver)
    {
        this.value = value;
	this.msg = msg;
        this.sender = sender;
        this.receiver = receiver;
        //this.linearId = linearId;
    }

    public Integer getValue() { return value; }
    public String getMsg() { return msg; }
    public Party getSender() { return sender; }
    public Party getReceiver() { return receiver; }

    //@Override public UniqueIdentifier getLinearId() { return linearId; }

    @Override public List<AbstractParty> getParticipants() {
        return Arrays.asList(sender, receiver);
    }


}
