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

package com.graphaware.runtime.module.thirdparty;

import com.graphaware.common.representation.GraphDetachedNode;
import com.graphaware.common.representation.GraphDetachedRelationship;
import com.graphaware.runtime.GraphAwareRuntime;
import com.graphaware.runtime.GraphAwareRuntimeFactory;
import com.graphaware.writer.thirdparty.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.neo4j.internal.helpers.collection.MapUtil;

import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThirdPartyIntegrationModuleTest {

    private Neo4j controls;
    private GraphDatabaseService database;

    @BeforeEach
    public void setUp() {
        controls = Neo4jBuilders.newInProcessBuilder().build();
        database = controls.defaultDatabaseService();
    }

    @AfterEach
    public void tearDown() {
        controls.close();
    }

    @Test
    public void modificationsShouldBeCorrectlyBuilt() {
        TestThirdPartyModule module = new TestThirdPartyModule("test");

        database.executeTransactionally("CREATE (p:Person {name:'Michal', age:30})-[:WORKS_FOR {since:2013, role:'MD'}]->(c:Company {name:'GraphAware', est: 2013})");
        database.executeTransactionally("MATCH (ga:Company {name:'GraphAware'}) CREATE (p:Person {name:'Adam'})-[:WORKS_FOR {since:2014}]->(ga)");

        long danielaId, michalId, adamId, gaId;
        try (Transaction tx = database.beginTx()) {
            michalId = tx.findNode(Label.label("Person"), "name", "Michal").getId();
            adamId = tx.findNode(Label.label("Person"), "name", "Adam").getId();
            gaId = tx.findNode(Label.label("Company"), "name", "GraphAware").getId();

            tx.commit();
        }

        GraphAwareRuntime runtime = GraphAwareRuntimeFactory.createRuntime(controls.databaseManagementService(), database);
        runtime.registerModule(module);
        runtime.start();
        runtime.waitUntilStarted();

        try (Transaction tx = database.beginTx()) {
            database.executeTransactionally("MATCH (ga:Company {name:'GraphAware'}) CREATE (p:Person {name:'Daniela'})-[:WORKS_FOR]->(ga)");
            database.executeTransactionally("MATCH (p:Person {name:'Michal'}) SET p.age=31");
            database.executeTransactionally("MATCH (p:Person {name:'Adam'})-[r]-() DELETE p,r");
            database.executeTransactionally("MATCH (p:Person {name:'Michal'})-[r:WORKS_FOR]->() REMOVE r.role");
            tx.commit();
        }

        try (Transaction tx = database.beginTx()) {
            danielaId = tx.findNode(Label.label("Person"), "name", "Daniela").getId();
            tx.commit();
        }

        Collection<WriteOperation<?>> writeOperations = module.getWriteOperations();
        assertEquals(6, writeOperations.size());

        assertTrue(writeOperations.contains(new NodeCreated<>(
                new GraphDetachedNode(danielaId, new String[]{"Person"}, MapUtil.map("name", "Daniela")))));

        assertTrue(writeOperations.contains(new NodeUpdated<>(
                new GraphDetachedNode(michalId, new String[]{"Person"}, MapUtil.map("name", "Michal", "age", 30L)),
                new GraphDetachedNode(michalId, new String[]{"Person"}, MapUtil.map("name", "Michal", "age", 31L)))));

        assertTrue(writeOperations.contains(new NodeDeleted<>(
                new GraphDetachedNode(adamId, new String[]{"Person"}, MapUtil.map("name", "Adam")))));

        assertTrue(writeOperations.contains(new RelationshipCreated<>(
                new GraphDetachedRelationship(21L, danielaId, gaId, "WORKS_FOR", Collections.<String, Object>emptyMap())
        )));

        assertTrue(writeOperations.contains(new RelationshipUpdated<>(
                new GraphDetachedRelationship(0L, michalId, gaId, "WORKS_FOR", MapUtil.map("since", 2013L, "role", "MD")),
                new GraphDetachedRelationship(0L, michalId, gaId, "WORKS_FOR", MapUtil.map("since", 2013L)))));

        assertTrue(writeOperations.contains(new RelationshipDeleted<>(
                new GraphDetachedRelationship(20L, adamId, gaId, "WORKS_FOR", MapUtil.map("since", 2014L))
        )));

        for (WriteOperation<?> operation : writeOperations) {
            if (operation.getDetails() instanceof GraphDetachedRelationship) {
                assertTrue(((GraphDetachedRelationship) operation.getDetails()).getStartNode() instanceof GraphDetachedNode);
                assertTrue(((GraphDetachedRelationship) operation.getDetails()).getEndNode() instanceof GraphDetachedNode);
            }
        }

        controls.close();
    }
}
