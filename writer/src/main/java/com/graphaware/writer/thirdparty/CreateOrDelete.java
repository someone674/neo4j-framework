/*
 * Copyright (c) 2013-2018 GraphAware
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

package com.graphaware.writer.thirdparty;

import com.graphaware.common.representation.DetachedEntity;
import org.neo4j.graphdb.Entity;

/**
 * {@link BaseWriteOperation} representing a create or delete operation.
 *
 * @param <R> type of the details object.
 * @param <T> type of the {@link Entity} that the operation was performed on.
 */
public abstract class CreateOrDelete<ID, R extends DetachedEntity<ID, T>, T extends Entity> extends BaseWriteOperation<R> {

    /**
     * Create the operation.
     *
     * @param details details about the operation. Must not be <code>null</code>.
     */
    protected CreateOrDelete(R details) {
        super(details);
    }
}
