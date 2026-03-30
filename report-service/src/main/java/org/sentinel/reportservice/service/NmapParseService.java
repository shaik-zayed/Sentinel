package org.sentinel.reportservice.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.reportservice.model.NmapRun;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NmapParseService {

    private final XmlMapper xmlMapper;

    public NmapParseService() {
        this.xmlMapper = new XmlMapper();
        // nmap adds fields we don't model - ignore them silently
        this.xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Parses raw nmap XML output into a NmapRun object.
     * Strips any DOCTYPE / processing instructions before the <nmaprun> element.
     */
    public NmapRun parse(String rawXml) throws Exception {
        String xml = rawXml;

        // Strip DOCTYPE declaration and processing instructions
        // nmap output begins with <?xml ... ?> <!DOCTYPE ...> <?xml-stylesheet ...>
        int nmapStart = xml.indexOf("<nmaprun");
        if (nmapStart > 0) {
            xml = xml.substring(nmapStart);
        }

        log.debug("Parsing nmap XML, length={}", xml.length());
        return xmlMapper.readValue(xml, NmapRun.class);
    }
}