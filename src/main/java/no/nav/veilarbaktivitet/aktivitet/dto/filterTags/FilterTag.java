package no.nav.veilarbaktivitet.aktivitet.dto.filterTags;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = FilterTagString.class, name = "string"),
    @JsonSubTypes.Type(value = FilterTagBool.class, name = "bool")
})
public abstract class FilterTag {}



