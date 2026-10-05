package com.landingpage.backend.domain;

public enum MediaResourceType {
    IMAGE("image"),
    VIDEO("video");

    private final String cloudinaryValue;

    MediaResourceType(String cloudinaryValue) {
        this.cloudinaryValue = cloudinaryValue;
    }

    public String cloudinaryValue() {
        return cloudinaryValue;
    }

    public static MediaResourceType fromCloudinaryValue(String value) {
        for (MediaResourceType type : values()) {
            if (type.cloudinaryValue.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported media resource type: " + value);
    }
}
