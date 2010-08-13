/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.openjpa.jdbc.sql;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.kernel.exps.FilterValue;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.PrimaryKey;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.schema.Unique;

/**
 * Dictionary for SolidDB database.
 */
public class SolidDBDictionary
    extends DBDictionary {

    /**
     * Sets whether tables are to be located in-memory or on disk.
     * Creating in-memory tables should append "STORE MEMORY" to the 
     * "CREATE TABLE" statement. Creating disk-based tables should 
     * append "STORE DISK". Since cursor hold over commit can not apply 
     * to M-tables (which will cause SOLID Table Error 13187: The cursor 
     * cannot continue accessing M-tables after the transaction has committed 
     * or aborted. The statement must be re-executed.), the default is 
     * STORE DISK.
     * 
     */
    public boolean storeIsMemory = false;

    public SolidDBDictionary() {
        platform = "SolidDB";
        bitTypeName = "TINYINT";
        blobTypeName = "LONG VARBINARY";
        booleanTypeName = "TINYINT";
        clobTypeName = "LONG VARCHAR";
        doubleTypeName = "DOUBLE PRECISION";
        
        allowsAliasInBulkClause = false;
        useGetStringForClobs = true;
        useSetStringForClobs = true;
        supportsDeferredConstraints = false;
        supportsNullUniqueColumn = false;
        
        concatenateFunction = "CONCAT({0},{1})";
        trimLeadingFunction = "LTRIM({0})";
        trimTrailingFunction = "RTRIM({0})";
        trimBothFunction = "TRIM({0})";
        
        reservedWordSet.addAll(Arrays.asList(new String[]{
            "BIGINT", "BINARY", "DATE", "TIME", 
            "TINYINT", "VARBINARY"
        }));
    }

    @Override
    public String[] getCreateTableSQL(Table table) {
        StringBuilder buf = new StringBuilder();
        buf.append("CREATE TABLE ").append(getFullName(table, false)).append(" (");
        Column[] cols = table.getColumns();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0)
                buf.append(", ");
            buf.append(getDeclareColumnSQL(cols[i], false));
        }

        PrimaryKey pk = table.getPrimaryKey();
        String pkStr;
        if (pk != null) {
            pkStr = getPrimaryKeyConstraintSQL(pk);
            if (!StringUtils.isEmpty(pkStr))
                buf.append(", ").append(pkStr);
        }

        Unique[] unqs = table.getUniques();
        String unqStr;
        for (int i = 0; i < unqs.length; i++) {
            unqStr = getUniqueConstraintSQL(unqs[i]);
            if (unqStr != null)
                buf.append(", ").append(unqStr);
        }

        buf.append(") STORE ");
        if (storeIsMemory)
            buf.append("MEMORY");
        else
            buf.append("DISK");
        return new String[]{ buf.toString() };
    }

    @Override
    public String convertSchemaCase(DBIdentifier objectName) {
        if (objectName != null && objectName.getName() == null)
            return "";
        return super.convertSchemaCase(objectName);
    }
    
    @Override
    public void substring(SQLBuffer buf, FilterValue str, FilterValue start,
            FilterValue end) {
        if (end != null) {
            super.substring(buf, str, start, end);
        } else {
            buf.append(substringFunctionName).append("(");
            str.appendTo(buf);
            buf.append(", ");
            if (start.getValue() instanceof Number) {
                long startLong = toLong(start);
                buf.append(Long.toString(startLong + 1));
            } else {
                buf.append("(");
                start.appendTo(buf);
                buf.append(" + 1)");
            }
            buf.append(", ");
            if (start.getValue() instanceof Number) {
                long startLong = toLong(start);
                long endLong = Integer.MAX_VALUE; //2G
                buf.append(Long.toString(endLong - startLong));
            } else {
                buf.append(Integer.toString(Integer.MAX_VALUE));
                buf.append(" - (");
                start.appendTo(buf);
                buf.append(")");
            }
            buf.append(")");
        }
    }
    
    @Override
    public void indexOf(SQLBuffer buf, FilterValue str, FilterValue find,
        FilterValue start) {
        buf.append("(POSITION((");
        find.appendTo(buf);
        buf.append(") IN (");

        if (start != null)
            substring(buf, str, start, null);
        else
            str.appendTo(buf);
        
        buf.append(")) - 1");

        if (start != null) {
            buf.append(" + ");
            start.appendTo(buf);
        }
        buf.append(")");
    }
    
    public String toUpper(String internalName, boolean force) {
        return internalName.toUpperCase();
    }    
}