package org.sentinel.scanservice.service;

import lombok.extern.slf4j.Slf4j;
import org.sentinel.scanservice.model.ServiceInfo;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class NmapXmlParser {

    private final DocumentBuilderFactory factory;

    public NmapXmlParser() {
        this.factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (Exception e) {
            log.warn("Could not configure XML parser security features: {}", e.getMessage());
        }
    }

    public List<ServiceInfo> parse(String xmlOutput) {
        List<ServiceInfo> services = new ArrayList<>();

        if (xmlOutput == null || xmlOutput.isBlank()) {
            log.warn("Received blank XML output — nothing to parse");
            return services;
        }

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(null);

            Document doc = builder.parse(new InputSource(new StringReader(xmlOutput)));
            NodeList ports = doc.getElementsByTagName("port");

            for (int i = 0; i < ports.getLength(); i++) {
                Element port = (Element) ports.item(i);

                NodeList stateNodes = port.getElementsByTagName("state");
                if (stateNodes.getLength() == 0) continue;

                String state = ((Element) stateNodes.item(0)).getAttribute("state");
                if (!"open".equals(state)) continue;

                NodeList serviceNodes = port.getElementsByTagName("service");
                if (serviceNodes.getLength() == 0) continue;

                Element service = (Element) serviceNodes.item(0);
                String product = service.getAttribute("product");
                String version = service.getAttribute("version");

                if (product == null || product.isBlank() ||
                        version == null || version.isBlank()) {
                    log.debug("Skipping port {} — no product/version detected",
                            port.getAttribute("portid"));
                    continue;
                }

                String cpe = null;
                NodeList cpeNodes = port.getElementsByTagName("cpe");
                if (cpeNodes.getLength() > 0) {
                    String cpeText = cpeNodes.item(0).getTextContent();
                    if (cpeText != null && !cpeText.isBlank()) {
                        cpe = cpeText.trim();
                    }
                }

                services.add(new ServiceInfo(
                        Integer.parseInt(port.getAttribute("portid")),
                        port.getAttribute("protocol"),
                        service.getAttribute("name"),
                        product.trim(),
                        version.trim(),
                        cpe
                ));
            }

            log.debug("Parsed {} qualifying services from Nmap XML", services.size());

        } catch (Exception e) {
            log.error("Failed to parse Nmap XML: {}", e.getMessage(), e);
            throw new RuntimeException("Nmap XML parsing failed", e);
        }

        return services;
    }
}