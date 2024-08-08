package com.mycocode.barcode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class SlipRequest {
    public static byte[] requestSlip() throws Exception, IOException, InterruptedException {
        String bodyData = """
-----------------------------365609023425885959402198407856
Content-Disposition: form-data; name="form_submitted"

1
-----------------------------365609023425885959402198407856
Content-Disposition: form-data; name="csrfKey"

0e86de096cb4f7a1f03acdd8b37547d4
-----------------------------365609023425885959402198407856
Content-Disposition: form-data; name="MAX_FILE_SIZE"

267386880
-----------------------------365609023425885959402198407856
Content-Disposition: form-data; name="plupload"

5d0638586182043571d0eb5487275b11
-----------------------------365609023425885959402198407856
Content-Disposition: form-data; name="slip_event_values"

132
-----------------------------365609023425885959402198407856
Content-Disposition: form-data; name="slip_event_original"


-----------------------------365609023425885959402198407856
Content-Disposition: form-data; name="slip_event"


-----------------------------365609023425885959402198407856
Content-Disposition: form-data; name="slip_total"

8
-----------------------------365609023425885959402198407856
Content-Disposition: form-data; name="radio_slip_per_page__empty"

1
-----------------------------365609023425885959402198407856
Content-Disposition: form-data; name="slip_per_page"

2
-----------------------------365609023425885959402198407856--
""";
        String url = "https://mycomap.com/events/event-slips?event=132?form_slip_total=1?form_slip_per_page=2";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(bodyData, StandardCharsets.UTF_8))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:128.0) Gecko/20100101 Firefox/128.0")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/png,image/svg+xml,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Accept-Encoding", "gzip, deflate, br, zstd")
                .header("Content-Type", "multipart/form-data; boundary=---------------------------365609023425885959402198407856")
                .header("Referer", "https://mycomap.com/events/event-slips?event=132?form_slip_total=1?form_slip_per_page=2")
                .header("Origin", "https://mycomap.com")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Sec-Fetch-User",
                        "?1")
                .header("Cookie", "ips4_IPSSessionFront=5bpfjp5pok0649jumki9f1rev3; ips4_ipsTimezone=America/Los_Angeles; ips4_hasJS=true; _ga=GA1.2.1438203160.1722798640; _gid=GA1.2.2018097640.1722798640; _ga_HNMN58BVF4=GS1.2.1722830440.2.1.1722832124.0.0.0")
                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                .build();
        HttpClient client = HttpClient.newHttpClient();
        System.err.println(request);
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        System.err.println(response.statusCode());
        System.err.println(response.statusCode());
        System.err.println(response.body().getClass());
        return response.body();
    }
    private static void get() throws Exception {
        String url = "https://mycomap.com/events/event-slips?event=132?form_slip_total=1?form_slip_per_page=2";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:128.0) Gecko/20100101 Firefox/128.0")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/png,image/svg+xml,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Accept-Encoding", "gzip, deflate, br, zstd")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-User",
                        "?1")
                .header("Cookie", "ips4_IPSSessionFront=5bpfjp5pok0649jumki9f1rev3; ips4_ipsTimezone=America/Los_Angeles; ips4_hasJS=true; _ga=GA1.2.1438203160.1722798640; _gid=GA1.2.2018097640.1722798640; _ga_HNMN58BVF4=GS1.2.1722798640.1.0.1722798640.0.0.0")
                .build();
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.err.println(response.statusCode());
        System.err.println(response.body());
    }
}