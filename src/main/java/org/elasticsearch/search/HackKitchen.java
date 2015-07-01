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

import org.apache.lucene.queries.FilterClause;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.NumericRangeFilter;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.lucene.search.XBooleanFilter;
import org.elasticsearch.search.internal.SearchContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @understands unadultrated ugly hacks
 */
public class HackKitchen {
    private static final ESLogger logger = Loggers.getLogger("hackery");

    public void validateQueryAndFilter(SearchContext context) {
        try {
            List<XBooleanFilter> instances = findType(context, XBooleanFilter.class);
            for (XBooleanFilter instance : instances) {
                logger.debug("------------------------------");
                logger.debug("XBooleanFilter: {}", instance);
                List<NumericRangeFilter> nrfs = findType(instances, NumericRangeFilter.class);
            }
        } catch (Exception e) {
        }
    }

    private <T> void checkTypeAndAdd(Object o, Class<T> targetType, List<T> collector, int level) {
        if (targetType.isAssignableFrom(o.getClass())) {
            collector.add((T) o);
        }
    }

    private <T> List<T> findType(Object o, Class<T> type) {
        Map<Class, Set<Object>> seen = new HashMap<>();
        List<T> collector = new ArrayList<>();
        findTypeRec(o, seen, type, collector, 0);
        return collector;
    }

    private <T> void findTypeRec(Object o, Map<Class, Set<Object>> seen, Class<T> targetType, List<T> collector, int level) {
        if (o == null) return;
        Set<Object> seenSet = seen.get(o.getClass());
        if (seenSet == null) {
            seenSet = new HashSet<>();
            seen.put(o.getClass(), seenSet);
        }
        if (seenSet.contains(o)) return;
        seenSet.add(o);
        checkTypeAndAdd(o, targetType, collector, level);
        Field[] fields = o.getClass().getDeclaredFields();
        for (Field field : fields) {
            int mod = field.getModifiers();
            if (field.isSynthetic() || field.isEnumConstant() || (mod & Modifier.STATIC) != 0) {
                continue;
            }
            Class<?> t = field.getType();
            if (t == boolean.class
                    || t == byte.class
                    || t == char.class
                    || t == short.class
                    || t == int.class
                    || t == long.class
                    || t == float.class
                    || t == double.class
                    || t == void.class) {
                continue;
            }
            if (t.getCanonicalName().startsWith("java.lang.")) continue;

            field.setAccessible(true);
            Object o1 = null;
            try {
                o1 = field.get(o);
                if (List.class.isAssignableFrom(t)) {
                    for (Object o2 : (List) o1) {
                        findTypeRec(o2, seen, targetType, collector, level + 1);
                    }
                } else if (Map.class.isAssignableFrom(t)) {
                    Map o2 = (Map<Object, Object>) o1;
                    for (Object o3k : o2.keySet()) {
                        Object o3 = o2.get(o3k);
                        findTypeRec(o3k, seen, targetType, collector, level + 1);
                        findTypeRec(o3, seen, targetType, collector, level + 1);
                    }
                } else {
                    findTypeRec(o1, seen, targetType, collector, level + 1);
                }
            } catch (IllegalAccessException e) {
            } catch (NullPointerException e) {
            }
        }
    }
}
