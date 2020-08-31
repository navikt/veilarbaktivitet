package no.nav.veilarbaktivitet.util;

import lombok.SneakyThrows;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetAddress;

import static org.slf4j.LoggerFactory.getLogger;


public class IsLeader {
    private static final Logger LOG = getLogger(IsLeader.class);

    private static JSONObject getJSONFromUrl(String url) throws IOException {
        return new JSONObject(url);
    }

    @SneakyThrows
    public static boolean isLeader() {
        String electorPath = System.getenv("ELECTOR_PATH");
        JSONObject leaderJson = getJSONFromUrl(electorPath);
        String leader = leaderJson.getString("name");
        String hostname = InetAddress.getLocalHost().getHostName();
        boolean isLeader = hostname.equals(leader);

        LOG.debug("is leader " + isLeader);
        return isLeader;
    }
}
