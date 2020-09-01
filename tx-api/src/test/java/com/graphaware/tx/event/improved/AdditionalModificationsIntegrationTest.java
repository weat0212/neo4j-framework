/*
 * Copyright (c) 2013-2020 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.graphaware.tx.event.improved;

import com.graphaware.test.data.CypherPopulator;
import com.graphaware.test.data.DatabasePopulator;
import com.graphaware.test.integration.DatabaseIntegrationTest;
import com.graphaware.tx.event.improved.api.ImprovedTransactionData;
import com.graphaware.tx.event.improved.api.LazyTransactionData;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventListenerAdapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AdditionalModificationsIntegrationTest extends DatabaseIntegrationTest {

    @Override
    protected DatabasePopulator databasePopulator() {
        return new CypherPopulator() {
            @Override
            protected String[] statementGroups() {
                return new String[]{"CREATE " +
                        "(m:Person {name:'Michal'})," +
                        "(l:City {name:'London'})," +
                        "(p:City {name:'Prague'})," +
                        "(m)-[:LIVES_IN]->(p)," +
                        "(m)-[:LIVES_IN]->(l)" +
                        ""};
            }
        };
    }

    @Test
    public void additionalCreatesShouldNotImpactTxData() {
        getNeo4j().databaseManagementService().registerTransactionEventListener(getNeo4j().defaultDatabaseService().databaseName(), new TransactionEventListenerAdapter<Void>() {

            @Override
            public Void beforeCommit(TransactionData data, Transaction transaction, GraphDatabaseService databaseService) throws Exception {
                ImprovedTransactionData transactionData = new LazyTransactionData(data, transaction);

                assertEquals(1, transactionData.getAllCreatedNodes().size());

                for (Node node : transactionData.getAllCreatedNodes()) {
                    Node unknownCity = transaction.createNode(Label.label("City"));
                    node.createRelationshipTo(unknownCity, RelationshipType.withName("LIVES_IN"));
                }

                assertEquals(1, transactionData.getAllCreatedNodes().size());

                return null;
            }
        });
    }


}
