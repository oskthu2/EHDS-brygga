
import sys
import argparse
import time as sys_time
import uuid
import re
import io
import json
import decimal
from datetime import datetime, time as datetime_time
from malac.utils import date as dateutil, exceptions, html as htmlutil
from html import escape as html_escape
import rivta_getdiagnosis_model
import malac.models.fhir.r4
from malac.models.fhir import utils
from malac.utils import fhirpath

description_text = "This has been compiled by the MApping LAnguage compiler for Health Data, short MaLaC-HD. See arguments for more details."
one_timestamp = datetime.now(datetime.now().astimezone().tzinfo)
fhirpath_utils = fhirpath.FHIRPathUtils(malac.models.fhir.r4)
shared_vars = {}

def init_argparse() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=description_text)
    parser.add_argument(
       '-s', '--source', help="the source file path, or '-' to read from stdin", required=True
    )
    parser.add_argument(
       '-t', '--target', help="the target file path the result will be written to, or '-' to write to stdout", required=True
    )
    parser.add_argument(
       '-st', '--source-type', choices=['xml', 'json'], help="source format; required when the source is '-' (stdin), otherwise detected from the file extension", required=False
    )
    parser.add_argument(
       '-tt', '--target-type', choices=['xml', 'json'], help="target format; required when the target is '-' (stdout), otherwise detected from the file extension", required=False
    )
    return parser

_SOURCE_JSON_SUPPORTED = False
_TARGET_JSON_SUPPORTED = True

def _resolve_format(path, explicit, kind, json_supported):
    fmt = explicit
    if not fmt:
        if path == '-':
            raise ValueError("when the %s is '-' its format must be set with --%s-type" % (kind, kind))
        if path.endswith('.xml'):
            fmt = 'xml'
        elif path.endswith('.json'):
            fmt = 'json'
        else:
            raise ValueError("unknown %s file ending (expected .xml or .json): %s" % (kind, path))
    if fmt == 'json' and not json_supported:
        raise ValueError("JSON is not supported for the %s of this mapping (the model is XML-only)" % kind)
    return fmt

def _read_source(source_path, source_type):
    if source_type == 'xml':
        src = sys.stdin.buffer if source_path == '-' else source_path
        return rivta_getdiagnosis_model.parse(src, silence=True)
    raise ValueError('unknown source type: ' + str(source_type))

def _write_target(target_path, target_type, result):
    out = sys.stdout if target_path == '-' else open(target_path, 'w', newline='', encoding='utf-8')
    try:
        if target_type == 'xml':
            out.write('<?xml version="1.0" encoding="UTF-8"?>\n')
            result.export(out, 0, namespacedef_='xmlns="http://hl7.org/fhir" xmlns:v3="urn:hl7-org:v3"')
        elif target_type == 'json':
            json.dump(result.exportJson(), out, indent=4, cls=malac.models.fhir.r4.GdsJsonEncoder_)
        else:
            raise ValueError('unknown target type: ' + str(target_type))
    finally:
        if target_path != '-':
            out.close()

def transform(source_path, target_path, source_type=None, target_type=None):
    start = sys_time.time()
    print('+++++++ Transformation from '+source_path+' to '+target_path+' started +++++++', file=sys.stderr)

    source_type = _resolve_format(source_path, source_type, 'source', _SOURCE_JSON_SUPPORTED)
    target_type = _resolve_format(target_path, target_type, 'target', _TARGET_JSON_SUPPORTED)
    src = _read_source(source_path, source_type)
    tgt = malac.models.fhir.r4.Condition()
    GetDiagnosisToCondition(src, tgt)
    _write_target(target_path, target_type, tgt)

    print('altogether in '+str(round(sys_time.time()-start,3))+' seconds.', file=sys.stderr)
    print('+++++++ Transformation from '+source_path+' to '+target_path+' ended  +++++++', file=sys.stderr)

