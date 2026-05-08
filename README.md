# ekstern-trekk-api
API for eksterne samhandlere, for å kunne melde inn trekkopplysninger til NAV

APIet skal aksesseres via Maskinporten, og er tenkt brukt slik:
1. Klient sender 1 trekkopplysningsdokument som body til dette endepunktet:
- `POST /v1/innrapportering`
<br>URL for å finne status finnes i respons-headeren "Location"<BR> 
  `Location: /v1/innrapportering/{id}`
2. Klienten poller status på behandling av dokumentet
- `GET /v1/innrapportering/{id}`
<br>Responsen er en JSON som en av disse:
- `{"status":"Melding mottatt og sendt til behandling","description":"Sendt inn <tidspunkt>"}` 
- `{"status":"Melding ferdig behandlet","description":"Kvittering mottatt <tidspunkt>"}`
- `{"status":"Melding behandlet, ikke akseptert","description":"Trekkvedtak finnes fra før"}`
<br>For et ikke akseptert dokument vil "description" inneholde begrunnelsen.

Kjøre lokalt:
- start `RunLocalContainers` i IDEen.
- start `LocalApp` i IDEen.
- verifisere innhold i DB eller på topics: kjør `LocalTestClient` i IDEen, med passende innhold.
- sende inn trekkopplysning (steg 1 over): `GET localhost:8080/test/putinnrapportering`. 
Dette fører til at dokumentet i `testbody.xml` sendes inn.
- hente status (steg 2 over): `GET localhost:8080/test/innrapportering/{id}`
- simulere mottatt respons fra fagsystem: kjør `LocalTestClient` i IDEen, med passende innhold.
- LocalApp kan killes, RunLocalContainers bør stoppes med kommando "stop"

Test-endepunktene kan også brukes mot en kjørende applikasjon i DEV.
Der kan man også verifisere at tilkobling til IBM MQ fungerer ved 
<br> `GET /testMq`