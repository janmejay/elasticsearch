/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.search;

import org.apache.lucene.search.NumericRangeFilter;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.search.internal.SearchContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @understands unadultrated ugly hacks
 */
public class HackKitchen {
    private static final ESLogger logger = Loggers.getLogger("hackery");

    public void validateQueryAndFilter(SearchContext context) {
        try {
            Map<Class, Set<Object>> map = new HashMap<>();
            walkAndCall(context, map);
        } catch (Exception e) {
        }
    }

    private void doValidate(Object o) {
        if (o instanceof NumericRangeFilter) {
            logger.error("FOUND A FILTER: " + o);
        }
    }

    private void walkAndCall(Object o, Map<Class, Set<Object>> seen) {
        //if (true) return;
        Set<Object> seenSet = seen.get(o.getClass());
        if (seenSet == null) {
            seenSet = new HashSet<>();
            seen.put(o.getClass(), seenSet);
        }
        if (seenSet.contains(o)) return;
        if (o == null) return;
        seenSet.add(o);
        doValidate(o);
        Field[] fields = o.getClass().getDeclaredFields();
        for (Field field : fields) {
            int mod = field.getModifiers();
            if (field.isSynthetic() || field.isEnumConstant() || (mod & Modifier.STATIC) != 0) {
                continue;
            }
            if (field.getType() == boolean.class
                    || field.getType() == byte.class
                    || field.getType() == char.class
                    || field.getType() == short.class
                    || field.getType() == int.class
                    || field.getType() == long.class
                    || field.getType() == float.class
                    || field.getType() == double.class
                    || field.getType() == void.class) {
                continue;
            }
            field.setAccessible(true);
            Object o1 = null;
            try {
                o1 = field.get(o);
                walkAndCall(o1, seen);
            } catch (IllegalAccessException e) {
            }
        }
    }
}