def GetDiagnosisToCondition(src, tgt):
    tgt.clinicalStatus = malac.models.fhir.r4.CodeableConcept(coding=[malac.models.fhir.r4.Coding(system=malac.models.fhir.r4.uri(value='http://terminology.hl7.org/CodeSystem/condition-clinical'), code=malac.models.fhir.r4.string(value='active'))])
    tgt.verificationStatus = malac.models.fhir.r4.CodeableConcept(coding=[malac.models.fhir.r4.Coding(system=malac.models.fhir.r4.uri(value='http://terminology.hl7.org/CodeSystem/condition-ver-status'), code=malac.models.fhir.r4.string(value='confirmed'))])
    for d in (getattr(src, 'diagnosis', None) if isinstance(getattr(src, 'diagnosis', None), list) else ([] if not getattr(src, 'diagnosis', None) else [getattr(src, 'diagnosis', None)])):
        for h in (getattr(d, 'diagnosisHeader', None) if isinstance(getattr(d, 'diagnosisHeader', None), list) else ([] if not getattr(d, 'diagnosisHeader', None) else [getattr(d, 'diagnosisHeader', None)])):
            for docId in (getattr(h, 'documentId', None) if isinstance(getattr(h, 'documentId', None), list) else ([] if not getattr(h, 'documentId', None) else [getattr(h, 'documentId', None)])):
                docIdentifier = malac.models.fhir.r4.Identifier()
                tgt.identifier.append(docIdentifier)
                for docId2 in (docId if isinstance(docId, list) else ([] if not docId else [docId])):
                    docIdentifier.value = (malac.models.fhir.r4.string(value=docId2) if isinstance(docId2, str) else malac.models.fhir.r4.string(value=docId2.value))
            for srcSys in (getattr(h, 'sourceSystemHSAId', None) if isinstance(getattr(h, 'sourceSystemHSAId', None), list) else ([] if not getattr(h, 'sourceSystemHSAId', None) else [getattr(h, 'sourceSystemHSAId', None)])):
                m = malac.models.fhir.r4.Meta()
                tgt.meta = m
                for srcSys2 in (srcSys if isinstance(srcSys, list) else ([] if not srcSys else [srcSys])):
                    metaSrc = malac.models.fhir.r4.uri()
                    m.source = metaSrc
                    metaSrc.value = srcSys2
                profSe = malac.models.fhir.r4.canonical()
                m.profile.append(profSe)
                profSe.value = 'https://ehds-brygga.inera.se/fhir/StructureDefinition/se-ehds-condition'
                profEu = malac.models.fhir.r4.canonical()
                m.profile.append(profEu)
                profEu.value = 'http://hl7.eu/fhir/eps/StructureDefinition/condition-obl-eu-eps'
            for pid in (getattr(h, 'patientId', None) if isinstance(getattr(h, 'patientId', None), list) else ([] if not getattr(h, 'patientId', None) else [getattr(h, 'patientId', None)])):
                subjRef = malac.models.fhir.r4.Reference()
                tgt.subject = subjRef
                subjId = malac.models.fhir.r4.Identifier()
                subjRef.identifier = subjId
                for pidVal in (getattr(pid, 'id', None) if isinstance(getattr(pid, 'id', None), list) else ([] if not getattr(pid, 'id', None) else [getattr(pid, 'id', None)])):
                    subjId.value = (malac.models.fhir.r4.string(value=pidVal) if isinstance(pidVal, str) else malac.models.fhir.r4.string(value=pidVal.value))
                for pidType in (getattr(pid, 'type_', None) if isinstance(getattr(pid, 'type_', None), list) else ([] if not getattr(pid, 'type_', None) else [getattr(pid, 'type_', None)])):
                    subjIdSys = malac.models.fhir.r4.uri()
                    subjId.system = subjIdSys
                    subjIdSys.value = pidType
            for ahp in (getattr(h, 'accountableHealthcareProfessional', None) if isinstance(getattr(h, 'accountableHealthcareProfessional', None), list) else ([] if not getattr(h, 'accountableHealthcareProfessional', None) else [getattr(h, 'accountableHealthcareProfessional', None)])):
                for recHsa in (getattr(ahp, 'healthcareProfessionalHSAId', None) if isinstance(getattr(ahp, 'healthcareProfessionalHSAId', None), list) else ([] if not getattr(ahp, 'healthcareProfessionalHSAId', None) else [getattr(ahp, 'healthcareProfessionalHSAId', None)])):
                    recorderRef = malac.models.fhir.r4.Reference()
                    tgt.recorder = recorderRef
                    recorderRef.type_ = malac.models.fhir.r4.uri(value='PractitionerRole')
                    recorderId = malac.models.fhir.r4.Identifier()
                    recorderRef.identifier = recorderId
                    for recHsaV in (recHsa if isinstance(recHsa, list) else ([] if not recHsa else [recHsa])):
                        recorderId.system = malac.models.fhir.r4.uri(value='urn:oid:1.2.752.129.2.1.4.1')
                    for recHsaV2 in (recHsa if isinstance(recHsa, list) else ([] if not recHsa else [recHsa])):
                        recorderId.value = (malac.models.fhir.r4.string(value=recHsaV2) if isinstance(recHsaV2, str) else malac.models.fhir.r4.string(value=recHsaV2.value))
                for authorTime in (getattr(ahp, 'authorTime', None) if isinstance(getattr(ahp, 'authorTime', None), list) else ([] if not getattr(ahp, 'authorTime', None) else [getattr(ahp, 'authorTime', None)])):
                    tgt.recordedDate = malac.models.fhir.r4.dateTime(value=dateutil.parse(authorTime).isoformat())
            for la in (getattr(h, 'legalAuthenticator', None) if isinstance(getattr(h, 'legalAuthenticator', None), list) else ([] if not getattr(h, 'legalAuthenticator', None) else [getattr(h, 'legalAuthenticator', None)])):
                for assHsa in (getattr(la, 'legalAuthenticatorHSAId', None) if isinstance(getattr(la, 'legalAuthenticatorHSAId', None), list) else ([] if not getattr(la, 'legalAuthenticatorHSAId', None) else [getattr(la, 'legalAuthenticatorHSAId', None)])):
                    asserterRef = malac.models.fhir.r4.Reference()
                    tgt.asserter = asserterRef
                    asserterRef.type_ = malac.models.fhir.r4.uri(value='PractitionerRole')
                    asserterId = malac.models.fhir.r4.Identifier()
                    asserterRef.identifier = asserterId
                    for assHsaV in (assHsa if isinstance(assHsa, list) else ([] if not assHsa else [assHsa])):
                        asserterId.system = malac.models.fhir.r4.uri(value='urn:oid:1.2.752.129.2.1.4.1')
                    for assHsaV2 in (assHsa if isinstance(assHsa, list) else ([] if not assHsa else [assHsa])):
                        asserterId.value = (malac.models.fhir.r4.string(value=assHsaV2) if isinstance(assHsaV2, str) else malac.models.fhir.r4.string(value=assHsaV2.value))
                for sigTime in (getattr(la, 'signatureTime', None) if isinstance(getattr(la, 'signatureTime', None), list) else ([] if not getattr(la, 'signatureTime', None) else [getattr(la, 'signatureTime', None)])):
                    assertedDateExt = malac.models.fhir.r4.Extension()
                    tgt.extension.append(assertedDateExt)
                    assertedDateExt.url = 'https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-asserted-date'
                    for sigTime2 in (sigTime if isinstance(sigTime, list) else ([] if not sigTime else [sigTime])):
                        assertedDateExt.valueDateTime = malac.models.fhir.r4.dateTime(value=dateutil.parse(sigTime2).isoformat())
        for b in (getattr(d, 'diagnosisBody', None) if isinstance(getattr(d, 'diagnosisBody', None), list) else ([] if not getattr(d, 'diagnosisBody', None) else [getattr(d, 'diagnosisBody', None)])):
            for onsetTime in (getattr(b, 'diagnosisTime', None) if isinstance(getattr(b, 'diagnosisTime', None), list) else ([] if not getattr(b, 'diagnosisTime', None) else [getattr(b, 'diagnosisTime', None)])):
                tgt.onsetDateTime = malac.models.fhir.r4.dateTime(value=dateutil.parse(onsetTime).isoformat())
            for diagType in (getattr(b, 'typeOfDiagnosis', None) if isinstance(getattr(b, 'typeOfDiagnosis', None), list) else ([] if not getattr(b, 'typeOfDiagnosis', None) else [getattr(b, 'typeOfDiagnosis', None)])):
                if fhirpath.single(fhirpath_utils.matches(diagType, ['^(Huvuddiagnos|Bidiagnos)$'])):
                    catMapped = malac.models.fhir.r4.CodeableConcept()
                    tgt.category.append(catMapped)
                    for diagType0 in (diagType if isinstance(diagType, list) else ([] if not diagType else [diagType])):
                        catMapped.coding.append(translate_single('http://ehds-brygga.inera.se/fhir/ConceptMap/diagnosis-type', code=(diagType0 if isinstance(diagType0, str) else diagType0.value), out_type='Coding'))
            for diagType2 in (getattr(b, 'typeOfDiagnosis', None) if isinstance(getattr(b, 'typeOfDiagnosis', None), list) else ([] if not getattr(b, 'typeOfDiagnosis', None) else [getattr(b, 'typeOfDiagnosis', None)])):
                if fhirpath.single(fhirpath_utils.bool_not(fhirpath_utils.matches(diagType2, ['^(Huvuddiagnos|Bidiagnos)$']))):
                    catAbsent = malac.models.fhir.r4.CodeableConcept()
                    tgt.category.append(catAbsent)
                    darExt = malac.models.fhir.r4.Extension()
                    catAbsent.extension.append(darExt)
                    darExt.url = 'http://hl7.org/fhir/StructureDefinition/data-absent-reason'
                    darCode = malac.models.fhir.r4.code()
                    darExt.valueCode = darCode
                    darCode.value = 'unknown'
            for chronic in (getattr(b, 'chronicDiagnosis', None) if isinstance(getattr(b, 'chronicDiagnosis', None), list) else ([] if not getattr(b, 'chronicDiagnosis', None) else [getattr(b, 'chronicDiagnosis', None)])):
                chronicExt = malac.models.fhir.r4.Extension()
                tgt.extension.append(chronicExt)
                chronicExt.url = 'https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-chronic-condition'
                chronicVal = malac.models.fhir.r4.boolean()
                chronicExt.valueBoolean = chronicVal
                for chronic2 in (chronic if isinstance(chronic, list) else ([] if not chronic else [chronic])):
                    chronicVal.value = chronic2
            for rel in (getattr(b, 'relatedDiagnosis', None) if isinstance(getattr(b, 'relatedDiagnosis', None), list) else ([] if not getattr(b, 'relatedDiagnosis', None) else [getattr(b, 'relatedDiagnosis', None)])):
                for relDocId in (getattr(rel, 'documentId', None) if isinstance(getattr(rel, 'documentId', None), list) else ([] if not getattr(rel, 'documentId', None) else [getattr(rel, 'documentId', None)])):
                    relatedExt = malac.models.fhir.r4.Extension()
                    tgt.extension.append(relatedExt)
                    relatedExt.url = 'https://ehds-brygga.inera.se/fhir/StructureDefinition/ext-related-condition'
                    relatedRef = malac.models.fhir.r4.Reference()
                    relatedExt.valueReference = relatedRef
                    relatedId = malac.models.fhir.r4.Identifier()
                    relatedRef.identifier = relatedId
                    for relDocId2 in (relDocId if isinstance(relDocId, list) else ([] if not relDocId else [relDocId])):
                        relatedId.value = (malac.models.fhir.r4.string(value=relDocId2) if isinstance(relDocId2, str) else malac.models.fhir.r4.string(value=relDocId2.value))
            for dCode in (getattr(b, 'diagnosisCode', None) if isinstance(getattr(b, 'diagnosisCode', None), list) else ([] if not getattr(b, 'diagnosisCode', None) else [getattr(b, 'diagnosisCode', None)])):
                codeCc = malac.models.fhir.r4.CodeableConcept()
                tgt.code = codeCc
                codeCoding = malac.models.fhir.r4.Coding()
                codeCc.coding.append(codeCoding)
                for codeVal in (getattr(dCode, 'code', None) if isinstance(getattr(dCode, 'code', None), list) else ([] if not getattr(dCode, 'code', None) else [getattr(dCode, 'code', None)])):
                    codeCoding.code = malac.models.fhir.r4.string(value=codeVal)
                for dispVal in (getattr(dCode, 'displayName', None) if isinstance(getattr(dCode, 'displayName', None), list) else ([] if not getattr(dCode, 'displayName', None) else [getattr(dCode, 'displayName', None)])):
                    codeCoding.display = (malac.models.fhir.r4.string(value=dispVal) if isinstance(dispVal, str) else malac.models.fhir.r4.string(value=dispVal.value))
                for cs in (getattr(dCode, 'codeSystem', None) if isinstance(getattr(dCode, 'codeSystem', None), list) else ([] if not getattr(dCode, 'codeSystem', None) else [getattr(dCode, 'codeSystem', None)])):
                    codeCodingSys = malac.models.fhir.r4.uri()
                    codeCoding.system = codeCodingSys
                    codeCodingSys.value = cs
                for origText in (getattr(dCode, 'originalText', None) if isinstance(getattr(dCode, 'originalText', None), list) else ([] if not getattr(dCode, 'originalText', None) else [getattr(dCode, 'originalText', None)])):
                    if [origText]:
                        codeCc.text = (malac.models.fhir.r4.string(value=origText) if isinstance(origText, str) else malac.models.fhir.r4.string(value=origText.value))
                for dCode2 in (dCode if isinstance(dCode, list) else ([] if not dCode else [dCode])):
                    if not fhirpath_utils.get(dCode2,'originalText'):
                        for dispVal2 in (getattr(dCode2, 'displayName', None) if isinstance(getattr(dCode2, 'displayName', None), list) else ([] if not getattr(dCode2, 'displayName', None) else [getattr(dCode2, 'displayName', None)])):
                            codeCc.text = (malac.models.fhir.r4.string(value=dispVal2) if isinstance(dispVal2, str) else malac.models.fhir.r4.string(value=dispVal2.value))

