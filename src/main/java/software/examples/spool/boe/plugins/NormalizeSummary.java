package software.examples.spool.boe.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import software.spool.core.pipeline.Step;

import java.util.List;

public class NormalizeSummary implements Step<JsonNode, JsonNode> {
    @Override
    public JsonNode apply(JsonNode input) {
        JsonNode dailyEntries = input.path("data").path("sumario").path("diario");
        if (!dailyEntries.isArray()) return input;
        normalizeDailyEntries(dailyEntries);
        return input;
    }

    private void normalizeDailyEntries(JsonNode dailyEntries) {
        for (JsonNode dailyEntry : dailyEntries) {
            ObjectNode dailyNode = (ObjectNode) dailyEntry;
            ArrayNode flatItems = dailyNode.arrayNode();
            JsonNode sections = dailyNode.get("seccion");
            if (sections != null && sections.isArray())
                sections.forEach(s -> flattenSection(s, flatItems));
            dailyNode.remove("seccion");
            dailyNode.set("items", flatItems);
        }
    }

    private void flattenSection(JsonNode section, ArrayNode target) {
        String sectionCode = section.path("codigo").asText(null);
        String sectionName = section.path("nombre").asText(null);
        section.path("departamento").forEach(d -> flattenDepartment(d, sectionCode, sectionName, target));
    }

    private void flattenDepartment(JsonNode department, String sectionCode, String sectionName, ArrayNode target) {
        String departmentCode = department.path("codigo").asText(null);
        String departmentName = department.path("nombre").asText(null);
        if (department.has("epigrafe")) {
            for (JsonNode epigraph : department.get("epigrafe")) {
                String epigraphName = epigraph.path("nombre").asText(null);
                collectItems(epigraph.path("item"), sectionCode, sectionName, departmentCode, departmentName, epigraphName, target);
            }
        } else if (department.has("item")) {
            collectItems(department.path("item"), sectionCode, sectionName, departmentCode, departmentName, null, target);
        }
    }

    private void collectItems(JsonNode items, String sectionCode, String sectionName, String departmentCode, String departmentName, String epigraphName, ArrayNode target) {
        if (items == null || items.isMissingNode() || items.isNull()) return;
        (items.isArray() ? items : List.of(items))
                .forEach(i -> target.add(
                        buildEnrichedItem(i, sectionCode, sectionName, departmentCode, departmentName, epigraphName)
                ));
    }

    private ObjectNode buildEnrichedItem(JsonNode item, String sectionCode, String sectionName, String departmentCode, String departmentName, String epigraphName) {
        ObjectNode enriched = item.deepCopy();
        enriched.put("identifier", item.path("identificador").asText(null));
        enriched.put("control", item.path("control").asText(null));
        enriched.put("title", item.path("titulo").asText(null));
        enriched.put("section_code", sectionCode);
        enriched.put("section_name", sectionName);
        enriched.put("department_code", departmentCode);
        enriched.put("department_name", departmentName);
        if (epigraphName != null) enriched.put("epigraph_name", epigraphName);
        enriched.put("url_html", item.path("url_html").asText(null));
        enriched.put("url_xml", item.path("url_xml").asText(null));
        enriched.put("url_pdf", item.path("url_pdf").path("texto").asText(null));
        return enriched;
    }
}