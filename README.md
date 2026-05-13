# ekstern-trekk-api

API for eksterne samhandlere for å melde inn trekkopplysninger til NAV.

Full API-dokumentasjon er tilgjengelig i Swagger UI, se [Miljøer](#miljøer).

## Autentisering

Tilgang krever et gyldig Maskinporten-token med scope:

```
nav:utbetaling/trekkopplysning/innmelding
```

Se [Maskinporten-dokumentasjonen](https://docs.digdir.no/docs/Maskinporten/) for informasjon om hvordan man anskaffer token.

## Miljøer

| Miljø       | Base-URL                                           | Swagger UI                                                                          |
|-------------|-----------------------------------------------------|-------------------------------------------------------------------------------------|
| Produksjon  | `https://ekstern-trekk-api.ekstern.nav.no`          | [swagger](https://ekstern-trekk-api.ekstern.nav.no/v1/swagger)                         |
| Test        | `https://ekstern-trekk-api.ekstern.dev.nav.no`      | [swagger](https://ekstern-trekk-api.ekstern.dev.nav.no/v1/swagger)                     |

## Bruk

### 1. Send inn trekkopplysning

```
POST /v1/innrapportering
Content-Type: application/xml
Authorization: Bearer <maskinporten-token>
```

Meldingen sendes som XML i henhold til `MsgHead-v1_2.xsd`
(`http://www.kith.no/xmlstds/msghead/2006-05-24`)
med innhold etter `InnrapporteringTrekk-2010-02-04.xsd`
(`http://www.kith.no/xmlstds/nav/innrapporteringtrekk/2010-02-04`).

Ved suksess returneres HTTP 202 med en `Location`-header der `{id}` er en UUID:

```
Location: /v1/innrapportering/7f41c4e9-b6bd-44a3-822b-622332b4e421
```

### 2. Poll behandlingsstatus

```
GET /v1/innrapportering/{id}
Authorization: Bearer <maskinporten-token>
```

Responsen er JSON med følgende felter:

| Felt                   | Beskrivelse                                                      |
|------------------------|------------------------------------------------------------------|
| `id`                   | Unik identifikator tildelt av tjenesten                          |
| `status`               | `PENDING`, `ACCEPTED` eller `REJECTED`                           |
| `submittedAt`          | Tidspunkt da meldingen ble mottatt (ISO-8601)                    |
| `updatedAt`            | Tidspunkt for siste statusendring (ISO-8601)                     |
| `rejectionDescription` | Beskrivelse av avvisningsårsak (kun satt når `REJECTED`)         |
| `rejectionCode`        | Maskinlesbar kode for avvisningsårsak (kun satt når `REJECTED`)  |

Poll til status er `ACCEPTED` eller `REJECTED`.

---

## Utvikling

### Kjøre lokalt

1. Start `RunLocalContainers` i IDEen.
2. Start `LocalApp` i IDEen.
3. Send inn en testtrekkopplysning: `GET localhost:8080/test/putinnrapportering`
   (sender innholdet i `testbody.xml`)
4. Hent status: `GET localhost:8080/test/innrapportering/{id}`
5. Simuler respons fra fagsystem: kjør `LocalTestClient` i IDEen med passende innhold.
6. Stopp: kill LocalApp, stopp RunLocalContainers med kommandoen "stop".

Test-endepunktene fungerer også mot DEV-miljøet. Der kan man i tillegg verifisere
IBM MQ-tilkobling via `GET /testMq`.

### Bygg, test og lint

```bash
# Bygg (inkluderer ktlint format + check)
./gradlew build

# Kjør alle tester
./gradlew test

# Kjør én testklasse
./gradlew test --tests "no.nav.trekkapi.persistence.TrekkInnmeldingRepositoryTest"

# Formater kode
./gradlew ktlintFormat
```

`GITHUB_TOKEN` må være satt for å hente `emottak-payload-xsd`-pakken fra GitHub Packages:

```bash
export GITHUB_TOKEN=<your-pat>
./gradlew build
```