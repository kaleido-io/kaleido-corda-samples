/*
 * Copyright 2020 Kaleido
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 package io.kaleido.samples.state;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;

import io.kaleido.samples.contract.IOUContract;
import io.kaleido.samples.schema.IOUSchemaV1;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;

/**
 * The state object recording IOU agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 */
@BelongsToContract(IOUContract.class)
public class IOUState implements LinearState, QueryableState {
    private final Integer value;
    private final AbstractParty lender;
    private final AbstractParty borrower;
    private final UniqueIdentifier linearId;

    /**
     * @param value the value of the IOU.
     * @param lender the party issuing the IOU.
     * @param borrower the party receiving and approving the IOU.
     */
    public IOUState(Integer value,
                    AbstractParty lender,
                    AbstractParty borrower,
                    UniqueIdentifier linearId)
    {
        this.value = value;
        this.lender = lender;
        this.borrower = borrower;
        this.linearId = linearId;
    }

    public Integer getValue() { return value; }
    public AbstractParty getLender() { return lender; }
    public AbstractParty getBorrower() { return borrower; }
    @Override public UniqueIdentifier getLinearId() { return linearId; }
    @Override public List<AbstractParty> getParticipants() {
        return Arrays.asList(lender, borrower);
    }

    @Override public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof IOUSchemaV1) {
            return new IOUSchemaV1.PersistentIOU(
                    this.lender.getOwningKey().toString(),
                    this.borrower.getOwningKey().toString(),
                    this.value,
                    this.linearId.getId());
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @Override public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new IOUSchemaV1());
    }

    @Override
    public String toString() {
        return String.format("IOUState(value=%s, lender=%s, borrower=%s, linearId=%s)", value, lender, borrower, linearId);
    }
}