# output
# 1..1 result (boolean)
# 0..1 message with error details for human (string)
# 0..* match with (list)
#   0..1 equivalence/relationship
#   0..1 concept
#       0..1 system
#       0..1 version
#       0..1 code
#       0..1 display 
#       0..1 userSelected will always be false, because this is a translation
#   0..1 source (conceptMap url)
# TODO implement reverse
def translate(url=None, conceptMapVersion=None, code=None, system=None, version=None, source=None, coding=None, codeableConcept=None, target=None, targetsystem=None, reverse=None, silent=False)              -> dict [bool, str, list[dict[str, dict[str, str, str, str, bool], str]]]:
    start = sys_time.time()
    
    # start validation and recall of translate in simple from
    if codeableConcept:
        if isinstance(codeableConcept, str): 
            codeableConcept = malac.models.fhir.r4.parseString(codeableConcept, silent)
        elif isinstance(codeableConcept, malac.models.fhir.r4.CodeableConcept):
            pass
        else:
            raise TypeError("The codeableConcept parameter has to be a string or a CodeableConcept Object (called method as library)!")
        # the first fit will be returned, else the last unfitted value will be returned
        # TODO check translate params
        for one_coding in codeableConcept.get_coding():
            if (ret := translate(url=url, source=source, coding=one_coding, 
                                 target=target, targetsystem=targetsystem, 
                                 reverse=reverse, silent=True))['result']:
                return ret
        else: return ret
    elif coding:
        if isinstance(coding, str): 
            coding = malac.models.fhir.r4.parseString(coding, silent)
        elif isinstance(coding, malac.models.fhir.r4.Coding):
            pass
        else:
            raise TypeError("The coding parameter has to be a string or a Coding Object (called method as library)!")
        # TODO check translate params
        return translate(url=url,  source=source, code=coding.code.value, system=(coding.system.value if coding.system else None), 
                         target=target, targetsystem=targetsystem, 
                         reverse=reverse, silent=True)
    elif code:
        if not isinstance(code,str): 
            raise TypeError("The code parameter has to be a string!")
    elif target:
        if not isinstance(code,str): 
            raise TypeError("The target parameter has to be a string!")
    elif targetsystem:
        if not isinstance(code,str): 
            raise TypeError("The targetsystem parameter has to be a string!")
    else:
        raise ValueError("At least codeableConcept, coding, code, target or targetSystem has to be given!")
    # end validation and recall of translate in simplier from

    # look for any information from the one ore more generated conceptMaps into conceptMap_as_7dimension_dict
    match = []
    unmapped = []
    if url and url not in conceptMap_as_7dimension_dict.keys():
        print('   #ERROR# ConceptMap with URL "'+ url +'" is not loaded to this compiled conceptMap #ERROR#')
    else:
        for url_lvl in conceptMap_as_7dimension_dict:
            if url_lvl == "%" or not url or url_lvl == str(url or ""):#+str(("/?version=" and conceptMapVersion) or ""):
                for source_lvl in conceptMap_as_7dimension_dict[url_lvl]:
                    if source_lvl == "%" or not source or source_lvl == source:
                        for target_lvl in conceptMap_as_7dimension_dict[url_lvl][source_lvl]:
                            if target_lvl == "%" or not target or target_lvl == target:
                                for system_lvl in conceptMap_as_7dimension_dict[url_lvl][source_lvl][target_lvl]:
                                    if system_lvl == "%" or not system or system_lvl == system:#+str(("/?version=" and version) or ""):
                                        for targetsystem_lvl in conceptMap_as_7dimension_dict[url_lvl][source_lvl][target_lvl][system_lvl]:
                                            if targetsystem_lvl == "%" or not targetsystem or targetsystem_lvl == targetsystem:
                                                for code_lvl in conceptMap_as_7dimension_dict[url_lvl][source_lvl][target_lvl][system_lvl][targetsystem_lvl]:
                                                    if code_lvl == "|" or code_lvl == "~" or code_lvl == "#":
                                                        unmapped += conceptMap_as_7dimension_dict[url_lvl][source_lvl][target_lvl][system_lvl][targetsystem_lvl][code_lvl]
                                                    if code_lvl == "%" or not code or code_lvl == code:
                                                        match += conceptMap_as_7dimension_dict[url_lvl][source_lvl][target_lvl][system_lvl][targetsystem_lvl][code_lvl]                
                                                    
    if not match:
        for one_unmapped in unmapped:
            tmp_system = ""
            tmp_version = ""
            tmp_code = ""
            tmp_display = ""
            # replace all "|" values with to translated code (provided from https://hl7.org/fhir/R4B/conceptmap-definitions.html#ConceptMap.group.unmapped.mode)
            if one_unmapped["concept"]["code"].startswith("|"):
                tmp_system = system
                tmp_version = version
                tmp_code = one_unmapped["concept"]["code"][1:] + code
            # replace all "~" values with fixed code (provided from https://hl7.org/fhir/R4B/conceptmap-definitions.html#ConceptMap.group.unmapped.mode)
            elif one_unmapped["concept"]["code"].startswith("~"):
                if tmp := one_unmapped["concept"]["system"]: tmp_system = tmp 
                tmp_code = one_unmapped["concept"]["code"][1:]
                tmp_display = one_unmapped["concept"]["display"]
            elif one_unmapped["concept"]["code"].startswith("#"):
                # TODO detect recursion like conceptMapA -> conceptMapB -> ConceptMapA -> ...
                return translate(one_unmapped["concept"]["code"][1:], None, code, system, version, source, 
                                 coding, codeableConcept, target, targetsystem, reverse, silent)
            # prepare the match.concept results
            concept = {}
            if tmp_system: concept["system"] = tmp_system
            if tmp_version: concept["version"] = tmp_version
            if tmp_code: concept["code"] = tmp_code
            if tmp_display: concept["display"] = tmp_display

            # if the concept dict is empty, than skip this broken value and give a warning, that there is a empty group
            if concept == {}:
                # TODO do a warning, that it seems like the conceptmap is broken, because there is a empty group
                continue
            
            match.append({"relationship": one_unmapped["relationship"], 
                          "concept": concept,
                          "source": one_unmapped["source"]})

    # see if any match is not in R4 "unmatched" or "disjoint" and in R5 "not-related-to"
    result = False
    message = ""
    for one_match in match:
        if one_match["relationship"] not in ['not-related-to']:
            result = True 
            # for printing only, if no url was initially given use the conceptmap
            if not url:
                url = one_match["source"]

    if not silent:
        print('Translation in '+str(round(sys_time.time()-start,3))+' seconds for code "'+(code or "NONE")+'" with ConceptMap "'+url+'"')
    return {"result": result, "message": message, "match": match}

