package org.springframework.data.cockroachdb.it.bank.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Value object just for JSONB serialization.
 */
public class ForeignSystem {
    private String id;

    private String label;

    private String owner;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime inceptionTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;

        private String label;

        private String owner;

        private LocalDateTime inceptionTime;

        private Builder() {
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withLabel(String label) {
            this.label = label;
            return this;
        }

        public Builder withOwner(String owner) {
            this.owner = owner;
            return this;
        }

        public Builder withInceptionTime(LocalDateTime inceptionTime) {
            this.inceptionTime = inceptionTime;
            return this;
        }

        public ForeignSystem build() {
            ForeignSystem foreignSystem = new ForeignSystem();
            foreignSystem.setId(id);
            foreignSystem.setLabel(label);
            foreignSystem.setOwner(owner);
            foreignSystem.inceptionTime = this.inceptionTime;
            return foreignSystem;
        }
    }
}
