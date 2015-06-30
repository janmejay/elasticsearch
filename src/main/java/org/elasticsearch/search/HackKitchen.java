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
