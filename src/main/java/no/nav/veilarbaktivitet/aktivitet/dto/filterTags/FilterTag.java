package no.nav.veilarbaktivitet.aktivitet.dto.filterTags;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = FilterTagString.class, name = "string"),
    @JsonSubTypes.Type(value = FilterTagBool.class, name = "bool")
})
@Data
public abstract class FilterTag {}



