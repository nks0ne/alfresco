/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.audit.generator;

import java.io.Serializable;

/**
 * Interface for Audit data value generators.These are used to produce auditable data values
 * extract auditable values from nothing; typically these values are derived from the system
 * state or from the thread context.
 * <p/>
 * <tt>null</tt> values will be ignored by the audit framework.
 * 
 * @author Derek Hulley
 * @since 3.2
 */
public interface DataGenerator
{
    /**
     * Get the data generated by the instance.
     * 
     * @return              Returns the generated data or <tt>null</tt> if no data could be generated
     * @throws Throwable    All exceptions are handled by the framework
     */
    public Serializable getData() throws Throwable;
}