conceptMap_as_7dimension_dict = {}


conceptMap_as_7dimension_dict["http://ehds-brygga.inera.se/fhir/ConceptMap/diagnosis-type"] = {
    "%": {
        "%": {
            "urn:riv:clinicalprocess:healthcond:description:enums:2:DiagnosisTypeEnum": {
                "https://terminologitjansten.inera.se/inera-kodverksforvaltning/kodverk/kv_diagnostyp": {
                    "Huvuddiagnos": [
                        {
                            "relationship": "equivalent",
                            "concept": {
                                "system": "https://terminologitjansten.inera.se/inera-kodverksforvaltning/kodverk/kv_diagnostyp",
                                "code": "HD",
                                "display": "Huvuddiagnos"
                            },
                            "source": "http://ehds-brygga.inera.se/fhir/ConceptMap/diagnosis-type"
                        }
                    ],
                    "Bidiagnos": [
                        {
                            "relationship": "equivalent",
                            "concept": {
                                "system": "https://terminologitjansten.inera.se/inera-kodverksforvaltning/kodverk/kv_diagnostyp",
                                "code": "BY",
                                "display": "Bidiagnos"
                            },
                            "source": "http://ehds-brygga.inera.se/fhir/ConceptMap/diagnosis-type"
                        }
                    ]
                }
            }
        }
    }
}

