package org.znerd.confluence.client.http;

import java.util.Objects;

public class ConfluenceLabel {
    private final String prefix;
    private final String name;
    private final String id;

    public ConfluenceLabel(String prefix, String name, String id) {
        this.prefix = prefix;
        this.name = name;
        this.id = id;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ConfluenceLabel that = (ConfluenceLabel) o;
        return Objects.equals(getPrefix(), that.getPrefix()) && Objects
            .equals(getName(), that.getName()) && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPrefix(), getName(), getId());
    }

    @Override
    public String toString() {
        return "ConfluenceLabel{" +
            "prefix='" + this.prefix + '\'' +
            ", name='" + this.name + '\'' +
            ", id='" + this.id +
            '}';
    }
}
