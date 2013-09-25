/*
 * Copyright (c) 2013 GraphAware
 *
 * This file is part of GraphAware.
 *
 * GraphAware is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.graphaware.description.property;

import com.graphaware.description.MapUtils;
import com.graphaware.tx.executor.single.SimpleTransactionExecutor;
import com.graphaware.tx.executor.single.VoidReturningCallback;
import org.junit.Before;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.test.TestGraphDatabaseFactory;

public abstract class PropertiesDescriptionTest {

    protected PropertyContainer propertyContainer;

    @Before
    public void setUp() {
        GraphDatabaseService databaseService = new TestGraphDatabaseFactory().newImpermanentDatabase();

        new SimpleTransactionExecutor(databaseService).executeInTransaction(new VoidReturningCallback() {
            @Override
            protected void doInTx(GraphDatabaseService database) {
                Node root = database.getNodeById(0);
                root.setProperty("two", 2);
                root.setProperty("three", "3");
                root.setProperty("array", new int[]{4, 5});
            }
        });

        propertyContainer = databaseService.getNodeById(0);
    }

    protected LazyPropertiesDescription lazy() {
        return new LazyPropertiesDescription(propertyContainer);
    }

    protected LiteralPropertiesDescription literal() {
        return new LiteralPropertiesDescription(propertyContainer);
    }

    protected WildcardPropertiesDescription wildcard() {
        return new WildcardPropertiesDescription(propertyContainer);
    }

    protected LiteralPropertiesDescription literal(Object... stringOrPredicate) {
        return new LiteralPropertiesDescription(MapUtils.toMap(stringOrPredicate));
    }

    protected WildcardPropertiesDescription wildcard(Object... stringOrPredicate) {
        return new WildcardPropertiesDescription(MapUtils.toMap(stringOrPredicate));
    }
}
