package events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import software.spool.core.model.Event;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BoeItem(
        String identifier,
        String control,
        String title,
        @JsonProperty("section_code")       String sectionCode,
        @JsonProperty("section_name")       String sectionName,
        @JsonProperty("department_code")    String departmentCode,
        @JsonProperty("department_name")    String departmentName,
        @JsonProperty("epigraph_name")      String epigraphName,
        @JsonProperty("url_html")           String urlHtml,
        @JsonProperty("url_xml")            String urlXml,
        @JsonProperty("url_pdf")            String urlPdf
) implements Event {

    @Override
    public String eventId() {
        return identifier;
    }

    @Override
    public String causationId() {
        return "";
    }

    @Override
    public String correlationId() {
        return "";
    }

    @Override
    public Instant timestamp() {
        return null;
    }
}