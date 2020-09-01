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

package com.graphaware.tx.executor.batch;

import com.graphaware.tx.executor.input.AllNodes;
import com.graphaware.tx.executor.input.AllNodesWithLabel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.graphaware.common.util.IterableUtils.countNodes;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for {@link com.graphaware.tx.executor.batch.IterableInputBatchTransactionExecutor}.
 */
public class IterableInputBatchTransactionExecutorTest {

    private Neo4j controls;
    protected GraphDatabaseService database;

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
    public void nodesShouldBeCreatedFromListOfNames() {
        List<String> nodeNames = Arrays.asList("Name1", "Name2", "Name3");

        BatchTransactionExecutor executor = new IterableInputBatchTransactionExecutor<>(database, 2, nodeNames, new UnitOfWork<String>() {
            @Override
            public void execute(Transaction tx, String nodeName, int batchNumber, int stepNumber) {
                Node node = tx.createNode();
                node.setProperty("name", nodeName + batchNumber + stepNumber);
            }
        });

        executor.execute();

        try (Transaction tx = database.beginTx()) {
            assertEquals(3, countNodes(tx));
            assertEquals("Name111", tx.getNodeById(0).getProperty("name"));
            assertEquals("Name212", tx.getNodeById(1).getProperty("name"));
            assertEquals("Name321", tx.getNodeById(20).getProperty("name"));
        }
    }

    @Test
    public void iterableAcquiredInTransactionShouldBeProcessed() {
        try (Transaction tx = database.beginTx()) {
            for (int i = 0; i < 100; i++) {
                tx.createNode();
            }
            tx.commit();
        }

        BatchTransactionExecutor executor = new IterableInputBatchTransactionExecutor<>(database, 10,
                new AllNodes(database, 10),
                new UnitOfWork<Node>() {
                    @Override
                    public void execute(Transaction tx, Node node, int batchNumber, int stepNumber) {
                        node.setProperty("name", "Name" + batchNumber + stepNumber);
                    }
                }
        );

        executor.execute();

        try (Transaction tx = database.beginTx()) {
            assertEquals("Name11", tx.getNodeById(0).getProperty("name"));
            assertEquals("Name12", tx.getNodeById(1).getProperty("name"));
            assertEquals("Name13", tx.getNodeById(2).getProperty("name"));
            assertEquals("Name108", tx.getNodeById(97).getProperty("name"));
            assertEquals("Name109", tx.getNodeById(98).getProperty("name"));
            assertEquals("Name1010", tx.getNodeById(99).getProperty("name"));
        }
    }

    @Test
    public void iterorAcquiredInTransactionShouldBeProcessed() {
        try (Transaction tx = database.beginTx()) {
            for (int i = 0; i < 100; i++) {
                tx.createNode(Label.label("Test"));
            }
            tx.commit();
        }

        BatchTransactionExecutor executor = new IterableInputBatchTransactionExecutor<>(database, 10,
                new AllNodesWithLabel(database, 10, Label.label("Test")),
                new UnitOfWork<Node>() {
                    @Override
                    public void execute(Transaction tx, Node node, int batchNumber, int stepNumber) {
                        node.setProperty("name", "Name" + batchNumber + stepNumber);
                    }
                }
        );

        executor.execute();

        try (Transaction tx = database.beginTx()) {
            assertEquals("Name11", tx.getNodeById(0).getProperty("name"));
            assertEquals("Name12", tx.getNodeById(1).getProperty("name"));
            assertEquals("Name13", tx.getNodeById(2).getProperty("name"));
            assertEquals("Name108", tx.getNodeById(97).getProperty("name"));
            assertEquals("Name109", tx.getNodeById(98).getProperty("name"));
            assertEquals("Name1010", tx.getNodeById(99).getProperty("name"));
        }
    }

    @Test
    public void bugTest() {
        final Label label = Label.label("TEST");

        try (Transaction tx = database.beginTx()) {
            tx.createNode(label);
            tx.createNode(label);
            tx.commit();
        }

        final AtomicInteger count = new AtomicInteger(0);

        new IterableInputBatchTransactionExecutor<>(database, 100, new AllNodesWithLabel(database, 100, label),
                new UnitOfWork<Node>() {
                    @Override
                    public void execute(Transaction tx, Node input, int batchNumber, int stepNumber) {
                        count.incrementAndGet();
                    }
                }
        ).execute();

        assertEquals(2, count.get());
    }
}
