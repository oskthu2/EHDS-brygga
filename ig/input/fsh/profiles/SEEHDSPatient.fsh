Profile: SEEHDSPatient
Parent: $ipsPatient
Id: se-ehds-patient
Title: "SE EHDS Patient"
Description: """
FHIR Patient-profil för EHDS-bryggan, byggd ovanpå IPS Patient (Patient-uv-ips).
Lägger till svenska identifier-slicar för personnummer, samordningsnummer och
nationelltReservnummer i enlighet med HL7 Sweden basprofiler (SEBasePatient).
Används som subject i SEEHDSCondition och SEEHDSDocumentReference.
"""

* identifier MS
* identifier ^slicing.discriminator.type = #value
* identifier ^slicing.discriminator.path = "system"
* identifier ^slicing.rules = #open
* identifier ^short = "Patientidentifierare – personnummer, samordningsnummer eller nationelltReservnummer"

* identifier contains
    personnummer 0..1 MS and
    samordningsnummer 0..1 MS and
    nationelltReservnummer 0..1 MS

* identifier[personnummer].system 1..1
* identifier[personnummer].system = $personnummerSystem (exactly)
* identifier[personnummer].system ^short = "http://electronichealth.se/identifier/personnummer"
* identifier[personnummer].value 1..1 MS
* identifier[personnummer].value ^short = "Personnummer, t.ex. 191212121212"

* identifier[samordningsnummer].system 1..1
* identifier[samordningsnummer].system = $samordningsnummerSystem (exactly)
* identifier[samordningsnummer].system ^short = "http://electronichealth.se/identifier/samordningsnummer"
* identifier[samordningsnummer].value 1..1 MS
* identifier[samordningsnummer].value ^short = "Samordningsnummer"

* identifier[nationelltReservnummer].system 1..1
* identifier[nationelltReservnummer].system = $nationelltReservnummer (exactly)
* identifier[nationelltReservnummer].system ^short = "http://electronichealth.se/identifier/nationelltReservnummer"
* identifier[nationelltReservnummer].value 1..1 MS
* identifier[nationelltReservnummer].value ^short = "Nationellt reservnummer"

* name MS
* name.family MS
* name.given MS

* birthDate MS
