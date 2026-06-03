package software.examples.spool.boe.plugins;

import software.spool.core.exception.SpoolException;
import software.spool.crawler.api.port.source.PollSource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class BOEHTTPPollSource implements PollSource<byte[]> {
    private final HttpClient httpClient;
    private final String url;
    private final String sourceId;

    public BOEHTTPPollSource(String url) {
        this.url = url + "/" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        this.sourceId = "BOE-Api";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public byte[] fetch() throws SpoolException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofByteArray()
            );

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        sourceId + "returned HTTP " + response.statusCode()
                );
            }

            return response.body();
        } catch (SpoolException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error polling: " + e.getMessage(), e);
        }
    }

    @Override
    public String sourceId() {
        return sourceId;
    }
}
