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
import org.apache.lucene.search.NumericRangeQuery;
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
    private static final String OBJECT_CLASS_NAME = Object.class.getCanonicalName();

    public void validateQueryAndFilter(SearchContext context) {
        try {
            //Sanitization 1: allow only upto 6 hours of viewing window
            List<XBooleanFilter> instances = findType(context, XBooleanFilter.class);
            List<NumericRangeQuery> nrqs = findType(instances, NumericRangeQuery.class);
            for (NumericRangeQuery nrq : nrqs) {
                String field = nrq.getField();
                if ("@timestamp".equals(field)) {
                    Long min = (Long) nrq.getMin();
                    Long max = (Long) nrq.getMax();
                    long diffMs = max - min;
                    int maxAllowedWidth = 6 * 60 * 60 * 1000;
                    if (diffMs > maxAllowedWidth) {
                        min = max - maxAllowedWidth;
                        logger.warn("Reducing @timestamp filter from {} hrs to {} hrs.", ((double) diffMs) / 1000 / 60 / 60, (max - min) / 1000 / 60 / 60);
                        setValue(nrq, "min", min);
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    private void setValue(Object o, String name, Object value) {
        Class<?> type = o.getClass();
        do {
            try {
                Field field = type.getField(name);
                field.setAccessible(true);
                field.set(o, value);
                return;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } while (OBJECT_CLASS_NAME.equals(type.getCanonicalName()));
    }

    private <T> void checkTypeAndAdd(Object o, Class<T> targetType, List<T> collector, int level) {
//        StringBuilder b = new StringBuilder();
//        for (int i = 0; i < level; i++) {
//            b.append(">> ");
//        }
//        logger.debug("{}{}", b.toString(), o.getClass());
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
        Class<?> oType = o.getClass();
        Set<Object> seenSet = seen.get(oType);
        if (seenSet == null) {
            seenSet = new HashSet<>();
            seen.put(oType, seenSet);
        }
        if (seenSet.contains(o)) return;
        seenSet.add(o);
        checkTypeAndAdd(o, targetType, collector, level);
        if (List.class.isAssignableFrom(oType)) {
            for (Object o1 : (List) o) {
                findTypeRec(o1, seen, targetType, collector, level + 1);
            }
        } else if (Map.class.isAssignableFrom(oType)) {
            Map o1 = (Map<Object, Object>) o;
            for (Object o2k : o1.keySet()) {
                Object o2v = o1.get(o2k);
                findTypeRec(o2k, seen, targetType, collector, level + 1);
                findTypeRec(o2v, seen, targetType, collector, level + 1);
            }
        } else {
            do {
                Field[] fields = oType.getDeclaredFields();
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
                        findTypeRec(o1, seen, targetType, collector, level + 1);
                    } catch (IllegalAccessException e) {
                    } catch (NullPointerException e) {
                    }
                }
                oType = oType.getSuperclass();
            } while (!oType.getCanonicalName().startsWith("java.lang."));
        }
    }
}
