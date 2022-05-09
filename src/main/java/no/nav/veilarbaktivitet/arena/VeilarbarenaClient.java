package no.nav.veilarbaktivitet.arena;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VeilarbarenaClient {
    private final OkHttpClient veilarbarenaClient;

    @Value("${VEILARBARENAAPI_URL}")
    private String baseUrl;


}