conceptMap_as_7dimension_dict["http://ehds-brygga.inera.se/fhir/ConceptMap/codesystem-oid"] = {
    "%": {
        "%": {
            "urn:oid:source": {
                "urn:ietf:na": {
                    "1.2.752.129.2.1.3.1": [
                        {
                            "relationship": "equivalent",
                            "concept": {
                                "system": "urn:ietf:na",
                                "code": "http://electronichealth.se/identifier/personnummer",
                                "display": "Personnummer"
                            },
                            "source": "http://ehds-brygga.inera.se/fhir/ConceptMap/codesystem-oid"
                        }
                    ],
                    "1.2.752.129.2.1.3.3": [
                        {
                            "relationship": "equivalent",
                            "concept": {
                                "system": "urn:ietf:na",
                                "code": "http://electronichealth.se/identifier/samordningsnummer",
                                "display": "Samordningsnummer"
                            },
                            "source": "http://ehds-brygga.inera.se/fhir/ConceptMap/codesystem-oid"
                        }
                    ],
                    "1.2.752.116.1.1.1.1.3": [
                        {
                            "relationship": "equivalent",
                            "concept": {
                                "system": "urn:ietf:na",
                                "code": "https://www.icd10.se/",
                                "display": "ICD-10-SE"
                            },
                            "source": "http://ehds-brygga.inera.se/fhir/ConceptMap/codesystem-oid"
                        }
                    ],
                    "2.16.840.1.113883.6.3": [
                        {
                            "relationship": "equivalent",
                            "concept": {
                                "system": "urn:ietf:na",
                                "code": "http://hl7.org/fhir/sid/icd-10",
                                "display": "ICD-10 (international)"
                            },
                            "source": "http://ehds-brygga.inera.se/fhir/ConceptMap/codesystem-oid"
                        }
                    ],
                    "2.16.840.1.113883.6.96": [
                        {
                            "relationship": "equivalent",
                            "concept": {
                                "system": "urn:ietf:na",
                                "code": "http://snomed.info/sct",
                                "display": "SNOMED CT"
                            },
                            "source": "http://ehds-brygga.inera.se/fhir/ConceptMap/codesystem-oid"
                        }
                    ]
                }
            }
        }
    }
}
def unpack_container(resource_container):
    if resource_container is None:
        return None
    if resource_container.Account is not None:
        return resource_container.Account
    if resource_container.ActivityDefinition is not None:
        return resource_container.ActivityDefinition
    if resource_container.AdministrableProductDefinition is not None:
        return resource_container.AdministrableProductDefinition
    if resource_container.AdverseEvent is not None:
        return resource_container.AdverseEvent
    if resource_container.AllergyIntolerance is not None:
        return resource_container.AllergyIntolerance
    if resource_container.Appointment is not None:
        return resource_container.Appointment
    if resource_container.AppointmentResponse is not None:
        return resource_container.AppointmentResponse
    if resource_container.AuditEvent is not None:
        return resource_container.AuditEvent
    if resource_container.Basic is not None:
        return resource_container.Basic
    if resource_container.Binary is not None:
        return resource_container.Binary
    if resource_container.BiologicallyDerivedProduct is not None:
        return resource_container.BiologicallyDerivedProduct
    if resource_container.BodyStructure is not None:
        return resource_container.BodyStructure
    if resource_container.Bundle is not None:
        return resource_container.Bundle
    if resource_container.CapabilityStatement is not None:
        return resource_container.CapabilityStatement
    if resource_container.CarePlan is not None:
        return resource_container.CarePlan
    if resource_container.CareTeam is not None:
        return resource_container.CareTeam
    if resource_container.CatalogEntry is not None:
        return resource_container.CatalogEntry
    if resource_container.ChargeItem is not None:
        return resource_container.ChargeItem
    if resource_container.ChargeItemDefinition is not None:
        return resource_container.ChargeItemDefinition
    if resource_container.Citation is not None:
        return resource_container.Citation
    if resource_container.Claim is not None:
        return resource_container.Claim
    if resource_container.ClaimResponse is not None:
        return resource_container.ClaimResponse
    if resource_container.ClinicalImpression is not None:
        return resource_container.ClinicalImpression
    if resource_container.ClinicalUseDefinition is not None:
        return resource_container.ClinicalUseDefinition
    if resource_container.CodeSystem is not None:
        return resource_container.CodeSystem
    if resource_container.Communication is not None:
        return resource_container.Communication
    if resource_container.CommunicationRequest is not None:
        return resource_container.CommunicationRequest
    if resource_container.CompartmentDefinition is not None:
        return resource_container.CompartmentDefinition
    if resource_container.Composition is not None:
        return resource_container.Composition
    if resource_container.ConceptMap is not None:
        return resource_container.ConceptMap
    if resource_container.Condition is not None:
        return resource_container.Condition
    if resource_container.Consent is not None:
        return resource_container.Consent
    if resource_container.Contract is not None:
        return resource_container.Contract
    if resource_container.Coverage is not None:
        return resource_container.Coverage
    if resource_container.CoverageEligibilityRequest is not None:
        return resource_container.CoverageEligibilityRequest
    if resource_container.CoverageEligibilityResponse is not None:
        return resource_container.CoverageEligibilityResponse
    if resource_container.DetectedIssue is not None:
        return resource_container.DetectedIssue
    if resource_container.Device is not None:
        return resource_container.Device
    if resource_container.DeviceDefinition is not None:
        return resource_container.DeviceDefinition
    if resource_container.DeviceMetric is not None:
        return resource_container.DeviceMetric
    if resource_container.DeviceRequest is not None:
        return resource_container.DeviceRequest
    if resource_container.DeviceUseStatement is not None:
        return resource_container.DeviceUseStatement
    if resource_container.DiagnosticReport is not None:
        return resource_container.DiagnosticReport
    if resource_container.DocumentManifest is not None:
        return resource_container.DocumentManifest
    if resource_container.DocumentReference is not None:
        return resource_container.DocumentReference
    if resource_container.Encounter is not None:
        return resource_container.Encounter
    if resource_container.Endpoint is not None:
        return resource_container.Endpoint
    if resource_container.EnrollmentRequest is not None:
        return resource_container.EnrollmentRequest
    if resource_container.EnrollmentResponse is not None:
        return resource_container.EnrollmentResponse
    if resource_container.EpisodeOfCare is not None:
        return resource_container.EpisodeOfCare
    if resource_container.EventDefinition is not None:
        return resource_container.EventDefinition
    if resource_container.Evidence is not None:
        return resource_container.Evidence
    if resource_container.EvidenceReport is not None:
        return resource_container.EvidenceReport
    if resource_container.EvidenceVariable is not None:
        return resource_container.EvidenceVariable
    if resource_container.ExampleScenario is not None:
        return resource_container.ExampleScenario
    if resource_container.ExplanationOfBenefit is not None:
        return resource_container.ExplanationOfBenefit
    if resource_container.FamilyMemberHistory is not None:
        return resource_container.FamilyMemberHistory
    if resource_container.Flag is not None:
        return resource_container.Flag
    if resource_container.Goal is not None:
        return resource_container.Goal
    if resource_container.GraphDefinition is not None:
        return resource_container.GraphDefinition
    if resource_container.Group is not None:
        return resource_container.Group
    if resource_container.GuidanceResponse is not None:
        return resource_container.GuidanceResponse
    if resource_container.HealthcareService is not None:
        return resource_container.HealthcareService
    if resource_container.ImagingStudy is not None:
        return resource_container.ImagingStudy
    if resource_container.Immunization is not None:
        return resource_container.Immunization
    if resource_container.ImmunizationEvaluation is not None:
        return resource_container.ImmunizationEvaluation
    if resource_container.ImmunizationRecommendation is not None:
        return resource_container.ImmunizationRecommendation
    if resource_container.ImplementationGuide is not None:
        return resource_container.ImplementationGuide
    if resource_container.Ingredient is not None:
        return resource_container.Ingredient
    if resource_container.InsurancePlan is not None:
        return resource_container.InsurancePlan
    if resource_container.Invoice is not None:
        return resource_container.Invoice
    if resource_container.Library is not None:
        return resource_container.Library
    if resource_container.Linkage is not None:
        return resource_container.Linkage
    if resource_container.List is not None:
        return resource_container.List
    if resource_container.Location is not None:
        return resource_container.Location
    if resource_container.ManufacturedItemDefinition is not None:
        return resource_container.ManufacturedItemDefinition
    if resource_container.Measure is not None:
        return resource_container.Measure
    if resource_container.MeasureReport is not None:
        return resource_container.MeasureReport
    if resource_container.Media is not None:
        return resource_container.Media
    if resource_container.Medication is not None:
        return resource_container.Medication
    if resource_container.MedicationAdministration is not None:
        return resource_container.MedicationAdministration
    if resource_container.MedicationDispense is not None:
        return resource_container.MedicationDispense
    if resource_container.MedicationKnowledge is not None:
        return resource_container.MedicationKnowledge
    if resource_container.MedicationRequest is not None:
        return resource_container.MedicationRequest
    if resource_container.MedicationStatement is not None:
        return resource_container.MedicationStatement
    if resource_container.MedicinalProductDefinition is not None:
        return resource_container.MedicinalProductDefinition
    if resource_container.MessageDefinition is not None:
        return resource_container.MessageDefinition
    if resource_container.MessageHeader is not None:
        return resource_container.MessageHeader
    if resource_container.MolecularSequence is not None:
        return resource_container.MolecularSequence
    if resource_container.NamingSystem is not None:
        return resource_container.NamingSystem
    if resource_container.NutritionOrder is not None:
        return resource_container.NutritionOrder
    if resource_container.NutritionProduct is not None:
        return resource_container.NutritionProduct
    if resource_container.Observation is not None:
        return resource_container.Observation
    if resource_container.ObservationDefinition is not None:
        return resource_container.ObservationDefinition
    if resource_container.OperationDefinition is not None:
        return resource_container.OperationDefinition
    if resource_container.OperationOutcome is not None:
        return resource_container.OperationOutcome
    if resource_container.Organization is not None:
        return resource_container.Organization
    if resource_container.OrganizationAffiliation is not None:
        return resource_container.OrganizationAffiliation
    if resource_container.PackagedProductDefinition is not None:
        return resource_container.PackagedProductDefinition
    if resource_container.Patient is not None:
        return resource_container.Patient
    if resource_container.PaymentNotice is not None:
        return resource_container.PaymentNotice
    if resource_container.PaymentReconciliation is not None:
        return resource_container.PaymentReconciliation
    if resource_container.Person is not None:
        return resource_container.Person
    if resource_container.PlanDefinition is not None:
        return resource_container.PlanDefinition
    if resource_container.Practitioner is not None:
        return resource_container.Practitioner
    if resource_container.PractitionerRole is not None:
        return resource_container.PractitionerRole
    if resource_container.Procedure is not None:
        return resource_container.Procedure
    if resource_container.Provenance is not None:
        return resource_container.Provenance
    if resource_container.Questionnaire is not None:
        return resource_container.Questionnaire
    if resource_container.QuestionnaireResponse is not None:
        return resource_container.QuestionnaireResponse
    if resource_container.RegulatedAuthorization is not None:
        return resource_container.RegulatedAuthorization
    if resource_container.RelatedPerson is not None:
        return resource_container.RelatedPerson
    if resource_container.RequestGroup is not None:
        return resource_container.RequestGroup
    if resource_container.ResearchDefinition is not None:
        return resource_container.ResearchDefinition
    if resource_container.ResearchElementDefinition is not None:
        return resource_container.ResearchElementDefinition
    if resource_container.ResearchStudy is not None:
        return resource_container.ResearchStudy
    if resource_container.ResearchSubject is not None:
        return resource_container.ResearchSubject
    if resource_container.RiskAssessment is not None:
        return resource_container.RiskAssessment
    if resource_container.Schedule is not None:
        return resource_container.Schedule
    if resource_container.SearchParameter is not None:
        return resource_container.SearchParameter
    if resource_container.ServiceRequest is not None:
        return resource_container.ServiceRequest
    if resource_container.Slot is not None:
        return resource_container.Slot
    if resource_container.Specimen is not None:
        return resource_container.Specimen
    if resource_container.SpecimenDefinition is not None:
        return resource_container.SpecimenDefinition
    if resource_container.StructureDefinition is not None:
        return resource_container.StructureDefinition
    if resource_container.StructureMap is not None:
        return resource_container.StructureMap
    if resource_container.Subscription is not None:
        return resource_container.Subscription
    if resource_container.SubscriptionStatus is not None:
        return resource_container.SubscriptionStatus
    if resource_container.SubscriptionTopic is not None:
        return resource_container.SubscriptionTopic
    if resource_container.Substance is not None:
        return resource_container.Substance
    if resource_container.SubstanceDefinition is not None:
        return resource_container.SubstanceDefinition
    if resource_container.SupplyDelivery is not None:
        return resource_container.SupplyDelivery
    if resource_container.SupplyRequest is not None:
        return resource_container.SupplyRequest
    if resource_container.Task is not None:
        return resource_container.Task
    if resource_container.TerminologyCapabilities is not None:
        return resource_container.TerminologyCapabilities
    if resource_container.TestReport is not None:
        return resource_container.TestReport
    if resource_container.TestScript is not None:
        return resource_container.TestScript
    if resource_container.ValueSet is not None:
        return resource_container.ValueSet
    if resource_container.VerificationResult is not None:
        return resource_container.VerificationResult
    if resource_container.VisionPrescription is not None:
        return resource_container.VisionPrescription
    if resource_container.Parameters is not None:
        return resource_container.Parameters
    return None

