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

package com.graphaware.common.json;

import com.graphaware.common.transform.NodeIdTransformer;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.util.Map;

/**
 * {@link JsonNode} with {@link String} custom ID.
 */
public class StringIdJsonNode extends JsonNode<String> {

    public StringIdJsonNode() {
    }

    public StringIdJsonNode(Node node, NodeIdTransformer<String> transformer) {
        super(node, transformer);
    }

    public StringIdJsonNode(Node node, String[] properties, NodeIdTransformer<String> transformer) {
        super(node, properties, transformer);
    }

    public StringIdJsonNode(String id, String[] labels, Map<String, Object> properties) {
        super(id, labels, properties);
    }

    @Override
    public Node produceEntity(Transaction database) {
        throw new UnsupportedOperationException("Please use produceEntity(GraphDatabaseService database, NodeIdTransformer<ID> transformer)");
    }
}