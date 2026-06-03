package software.examples.spool.boe.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import software.spool.core.pipeline.Step;

import java.util.List;

public class NormalizeSummary implements Step<JsonNode, JsonNode> {

    @Override
    public JsonNode apply(JsonNode input) {
        JsonNode metadata = input.path("data").path("sumario").path("metadatos");
        String publishDate = metadata.path("fecha_publicacion").asText(null);

        JsonNode dailyEntries = input.path("data").path("sumario").path("diario");
        if (!dailyEntries.isArray()) return input;

        normalizeDailyEntries(dailyEntries, publishDate);
        return input;
    }

    private void normalizeDailyEntries(JsonNode dailyEntries, String publishDate) {
        for (JsonNode dailyEntry : dailyEntries) {
            ObjectNode dailyNode = (ObjectNode) dailyEntry;
            String summaryId = dailyNode.path("sumario_diario").path("identificador").asText(null);

            ArrayNode flatItems = dailyNode.arrayNode();
            JsonNode sections = dailyNode.get("seccion");
            if (sections != null && sections.isArray())
                sections.forEach(s -> flattenSection(s, summaryId, publishDate, flatItems));

            dailyNode.remove("seccion");
            dailyNode.set("items", flatItems);
        }
    }

    private void flattenSection(JsonNode section, String summaryId, String publishDate, ArrayNode target) {
        String sectionCode = section.path("codigo").asText(null);
        String sectionName = section.path("nombre").asText(null);
        section.path("departamento").forEach(d ->
                flattenDepartment(d, sectionCode, sectionName, summaryId, publishDate, target));
    }

    private void flattenDepartment(JsonNode department, String sectionCode, String sectionName,
                                   String summaryId, String publishDate, ArrayNode target) {
        String departmentCode = department.path("codigo").asText(null);
        String departmentName = department.path("nombre").asText(null);
        if (department.has("epigrafe")) {
            for (JsonNode epigraph : department.get("epigrafe")) {
                String epigraphName = epigraph.path("nombre").asText(null);
                collectItems(epigraph.path("item"), sectionCode, sectionName,
                        departmentCode, departmentName, epigraphName, summaryId, publishDate, target);
            }
        } else if (department.has("item")) {
            collectItems(department.path("item"), sectionCode, sectionName,
                    departmentCode, departmentName, null, summaryId, publishDate, target);
        }
    }

    private void collectItems(JsonNode items, String sectionCode, String sectionName,
                              String departmentCode, String departmentName, String epigraphName,
                              String summaryId, String publishDate, ArrayNode target) {
        if (items == null || items.isMissingNode() || items.isNull()) return;
        (items.isArray() ? items : List.of(items))
                .forEach(i -> target.add(
                        buildEnrichedItem(i, sectionCode, sectionName, departmentCode, departmentName,
                                epigraphName, summaryId, publishDate)
                ));
    }

    private ObjectNode buildEnrichedItem(JsonNode item, String sectionCode, String sectionName,
                                         String departmentCode, String departmentName, String epigraphName,
                                         String summaryId, String publishDate) {
        ObjectNode enriched = item.deepCopy();
        enriched.put("identifier",       item.path("identificador").asText(null));
        enriched.put("control",          item.path("control").asText(null));
        enriched.put("title",            item.path("titulo").asText(null));
        enriched.put("section_code",     sectionCode);
        enriched.put("section_name",     sectionName);
        enriched.put("department_code",  departmentCode);
        enriched.put("department_name",  departmentName);
        if (epigraphName != null) enriched.put("epigraph_name", epigraphName);
        enriched.put("url_html",         item.path("url_html").asText(null));
        enriched.put("url_xml",          item.path("url_xml").asText(null));
        enriched.put("url_pdf",          item.path("url_pdf").path("texto").asText(null));
        enriched.put("correlation_id",   summaryId);
        enriched.put("publish_date",     publishDate);
        return enriched;
    }
}