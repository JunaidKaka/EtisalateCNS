package com.etisalat.convertor;

import com.etisalat.models.ChangeSet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter(autoApply = false)
public class ChangeSetConverter implements AttributeConverter<ChangeSet, String> {

    private static final Logger log = LoggerFactory.getLogger(ChangeSetConverter.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .findAndRegisterModules()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public String convertToDatabaseColumn(ChangeSet attribute) {
        if (attribute == null) return null;
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ChangeSet to JSON", e);
            throw new IllegalStateException("Unable to serialize ChangeSet", e);
        }
    }

    @Override
    public ChangeSet convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return new ChangeSet(); // or return null if you prefer
        try {
            return MAPPER.readValue(dbData, ChangeSet.class);
        } catch (Exception e) {
            log.warn("Failed to parse ChangeSet JSON, returning empty ChangeSet. raw: {}", dbData, e);
            return new ChangeSet();
        }
    }
}