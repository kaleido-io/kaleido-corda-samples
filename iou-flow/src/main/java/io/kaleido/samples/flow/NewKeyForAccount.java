package io.kaleido.samples.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.flows.StartableByService;
import net.corda.core.identity.PartyAndCertificate;

import java.security.PublicKey;
import java.util.*;

@StartableByRPC
@StartableByService
public class NewKeyForAccount extends FlowLogic<PublicKey>{

    private final UUID accountID;

    public NewKeyForAccount(UUID accountID) {
        this.accountID = accountID;
    }

    @Override
    @Suspendable
    public PublicKey call() throws FlowException {
        return getServiceHub().getKeyManagementService().freshKey(accountID);
    }
}