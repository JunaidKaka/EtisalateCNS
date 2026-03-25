package com.etisalat.repos;

import com.etisalat.dto.FilterDescriptor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class GenericSpecifications<T> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Specification<T> buildSpecification(
            Class<T> entityClass,
            String filtersJson,
            String globalSearch,
            List<String> allowedFields,
            List<String> globalFields) {

        Map<String, FilterDescriptor> filters = parseFilters(filtersJson);

        final Map<String, FilterDescriptor> finalFilters = filters;
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Per-column filters
            for (Map.Entry<String, FilterDescriptor> entry : finalFilters.entrySet()) {
                String rawField = entry.getKey();
                FilterDescriptor fd = entry.getValue();

                if (fd == null || fd.getValue() == null) continue; // nothing to filter

                // security: only allow whitelisted fields (if provided)
                if (allowedFields != null && !allowedFields.isEmpty() && !allowedFields.contains(rawField)) {
                    continue; // skip non-whitelisted field
                }

                Path<?> path = getPath(root, rawField);
                if (path == null) continue;

                // For collections or lists, we sometimes need to convert each element
                Object converted = (fd.getValue() instanceof Collection)
                        ? convertCollectionValues(path.getJavaType(), (Collection<?>) fd.getValue())
                        : convertValue(path.getJavaType(), fd.getValue());

                Predicate p = buildPredicate(cb, path, fd.getMatchMode(), converted);
                if (p != null) predicates.add(p);
            }

            // Global search (q) — OR across configured globalFields (only if q present)
            if (globalSearch != null && !globalSearch.isBlank() && globalFields != null && !globalFields.isEmpty()) {
                String like = "%" + globalSearch.trim().toLowerCase() + "%";
                List<Predicate> ors = new ArrayList<>();
                for (String f : globalFields) {
                    // whitelist again
                    if (allowedFields != null && !allowedFields.isEmpty() && !allowedFields.contains(f)) continue;
                    Path<?> path = getPath(root, f);
                    if (path == null) continue;
                    Expression<String> exp = cb.lower(path.as(String.class));
                    ors.add(cb.like(exp, like));
                }
                if (!ors.isEmpty()) {
                    predicates.add(cb.or(ors.toArray(new Predicate[0])));
                }
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Robust filter parsing: supports array-of-objects (PrimeNG default), array-of-values,
     * object {value,matchMode}, and direct value nodes.
     */
    private Map<String, FilterDescriptor> parseFilters(String filtersJson) {
        Map<String, FilterDescriptor> filters = new HashMap<>();
        if (filtersJson == null || filtersJson.isBlank()) return filters;

        try {
            JsonNode root = objectMapper.readTree(filtersJson);
            if (!root.isObject()) return filters;

            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> fn = fields.next();
                String key = fn.getKey();
                JsonNode node = fn.getValue();

                FilterDescriptor fd = new FilterDescriptor();

                if (node == null || node.isNull()) {
                    // skip
                    continue;

                } else if (node.isArray()) {
                    // Distinguish: array of primitives e.g. ["A","B"]  OR array of objects e.g. [{value:...,matchMode:...}]
                    if (node.size() == 0) {
                        continue;
                    }

                    JsonNode first = node.get(0);
                    if (first.isValueNode()) {
                        // array of primitives -> treat as IN
                        List<Object> values = objectMapper.convertValue(node, new TypeReference<List<Object>>() {
                        });
                        fd.setValue(values);
                        fd.setMatchMode("in");
                    } else if (first.isObject()) {
                        // array of constraint objects (PrimeNG often sends this)
                        // strategy: prefer the first element that has a non-null value.
                        List<Object> collected = new ArrayList<>();
                        String chosenMatchMode = null;
                        String operator = null;

                        for (JsonNode el : node) {
                            if (el == null || el.isNull()) continue;
                            JsonNode valueNode = el.get("value");
                            JsonNode mmNode = el.get("matchMode");
                            JsonNode opNode = el.get("operator");

                            if (opNode != null && !opNode.isNull()) operator = opNode.asText();

                            if (valueNode != null && !valueNode.isNull()) {
                                // if value is array -> convert to list, else single value
                                if (valueNode.isArray()) {
                                    List<Object> vs = objectMapper.convertValue(valueNode, new TypeReference<List<Object>>() {
                                    });
                                    collected.addAll(vs);
                                } else {
                                    Object v = objectMapper.convertValue(valueNode, Object.class);
                                    collected.add(v);
                                }
                                if (chosenMatchMode == null && mmNode != null && !mmNode.isNull()) {
                                    chosenMatchMode = mmNode.asText();
                                }
                            }
                        }

                        if (collected.isEmpty()) {
                            // nothing to filter (all constraints had null values)
                            continue;
                        } else if (collected.size() == 1) {
                            fd.setValue(collected.get(0));
                            fd.setMatchMode(chosenMatchMode != null ? chosenMatchMode : "equals");
                        } else {
                            // multiple values found.
                            // Best effort: if client actually intended multi-value selection, treat as IN.
                            // If client intended multiple constraints with AND/OR, this basic normalizer will collapse them to IN.
                            fd.setValue(collected);
                            // prefer equals/in if matchMode suggests equality, otherwise fallback to 'in'
                            if (chosenMatchMode != null && chosenMatchMode.equalsIgnoreCase("equals")) {
                                fd.setMatchMode("in");
                            } else {
                                fd.setMatchMode("in");
                            }
                        }
                    } else {
                        // unknown array element type -> skip
                        continue;
                    }

                } else if (node.isObject()) {
                    // e.g. { "value": ..., "matchMode": "contains" } (single object)
                    JsonNode valueNode = node.get("value");
                    JsonNode mmNode = node.get("matchMode");

                    String mm = mmNode != null && !mmNode.isNull() ? mmNode.asText() : null;

                    if (valueNode != null && !valueNode.isNull()) {
                        if (valueNode.isArray()) {
                            List<Object> values = objectMapper.convertValue(valueNode, new TypeReference<List<Object>>() {
                            });
                            fd.setValue(values);
                        } else {
                            Object v = objectMapper.convertValue(valueNode, Object.class);
                            fd.setValue(v);
                        }
                        fd.setMatchMode(mm);
                    } else {
                        // object without a 'value' field (rare) -> attempt to look for constraints inside
                        if (node.has("constraints")) {
                            JsonNode constraints = node.get("constraints");
                            if (constraints.isArray() && constraints.size() > 0) {
                                JsonNode first = constraints.get(0);
                                JsonNode v = first.get("value");
                                if (v != null && !v.isNull()) {
                                    Object vv = objectMapper.convertValue(v, Object.class);
                                    fd.setValue(vv);
                                    JsonNode mm2 = first.get("matchMode");
                                    fd.setMatchMode(mm2 != null && !mm2.isNull() ? mm2.asText() : mm);
                                }
                            }
                        }
                    }

                } else if (node.isValueNode()) {
                    // e.g. "notid":"abc"  -> equals
                    Object v = objectMapper.convertValue(node, Object.class);
                    fd.setValue(v);
                    fd.setMatchMode("equals");

                } else {
                    // unknown shape -> skip
                    continue;
                }

                filters.put(key, fd);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return filters;
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate buildPredicate(CriteriaBuilder cb, Path<?> path, String matchMode, Object value) {
        if (matchMode == null) matchMode = "contains"; // default

        String mm = matchMode.toLowerCase(Locale.ROOT);
        Class<?> javaType = path.getJavaType();

        switch (mm) {
            case "equals":
                return cb.equal(path, value);
            case "contains":
                return cb.like(cb.lower(path.as(String.class)), "%" + String.valueOf(value).toLowerCase() + "%");
            case "notcontains":
                return cb.notLike(cb.lower(path.as(String.class)), "%" + String.valueOf(value).toLowerCase() + "%");
            case "notEquals":
                return cb.notEqual(path, value);
            case "startswith":
            case "startsWith":
                return cb.like(cb.lower(path.as(String.class)), String.valueOf(value).toLowerCase() + "%");
            case "endswith":
            case "endsWith":
                return cb.like(cb.lower(path.as(String.class)), "%" + String.valueOf(value).toLowerCase());
            case "in":
                if (value instanceof Collection) {
                    Collection<?> coll = (Collection<?>) value;
                    return path.in(coll);
                } else if (value instanceof Object[]) {
                    return path.in(Arrays.asList((Object[]) value));
                } else if (value instanceof String) {
                    List<String> parts = Arrays.stream(((String) value).split(","))
                            .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                    return path.in(parts);
                }
                break;
            case "between":
                if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    if (list.size() >= 2) {
                        Object c1 = convertValue(javaType, list.get(0));
                        Object c2 = convertValue(javaType, list.get(1));
                        if (Comparable.class.isAssignableFrom(javaType)) {
                            return cb.between((Expression) path.as((Class) javaType), (Comparable) c1, (Comparable) c2);
                        }
                    }
                }
                break;
            case "lt":
            case "lessthan":
                if (Comparable.class.isAssignableFrom(javaType)) {
                    return cb.lessThan((Expression) path.as((Class) javaType), (Comparable) value);
                }
                break;
            case "lte":
            case "lessthanorequal":
                if (Comparable.class.isAssignableFrom(javaType)) {
                    return cb.lessThanOrEqualTo((Expression) path.as((Class) javaType), (Comparable) value);
                }
                break;
            case "gt":
            case "greaterthan":
                if (Comparable.class.isAssignableFrom(javaType)) {
                    return cb.greaterThan((Expression) path.as((Class) javaType), (Comparable) value);
                }
                break;
            case "gte":
            case "datebefore":   // client sends dateBefore
                if (Comparable.class.isAssignableFrom(javaType)) {
                    return cb.lessThanOrEqualTo((Expression) path.as((Class) javaType), (Comparable) value);
                }
                break;

            case "dateafter":    // client sends dateAfter
                if (Comparable.class.isAssignableFrom(javaType)) {
                    return cb.greaterThanOrEqualTo((Expression) path.as((Class) javaType), (Comparable) value);
                }

            case "dateis": {
                if (value == null) break;

                if (value instanceof Instant) {

                    Instant inst = (Instant) value;

                    // convert to LocalDate (UTC or your business zone)
                    LocalDate date = inst.atZone(ZoneOffset.UTC).toLocalDate();

                    Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
                    Instant end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1);

                    return cb.between(path.as(Instant.class), start, end);
                }

                return cb.equal(path, value);
            }

            case "dateisnot": {
                if (value == null) break;

                if (value instanceof Instant) {

                    Instant inst = (Instant) value;

                    LocalDate date = inst.atZone(ZoneOffset.UTC).toLocalDate();

                    Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
                    Instant end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1);

                    return cb.not(cb.between(path.as(Instant.class), start, end));
                }

                return cb.notEqual(path, value);
            }

            case "greaterthanorequal":
                if (Comparable.class.isAssignableFrom(javaType)) {
                    return cb.greaterThanOrEqualTo((Expression) path.as((Class) javaType), (Comparable) value);
                }
                break;
            default:
                return cb.like(cb.lower(path.as(String.class)), "%" + String.valueOf(value).toLowerCase() + "%");
        }

        return null;
    }

    private Collection<?> convertCollectionValues(Class<?> targetType, Collection<?> rawCollection) {
        if (rawCollection == null) return null;
        return rawCollection.stream()
                .map(v -> convertValue(targetType, v))
                .collect(Collectors.toList());
    }

    private Path<?> getPath(Root<?> root, String field) {
        if (field.contains(".")) {
            String[] parts = field.split("\\.");
            Path<?> p = root;
            for (String part : parts) {
                if (p == null) return null;
                p = p.get(part);
            }
            return p;
        } else {
            try {
                return root.get(field);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    private Object convertValue(Class<?> targetType, Object raw) {
        if (raw == null) return null;
        if (targetType == null) return raw;

        if (targetType.isInstance(raw)) return raw;

        String s = String.valueOf(raw);
        try {
            if (targetType == String.class) return s;

            if (Enum.class.isAssignableFrom(targetType)) {
                @SuppressWarnings({"rawtypes", "unchecked"})
                Class<? extends Enum> enumClass = (Class<? extends Enum>) targetType;
                return Enum.valueOf(enumClass, s);
            }

            if (targetType == Integer.class || targetType == int.class) return Integer.valueOf(s);
            if (targetType == Long.class || targetType == long.class) return Long.valueOf(s);
            if (targetType == Double.class || targetType == double.class) return Double.valueOf(s);
            if (targetType == Float.class || targetType == float.class) return Float.valueOf(s);
            if (targetType == Boolean.class || targetType == boolean.class) return Boolean.valueOf(s);

            if (targetType == LocalDate.class) return LocalDate.parse(s);
            if (targetType == LocalDateTime.class) return LocalDateTime.parse(s);
            if (targetType == OffsetDateTime.class) return OffsetDateTime.parse(s);
            if (targetType == Instant.class) return Instant.parse(s);
            if (targetType == Date.class) {
                try {
                    Instant inst = Instant.parse(s);
                    return Date.from(inst);
                } catch (DateTimeParseException ignored) {
                }
            }

            return s;
        } catch (Exception ex) {
            return s;
        }
    }
}
