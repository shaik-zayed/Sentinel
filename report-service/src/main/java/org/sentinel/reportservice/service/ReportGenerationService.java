package org.sentinel.reportservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.sentinel.reportservice.model.NmapRun;

@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private final NmapParseService nmapParseService;

    private final String nmapXml = """
             <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE nmaprun>
            <?xml-stylesheet href="file:///usr/bin/../share/nmap/nmap.xsl" type="text/xsl"?>
            <!-- Nmap 7.95 scan initiated Wed Feb 11 09:59:50 2026 as: /usr/bin/nmap -sS -sV -&#45;version-intensity 5 -O -&#45;osscan-limit -&#45;top-ports 100 -T4 -A -oX - 192.168.29.1 -->
            <nmaprun scanner="nmap" args="/usr/bin/nmap -sS -sV -&#45;version-intensity 5 -O -&#45;osscan-limit -&#45;top-ports 100 -T4 -A -oX - 192.168.29.1" start="1770803990" startstr="Wed Feb 11 09:59:50 2026" version="7.95" xmloutputversion="1.05">
            <scaninfo type="syn" protocol="tcp" numservices="100" services="7,9,13,21-23,25-26,37,53,79-81,88,106,110-111,113,119,135,139,143-144,179,199,389,427,443-445,465,513-515,543-544,548,554,587,631,646,873,990,993,995,1025-1029,1110,1433,1720,1723,1755,1900,2000-2001,2049,2121,2717,3000,3128,3306,3389,3986,4899,5000,5009,5051,5060,5101,5190,5357,5432,5631,5666,5800,5900,6000-6001,6646,7070,8000,8008-8009,8080-8081,8443,8888,9100,9999-10000,32768,49152-49157"/>
            <verbose level="0"/>
            <debugging level="0"/>
            <hosthint><status state="up" reason="unknown-response" reason_ttl="0"/>
            <address addr="192.168.29.1" addrtype="ipv4"/>
            <hostnames>
            </hostnames>
            </hosthint>
            <host starttime="1770804003" endtime="1770804105"><status state="up" reason="reset" reason_ttl="63"/>
            <address addr="192.168.29.1" addrtype="ipv4"/>
            <hostnames>
            </hostnames>
            <ports><extraports state="filtered" count="95">
            <extrareasons reason="no-response" count="95" proto="tcp" ports="7,9,13,21-23,25-26,37,53,79,81,88,106,110-111,113,119,135,139,143-144,179,199,389,427,444-445,465,513-515,543-544,548,554,587,631,646,873,990,993,995,1025-1029,1110,1433,1720,1723,1755,2000-2001,2049,2121,2717,3000,3128,3306,3389,3986,4899,5000,5009,5051,5060,5101,5190,5357,5432,5631,5666,5800,5900,6000-6001,6646,7070,8000,8008-8009,8081,8888,9100,9999-10000,32768,49152-49157"/>
            </extraports>
            <port protocol="tcp" portid="80"><state state="open" reason="syn-ack" reason_ttl="63"/><service name="http" product="lighttpd" method="probed" conf="10"><cpe>cpe:/a:lighttpd:lighttpd</cpe></service></port>
            <port protocol="tcp" portid="443"><state state="open" reason="syn-ack" reason_ttl="63"/><service name="http" product="lighttpd" tunnel="ssl" method="probed" conf="10"><cpe>cpe:/a:lighttpd:lighttpd</cpe></service><script id="ssl-date" output="TLS randomness does not represent time"></script><script id="ssl-cert" output="Subject: commonName=RILSELFCERT/organizationName=Reliance Jio Infocomm Limited&#xa;Not valid before: 2018-06-27T00:00:02&#xa;Not valid after:  2028-06-24T00:00:02"><table key="subject">
            <elem key="commonName">RILSELFCERT</elem>
            <elem key="organizationName">Reliance Jio Infocomm Limited</elem>
            <elem key="organizationalUnitName">Reliance Project Vijay</elem>
            </table>
            <table key="issuer">
            <elem key="commonName">RILSELFCERT</elem>
            <elem key="organizationName">Reliance Jio Infocomm Limited</elem>
            <elem key="organizationalUnitName">Reliance Project Vijay</elem>
            </table>
            <table key="pubkey">
            <elem key="type">rsa</elem>
            <elem key="bits">2048</elem>
            <elem key="modulus">BC87A47928E6C4E680E2648FEDBBBBD0C18814B60594E052067D9C250EAE81978C4DDFB7EF8B02D0388456C690CF338DD690F05E4DD81453F64A6F2E5C833A21454ABBBA6F8DE765680E69BA0F5BB0CAEEF1D57591DAD5C6F6C9D9A9C2E0B27F9D7D06D3402F9545955D077690EFD42B1EE87B8C1D63D079CE4B4D048DD431FC510DA8E6354A2D20DA1B3D6B6B642622C5C58C9974E3FA2F151CDCE16CC33394799EF5C9BE4AD15F2979A9CA3977D6868E1FB8F99D7E6F5FBF43679A759D186A8AAD2DD1083E181C02E97CA8379A5FA845BBA0D48E35642D9E8F549890608B3117AFA7F885760376FFFC59DD8732DEA0777809C3CC57126AAED0FB4A4A41E56B</elem>
            <elem key="exponent">65537</elem>
            </table>
            <elem key="sig_algo">sha256WithRSAEncryption</elem>
            <table key="validity">
            <elem key="notBefore">2018-06-27T00:00:02</elem>
            <elem key="notAfter">2028-06-24T00:00:02</elem>
            </table>
            <elem key="md5">b0124f164ef30608492efdd31ba3f1b4</elem>
            <elem key="sha1">ed20604786b822d6ffceb722995a5e663ee59eeb</elem>
            <elem key="pem">-&#45;&#45;&#45;&#45;BEGIN CERTIFICATE-&#45;&#45;&#45;&#45;&#xa;MIIDNzCCAh+gAwIBAgIBADANBgkqhkiG9w0BAQsFADBfMRQwEgYDVQQDDAtSSUxT&#xa;RUxGQ0VSVDEfMB0GA1UECwwWUmVsaWFuY2UgUHJvamVjdCBWaWpheTEmMCQGA1UE&#xa;CgwdUmVsaWFuY2UgSmlvIEluZm9jb21tIExpbWl0ZWQwHhcNMTgwNjI3MDAwMDAy&#xa;WhcNMjgwNjI0MDAwMDAyWjBfMRQwEgYDVQQDDAtSSUxTRUxGQ0VSVDEfMB0GA1UE&#xa;CwwWUmVsaWFuY2UgUHJvamVjdCBWaWpheTEmMCQGA1UECgwdUmVsaWFuY2UgSmlv&#xa;IEluZm9jb21tIExpbWl0ZWQwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB&#xa;AQC8h6R5KObE5oDiZI/tu7vQwYgUtgWU4FIGfZwlDq6Bl4xN37fviwLQOIRWxpDP&#xa;M43WkPBeTdgUU/ZKby5cgzohRUq7um+N52VoDmm6D1uwyu7x1XWR2tXG9snZqcLg&#xa;sn+dfQbTQC+VRZVdB3aQ79QrHuh7jB1j0HnOS00EjdQx/FENqOY1Si0g2hs9a2tk&#xa;JiLFxYyZdOP6LxUc3OFswzOUeZ71yb5K0V8peanKOXfWho4fuPmdfm9fv0NnmnWd&#xa;GGqKrS3RCD4YHALpfKg3ml+oRbug1I41ZC2ej1SYkGCLMRevp/iFdgN2//xZ3Ycy&#xa;3qB3eAnDzFcSaq7Q+0pKQeVrAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAH7sImK6&#xa;1p8Rllfi2mpVTFcaDW0R5AB47urjW4lH2pOEzOnb4LBJknN6ZZbuqgeLH+MbTVAU&#xa;Kj6aruOFkYjyngqTGvamkErlHfDjhBKLHA4JP3UiR56Qbcs/bvaaA+N0/x+7+4qH&#xa;BrhaHqwYRtxNELd9rN5d1w9l+H0wsUizZJ1Vdtv71FYGh7Zi72fVzOSlCVeRqfOC&#xa;P/Pqe44bS2fpyIcL2aqGqBVBmEleDQz4DZCfo7jkT8XYTKqWdlKk9O2wgi5Dhmfr&#xa;i3T/mp8+LvK/VvW0EL6DTIUvmx2V/UDtQstn1Da2/tJ2pDxJ42BEcENWQ722UoBr&#xa;00U9Tez9RNppQsA=&#xa;-&#45;&#45;&#45;&#45;END CERTIFICATE-&#45;&#45;&#45;&#45;&#xa;</elem>
            </script></port>
            <port protocol="tcp" portid="1900"><state state="open" reason="syn-ack" reason_ttl="63"/><service name="upnp" method="probed" conf="10"/></port>
            <port protocol="tcp" portid="8080"><state state="open" reason="syn-ack" reason_ttl="63"/><service name="http-proxy" servicefp="SF-Port8080-TCP:V=7.95%I=5%D=2/11%Time=698C532B%P=x86_64-unknown-linux-gnu%r(GetRequest,98,&quot;HTTP/1\\.0\\x20503\\x20Service\\x20Unavailable\\r\\nContent-Length:\\x2019\\r\\nContent-Type:\\x20text/html\\r\\nConnection:\\x20close\\r\\nServer:\\x20JCOW407/JUICEJFV-1\\.3\\.31\\r\\n\\r\\nService\\x20Unavailable&quot;)%r(HTTPOptions,90,&quot;HTTP/1\\.0\\x20501\\x20Not\\x20Implemented\\r\\nContent-Length:\\x2015\\r\\nContent-Type:\\x20text/html\\r\\nConnection:\\x20close\\r\\nServer:\\x20JCOW407/JUICEJFV-1\\.3\\.31\\r\\n\\r\\nNot\\x20implemented&quot;)%r(FourOhFourRequest,88,&quot;HTTP/1\\.0\\x20400\\x20Bad\\x20Request\\r\\nContent-Length:\\x2011\\r\\nContent-Type:\\x20text/html\\r\\nConnection:\\x20close\\r\\nServer:\\x20JCOW407/JUICEJFV-1\\.3\\.31\\r\\n\\r\\nBad\\x20Request&quot;);" method="table" conf="3"/><script id="http-title" output="Site doesn&apos;t have a title (text/html)."></script><script id="fingerprint-strings" output="&#xa;  FourOhFourRequest: &#xa;    HTTP/1.0 400 Bad Request&#xa;    Content-Length: 11&#xa;    Content-Type: text/html&#xa;    Connection: close&#xa;    Server: JCOW407/JUICEJFV-1.3.31&#xa;    Request&#xa;  GetRequest: &#xa;    HTTP/1.0 503 Service Unavailable&#xa;    Content-Length: 19&#xa;    Content-Type: text/html&#xa;    Connection: close&#xa;    Server: JCOW407/JUICEJFV-1.3.31&#xa;    Service Unavailable&#xa;  HTTPOptions: &#xa;    HTTP/1.0 501 Not Implemented&#xa;    Content-Length: 15&#xa;    Content-Type: text/html&#xa;    Connection: close&#xa;    Server: JCOW407/JUICEJFV-1.3.31&#xa;    implemented"><elem key="FourOhFourRequest">&#xa;    HTTP/1.0 400 Bad Request&#xa;    Content-Length: 11&#xa;    Content-Type: text/html&#xa;    Connection: close&#xa;    Server: JCOW407/JUICEJFV-1.3.31&#xa;    Request</elem>
            <elem key="GetRequest">&#xa;    HTTP/1.0 503 Service Unavailable&#xa;    Content-Length: 19&#xa;    Content-Type: text/html&#xa;    Connection: close&#xa;    Server: JCOW407/JUICEJFV-1.3.31&#xa;    Service Unavailable</elem>
            <elem key="HTTPOptions">&#xa;    HTTP/1.0 501 Not Implemented&#xa;    Content-Length: 15&#xa;    Content-Type: text/html&#xa;    Connection: close&#xa;    Server: JCOW407/JUICEJFV-1.3.31&#xa;    implemented</elem>
            </script></port>
            <port protocol="tcp" portid="8443"><state state="open" reason="syn-ack" reason_ttl="63"/><service name="https-alt" servicefp="SF-Port8443-TCP:V=7.95%T=SSL%I=5%D=2/11%Time=698C5335%P=x86_64-unknown-linux-gnu%r(GetRequest,88,&quot;HTTP/1\\.0\\x20400\\x20Bad\\x20Request\\r\\nContent-Length:\\x2011\\r\\nContent-Type:\\x20text/html\\r\\nConnection:\\x20close\\r\\nServer:\\x20JCOW407/JUICEJFV-1\\.3\\.31\\r\\n\\r\\nBad\\x20Request&quot;)%r(HTTPOptions,90,&quot;HTTP/1\\.0\\x20501\\x20Not\\x20Implemented\\r\\nContent-Length:\\x2015\\r\\nContent-Type:\\x20text/html\\r\\nConnection:\\x20close\\r\\nServer:\\x20JCOW407/JUICEJFV-1\\.3\\.31\\r\\n\\r\\nNot\\x20implemented&quot;)%r(FourOhFourRequest,88,&quot;HTTP/1\\.0\\x20400\\x20Bad\\x20Request\\r\\nContent-Length:\\x2011\\r\\nContent-Type:\\x20text/html\\r\\nConnection:\\x20close\\r\\nServer:\\x20JCOW407/JUICEJFV-1\\.3\\.31\\r\\n\\r\\nBad\\x20Request&quot;)%r(DNSVersionBindReqTCP,65,&quot;\\(null\\)\\x20400\\x20Bad\\x20Request\\r\\nContent-Length:\\x2011\\r\\nContent-Type:\\x20text/html\\r\\nConnection:\\x20close\\r\\n\\r\\nBad\\x20Request&quot;);" tunnel="ssl" method="table" conf="3"/><script id="fingerprint-strings" output="&#xa;  DNSVersionBindReqTCP: &#xa;    (null) 400 Bad Request&#xa;    Content-Length: 11&#xa;    Content-Type: text/html&#xa;    Connection: close&#xa;    Request&#xa;  FourOhFourRequest, GetRequest: &#xa;    HTTP/1.0 400 Bad Request&#xa;    Content-Length: 11&#xa;    Content-Type: text/html&#xa;    Connection: close&#xa;    Server: JCOW407/JUICEJFV-1.3.31&#xa;    Request&#xa;  HTTPOptions: &#xa;    HTTP/1.0 501 Not Implemented&#xa;    Content-Length: 15&#xa;    Content-Type: text/html&#xa;    Connection: close&#xa;    Server: JCOW407/JUICEJFV-1.3.31&#xa;    implemented"><elem key="DNSVersionBindReqTCP">&#xa;    (null) 400 Bad Request&#xa;    Content-Length: 11&#xa;    Content-Type: text/html&#xa;    Connection: close&#xa;    Request</elem>
            <elem key="FourOhFourRequest, GetRequest">&#xa;    HTTP/1.0 400 Bad Request&#xa;    Content-Length: 11&#xa;    Content-Type: text/html&#xa;    Connection: close&#xa;    Server: JCOW407/JUICEJFV-1.3.31&#xa;    Request</elem>
            <elem key="HTTPOptions">&#xa;    HTTP/1.0 501 Not Implemented&#xa;    Content-Length: 15&#xa;    Content-Type: text/html&#xa;    Connection: close&#xa;    Server: JCOW407/JUICEJFV-1.3.31&#xa;    implemented</elem>
            </script><script id="ssl-cert" output="Subject: commonName=jiofiber.local.html/organizationName=Jio Platforms Limited/stateOrProvinceName=KA/countryName=IN&#xa;Not valid before: 2021-11-22T04:56:50&#xa;Not valid after:  2121-10-29T04:56:50"><table key="subject">
            <elem key="commonName">jiofiber.local.html</elem>
            <elem key="countryName">IN</elem>
            <elem key="emailAddress">jio4gvoice.support@zmail.ril.com</elem>
            <elem key="localityName">Bangalore</elem>
            <elem key="organizationName">Jio Platforms Limited</elem>
            <elem key="organizationalUnitName">Devices</elem>
            <elem key="stateOrProvinceName">KA</elem>
            </table>
            <table key="issuer">
            <elem key="commonName">jiofiber.local.html</elem>
            <elem key="countryName">IN</elem>
            <elem key="emailAddress">jio4gvoice.support@zmail.ril.com</elem>
            <elem key="localityName">Bangalore</elem>
            <elem key="organizationName">Jio Platforms Limited</elem>
            <elem key="organizationalUnitName">Devices</elem>
            <elem key="stateOrProvinceName">KA</elem>
            </table>
            <table key="pubkey">
            <elem key="type">rsa</elem>
            <elem key="bits">4096</elem>
            <elem key="modulus">CA3656C921C10FFDD9E03510355E98ECE1E8193FFFC81CD9E2BF6904DA695967F5A105B3F87096BC38ECD937F9EC58E7A8E83507B3F7EDE6CB33D82D89220A4B5A8DB00083CC89F2835AABE74D71DBE0231F363C5059DEAC6015CD8DBF4A5E0690FCC1C1573A35C1AB058352024D53D2D66823F04E034CDBFEE07D437E29B19804FE99AE6F71EFC3234E3145056DDA97A3007A0A5317B7ECEE18D0CBB281C38BE3582498F351EF808C63F9EEAE44ACC4D5F04BE4C8D5AD863926AB4A6982B7FE7F2334B394D558DBFBD98632BDDC2772D9536B76579C6B77A2EC0F057DEAC7D0B9367B11E198DB644AC6EE128842D5F31F19B9CEE177C6F90E9DC207687BC736A94D90B58EBD623540DDE1F2170A6E059E6EE3D3AC52AA42F2FAD8EF27728943BDECB3E02DF37BA00B498ABED054F48646FE58341F6C33EF038E74E8F3278CCAB245B79C65D787967CAC2E935341FEE1CCA3511C83BDB5B5A5824397D44B6F393D3831F48F673F78571591483D91E01FB5BF1FC75A829DB7733A9B4D588ECB22B2429A4CC6EA69EDB861FD2D0001674E6E7F613E746A33713B218C788D77EFEC063FE4E372A58BA8FB7CB5A7B9CC0D689110AE896413F9E7A74EFB5921EA2612D15115257FC8081D88079BBBF9A7F11B2E739CA45A56315F8585B7B0881F7B7B0A1C228726A427E77731787B98C6E5AD004140898D10A9AF555776B2980F34A7</elem>
            <elem key="exponent">65537</elem>
            </table>
            <elem key="sig_algo">sha256WithRSAEncryption</elem>
            <table key="validity">
            <elem key="notBefore">2021-11-22T04:56:50</elem>
            <elem key="notAfter">2121-10-29T04:56:50</elem>
            </table>
            <elem key="md5">a0197a2148b429a2e252bf01b35aca7f</elem>
            <elem key="sha1">21928072a6f2381473d8c9edb6c16b18cfc8b66e</elem>
            <elem key="pem">-&#45;&#45;&#45;&#45;BEGIN CERTIFICATE-&#45;&#45;&#45;&#45;&#xa;MIIF1jCCA74CAQEwDQYJKoZIhvcNAQELBQAwga8xCzAJBgNVBAYTAklOMQswCQYD&#xa;VQQIDAJLQTESMBAGA1UEBwwJQmFuZ2Fsb3JlMR4wHAYDVQQKDBVKaW8gUGxhdGZv&#xa;cm1zIExpbWl0ZWQxEDAOBgNVBAsMB0RldmljZXMxHDAaBgNVBAMME2ppb2ZpYmVy&#xa;LmxvY2FsLmh0bWwxLzAtBgkqhkiG9w0BCQEWIGppbzRndm9pY2Uuc3VwcG9ydEB6&#xa;bWFpbC5yaWwuY29tMCAXDTIxMTEyMjA0NTY1MFoYDzIxMjExMDI5MDQ1NjUwWjCB&#xa;rzELMAkGA1UEBhMCSU4xCzAJBgNVBAgMAktBMRIwEAYDVQQHDAlCYW5nYWxvcmUx&#xa;HjAcBgNVBAoMFUppbyBQbGF0Zm9ybXMgTGltaXRlZDEQMA4GA1UECwwHRGV2aWNl&#xa;czEcMBoGA1UEAwwTamlvZmliZXIubG9jYWwuaHRtbDEvMC0GCSqGSIb3DQEJARYg&#xa;amlvNGd2b2ljZS5zdXBwb3J0QHptYWlsLnJpbC5jb20wggIiMA0GCSqGSIb3DQEB&#xa;AQUAA4ICDwAwggIKAoICAQDKNlbJIcEP/dngNRA1Xpjs4egZP//IHNniv2kE2mlZ&#xa;Z/WhBbP4cJa8OOzZN/nsWOeo6DUHs/ft5ssz2C2JIgpLWo2wAIPMifKDWqvnTXHb&#xa;4CMfNjxQWd6sYBXNjb9KXgaQ/MHBVzo1wasFg1ICTVPS1mgj8E4DTNv+4H1Dfimx&#xa;mAT+ma5vce/DI04xRQVt2pejAHoKUxe37O4Y0MuygcOL41gkmPNR74CMY/nurkSs&#xa;xNXwS+TI1a2GOSarSmmCt/5/IzSzlNVY2/vZhjK93Cdy2VNrdleca3ei7A8FferH&#xa;0Lk2exHhmNtkSsbuEohC1fMfGbnO4XfG+Q6dwgdoe8c2qU2QtY69YjVA3eHyFwpu&#xa;BZ5u49OsUqpC8vrY7ydyiUO97LPgLfN7oAtJir7QVPSGRv5YNB9sM+8DjnTo8yeM&#xa;yrJFt5xl14eWfKwuk1NB/uHMo1Ecg721taWCQ5fUS285PTgx9I9nP3hXFZFIPZHg&#xa;H7W/H8dagp23czqbTViOyyKyQppMxupp7bhh/S0AAWdObn9hPnRqM3E7IYx4jXfv&#xa;7AY/5ONypYuo+3y1p7nMDWiREK6JZBP556dO+1kh6iYS0VEVJX/ICB2IB5u7+afx&#xa;Gy5znKRaVjFfhYW3sIgfe3sKHCKHJqQn53cxeHuYxuWtAEFAiY0Qqa9VV3aymA80&#xa;pwIDAQABMA0GCSqGSIb3DQEBCwUAA4ICAQCWFWsbqR+AuULmluy5P4fkS3neJOOv&#xa;cFsVWGS+GX7lsXU0EKUtaSuBcAkKD6Ef3NMoNHE5CEKQSm2evJGncCNay9JKiuSO&#xa;9cu/97b0Uq+FDBm5TFd+Y3uWgFvBp7jhqFpOueHV0ICbEKE2maLKNLMhEc0laCAJ&#xa;O5FATeYdtk2+t6D8VdncMqhZcL7iNbaeP7EKrbNA7rKg4Ln20hJ3lKOhK97T6DFO&#xa;T1+luplRrXUSCJtLzjvUWAp2Y48PjzbhsIlSnAJ6VzFNf5Gdhu3bHTiJfk+zeIK9&#xa;ucQh4QegIsOghp9zsAgKQyalIbULqcPJVfZJBZowEs9TaHv8Z9y1CwSN38qU771A&#xa;0//1A87Fub7gR8xFH+Eu6FsuFG77YS3g/ajb+lIC9zNjRmgrqBO43MBiTdKgyvNY&#xa;p16stcXYVax1k3AS1/v8llOqtzigxwQk7QMKpapilbLr7ruNMtk3rVhKef/IwPAH&#xa;OBMA0grBMTIqMMZPK42SxfqtFUoy58zrMkHMmP5yTufF7rWCXIkuRn/6FL6VO3a3&#xa;oVV36jWpkw14bWtcJfoXo3Mrz+m+PnxmOYz2W2ZyAbUNCJSRT0hXQm8O/3PVhWDe&#xa;qF1jT86asOgC3uyabDcZFWSDo9fIQwMFzIacFZx6hG0FVzs0e89xFmR+zgeU7nAC&#xa;3nuJusm8KrRW1g==&#xa;-&#45;&#45;&#45;&#45;END CERTIFICATE-&#45;&#45;&#45;&#45;&#xa;</elem>
            </script><script id="ssl-date" output="TLS randomness does not represent time"></script></port>
            </ports>
            <trace port="80" proto="tcp">
            <hop ttl="1" ipaddr="172.17.0.1" rtt="0.34"/>
            <hop ttl="2" ipaddr="192.168.29.1" rtt="1.21"/>
            </trace>
            <times srtt="2236" rttvar="1880" to="100000"/>
            </host>
            <runstats><finished time="1770804105" timestr="Wed Feb 11 10:01:45 2026" summary="Nmap done at Wed Feb 11 10:01:45 2026; 1 IP address (1 host up) scanned in 115.34 seconds" elapsed="115.34" exit="success"/><hosts up="1" down="0" total="1"/>
            </runstats>
            </nmaprun>
            """;

    private final String xmlNmap = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE nmaprun>
            <?xml-stylesheet href="file:///usr/bin/../share/nmap/nmap.xsl" type="text/xsl"?>
            <!-- Nmap 7.95 scan initiated Sat Feb 14 17:29:27 2026 as: /usr/bin/nmap -sS -sV -&#45;version-intensity 5 -O -&#45;osscan-limit -&#45;top-ports 1000 -T4 -A -oX - 192.168.29.136 -->
            <nmaprun scanner="nmap" args="/usr/bin/nmap -sS -sV -&#45;version-intensity 5 -O -&#45;osscan-limit -&#45;top-ports 1000 -T4 -A -oX - 192.168.29.136" start="1771090167" startstr="Sat Feb 14 17:29:27 2026" version="7.95" xmloutputversion="1.05">
            <scaninfo type="syn" protocol="tcp" numservices="1000" services="1,3-4,6-7,9,13,17,19-26,30,32-33,37,42-43,49,53,70,79-85,88-90,99-100,106,109-111,113,119,125,135,139,143-144,146,161,163,179,199,211-212,222,254-256,259,264,280,301,306,311,340,366,389,406-407,416-417,425,427,443-445,458,464-465,481,497,500,512-515,524,541,543-545,548,554-555,563,587,593,616-617,625,631,636,646,648,666-668,683,687,691,700,705,711,714,720,722,726,749,765,777,783,787,800-801,808,843,873,880,888,898,900-903,911-912,981,987,990,992-993,995,999-1002,1007,1009-1011,1021-1100,1102,1104-1108,1110-1114,1117,1119,1121-1124,1126,1130-1132,1137-1138,1141,1145,1147-1149,1151-1152,1154,1163-1166,1169,1174-1175,1183,1185-1187,1192,1198-1199,1201,1213,1216-1218,1233-1234,1236,1244,1247-1248,1259,1271-1272,1277,1287,1296,1300-1301,1309-1311,1322,1328,1334,1352,1417,1433-1434,1443,1455,1461,1494,1500-1501,1503,1521,1524,1533,1556,1580,1583,1594,1600,1641,1658,1666,1687-1688,1700,1717-1721,1723,1755,1761,1782-1783,1801,1805,1812,1839-1840,1862-1864,1875,1900,1914,1935,1947,1971-1972,1974,1984,1998-2010,2013,2020-2022,2030,2033-2035,2038,2040-2043,2045-2049,2065,2068,2099-2100,2103,2105-2107,2111,2119,2121,2126,2135,2144,2160-2161,2170,2179,2190-2191,2196,2200,2222,2251,2260,2288,2301,2323,2366,2381-2383,2393-2394,2399,2401,2492,2500,2522,2525,2557,2601-2602,2604-2605,2607-2608,2638,2701-2702,2710,2717-2718,2725,2800,2809,2811,2869,2875,2909-2910,2920,2967-2968,2998,3000-3001,3003,3005-3006,3011,3017,3030-3031,3052,3071,3077,3128,3168,3211,3221,3260-3261,3268-3269,3283,3300-3301,3306,3322-3325,3333,3351,3367,3369-3372,3389-3390,3404,3476,3493,3517,3527,3546,3551,3580,3659,3689-3690,3703,3737,3766,3784,3800-3801,3809,3814,3826-3828,3851,3869,3871,3878,3880,3889,3905,3914,3918,3920,3945,3971,3986,3995,3998,4000-4006,4045,4111,4125-4126,4129,4224,4242,4279,4321,4343,4443-4446,4449,4550,4567,4662,4848,4899-4900,4998,5000-5004,5009,5030,5033,5050-5051,5054,5060-5061,5080,5087,5100-5102,5120,5190,5200,5214,5221-5222,5225-5226,5269,5280,5298,5357,5405,5414,5431-5432,5440,5500,5510,5544,5550,5555,5560,5566,5631,5633,5666,5678-5679,5718,5730,5800-5802,5810-5811,5815,5822,5825,5850,5859,5862,5877,5900-5904,5906-5907,5910-5911,5915,5922,5925,5950,5952,5959-5963,5985-5989,5998-6007,6009,6025,6059,6100-6101,6106,6112,6123,6129,6156,6346,6389,6502,6510,6543,6547,6565-6567,6580,6646,6666-6669,6689,6692,6699,6779,6788-6789,6792,6839,6881,6901,6969,7000-7002,7004,7007,7019,7025,7070,7100,7103,7106,7200-7201,7402,7435,7443,7496,7512,7625,7627,7676,7741,7777-7778,7800,7911,7920-7921,7937-7938,7999-8002,8007-8011,8021-8022,8031,8042,8045,8080-8090,8093,8099-8100,8180-8181,8192-8194,8200,8222,8254,8290-8292,8300,8333,8383,8400,8402,8443,8500,8600,8649,8651-8652,8654,8701,8800,8873,8888,8899,8994,9000-9003,9009-9011,9040,9050,9071,9080-9081,9090-9091,9099-9103,9110-9111,9200,9207,9220,9290,9415,9418,9485,9500,9502-9503,9535,9575,9593-9595,9618,9666,9876-9878,9898,9900,9917,9929,9943-9944,9968,9998-10004,10009-10010,10012,10024-10025,10082,10180,10215,10243,10566,10616-10617,10621,10626,10628-10629,10778,11110-11111,11967,12000,12174,12265,12345,13456,13722,13782-13783,14000,14238,14441-14442,15000,15002-15004,15660,15742,16000-16001,16012,16016,16018,16080,16113,16992-16993,17877,17988,18040,18101,18988,19101,19283,19315,19350,19780,19801,19842,20000,20005,20031,20221-20222,20828,21571,22939,23502,24444,24800,25734-25735,26214,27000,27352-27353,27355-27356,27715,28201,30000,30718,30951,31038,31337,32768-32785,33354,33899,34571-34573,35500,38292,40193,40911,41511,42510,44176,44442-44443,44501,45100,48080,49152-49161,49163,49165,49167,49175-49176,49400,49999-50003,50006,50300,50389,50500,50636,50800,51103,51493,52673,52822,52848,52869,54045,54328,55055-55056,55555,55600,56737-56738,57294,57797,58080,60020,60443,61532,61900,62078,63331,64623,64680,65000,65129,65389"/>
            <verbose level="0"/>
            <debugging level="0"/>
            <hosthint><status state="up" reason="unknown-response" reason_ttl="0"/>
            <address addr="192.168.29.136" addrtype="ipv4"/>
            <hostnames>
            </hostnames>
            </hosthint>
            <host starttime="1771090182" endtime="1771090287"><status state="up" reason="echo-reply" reason_ttl="63"/>
            <address addr="192.168.29.136" addrtype="ipv4"/>
            <hostnames>
            </hostnames>
            <ports><extraports state="filtered" count="990">
            <extrareasons reason="no-response" count="990" proto="tcp" ports="1,3-4,6-7,9,13,17,19-26,30,32-33,37,42-43,49,53,70,79-85,88-90,99-100,106,109-111,113,119,125,144,146,161,163,179,199,211-212,222,254-256,259,264,280,301,306,311,340,366,389,406-407,416-417,425,427,443-444,458,464-465,481,497,500,512-515,524,541,543-545,548,554-555,563,587,593,616-617,625,631,636,646,648,666-668,683,687,691,700,705,711,714,720,722,726,749,765,777,783,787,800-801,808,843,873,880,888,898,900-902,911-912,981,987,990,992-993,995,999-1002,1007,1009-1011,1021-1100,1102,1104-1108,1110-1114,1117,1119,1121-1124,1126,1130-1132,1137-1138,1141,1145,1147-1149,1151-1152,1154,1163-1166,1169,1174-1175,1183,1185-1187,1192,1198-1199,1201,1213,1216-1218,1233-1234,1236,1244,1247-1248,1259,1271-1272,1277,1287,1296,1300-1301,1309-1311,1322,1328,1334,1352,1417,1433-1434,1443,1455,1461,1494,1500-1501,1503,1521,1524,1533,1556,1580,1583,1594,1600,1641,1658,1666,1687-1688,1700,1717-1721,1723,1755,1761,1782-1783,1801,1805,1812,1839-1840,1862-1864,1875,1900,1914,1935,1947,1971-1972,1974,1984,1998-2010,2013,2020-2022,2030,2033-2035,2038,2040-2043,2045-2049,2065,2068,2099-2100,2103,2105-2107,2111,2119,2121,2126,2135,2144,2160-2161,2170,2179,2190-2191,2196,2200,2222,2251,2260,2288,2301,2323,2366,2381-2383,2393-2394,2399,2401,2492,2500,2522,2557,2601-2602,2604-2605,2607-2608,2638,2701-2702,2710,2717-2718,2725,2800,2809,2811,2869,2875,2909-2910,2920,2967-2968,2998,3000-3001,3003,3005-3006,3011,3017,3030-3031,3052,3071,3077,3128,3168,3211,3221,3260-3261,3268-3269,3283,3300-3301,3322-3325,3333,3351,3367,3369-3372,3389-3390,3404,3476,3493,3517,3527,3546,3551,3580,3659,3689-3690,3703,3737,3766,3784,3800-3801,3809,3814,3826-3828,3851,3869,3871,3878,3880,3889,3905,3914,3918,3920,3945,3971,3986,3995,3998,4000-4006,4045,4111,4125-4126,4129,4224,4242,4279,4321,4343,4443-4446,4449,4550,4567,4662,4848,4899-4900,4998,5001-5004,5009,5030,5033,5050-5051,5054,5060-5061,5080,5087,5100-5102,5120,5190,5200,5214,5221-5222,5225-5226,5269,5280,5298,5357,5405,5414,5431-5432,5440,5500,5510,5544,5550,5555,5560,5566,5631,5633,5666,5678-5679,5718,5730,5800-5802,5810-5811,5815,5822,5825,5850,5859,5862,5877,5900-5904,5906-5907,5910-5911,5915,5922,5925,5950,5952,5959-5963,5985-5989,5998-6007,6009,6025,6059,6100-6101,6106,6112,6123,6129,6156,6346,6389,6502,6510,6543,6547,6565-6567,6580,6646,6666-6669,6689,6692,6699,6779,6788-6789,6792,6839,6881,6901,6969,7000-7002,7004,7007,7019,7025,7070,7100,7103,7106,7200-7201,7402,7435,7443,7496,7512,7625,7627,7676,7741,7777-7778,7800,7911,7920-7921,7937-7938,7999-8002,8007-8011,8021-8022,8031,8042,8045,8080-8081,8084-8090,8093,8099-8100,8180-8181,8192-8194,8200,8222,8254,8290-8292,8300,8333,8383,8400,8402,8443,8500,8600,8649,8651-8652,8654,8701,8800,8873,8888,8899,8994,9000-9003,9009-9011,9040,9050,9071,9080-9081,9090-9091,9099-9103,9110-9111,9200,9207,9220,9290,9415,9418,9485,9500,9502-9503,9535,9575,9593-9595,9618,9666,9876-9878,9898,9900,9917,9929,9943-9944,9968,9998-10004,10009-10010,10012,10024-10025,10082,10180,10215,10243,10566,10616-10617,10621,10626,10628-10629,10778,11110-11111,11967,12000,12174,12265,12345,13456,13722,13782-13783,14000,14238,14441-14442,15000,15002-15004,15660,15742,16000-16001,16012,16016,16018,16080,16113,16992-16993,17877,17988,18040,18101,18988,19101,19283,19315,19350,19780,19801,19842,20000,20005,20031,20221-20222,20828,21571,22939,23502,24444,24800,25734-25735,26214,27000,27352-27353,27355-27356,27715,28201,30000,30718,30951,31038,31337,32768-32785,33354,33899,34571-34573,35500,38292,40193,40911,41511,42510,44176,44442-44443,44501,45100,48080,49152-49161,49163,49165,49167,49175-49176,49400,49999-50003,50006,50300,50389,50500,50636,50800,51103,51493,52673,52822,52848,52869,54045,54328,55055-55056,55555,55600,56737-56738,57294,57797,58080,60020,60443,61532,61900,62078,63331,64623,64680,65000,65129,65389"/>
            </extraports>
            <port protocol="tcp" portid="135"><state state="open" reason="syn-ack" reason_ttl="63"/><service name="msrpc" product="Microsoft Windows RPC" ostype="Windows" method="probed" conf="10"><cpe>cpe:/o:microsoft:windows</cpe></service></port>
            <port protocol="tcp" portid="139"><state state="open" reason="syn-ack" reason_ttl="63"/><service name="netbios-ssn" product="Microsoft Windows netbios-ssn" ostype="Windows" method="probed" conf="10"><cpe>cpe:/o:microsoft:windows</cpe></service></port>
            <port protocol="tcp" portid="143"><state state="open" reason="syn-ack" reason_ttl="63"/><service name="imap" servicefp="SF-Port143-TCP:V=7.95%I=5%D=2/14%Time=6990B110%P=x86_64-unknown-linux-gnu%r(NULL,F,&quot;\\*\\x20OK\\x20smtp4dev\\r\\n&quot;)%r(GetRequest,64,&quot;\\*\\x20OK\\x20smtp4dev\\r\\nGET\\x20BAD\\x20Error:\\x20Command\\x20&apos;/&apos;\\x20not\\x20recognized\\.\\r\\n\\*\\x20BAD\\x20Error:\\x20Command\\x20&apos;&apos;\\x20not\\x20recognized\\.\\r\\n&quot;)%r(GenericLines,61,&quot;\\*\\x20OK\\x20smtp4dev\\r\\n\\*\\x20BAD\\x20Error:\\x20Command\\x20&apos;&apos;\\x20not\\x20recognized\\.\\r\\n\\*\\x20BAD\\x20Error:\\x20Command\\x20&apos;&apos;\\x20not\\x20recognized\\.\\r\\n&quot;)%r(RTSPRequest,68,&quot;\\*\\x20OK\\x20smtp4dev\\r\\nOPTIONS\\x20BAD\\x20Error:\\x20Command\\x20&apos;/&apos;\\x20not\\x20recognized\\.\\r\\n\\*\\x20BAD\\x20Error:\\x20Command\\x20&apos;&apos;\\x20not\\x20recognized\\.\\r\\n&quot;)%r(RPCCheck,F,&quot;\\*\\x20OK\\x20smtp4dev\\r\\n&quot;)%r(DNSVersionBindReqTCP,F,&quot;\\*\\x20OK\\x20smtp4dev\\r\\n&quot;)%r(Help,3C,&quot;\\*\\x20OK\\x20smtp4dev\\r\\n\\*\\x20BAD\\x20Error:\\x20Command\\x20&apos;HELP&apos;\\x20not\\x20recognized\\.\\r\\n&quot;)%r(SSLSessionReq,F,&quot;\\*\\x20OK\\x20smtp4dev\\r\\n&quot;)%r(TLSSessionReq,F,&quot;\\*\\x20OK\\x20smtp4dev\\r\\n&quot;)%r(Kerberos,F,&quot;\\*\\x20OK\\x20smtp4dev\\r\\n&quot;)%r(SMBProgNeg,F,&quot;\\*\\x20OK\\x20smtp4dev\\r\\n&quot;)%r(X11Probe,F,&quot;\\*\\x20OK\\x20smtp4dev\\r\\n&quot;)%r(SIPOptions,269,&quot;\\*\\x20OK\\x20smtp4dev\\r\\nOPTIONS\\x20BAD\\x20Error:\\x20Command\\x20&apos;SIP:NM&apos;\\x20not\\x20recognized\\.\\r\\nVia:\\x20BAD\\x20Error:\\x20Command\\x20&apos;SIP/2\\.0/TCP&apos;\\x20not\\x20recognized\\.\\r\\nFrom:\\x20BAD\\x20Error:\\x20Command\\x20&apos;&lt;SIP:NM@NM&gt;;TAG=ROOT&apos;\\x20not\\x20recognized\\.\\r\\nTo:\\x20BAD\\x20Error:\\x20Command\\x20&apos;&lt;SIP:NM2@NM2&gt;&apos;\\x20not\\x20recognized\\.\\r\\nCall-ID:\\x20BAD\\x20Error:\\x20Command\\x20&apos;50000&apos;\\x20not\\x20recognized\\.\\r\\nCSeq:\\x20BAD\\x20Error:\\x20Command\\x20&apos;42&apos;\\x20not\\x20recognized\\.\\r\\nMax-Forwards:\\x20BAD\\x20Error:\\x20Command\\x20&apos;70&apos;\\x20not\\x20recognized\\.\\r\\nContent-Length:\\x20BAD\\x20Error:\\x20Command\\x20&apos;0&apos;\\x20not\\x20recognized\\.\\r\\nContact:\\x20BAD\\x20Error:\\x20Command\\x20&apos;&lt;SIP:NM@NM&gt;&apos;\\x20not\\x20recognized\\.\\r\\nAccept:\\x20BAD\\x20Error:\\x20Command\\x20&apos;APPLICATION/SDP&apos;\\x20not\\x20recognized\\.\\r\\n\\*\\x20BAD\\x20Error:\\x20Command\\x20&apos;&apos;\\x20not\\x20recognized\\.\\r\\n&quot;);" method="table" conf="3"/><script id="imap-capabilities" output="IMAP4rev1 completed ACL IDLE CAPABILITY OK ENABLE SASL-IRA0001"/><script id="fingerprint-strings" output="&#xa;  DNSVersionBindReqTCP, Kerberos, NULL, RPCCheck, SMBProgNeg, SSLSessionReq, TLSSessionReq, X11Probe: &#xa;    * OK smtp4dev&#xa;  GenericLines: &#xa;    * OK smtp4dev&#xa;    Error: Command &apos;&apos; not recognized.&#xa;    Error: Command &apos;&apos; not recognized.&#xa;  GetRequest: &#xa;    * OK smtp4dev&#xa;    Error: Command &apos;/&apos; not recognized.&#xa;    Error: Command &apos;&apos; not recognized.&#xa;  Help: &#xa;    * OK smtp4dev&#xa;    Error: Command &apos;HELP&apos; not recognized.&#xa;  RTSPRequest: &#xa;    * OK smtp4dev&#xa;    OPTIONS BAD Error: Command &apos;/&apos; not recognized.&#xa;    Error: Command &apos;&apos; not recognized.&#xa;  SIPOptions: &#xa;    * OK smtp4dev&#xa;    OPTIONS BAD Error: Command &apos;SIP:NM&apos; not recognized.&#xa;    Via: BAD Error: Command &apos;SIP/2.0/TCP&apos; not recognized.&#xa;    From: BAD Error: Command &apos;&lt;SIP:NM@NM&gt;;TAG=ROOT&apos; not recognized.&#xa;    Error: Command &apos;&lt;SIP:NM2@NM2&gt;&apos; not recognized.&#xa;    Call-ID: BAD Error: Command &apos;50000&apos; not recognized.&#xa;    CSeq: BAD Error: Command &apos;42&apos; not recognized.&#xa;    Max-Forwards: BAD Error: Command &apos;70&apos; not recognized.&#xa;    Content-Length: BAD Error: Command &apos;0&apos; not recognized.&#xa;    Contact: BAD Error: Command &apos;&lt;SIP:NM@NM&gt;&apos; not recognized.&#xa;    Accept: BAD Error: Command &apos;APPLICATION/SDP&apos; not recognized.&#xa;    Error: Command &apos;&apos; not recognized."><elem key="DNSVersionBindReqTCP, Kerberos, NULL, RPCCheck, SMBProgNeg, SSLSessionReq, TLSSessionReq, X11Probe">&#xa;    * OK smtp4dev</elem>
            <elem key="GenericLines">&#xa;    * OK smtp4dev&#xa;    Error: Command &apos;&apos; not recognized.&#xa;    Error: Command &apos;&apos; not recognized.</elem>
            <elem key="GetRequest">&#xa;    * OK smtp4dev&#xa;    Error: Command &apos;/&apos; not recognized.&#xa;    Error: Command &apos;&apos; not recognized.</elem>
            <elem key="Help">&#xa;    * OK smtp4dev&#xa;    Error: Command &apos;HELP&apos; not recognized.</elem>
            <elem key="RTSPRequest">&#xa;    * OK smtp4dev&#xa;    OPTIONS BAD Error: Command &apos;/&apos; not recognized.&#xa;    Error: Command &apos;&apos; not recognized.</elem>
            <elem key="SIPOptions">&#xa;    * OK smtp4dev&#xa;    OPTIONS BAD Error: Command &apos;SIP:NM&apos; not recognized.&#xa;    Via: BAD Error: Command &apos;SIP/2.0/TCP&apos; not recognized.&#xa;    From: BAD Error: Command &apos;&lt;SIP:NM@NM&gt;;TAG=ROOT&apos; not recognized.&#xa;    Error: Command &apos;&lt;SIP:NM2@NM2&gt;&apos; not recognized.&#xa;    Call-ID: BAD Error: Command &apos;50000&apos; not recognized.&#xa;    CSeq: BAD Error: Command &apos;42&apos; not recognized.&#xa;    Max-Forwards: BAD Error: Command &apos;70&apos; not recognized.&#xa;    Content-Length: BAD Error: Command &apos;0&apos; not recognized.&#xa;    Contact: BAD Error: Command &apos;&lt;SIP:NM@NM&gt;&apos; not recognized.&#xa;    Accept: BAD Error: Command &apos;APPLICATION/SDP&apos; not recognized.&#xa;    Error: Command &apos;&apos; not recognized.</elem>
            </script></port>
            <port protocol="tcp" portid="445"><state state="open" reason="syn-ack" reason_ttl="63"/><service name="microsoft-ds" method="table" conf="3"/></port>
            <port protocol="tcp" portid="903"><state state="open" reason="syn-ack" reason_ttl="63"/><service name="vmware-auth" product="VMware Authentication Daemon" version="1.10" extrainfo="Uses VNC, SOAP" tunnel="ssl" method="probed" conf="10"/></port>
            <port protocol="tcp" portid="2525"><state state="open" reason="syn-ack" reason_ttl="63"/><service name="smtp" product="smtp4dev" hostname="5515fffee728" method="probed" conf="10"/><script id="smtp-commands" output="Nice to meet you., 8BITMIME, SIZE, SMTPUTF8, AUTH=CRAM-MD5 PLAIN LOGIN, AUTH CRAM-MD5 PLAIN LOGIN"/></port>
            <port protocol="tcp" portid="3306"><state state="open" reason="syn-ack" reason_ttl="63"/><service name="mysql" product="MySQL" version="8.0.36" method="probed" conf="10"><cpe>cpe:/a:mysql:mysql:8.0.36</cpe></service><script id="ssl-cert" output="Subject: commonName=MySQL_Server_8.0.36_Auto_Generated_Server_Certificate&#xa;Not valid before: 2024-01-21T11:58:39&#xa;Not valid after:  2034-01-18T11:58:39"><table key="subject">
            <elem key="commonName">MySQL_Server_8.0.36_Auto_Generated_Server_Certificate</elem>
            </table>
            <table key="issuer">
            <elem key="commonName">MySQL_Server_8.0.36_Auto_Generated_CA_Certificate</elem>
            </table>
            <table key="pubkey">
            <elem key="type">rsa</elem>
            <elem key="bits">2048</elem>
            <elem key="modulus">B0C0F790088B6016B751C295A9020A57F0C5E4F9B373D240A95719E8514CFE6DC547F89399D07BD7DC0AE6C5188057DE0AD36E45F7FB0C16513943636860727F6271AE5A15C9DE68E9CE789B8D42AA43D359E4CB95DC8EC42E4C41C02FA160F4835AADDA31908C693B95A584B37776613AE3FC77C82139417F15E6AB7FA9163A8339A879BBBAA867B59AE561E47236B5C3FF13404E0790666A30EE2E375256F6E7BE110237399C176EA8C62E0998C6E0F1F446A8F7530247B97187CE85DFCE428C6E7160F4F44A536666D9698F7EBAC354E8857E1CAA68DA21B3ED6A4DEC529425148256FE433D6A1533659F08E5117052F3BDAF55F958C74A54D0F97E30546F</elem>
            <elem key="exponent">65537</elem>
            </table>
            <table key="extensions">
            <table>
            <elem key="name">X509v3 Basic Constraints</elem>
            <elem key="value">CA:FALSE</elem>
            <elem key="critical">true</elem>
            </table>
            </table>
            <elem key="sig_algo">sha256WithRSAEncryption</elem>
            <table key="validity">
            <elem key="notBefore">2024-01-21T11:58:39</elem>
            <elem key="notAfter">2034-01-18T11:58:39</elem>
            </table>
            <elem key="md5">414955da67ac0261de59671c2d8d7131</elem>
            <elem key="sha1">c86b8e7e5ae25d8a53a3a89f60558e59c293b4ae</elem>
            <elem key="pem">-&#45;&#45;&#45;&#45;BEGIN CERTIFICATE-&#45;&#45;&#45;&#45;&#xa;MIIDBzCCAe+gAwIBAgIBAjANBgkqhkiG9w0BAQsFADA8MTowOAYDVQQDDDFNeVNR&#xa;TF9TZXJ2ZXJfOC4wLjM2X0F1dG9fR2VuZXJhdGVkX0NBX0NlcnRpZmljYXRlMB4X&#xa;DTI0MDEyMTExNTgzOVoXDTM0MDExODExNTgzOVowQDE+MDwGA1UEAww1TXlTUUxf&#xa;U2VydmVyXzguMC4zNl9BdXRvX0dlbmVyYXRlZF9TZXJ2ZXJfQ2VydGlmaWNhdGUw&#xa;ggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCwwPeQCItgFrdRwpWpAgpX&#xa;8MXk+bNz0kCpVxnoUUz+bcVH+JOZ0HvX3ArmxRiAV94K025F9/sMFlE5Q2NoYHJ/&#xa;YnGuWhXJ3mjpznibjUKqQ9NZ5MuV3I7ELkxBwC+hYPSDWq3aMZCMaTuVpYSzd3Zh&#xa;OuP8d8ghOUF/Fearf6kWOoM5qHm7uqhntZrlYeRyNrXD/xNATgeQZmow7i43Ulb2&#xa;574RAjc5nBduqMYuCZjG4PH0Rqj3UwJHuXGHzoXfzkKMbnFg9PRKU2Zm2WmPfrrD&#xa;VOiFfhyqaNohs+1qTexSlCUUglb+Qz1qFTNlnwjlEXBS872vVflYx0pU0Pl+MFRv&#xa;AgMBAAGjEDAOMAwGA1UdEwEB/wQCMAAwDQYJKoZIhvcNAQELBQADggEBABWoAPlo&#xa;fZS79D2r0qiDcP6/qc40/DH4jaADogsCSx92V1ycLONr0MoTNeANvsr055gsNlE7&#xa;rJwAiHM552Z7N3vpM9K2rxeX2ewqtkrQX+VM+ZZ9NaaPK0nLeTjcbDzJo1jYws8U&#xa;nW5/Z5Z8rGhVyScJGXfypvBSj0ynnqKoUh3/GVpF1SbY13Z/XXocXMVZlsNmdeGH&#xa;gcqX77LqDUUhguuZNDPtav1/TVdBHHwruX+M0xadkImZIhlKDirF90aYQgKIpuym&#xa;C8cHKcNvuV6jNHBX0rnNiwDYuortBR5ozHYIIAazuTYZ/w1OYEKUgPK/vxSMT7a2&#xa;d2P5I0y8W6JM7rE=&#xa;-&#45;&#45;&#45;&#45;END CERTIFICATE-&#45;&#45;&#45;&#45;&#xa;</elem>
            </script><script id="ssl-date" output="TLS randomness does not represent time"></script><script id="mysql-info" output="&#xa;  Protocol: 10&#xa;  Version: 8.0.36&#xa;  Thread ID: 31&#xa;  Capabilities flags: 65535&#xa;  Some Capabilities: SwitchToSSLAfterHandshake, Speaks41ProtocolNew, ODBCClient, DontAllowDatabaseTableColumn, Speaks41ProtocolOld, SupportsLoadDataLocal, LongPassword, InteractiveClient, IgnoreSpaceBeforeParenthesis, Support41Auth, ConnectWithDatabase, LongColumnFlag, IgnoreSigpipes, FoundRows, SupportsTransactions, SupportsCompression, SupportsAuthPlugins, SupportsMultipleStatments, SupportsMultipleResults&#xa;  Status: Autocommit&#xa;  Salt: yR\\x1C(\\x0F6\\x1D{o?f\\x03F`_7\\x0FO\\x02w&#xa;  Auth Plugin Name: caching_sha2_password"><elem key="Protocol">10</elem>
            <elem key="Version">8.0.36</elem>
            <elem key="Thread ID">31</elem>
            <elem key="Capabilities flags">65535</elem>
            <table key="Some Capabilities">
            <elem>SwitchToSSLAfterHandshake</elem>
            <elem>Speaks41ProtocolNew</elem>
            <elem>ODBCClient</elem>
            <elem>DontAllowDatabaseTableColumn</elem>
            <elem>Speaks41ProtocolOld</elem>
            <elem>SupportsLoadDataLocal</elem>
            <elem>LongPassword</elem>
            <elem>InteractiveClient</elem>
            <elem>IgnoreSpaceBeforeParenthesis</elem>
            <elem>Support41Auth</elem>
            <elem>ConnectWithDatabase</elem>
            <elem>LongColumnFlag</elem>
            <elem>IgnoreSigpipes</elem>
            <elem>FoundRows</elem>
            <elem>SupportsTransactions</elem>
            <elem>SupportsCompression</elem>
            <elem>SupportsAuthPlugins</elem>
            <elem>SupportsMultipleStatments</elem>
            <elem>SupportsMultipleResults</elem>
            </table>
            <elem key="Status">Autocommit</elem>
            <elem key="Salt">yR\\x1C(\\x0F6\\x1D{o?f\\x03F`_7\\x0FO\\x02w</elem>
            <elem key="Auth Plugin Name">caching_sha2_password</elem>
            </script></port>
            <port protocol="tcp" portid="5000"><state state="open" reason="syn-ack" reason_ttl="63"/><service name="http" product="Microsoft Kestrel httpd" method="probed" conf="10"><cpe>cpe:/a:microsoft:kestrel</cpe></service><script id="http-title" output="smtp4dev"><elem key="title">smtp4dev</elem>
            </script></port>
            <port protocol="tcp" portid="8082"><state state="open" reason="syn-ack" reason_ttl="63"/><service name="http" product="Apache Tomcat" extrainfo="language: en" method="probed" conf="10"><cpe>cpe:/a:apache:tomcat</cpe></service><script id="http-title" output="Site doesn&apos;t have a title (application/json)."></script></port>
            <port protocol="tcp" portid="8083"><state state="open" reason="syn-ack" reason_ttl="63"/><service name="http" product="Apache Tomcat" extrainfo="language: en" method="probed" conf="10"><cpe>cpe:/a:apache:tomcat</cpe></service><script id="http-title" output="Site doesn&apos;t have a title (application/json;charset=UTF-8)."></script></port>
            </ports>
            <hostscript><script id="smb2-time" output="&#xa;  date: 2026-02-14T17:31:16&#xa;  start_date: N/A"><elem key="date">2026-02-14T17:31:16</elem>
            <elem key="start_date">N/A</elem>
            </script><script id="smb2-security-mode" output="&#xa;  3:1:1: &#xa;    Message signing enabled but not required"><table key="3:1:1">
            <elem>Message signing enabled but not required</elem>
            </table>
            </script></hostscript><trace port="143" proto="tcp">
            <hop ttl="1" ipaddr="172.17.0.1" rtt="0.75"/>
            <hop ttl="2" ipaddr="192.168.29.136" rtt="8.46"/>
            </trace>
            <times srtt="4962" rttvar="3217" to="100000"/>
            </host>
            <runstats><finished time="1771090287" timestr="Sat Feb 14 17:31:27 2026" summary="Nmap done at Sat Feb 14 17:31:27 2026; 1 IP address (1 host up) scanned in 119.89 seconds" elapsed="119.89" exit="success"/><hosts up="1" down="0" total="1"/>
            </runstats>
            </nmaprun>
            
            """;

    public NmapRun getString() throws Exception {
        return nmapParseService.parse(xmlNmap.substring(xmlNmap.indexOf("<nmap")));
    }
}
