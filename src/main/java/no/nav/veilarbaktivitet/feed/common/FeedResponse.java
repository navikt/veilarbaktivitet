package no.nav.veilarbaktivitet.feed.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
@EqualsAndHashCode
@AllArgsConstructor
public class FeedResponse<DOMAINOBJECT extends Comparable<DOMAINOBJECT>> {
    String nextPageId;
    List<FeedElement<DOMAINOBJECT>> elements;
}
