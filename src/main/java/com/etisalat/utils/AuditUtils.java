package com.etisalat.utils;

import com.etisalat.models.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AuditUtils {

    private final ObjectMapper mapper;

    public AuditUtils() {
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules();
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Convert an object to a JSON string for storing in history. For nulls returns null.
     */
    public String toJson(Object o) {
        try {
            if (o == null) return null;
            // special case User -> only keep id and username to avoid heavy payload/recursion
            if (o instanceof User user) {
                return mapper.writeValueAsString(new SimpleUser(user.getId(), user.getUsername()));
            }
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            // fallback to toString()
            return Objects.toString(o, null);
        }
    }

    /**
     * Simple POJO for users stored in history to avoid embedding full user objects.
     */
    record SimpleUser(Long id, String username) {}
}