Instance: SEEHDSPatientExample
InstanceOf: SEEHDSPatient
Title: "SE EHDS Patient – personnummer-exempel"
Description: "Minimalt exempel på en patient som uppfyller SEEHDSPatient-profilen med personnummer-slice."
Usage: #example

* identifier[personnummer].system = "http://electronichealth.se/identifier/personnummer"
* identifier[personnummer].value = "191212121212"

* name.use = #official
* name.family = "Svensson"
* name.given[0] = "Erik"

* gender = #male

* birthDate = "1912-12-12"