def translate_unmapped(url, code):
    if url == 'http://hl7.org/fhir/ConceptMap/special-oid2uri': return [{'uri': 'urn:oid:%s' % code}]
    if url == 'OIDtoURI': return [{'code': 'urn:oid:%s' % code}]
    if url == 'StructureMapGroupTypeMode': return [{'code': 'none'}]
    if url == 'AllergyCategoryMap': return [{'code': None}]
    raise LookupError('Code %s could not be mapped to any code in concept map %s and no exception defined' % (code, url))

def translate_single(url, code=None, coding=None, codeableConcept=None, out_type='code'):
    trans_out = translate(url=url, code=code, coding=coding, codeableConcept=codeableConcept, silent=True)
    matches = [match['concept'] for match in trans_out['match'] if match['relationship']=='equivalent' or match['relationship']=='equal']
    # if there are mutliple 'equivalent' or 'equal' matches and CodeableConcept is not the output param, than throw an error
    if len(matches) > 1:
        raise ValueError("There are multiple 'equivalent' or 'equal' matches in the results of the translate and output type is not CodeableConcept!")
    elif len(matches) == 0:
        matches = translate_unmapped(url=url, code=code)
    if out_type == "Coding":
        return malac.models.fhir.r4.Coding(system=(malac.models.fhir.r4.uri(value=matches[0]['system']) if "system" in matches[0] else None), 
                              version=(malac.models.fhir.r4.string(value=matches[0]['version']) if "version" in matches[0] else None), 
                              code=(malac.models.fhir.r4.string(value=matches[0]['code']) if "code" in matches[0] else None), 
                              display=(malac.models.fhir.r4.string(value=matches[0]['display']) if "display" in  matches[0] else None), 
                              userSelected=(malac.models.fhir.r4.string(value=matches[0]['userSelected']) if "userSelected" in matches[0] else None))
    else:
        return matches[0][out_type]

if __name__ == "__main__":
    parser = init_argparse()
    args = parser.parse_args()
    transform(args.source, args.target, args.source_type, args.target_type)
