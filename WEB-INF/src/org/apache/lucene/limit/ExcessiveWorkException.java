package org.apache.lucene.limit;

/**
 * Copyright 2005 The Apache Software Foundation
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

import java.io.IOException;

/**
 * Thrown when the maximum amount of work for a query has been exceeded. This 
 * is derived from IOException instead of just Exception because it may be 
 * thrown inside Lucene methods that are only prepared to handle IOExceptions.
 * 
 * @author Martin Haye
 * @version $Id: ExcessiveWorkException.java,v 1.1 2005-02-08 23:19:34 mhaye Exp $
 */
public class ExcessiveWorkException extends IOException
{
    public ExcessiveWorkException()
    {
        super( "The query references too many potential matches. " +
               "Making it more specific would help." );
    }
} // ExcessiveWorkException
