package no.nav.veilarbaktivitet.feed.common;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FeedWebhookRequest {
    public String callbackUrl;
}
