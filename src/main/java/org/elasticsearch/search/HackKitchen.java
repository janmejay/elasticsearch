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
import java.util.HashSet;
import java.util.Set;

/**
 * @understands unadultrated ugly hacks
 */
public class HackKitchen {
    private static final ESLogger logger = Loggers.getLogger("hackery");

    public void validateQueryAndFilter(SearchContext context) {
        try {
            Set<Object> set = new HashSet<>();
            walkAndCall(context, set);
        } catch (Exception e) {

        }
    }

    private void doValidate(Object o) {
        if (o instanceof NumericRangeFilter) {
            logger.error("FOUND A FILTER: " + o);
        }
    }

    private void walkAndCall(Object o, Set<Object> set) {
        if (true) return;
        if (set.contains(o)) return;
        if (o == null) return;
        set.add(o);
        doValidate(o);
        Field[] fields = o.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object o1 = field.get(o);
                walkAndCall(o1, set);
            } catch (IllegalAccessException e) {
            }
        }
    }


}
