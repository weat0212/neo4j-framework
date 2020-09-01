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

package com.graphaware.common.policy.inclusion.spel;

import com.graphaware.common.policy.inclusion.RelationshipPropertyInclusionPolicy;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Transaction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Unit test for {@link com.graphaware.common.policy.inclusion.spel.SpelRelationshipPropertyInclusionPolicy}.
 */
public class SpelRelationshipPropertyInclusionPolicyTest extends SpelInclusionPolicyTest {

    @Test
    public void shouldIncludeCorrectProps() {
        RelationshipPropertyInclusionPolicy policy1 = new SpelRelationshipPropertyInclusionPolicy("key != 'since'");
        RelationshipPropertyInclusionPolicy policy2 = new SpelRelationshipPropertyInclusionPolicy("relationship.isType('WORKS_FOR')");

        try (Transaction tx = database.beginTx()) {
            assertFalse(policy1.include("since", vojtaWorksFor(tx)));
            assertFalse(policy1.include("since", michalWorksFor(tx)));
            assertTrue(policy1.include("until", michalWorksFor(tx)));
            assertTrue(policy1.include("until", vojtaWorksFor(tx)));

            assertTrue(policy2.include("since", michalWorksFor(tx)));
            assertFalse(policy2.include("since", michalLivesIn(tx)));
            assertTrue(policy2.include("since", vojtaWorksFor(tx)));
            assertFalse(policy2.include("since", vojtaLivesIn(tx)));

            tx.commit();
        }
    }
}
