#!/usr/bin/env python
# -*- coding: utf-8 -*-

#
# Generated Wed Nov 18 11:40:42 2026 by generateDS.py version 2.44.3.
# Python 3.13.18 (main, Oct 26 2026, 14:08:19) [Clang 23.1.3 ]
#
# Command line options:
#   ('-o', 'rivta_getdiagnosis_model.py')
#
# Command line arguments:
#   ../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/GetDiagnosisResponder_2.0.xsd
#
# Command line:
#   /tmp/claude-0/-home-user/f734d4fb-f37b-52fb-a1c9-8c9e6005e0dd/scratchpad/malac-venv/bin/generateDS.py -o "rivta_getdiagnosis_model.py" ../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/GetDiagnosisResponder_2.0.xsd
#
# Current working directory (os.getcwd()):
#   model
#

import sys
try:
    ModulenotfoundExp_ = ModuleNotFoundError
except NameError:
    ModulenotfoundExp_ = ImportError
from six.moves import zip_longest
import os
import re as re_
import base64
import datetime as datetime_
import decimal as decimal_
from lxml import etree as etree_


Validate_simpletypes_ = True
SaveElementTreeNode = True
TagNamePrefix = ""
if sys.version_info.major == 2:
    BaseStrType_ = basestring
else:
    BaseStrType_ = str


def parsexml_(infile, parser=None, **kwargs):
    if parser is None:
        # Use the lxml ElementTree compatible parser so that, e.g.,
        #   we ignore comments.
        try:
            parser = etree_.ETCompatXMLParser()
        except AttributeError:
            # fallback to xml.etree
            parser = etree_.XMLParser()
    try:
        if isinstance(infile, os.PathLike):
            infile = os.path.join(infile)
    except AttributeError:
        pass
    doc = etree_.parse(infile, parser=parser, **kwargs)
    return doc

def parsexmlstring_(instring, parser=None, **kwargs):
    if parser is None:
        # Use the lxml ElementTree compatible parser so that, e.g.,
        #   we ignore comments.
        try:
            parser = etree_.ETCompatXMLParser()
        except AttributeError:
            # fallback to xml.etree
            parser = etree_.XMLParser()
    element = etree_.fromstring(instring, parser=parser, **kwargs)
    return element

#
# Namespace prefix definition table (and other attributes, too)
#
# The module generatedsnamespaces, if it is importable, must contain
# a dictionary named GeneratedsNamespaceDefs.  This Python dictionary
# should map element type names (strings) to XML schema namespace prefix
# definitions.  The export method for any class for which there is
# a namespace prefix definition, will export that definition in the
# XML representation of that element.  See the export method of
# any generated element type class for an example of the use of this
# table.
# A sample table is:
#
#     # File: generatedsnamespaces.py
#
#     GenerateDSNamespaceDefs = {
#         "ElementtypeA": "http://www.xxx.com/namespaceA",
#         "ElementtypeB": "http://www.xxx.com/namespaceB",
#     }
#
# Additionally, the generatedsnamespaces module can contain a python
# dictionary named GenerateDSNamespaceTypePrefixes that associates element
# types with the namespace prefixes that are to be added to the
# "xsi:type" attribute value.  See the _exportAttributes method of
# any generated element type and the generation of "xsi:type" for an
# example of the use of this table.
# An example table:
#
#     # File: generatedsnamespaces.py
#
#     GenerateDSNamespaceTypePrefixes = {
#         "ElementtypeC": "aaa:",
#         "ElementtypeD": "bbb:",
#     }
#

try:
    from generatedsnamespaces import GenerateDSNamespaceDefs as GenerateDSNamespaceDefs_
except ModulenotfoundExp_ :
    GenerateDSNamespaceDefs_ = {}
try:
    from generatedsnamespaces import GenerateDSNamespaceTypePrefixes as GenerateDSNamespaceTypePrefixes_
except ModulenotfoundExp_ :
    GenerateDSNamespaceTypePrefixes_ = {}

#
# You can replace the following class definition by defining an
# importable module named "generatedscollector" containing a class
# named "GdsCollector".  See the default class definition below for
# clues about the possible content of that class.
#
try:
    from generatedscollector import GdsCollector as GdsCollector_
except ModulenotfoundExp_ :

    class GdsCollector_(object):

        def __init__(self, messages=None):
            if messages is None:
                self.messages = []
            else:
                self.messages = messages

        def add_message(self, msg):
            self.messages.append(msg)

        def get_messages(self):
            return self.messages

        def clear_messages(self):
            self.messages = []

        def print_messages(self):
            for msg in self.messages:
                print("Warning: {}".format(msg))

        def write_messages(self, outstream):
            for msg in self.messages:
                outstream.write("Warning: {}\n".format(msg))


#
# The super-class for enum types
#

try:
    from enum import Enum
except ModulenotfoundExp_ :
    Enum = object

#
# The root super-class for element type classes
#
# Calls to the methods in these classes are generated by generateDS.py.
# You can replace these methods by re-implementing the following class
#   in a module named generatedssuper.py.

try:
    from generatedssuper import GeneratedsSuper
except ModulenotfoundExp_ as exp:
    try:
        from generatedssupersuper import GeneratedsSuperSuper
    except ModulenotfoundExp_ as exp:
        class GeneratedsSuperSuper(object):
            pass
    
    class GeneratedsSuper(GeneratedsSuperSuper):
        __hash__ = object.__hash__
        tzoff_pattern = re_.compile('(\\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00)$')
        class _FixedOffsetTZ(datetime_.tzinfo):
            def __init__(self, offset, name):
                self.__offset = datetime_.timedelta(minutes=offset)
                self.__name = name
            def utcoffset(self, dt):
                return self.__offset
            def tzname(self, dt):
                return self.__name
            def dst(self, dt):
                return None
        def __str__(self):
            settings = {
                'str_pretty_print': True,
                'str_indent_level': 0,
                'str_namespaceprefix': '',
                'str_name': self.__class__.__name__,
                'str_namespacedefs': '',
            }
            for n in settings:
                if hasattr(self, n):
                    settings[n] = getattr(self, n)
            if sys.version_info.major == 2:
                from StringIO import StringIO
            else:
                from io import StringIO
            output = StringIO()
            self.export(
                output,
                settings['str_indent_level'],
                pretty_print=settings['str_pretty_print'],
                namespaceprefix_=settings['str_namespaceprefix'],
                name_=settings['str_name'],
                namespacedef_=settings['str_namespacedefs']
            )
            strval = output.getvalue()
            output.close()
            return strval
        def gds_format_string(self, input_data, input_name=''):
            return input_data
        def gds_parse_string(self, input_data, node=None, input_name=''):
            return input_data
        def gds_validate_string(self, input_data, node=None, input_name=''):
            if not input_data:
                return ''
            else:
                return input_data
        def gds_format_base64(self, input_data, input_name=''):
            return base64.b64encode(input_data).decode('ascii')
        def gds_validate_base64(self, input_data, node=None, input_name=''):
            return input_data
        def gds_format_integer(self, input_data, input_name=''):
            return '%d' % int(input_data)
        def gds_parse_integer(self, input_data, node=None, input_name=''):
            try:
                ival = int(input_data)
            except (TypeError, ValueError) as exp:
                raise_parse_error(node, 'Requires integer value: %s' % exp)
            return ival
        def gds_validate_integer(self, input_data, node=None, input_name=''):
            try:
                value = int(input_data)
            except (TypeError, ValueError):
                raise_parse_error(node, 'Requires integer value')
            return value
        def gds_format_integer_list(self, input_data, input_name=''):
            if len(input_data) > 0 and not isinstance(input_data[0], BaseStrType_):
                input_data = [str(s) for s in input_data]
            return '%s' % ' '.join(input_data)
        def gds_validate_integer_list(
                self, input_data, node=None, input_name=''):
            values = input_data.split()
            for value in values:
                try:
                    int(value)
                except (TypeError, ValueError):
                    raise_parse_error(node, 'Requires sequence of integer values')
            return values
        def gds_format_float(self, input_data, input_name=''):
            value = ('%.15f' % float(input_data)).rstrip('0')
            if value.endswith('.'):
                value += '0'
            return value
    
        def gds_parse_float(self, input_data, node=None, input_name=''):
            try:
                fval_ = float(input_data)
            except (TypeError, ValueError) as exp:
                raise_parse_error(node, 'Requires float or double value: %s' % exp)
            return fval_
        def gds_validate_float(self, input_data, node=None, input_name=''):
            try:
                value = float(input_data)
            except (TypeError, ValueError):
                raise_parse_error(node, 'Requires float value')
            return value
        def gds_format_float_list(self, input_data, input_name=''):
            if len(input_data) > 0 and not isinstance(input_data[0], BaseStrType_):
                input_data = [str(s) for s in input_data]
            return '%s' % ' '.join(input_data)
        def gds_validate_float_list(
                self, input_data, node=None, input_name=''):
            values = input_data.split()
            for value in values:
                try:
                    float(value)
                except (TypeError, ValueError):
                    raise_parse_error(node, 'Requires sequence of float values')
            return values
        def gds_format_decimal(self, input_data, input_name=''):
            return_value = '%s' % input_data
            if '.' in return_value:
                return_value = return_value.rstrip('0')
                if return_value.endswith('.'):
                    return_value = return_value.rstrip('.')
            return return_value
        def gds_parse_decimal(self, input_data, node=None, input_name=''):
            try:
                decimal_value = decimal_.Decimal(input_data)
            except (TypeError, ValueError):
                raise_parse_error(node, 'Requires decimal value')
            return decimal_value
        def gds_validate_decimal(self, input_data, node=None, input_name=''):
            try:
                value = decimal_.Decimal(input_data)
            except (TypeError, ValueError):
                raise_parse_error(node, 'Requires decimal value')
            return value
        def gds_format_decimal_list(self, input_data, input_name=''):
            if len(input_data) > 0 and not isinstance(input_data[0], BaseStrType_):
                input_data = [str(s) for s in input_data]
            return ' '.join([self.gds_format_decimal(item) for item in input_data])
        def gds_validate_decimal_list(
                self, input_data, node=None, input_name=''):
            values = input_data.split()
            for value in values:
                try:
                    decimal_.Decimal(value)
                except (TypeError, ValueError):
                    raise_parse_error(node, 'Requires sequence of decimal values')
            return values
        def gds_format_double(self, input_data, input_name=''):
            return '%s' % input_data
        def gds_parse_double(self, input_data, node=None, input_name=''):
            try:
                fval_ = float(input_data)
            except (TypeError, ValueError) as exp:
                raise_parse_error(node, 'Requires double or float value: %s' % exp)
            return fval_
        def gds_validate_double(self, input_data, node=None, input_name=''):
            try:
                value = float(input_data)
            except (TypeError, ValueError):
                raise_parse_error(node, 'Requires double or float value')
            return value
        def gds_format_double_list(self, input_data, input_name=''):
            if len(input_data) > 0 and not isinstance(input_data[0], BaseStrType_):
                input_data = [str(s) for s in input_data]
            return '%s' % ' '.join(input_data)
        def gds_validate_double_list(
                self, input_data, node=None, input_name=''):
            values = input_data.split()
            for value in values:
                try:
                    float(value)
                except (TypeError, ValueError):
                    raise_parse_error(
                        node, 'Requires sequence of double or float values')
            return values
        def gds_format_boolean(self, input_data, input_name=''):
            return ('%s' % input_data).lower()
        def gds_parse_boolean(self, input_data, node=None, input_name=''):
            input_data = input_data.strip()
            if input_data in ('true', '1'):
                bval = True
            elif input_data in ('false', '0'):
                bval = False
            else:
                raise_parse_error(node, 'Requires boolean value')
            return bval
        def gds_validate_boolean(self, input_data, node=None, input_name=''):
            if input_data not in (True, 1, False, 0, ):
                raise_parse_error(
                    node,
                    'Requires boolean value '
                    '(one of True, 1, False, 0)')
            return input_data
        def gds_format_boolean_list(self, input_data, input_name=''):
            if len(input_data) > 0 and not isinstance(input_data[0], BaseStrType_):
                input_data = [str(s) for s in input_data]
            return '%s' % ' '.join(input_data)
        def gds_validate_boolean_list(
                self, input_data, node=None, input_name=''):
            values = input_data.split()
            for value in values:
                value = self.gds_parse_boolean(value, node, input_name)
                if value not in (True, 1, False, 0, ):
                    raise_parse_error(
                        node,
                        'Requires sequence of boolean values '
                        '(one of True, 1, False, 0)')
            return values
        def gds_validate_datetime(self, input_data, node=None, input_name=''):
            return input_data
        def gds_format_datetime(self, input_data, input_name=''):
            if input_data.microsecond == 0:
                _svalue = '%04d-%02d-%02dT%02d:%02d:%02d' % (
                    input_data.year,
                    input_data.month,
                    input_data.day,
                    input_data.hour,
                    input_data.minute,
                    input_data.second,
                )
            else:
                _svalue = '%04d-%02d-%02dT%02d:%02d:%02d.%s' % (
                    input_data.year,
                    input_data.month,
                    input_data.day,
                    input_data.hour,
                    input_data.minute,
                    input_data.second,
                    ('%f' % (float(input_data.microsecond) / 1000000))[2:],
                )
            if input_data.tzinfo is not None:
                tzoff = input_data.tzinfo.utcoffset(input_data)
                if tzoff is not None:
                    total_seconds = tzoff.seconds + (86400 * tzoff.days)
                    if total_seconds == 0:
                        _svalue += 'Z'
                    else:
                        if total_seconds < 0:
                            _svalue += '-'
                            total_seconds *= -1
                        else:
                            _svalue += '+'
                        hours = total_seconds // 3600
                        minutes = (total_seconds - (hours * 3600)) // 60
                        _svalue += '{0:02d}:{1:02d}'.format(hours, minutes)
            return _svalue
        @classmethod
        def gds_parse_datetime(cls, input_data):
            tz = None
            if input_data[-1] == 'Z':
                tz = GeneratedsSuper._FixedOffsetTZ(0, 'UTC')
                input_data = input_data[:-1]
            else:
                results = GeneratedsSuper.tzoff_pattern.search(input_data)
                if results is not None:
                    tzoff_parts = results.group(2).split(':')
                    tzoff = int(tzoff_parts[0]) * 60 + int(tzoff_parts[1])
                    if results.group(1) == '-':
                        tzoff *= -1
                    tz = GeneratedsSuper._FixedOffsetTZ(
                        tzoff, results.group(0))
                    input_data = input_data[:-6]
            time_parts = input_data.split('.')
            if len(time_parts) > 1:
                micro_seconds = int(float('0.' + time_parts[1]) * 1000000)
                input_data = '%s.%s' % (
                    time_parts[0], "{}".format(micro_seconds).rjust(6, "0"), )
                dt = datetime_.datetime.strptime(
                    input_data, '%Y-%m-%dT%H:%M:%S.%f')
            else:
                dt = datetime_.datetime.strptime(
                    input_data, '%Y-%m-%dT%H:%M:%S')
            dt = dt.replace(tzinfo=tz)
            return dt
        def gds_validate_date(self, input_data, node=None, input_name=''):
            return input_data
        def gds_format_date(self, input_data, input_name=''):
            _svalue = '%04d-%02d-%02d' % (
                input_data.year,
                input_data.month,
                input_data.day,
            )
            try:
                if input_data.tzinfo is not None:
                    tzoff = input_data.tzinfo.utcoffset(input_data)
                    if tzoff is not None:
                        total_seconds = tzoff.seconds + (86400 * tzoff.days)
                        if total_seconds == 0:
                            _svalue += 'Z'
                        else:
                            if total_seconds < 0:
                                _svalue += '-'
                                total_seconds *= -1
                            else:
                                _svalue += '+'
                            hours = total_seconds // 3600
                            minutes = (total_seconds - (hours * 3600)) // 60
                            _svalue += '{0:02d}:{1:02d}'.format(
                                hours, minutes)
            except AttributeError:
                pass
            return _svalue
        @classmethod
        def gds_parse_date(cls, input_data):
            tz = None
            if input_data[-1] == 'Z':
                tz = GeneratedsSuper._FixedOffsetTZ(0, 'UTC')
                input_data = input_data[:-1]
            else:
                results = GeneratedsSuper.tzoff_pattern.search(input_data)
                if results is not None:
                    tzoff_parts = results.group(2).split(':')
                    tzoff = int(tzoff_parts[0]) * 60 + int(tzoff_parts[1])
                    if results.group(1) == '-':
                        tzoff *= -1
                    tz = GeneratedsSuper._FixedOffsetTZ(
                        tzoff, results.group(0))
                    input_data = input_data[:-6]
            dt = datetime_.datetime.strptime(input_data, '%Y-%m-%d')
            dt = dt.replace(tzinfo=tz)
            return dt.date()
        def gds_validate_time(self, input_data, node=None, input_name=''):
            return input_data
        def gds_format_time(self, input_data, input_name=''):
            if input_data.microsecond == 0:
                _svalue = '%02d:%02d:%02d' % (
                    input_data.hour,
                    input_data.minute,
                    input_data.second,
                )
            else:
                _svalue = '%02d:%02d:%02d.%s' % (
                    input_data.hour,
                    input_data.minute,
                    input_data.second,
                    ('%f' % (float(input_data.microsecond) / 1000000))[2:],
                )
            if input_data.tzinfo is not None:
                tzoff = input_data.tzinfo.utcoffset(input_data)
                if tzoff is not None:
                    total_seconds = tzoff.seconds + (86400 * tzoff.days)
                    if total_seconds == 0:
                        _svalue += 'Z'
                    else:
                        if total_seconds < 0:
                            _svalue += '-'
                            total_seconds *= -1
                        else:
                            _svalue += '+'
                        hours = total_seconds // 3600
                        minutes = (total_seconds - (hours * 3600)) // 60
                        _svalue += '{0:02d}:{1:02d}'.format(hours, minutes)
            return _svalue
        def gds_validate_simple_patterns(self, patterns, target):
            # pat is a list of lists of strings/patterns.
            # The target value must match at least one of the patterns
            # in order for the test to succeed.
            found1 = True
            target = str(target)
            for patterns1 in patterns:
                found2 = False
                for patterns2 in patterns1:
                    mo = re_.search(patterns2, target)
                    if mo is not None and len(mo.group(0)) == len(target):
                        found2 = True
                        break
                if not found2:
                    found1 = False
                    break
            return found1
        @classmethod
        def gds_parse_time(cls, input_data):
            tz = None
            if input_data[-1] == 'Z':
                tz = GeneratedsSuper._FixedOffsetTZ(0, 'UTC')
                input_data = input_data[:-1]
            else:
                results = GeneratedsSuper.tzoff_pattern.search(input_data)
                if results is not None:
                    tzoff_parts = results.group(2).split(':')
                    tzoff = int(tzoff_parts[0]) * 60 + int(tzoff_parts[1])
                    if results.group(1) == '-':
                        tzoff *= -1
                    tz = GeneratedsSuper._FixedOffsetTZ(
                        tzoff, results.group(0))
                    input_data = input_data[:-6]
            if len(input_data.split('.')) > 1:
                dt = datetime_.datetime.strptime(input_data, '%H:%M:%S.%f')
            else:
                dt = datetime_.datetime.strptime(input_data, '%H:%M:%S')
            dt = dt.replace(tzinfo=tz)
            return dt.time()
        def gds_check_cardinality_(
                self, value, input_name,
                min_occurs=0, max_occurs=1, required=None):
            if value is None:
                length = 0
            elif isinstance(value, list):
                length = len(value)
            else:
                length = 1
            if required is not None :
                if required and length < 1:
                    self.gds_collector_.add_message(
                        "Required value {}{} is missing".format(
                            input_name, self.gds_get_node_lineno_()))
            if length < min_occurs:
                self.gds_collector_.add_message(
                    "Number of values for {}{} is below "
                    "the minimum allowed, "
                    "expected at least {}, found {}".format(
                        input_name, self.gds_get_node_lineno_(),
                        min_occurs, length))
            elif length > max_occurs:
                self.gds_collector_.add_message(
                    "Number of values for {}{} is above "
                    "the maximum allowed, "
                    "expected at most {}, found {}".format(
                        input_name, self.gds_get_node_lineno_(),
                        max_occurs, length))
        def gds_validate_builtin_ST_(
                self, validator, value, input_name,
                min_occurs=None, max_occurs=None, required=None):
            if value is not None:
                try:
                    validator(value, input_name=input_name)
                except GDSParseError as parse_error:
                    self.gds_collector_.add_message(str(parse_error))
        def gds_validate_defined_ST_(
                self, validator, value, input_name,
                min_occurs=None, max_occurs=None, required=None):
            if value is not None:
                try:
                    validator(value)
                except GDSParseError as parse_error:
                    self.gds_collector_.add_message(str(parse_error))
        def gds_str_lower(self, instring):
            return instring.lower()
        def get_path_(self, node):
            path_list = []
            self.get_path_list_(node, path_list)
            path_list.reverse()
            path = '/'.join(path_list)
            return path
        Tag_strip_pattern_ = re_.compile(r'{.*}')
        def get_path_list_(self, node, path_list):
            if node is None:
                return
            tag = GeneratedsSuper.Tag_strip_pattern_.sub('', node.tag)
            if tag:
                path_list.append(tag)
            self.get_path_list_(node.getparent(), path_list)
        def get_class_obj_(self, node, default_class=None):
            class_obj1 = default_class
            if 'xsi' in node.nsmap:
                classname = node.get('{%s}type' % node.nsmap['xsi'])
                if classname is not None:
                    names = classname.split(':')
                    if len(names) == 2:
                        classname = names[1]
                    class_obj2 = globals().get(classname)
                    if class_obj2 is not None:
                        class_obj1 = class_obj2
            return class_obj1
        def gds_build_any(self, node, type_name=None):
            # provide default value in case option --disable-xml is used.
            content = ""
            content = etree_.tostring(node, encoding="unicode")
            return content
        @classmethod
        def gds_reverse_node_mapping(cls, mapping):
            return dict(((v, k) for k, v in mapping.items()))
        @staticmethod
        def gds_encode(instring):
            if sys.version_info.major == 2:
                if ExternalEncoding:
                    encoding = ExternalEncoding
                else:
                    encoding = 'utf-8'
                return instring.encode(encoding)
            else:
                return instring
        @staticmethod
        def convert_unicode(instring):
            if isinstance(instring, str):
                result = quote_xml(instring)
            elif sys.version_info.major == 2 and isinstance(instring, unicode):
                result = quote_xml(instring).encode('utf8')
            else:
                result = GeneratedsSuper.gds_encode(str(instring))
            return result
        def __eq__(self, other):
            def excl_select_objs_(obj):
                return (obj[0] != 'parent_object_' and
                        obj[0] != 'gds_collector_')
            if type(self) != type(other):
                return False
            return all(x == y for x, y in zip_longest(
                filter(excl_select_objs_, self.__dict__.items()),
                filter(excl_select_objs_, other.__dict__.items())))
        def __ne__(self, other):
            return not self.__eq__(other)
        # Django ETL transform hooks.
        def gds_djo_etl_transform(self):
            pass
        def gds_djo_etl_transform_db_obj(self, dbobj):
            pass
        # SQLAlchemy ETL transform hooks.
        def gds_sqa_etl_transform(self):
            return 0, None
        def gds_sqa_etl_transform_db_obj(self, dbobj):
            pass
        def gds_get_node_lineno_(self):
            if (hasattr(self, "gds_elementtree_node_") and
                    self.gds_elementtree_node_ is not None):
                return ' near line {}'.format(
                    self.gds_elementtree_node_.sourceline)
            else:
                return ""
    
    
    def getSubclassFromModule_(module, class_):
        '''Get the subclass of a class from a specific module.'''
        name = class_.__name__ + 'Sub'
        if hasattr(module, name):
            return getattr(module, name)
        else:
            return None


#
# If you have installed IPython you can uncomment and use the following.
# IPython is available from http://ipython.scipy.org/.
#

## from IPython.Shell import IPShellEmbed
## args = ''
## ipshell = IPShellEmbed(args,
##     banner = 'Dropping into IPython',
##     exit_msg = 'Leaving Interpreter, back to program.')

# Then use the following line where and when you want to drop into the
# IPython shell:
#    ipshell('<some message> -- Entering ipshell.\nHit Ctrl-D to exit')

#
# Globals
#

ExternalEncoding = ''
# Set this to false in order to deactivate during export, the use of
# name space prefixes captured from the input document.
UseCapturedNS_ = True
CapturedNsmap_ = {}
Tag_pattern_ = re_.compile(r'({.*})?(.*)')
String_cleanup_pat_ = re_.compile(r"[\n\r\s]+")
Namespace_extract_pat_ = re_.compile(r'{(.*)}(.*)')
CDATA_pattern_ = re_.compile(r"<!\[CDATA\[.*?\]\]>", re_.DOTALL)

# Change this to redirect the generated superclass module to use a
# specific subclass module.
CurrentSubclassModule_ = None

#
# Support/utility functions.
#


def showIndent(outfile, level, pretty_print=True):
    if pretty_print:
        for idx in range(level):
            outfile.write('    ')


def quote_xml(inStr):
    "Escape markup chars, but do not modify CDATA sections."
    if not inStr:
        return ''
    s1 = (isinstance(inStr, BaseStrType_) and inStr or '%s' % inStr)
    s2 = ''
    pos = 0
    matchobjects = CDATA_pattern_.finditer(s1)
    for mo in matchobjects:
        s3 = s1[pos:mo.start()]
        s2 += quote_xml_aux(s3)
        s2 += s1[mo.start():mo.end()]
        pos = mo.end()
    s3 = s1[pos:]
    s2 += quote_xml_aux(s3)
    return s2


def quote_xml_aux(inStr):
    s1 = inStr.replace('&', '&amp;')
    s1 = s1.replace('<', '&lt;')
    s1 = s1.replace('>', '&gt;')
    return s1


def quote_attrib(inStr):
    s1 = (isinstance(inStr, BaseStrType_) and inStr or '%s' % inStr)
    s1 = s1.replace('&', '&amp;')
    s1 = s1.replace('<', '&lt;')
    s1 = s1.replace('>', '&gt;')
    s1 = s1.replace('\n', '&#10;')
    if '"' in s1:
        if "'" in s1:
            s1 = '"%s"' % s1.replace('"', "&quot;")
        else:
            s1 = "'%s'" % s1
    else:
        s1 = '"%s"' % s1
    return s1


def quote_python(inStr):
    s1 = inStr
    if s1.find("'") == -1:
        if s1.find('\n') == -1:
            return "'%s'" % s1
        else:
            return "'''%s'''" % s1
    else:
        if s1.find('"') != -1:
            s1 = s1.replace('"', '\\"')
        if s1.find('\n') == -1:
            return '"%s"' % s1
        else:
            return '"""%s"""' % s1


def get_all_text_(node):
    if node.text is not None:
        text = node.text
    else:
        text = ''
    for child in node:
        if child.tail is not None:
            text += child.tail
    return text


def find_attr_value_(attr_name, node):
    attrs = node.attrib
    attr_parts = attr_name.split(':')
    value = None
    if len(attr_parts) == 1:
        value = attrs.get(attr_name)
    elif len(attr_parts) == 2:
        prefix, name = attr_parts
        if prefix == 'xml':
            namespace = 'http://www.w3.org/XML/1998/namespace'
        else:
            namespace = node.nsmap.get(prefix)
        if namespace is not None:
            value = attrs.get('{%s}%s' % (namespace, name, ))
    return value


def encode_str_2_3(instr):
    return instr


class GDSParseError(Exception):
    pass


def raise_parse_error(node, msg):
    if node is not None:
        msg = '%s (element %s/line %d)' % (msg, node.tag, node.sourceline, )
    raise GDSParseError(msg)


class MixedContainer:
    # Constants for category:
    CategoryNone = 0
    CategoryText = 1
    CategorySimple = 2
    CategoryComplex = 3
    # Constants for content_type:
    TypeNone = 0
    TypeText = 1
    TypeString = 2
    TypeInteger = 3
    TypeFloat = 4
    TypeDecimal = 5
    TypeDouble = 6
    TypeBoolean = 7
    TypeBase64 = 8
    def __init__(self, category, content_type, name, value):
        self.category = category
        self.content_type = content_type
        self.name = name
        self.value = value
    def getCategory(self):
        return self.category
    def getContenttype(self, content_type):
        return self.content_type
    def getValue(self):
        return self.value
    def getName(self):
        return self.name
    def export(self, outfile, level, name, namespace,
               pretty_print=True):
        if self.category == MixedContainer.CategoryText:
            # Prevent exporting empty content as empty lines.
            if self.value.strip():
                outfile.write(self.value)
        elif self.category == MixedContainer.CategorySimple:
            self.exportSimple(outfile, level, name)
        else:    # category == MixedContainer.CategoryComplex
            self.value.export(
                outfile, level, namespace, name_=name,
                pretty_print=pretty_print)
    def exportSimple(self, outfile, level, name):
        if self.content_type == MixedContainer.TypeString:
            outfile.write('<%s>%s</%s>' % (
                self.name, self.value, self.name))
        elif self.content_type == MixedContainer.TypeInteger or \
                self.content_type == MixedContainer.TypeBoolean:
            outfile.write('<%s>%d</%s>' % (
                self.name, self.value, self.name))
        elif self.content_type == MixedContainer.TypeFloat or \
                self.content_type == MixedContainer.TypeDecimal:
            outfile.write('<%s>%f</%s>' % (
                self.name, self.value, self.name))
        elif self.content_type == MixedContainer.TypeDouble:
            outfile.write('<%s>%g</%s>' % (
                self.name, self.value, self.name))
        elif self.content_type == MixedContainer.TypeBase64:
            outfile.write('<%s>%s</%s>' % (
                self.name,
                base64.b64encode(self.value),
                self.name))
    def to_etree(self, element, mapping_=None, reverse_mapping_=None, nsmap_=None):
        if self.category == MixedContainer.CategoryText:
            # Prevent exporting empty content as empty lines.
            if self.value.strip():
                if len(element) > 0:
                    if element[-1].tail is None:
                        element[-1].tail = self.value
                    else:
                        element[-1].tail += self.value
                else:
                    if element.text is None:
                        element.text = self.value
                    else:
                        element.text += self.value
        elif self.category == MixedContainer.CategorySimple:
            subelement = etree_.SubElement(
                element, '%s' % self.name)
            subelement.text = self.to_etree_simple()
        else:    # category == MixedContainer.CategoryComplex
            self.value.to_etree(element)
    def to_etree_simple(self, mapping_=None, reverse_mapping_=None, nsmap_=None):
        if self.content_type == MixedContainer.TypeString:
            text = self.value
        elif (self.content_type == MixedContainer.TypeInteger or
                self.content_type == MixedContainer.TypeBoolean):
            text = '%d' % self.value
        elif (self.content_type == MixedContainer.TypeFloat or
                self.content_type == MixedContainer.TypeDecimal):
            text = '%f' % self.value
        elif self.content_type == MixedContainer.TypeDouble:
            text = '%g' % self.value
        elif self.content_type == MixedContainer.TypeBase64:
            text = '%s' % base64.b64encode(self.value)
        return text
    def exportLiteral(self, outfile, level, name):
        if self.category == MixedContainer.CategoryText:
            showIndent(outfile, level)
            outfile.write(
                'model_.MixedContainer(%d, %d, "%s", "%s"),\n' % (
                    self.category, self.content_type,
                    self.name, self.value))
        elif self.category == MixedContainer.CategorySimple:
            showIndent(outfile, level)
            outfile.write(
                'model_.MixedContainer(%d, %d, "%s", "%s"),\n' % (
                    self.category, self.content_type,
                    self.name, self.value))
        else:    # category == MixedContainer.CategoryComplex
            showIndent(outfile, level)
            outfile.write(
                'model_.MixedContainer(%d, %d, "%s",\n' % (
                    self.category, self.content_type, self.name,))
            self.value.exportLiteral(outfile, level + 1)
            showIndent(outfile, level)
            outfile.write(')\n')


class MemberSpec_(object):
    def __init__(self, name='', data_type='', container=0,
            optional=0, child_attrs=None, choice=None):
        self.name = name
        self.data_type = data_type
        self.container = container
        self.child_attrs = child_attrs
        self.choice = choice
        self.optional = optional
    def set_name(self, name): self.name = name
    def get_name(self): return self.name
    def set_data_type(self, data_type): self.data_type = data_type
    def get_data_type_chain(self): return self.data_type
    def get_data_type(self):
        if isinstance(self.data_type, list):
            if len(self.data_type) > 0:
                return self.data_type[-1]
            else:
                return 'xs:string'
        else:
            return self.data_type
    def set_container(self, container): self.container = container
    def get_container(self): return self.container
    def set_child_attrs(self, child_attrs): self.child_attrs = child_attrs
    def get_child_attrs(self): return self.child_attrs
    def set_choice(self, choice): self.choice = choice
    def get_choice(self): return self.choice
    def set_optional(self, optional): self.optional = optional
    def get_optional(self): return self.optional


def _cast(typ, value):
    if typ is None or value is None:
        return value
    return typ(value)


#
# Start enum classes
#
class AssessmentCategoryEnum(str, Enum):
    """AssessmentCategoryEnum --
    Bed
    ö
    mningskategori.
    Beskriver vilken kategori av bed
    ö
    mning som
    ä
    r gjord. Till
    å
    tna v
    ä
    rden
    ä
    r "pad-pad" (f
    ö
    r PADL-bed
    ö
    mning) och "fun-fun" (f
    ö
    r funktionsneds
    ä
    ttningsbed
    ö
    mningar).
    
    """
    PADPAD='pad-pad' # PADL-bedömning
    FUNFUN='fun-fun' # funktionsnedsättningsbedömningar


class ClinicalDocumentNoteCodeEnum(str, Enum):
    """ClinicalDocumentNoteCodeEnum --
    Kod f
    ö
    r typ av anteckning.
    Till
    å
    tna v
    ä
    rden fr
    å
    n kodverk Anteckningstyp (1.2.752.129.2.2.2.11):
    
    """
    UTR='utr' # Utredning
    ATB='atb' # Åtgärd/Behandling
    SAM='sam' # Sammanfattning
    SAO='sao' # Samordning
    INS='ins' # Inskrivning
    SLU='slu' # Slutanteckning
    AUF='auf' # Anteckning utan fysiskt möte
    SVA='sva' # Slutenvårdsanteckning
    BES='bes' # Besöksanteckning


class ClinicalDocumentTypeCodeEnum(str, Enum):
    """ClinicalDocumentTypeCodeEnum --
    Kod f
    ö
    r typ av anteckning.
    Till
    å
    tna v
    ä
    rden fr
    å
    n kodverk (?.?.?.?.?.?):
    
    """
    EPI='epi' # Epikris
    INT='int' # Intagninganteckning
    DAG='dag' # Daganteckning
    OVA='ova' # Öppenvårdsanteckning
    OVS='ovs' # Öppenvårdssammanfattning
    OVR='ovr' # Övrigt dokument


class DiagnosisTypeEnum(str, Enum):
    """DiagnosisTypeEnum --
    Anges som "Huvuddiagnos", "Bidiagnos".
    
    """
    HUVUDDIAGNOS='Huvuddiagnos' # Huvuddiagnos
    BIDIAGNOS='Bidiagnos' # Bidiagnos


class ErrorCodeEnum(str, Enum):
    INVALID_REQUEST='INVALID_REQUEST'


class MediaTypeEnum(str, Enum):
    APPLICATIONDICOM='application/dicom'
    APPLICATIONMSWORD='application/msword'
    APPLICATIONPDF='application/pdf'
    AUDIOBASIC='audio/basic'
    AUDIOK_32_ADPCM='audio/k32adpcm'
    AUDIOMPEG='audio/mpeg'
    IMAGEG_3_FAX='image/g3fax'
    IMAGEGIF='image/gif'
    IMAGEJPEG='image/jpeg'
    IMAGEPNG='image/png'
    IMAGETIFF='image/tiff'
    MODELVRML='model/vrml'
    MULTIPARTXHL_7CDALEVEL_1='multipart/x-hl7-cda-level1'
    TEXTHTML='text/html'
    TEXTPLAIN='text/plain'
    TEXTRTF='text/rtf'
    TEXTSGML='text/sgml'
    TEXTXHL_7FT='text/x-hl7-ft'
    TEXTXML='text/xml'
    VIDEOMPEG='video/mpeg'
    VIDEOXAVI='video/x-avi'


class ResultCodeEnum(str, Enum):
    OK='OK'
    ERROR='ERROR'
    INFO='INFO'


#
# Start data representation classes
#
class GetDiagnosisType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, careUnitHSAId=None, patientId=None, timePeriod=None, sourceSystemHSAId=None, careContactId=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "tns"
        if careUnitHSAId is None:
            self.careUnitHSAId = []
        else:
            self.careUnitHSAId = careUnitHSAId
        self.careUnitHSAId_nsprefix_ = "core"
        self.patientId = patientId
        self.patientId_nsprefix_ = "core"
        self.timePeriod = timePeriod
        self.timePeriod_nsprefix_ = "core"
        self.sourceSystemHSAId = sourceSystemHSAId
        self.validate_HSAIdType(self.sourceSystemHSAId)
        self.sourceSystemHSAId_nsprefix_ = "core"
        if careContactId is None:
            self.careContactId = []
        else:
            self.careContactId = careContactId
        self.careContactId_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, GetDiagnosisType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if GetDiagnosisType.subclass:
            return GetDiagnosisType.subclass(*args_, **kwargs_)
        else:
            return GetDiagnosisType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_careUnitHSAId(self):
        return self.careUnitHSAId
    def set_careUnitHSAId(self, careUnitHSAId):
        self.careUnitHSAId = careUnitHSAId
    def add_careUnitHSAId(self, value):
        self.careUnitHSAId.append(value)
    def insert_careUnitHSAId_at(self, index, value):
        self.careUnitHSAId.insert(index, value)
    def replace_careUnitHSAId_at(self, index, value):
        self.careUnitHSAId[index] = value
    def get_patientId(self):
        return self.patientId
    def set_patientId(self, patientId):
        self.patientId = patientId
    def get_timePeriod(self):
        return self.timePeriod
    def set_timePeriod(self, timePeriod):
        self.timePeriod = timePeriod
    def get_sourceSystemHSAId(self):
        return self.sourceSystemHSAId
    def set_sourceSystemHSAId(self, sourceSystemHSAId):
        self.sourceSystemHSAId = sourceSystemHSAId
    def get_careContactId(self):
        return self.careContactId
    def set_careContactId(self, careContactId):
        self.careContactId = careContactId
    def add_careContactId(self, value):
        self.careContactId.append(value)
    def insert_careContactId_at(self, index, value):
        self.careContactId.insert(index, value)
    def replace_careContactId_at(self, index, value):
        self.careContactId[index] = value
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def validate_HSAIdType(self, value):
        result = True
        # Validate type HSAIdType, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            pass
        return result
    def has__content(self):
        if (
            self.careUnitHSAId or
            self.patientId is not None or
            self.timePeriod is not None or
            self.sourceSystemHSAId is not None or
            self.careContactId or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='', namespacedef_='xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" ', name_='GetDiagnosisType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('GetDiagnosisType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'GetDiagnosisType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='GetDiagnosisType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='GetDiagnosisType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='', name_='GetDiagnosisType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='', namespacedef_='xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" ', name_='GetDiagnosisType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        for careUnitHSAId_ in self.careUnitHSAId:
            namespaceprefix_ = self.careUnitHSAId_nsprefix_ + ':' if (UseCapturedNS_ and self.careUnitHSAId_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%scareUnitHSAId>%s</%scareUnitHSAId>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(careUnitHSAId_), input_name='careUnitHSAId')), namespaceprefix_ , eol_))
        if self.patientId is not None:
            namespaceprefix_ = self.patientId_nsprefix_ + ':' if (UseCapturedNS_ and self.patientId_nsprefix_) else ''
            self.patientId.export(outfile, level, namespaceprefix_, namespacedef_='', name_='patientId', pretty_print=pretty_print)
        if self.timePeriod is not None:
            namespaceprefix_ = self.timePeriod_nsprefix_ + ':' if (UseCapturedNS_ and self.timePeriod_nsprefix_) else ''
            self.timePeriod.export(outfile, level, namespaceprefix_, namespacedef_='', name_='timePeriod', pretty_print=pretty_print)
        if self.sourceSystemHSAId is not None:
            namespaceprefix_ = self.sourceSystemHSAId_nsprefix_ + ':' if (UseCapturedNS_ and self.sourceSystemHSAId_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%ssourceSystemHSAId>%s</%ssourceSystemHSAId>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.sourceSystemHSAId), input_name='sourceSystemHSAId')), namespaceprefix_ , eol_))
        for careContactId_ in self.careContactId:
            namespaceprefix_ = self.careContactId_nsprefix_ + ':' if (UseCapturedNS_ and self.careContactId_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%scareContactId>%s</%scareContactId>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(careContactId_), input_name='careContactId')), namespaceprefix_ , eol_))
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'careUnitHSAId':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'careUnitHSAId')
            value_ = self.gds_validate_string(value_, node, 'careUnitHSAId')
            self.careUnitHSAId.append(value_)
            self.careUnitHSAId_nsprefix_ = child_.prefix
            # validate type HSAIdType
            self.validate_HSAIdType(self.careUnitHSAId[-1])
        elif nodeName_ == 'patientId':
            obj_ = PersonIdType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.patientId = obj_
            obj_.original_tagname_ = 'patientId'
        elif nodeName_ == 'timePeriod':
            obj_ = DatePeriodType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.timePeriod = obj_
            obj_.original_tagname_ = 'timePeriod'
        elif nodeName_ == 'sourceSystemHSAId':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'sourceSystemHSAId')
            value_ = self.gds_validate_string(value_, node, 'sourceSystemHSAId')
            self.sourceSystemHSAId = value_
            self.sourceSystemHSAId_nsprefix_ = child_.prefix
            # validate type HSAIdType
            self.validate_HSAIdType(self.sourceSystemHSAId)
        elif nodeName_ == 'careContactId':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'careContactId')
            value_ = self.gds_validate_string(value_, node, 'careContactId')
            self.careContactId.append(value_)
            self.careContactId_nsprefix_ = child_.prefix
        else:
            content_ = self.gds_build_any(child_, 'GetDiagnosisType')
            self.anytypeobjs_.append(content_)
# end class GetDiagnosisType


class GetDiagnosisResponseType(GeneratedsSuper):
    """GetDiagnosisResponseType --
    careDocumentation:
    De v
    å
    rddokument som matchar beg
    ä
    ran.
    
    """
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, diagnosis=None, result=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "tns"
        if diagnosis is None:
            self.diagnosis = []
        else:
            self.diagnosis = diagnosis
        self.diagnosis_nsprefix_ = "core"
        self.result = result
        self.result_nsprefix_ = "core"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, GetDiagnosisResponseType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if GetDiagnosisResponseType.subclass:
            return GetDiagnosisResponseType.subclass(*args_, **kwargs_)
        else:
            return GetDiagnosisResponseType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_diagnosis(self):
        return self.diagnosis
    def set_diagnosis(self, diagnosis):
        self.diagnosis = diagnosis
    def add_diagnosis(self, value):
        self.diagnosis.append(value)
    def insert_diagnosis_at(self, index, value):
        self.diagnosis.insert(index, value)
    def replace_diagnosis_at(self, index, value):
        self.diagnosis[index] = value
    def get_result(self):
        return self.result
    def set_result(self, result):
        self.result = result
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.diagnosis or
            self.result is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='', namespacedef_='xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" ', name_='GetDiagnosisResponseType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('GetDiagnosisResponseType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'GetDiagnosisResponseType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='GetDiagnosisResponseType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='GetDiagnosisResponseType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='', name_='GetDiagnosisResponseType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='', namespacedef_='xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" ', name_='GetDiagnosisResponseType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        for diagnosis_ in self.diagnosis:
            namespaceprefix_ = self.diagnosis_nsprefix_ + ':' if (UseCapturedNS_ and self.diagnosis_nsprefix_) else ''
            diagnosis_.export(outfile, level, namespaceprefix_, namespacedef_='', name_='diagnosis', pretty_print=pretty_print)
        if self.result is not None:
            namespaceprefix_ = self.result_nsprefix_ + ':' if (UseCapturedNS_ and self.result_nsprefix_) else ''
            self.result.export(outfile, level, namespaceprefix_, namespacedef_='', name_='result', pretty_print=pretty_print)
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'diagnosis':
            obj_ = DiagnosisType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.diagnosis.append(obj_)
            obj_.original_tagname_ = 'diagnosis'
        elif nodeName_ == 'result':
            obj_ = ResultType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.result = obj_
            obj_.original_tagname_ = 'result'
        else:
            content_ = self.gds_build_any(child_, 'GetDiagnosisResponseType')
            self.anytypeobjs_.append(content_)
# end class GetDiagnosisResponseType


class CareDocumentationType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, careDocumentationHeader=None, careDocumentationBody=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.careDocumentationHeader = careDocumentationHeader
        self.careDocumentationHeader_nsprefix_ = "tns"
        self.careDocumentationBody = careDocumentationBody
        self.careDocumentationBody_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, CareDocumentationType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if CareDocumentationType.subclass:
            return CareDocumentationType.subclass(*args_, **kwargs_)
        else:
            return CareDocumentationType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_careDocumentationHeader(self):
        return self.careDocumentationHeader
    def set_careDocumentationHeader(self, careDocumentationHeader):
        self.careDocumentationHeader = careDocumentationHeader
    def get_careDocumentationBody(self):
        return self.careDocumentationBody
    def set_careDocumentationBody(self, careDocumentationBody):
        self.careDocumentationBody = careDocumentationBody
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.careDocumentationHeader is not None or
            self.careDocumentationBody is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='CareDocumentationType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('CareDocumentationType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'CareDocumentationType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='CareDocumentationType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='CareDocumentationType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='CareDocumentationType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='CareDocumentationType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.careDocumentationHeader is not None:
            namespaceprefix_ = self.careDocumentationHeader_nsprefix_ + ':' if (UseCapturedNS_ and self.careDocumentationHeader_nsprefix_) else ''
            self.careDocumentationHeader.export(outfile, level, namespaceprefix_, namespacedef_='', name_='careDocumentationHeader', pretty_print=pretty_print)
        if self.careDocumentationBody is not None:
            namespaceprefix_ = self.careDocumentationBody_nsprefix_ + ':' if (UseCapturedNS_ and self.careDocumentationBody_nsprefix_) else ''
            self.careDocumentationBody.export(outfile, level, namespaceprefix_, namespacedef_='', name_='careDocumentationBody', pretty_print=pretty_print)
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'careDocumentationHeader':
            obj_ = CPatientSummaryHeaderType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.careDocumentationHeader = obj_
            obj_.original_tagname_ = 'careDocumentationHeader'
        elif nodeName_ == 'careDocumentationBody':
            obj_ = CareDocumentationBodyType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.careDocumentationBody = obj_
            obj_.original_tagname_ = 'careDocumentationBody'
        else:
            content_ = self.gds_build_any(child_, 'CareDocumentationType')
            self.anytypeobjs_.append(content_)
# end class CareDocumentationType


class CareDocumentationBodyType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, clinicalDocumentNote=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.clinicalDocumentNote = clinicalDocumentNote
        self.clinicalDocumentNote_nsprefix_ = "tns"
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, CareDocumentationBodyType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if CareDocumentationBodyType.subclass:
            return CareDocumentationBodyType.subclass(*args_, **kwargs_)
        else:
            return CareDocumentationBodyType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_clinicalDocumentNote(self):
        return self.clinicalDocumentNote
    def set_clinicalDocumentNote(self, clinicalDocumentNote):
        self.clinicalDocumentNote = clinicalDocumentNote
    def has__content(self):
        if (
            self.clinicalDocumentNote is not None
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='CareDocumentationBodyType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('CareDocumentationBodyType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'CareDocumentationBodyType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='CareDocumentationBodyType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='CareDocumentationBodyType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='CareDocumentationBodyType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='CareDocumentationBodyType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.clinicalDocumentNote is not None:
            namespaceprefix_ = self.clinicalDocumentNote_nsprefix_ + ':' if (UseCapturedNS_ and self.clinicalDocumentNote_nsprefix_) else ''
            self.clinicalDocumentNote.export(outfile, level, namespaceprefix_, namespacedef_='', name_='clinicalDocumentNote', pretty_print=pretty_print)
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'clinicalDocumentNote':
            obj_ = ClinicalDocumentNoteType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.clinicalDocumentNote = obj_
            obj_.original_tagname_ = 'clinicalDocumentNote'
# end class CareDocumentationBodyType


class FunctionalStatusAssessmentType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, functionalStatusAssessmentHeader=None, functionalStatusAssessmentBody=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.functionalStatusAssessmentHeader = functionalStatusAssessmentHeader
        self.functionalStatusAssessmentHeader_nsprefix_ = "tns"
        self.functionalStatusAssessmentBody = functionalStatusAssessmentBody
        self.functionalStatusAssessmentBody_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, FunctionalStatusAssessmentType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if FunctionalStatusAssessmentType.subclass:
            return FunctionalStatusAssessmentType.subclass(*args_, **kwargs_)
        else:
            return FunctionalStatusAssessmentType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_functionalStatusAssessmentHeader(self):
        return self.functionalStatusAssessmentHeader
    def set_functionalStatusAssessmentHeader(self, functionalStatusAssessmentHeader):
        self.functionalStatusAssessmentHeader = functionalStatusAssessmentHeader
    def get_functionalStatusAssessmentBody(self):
        return self.functionalStatusAssessmentBody
    def set_functionalStatusAssessmentBody(self, functionalStatusAssessmentBody):
        self.functionalStatusAssessmentBody = functionalStatusAssessmentBody
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.functionalStatusAssessmentHeader is not None or
            self.functionalStatusAssessmentBody is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='FunctionalStatusAssessmentType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('FunctionalStatusAssessmentType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'FunctionalStatusAssessmentType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='FunctionalStatusAssessmentType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='FunctionalStatusAssessmentType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='FunctionalStatusAssessmentType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='FunctionalStatusAssessmentType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.functionalStatusAssessmentHeader is not None:
            namespaceprefix_ = self.functionalStatusAssessmentHeader_nsprefix_ + ':' if (UseCapturedNS_ and self.functionalStatusAssessmentHeader_nsprefix_) else ''
            self.functionalStatusAssessmentHeader.export(outfile, level, namespaceprefix_, namespacedef_='', name_='functionalStatusAssessmentHeader', pretty_print=pretty_print)
        if self.functionalStatusAssessmentBody is not None:
            namespaceprefix_ = self.functionalStatusAssessmentBody_nsprefix_ + ':' if (UseCapturedNS_ and self.functionalStatusAssessmentBody_nsprefix_) else ''
            self.functionalStatusAssessmentBody.export(outfile, level, namespaceprefix_, namespacedef_='', name_='functionalStatusAssessmentBody', pretty_print=pretty_print)
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'functionalStatusAssessmentHeader':
            obj_ = PatientSummaryHeaderType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.functionalStatusAssessmentHeader = obj_
            obj_.original_tagname_ = 'functionalStatusAssessmentHeader'
        elif nodeName_ == 'functionalStatusAssessmentBody':
            obj_ = FunctionalStatusAssessmentBodyType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.functionalStatusAssessmentBody = obj_
            obj_.original_tagname_ = 'functionalStatusAssessmentBody'
        else:
            content_ = self.gds_build_any(child_, 'FunctionalStatusAssessmentType')
            self.anytypeobjs_.append(content_)
# end class FunctionalStatusAssessmentType


class FunctionalStatusAssessmentBodyType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, assessmentCategory=None, comment=None, padl=None, disability=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.assessmentCategory = assessmentCategory
        self.validate_AssessmentCategoryEnum(self.assessmentCategory)
        self.assessmentCategory_nsprefix_ = "codes"
        self.comment = comment
        self.comment_nsprefix_ = "tns"
        if padl is None:
            self.padl = []
        else:
            self.padl = padl
        self.padl_nsprefix_ = "tns"
        self.disability = disability
        self.disability_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, FunctionalStatusAssessmentBodyType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if FunctionalStatusAssessmentBodyType.subclass:
            return FunctionalStatusAssessmentBodyType.subclass(*args_, **kwargs_)
        else:
            return FunctionalStatusAssessmentBodyType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_assessmentCategory(self):
        return self.assessmentCategory
    def set_assessmentCategory(self, assessmentCategory):
        self.assessmentCategory = assessmentCategory
    def get_comment(self):
        return self.comment
    def set_comment(self, comment):
        self.comment = comment
    def get_padl(self):
        return self.padl
    def set_padl(self, padl):
        self.padl = padl
    def add_padl(self, value):
        self.padl.append(value)
    def insert_padl_at(self, index, value):
        self.padl.insert(index, value)
    def replace_padl_at(self, index, value):
        self.padl[index] = value
    def get_disability(self):
        return self.disability
    def set_disability(self, disability):
        self.disability = disability
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def validate_AssessmentCategoryEnum(self, value):
        result = True
        # Validate type AssessmentCategoryEnum, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            value = value
            enumerations = ['pad-pad', 'fun-fun']
            if value not in enumerations:
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s does not match xsd enumeration restriction on AssessmentCategoryEnum' % {"value" : encode_str_2_3(value), "lineno": lineno} )
                result = False
        return result
    def has__content(self):
        if (
            self.assessmentCategory is not None or
            self.comment is not None or
            self.padl or
            self.disability is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:codes="urn:riv:clinicalprocess:healthcond:description:enums:2"  xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='FunctionalStatusAssessmentBodyType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('FunctionalStatusAssessmentBodyType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'FunctionalStatusAssessmentBodyType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='FunctionalStatusAssessmentBodyType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='FunctionalStatusAssessmentBodyType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='FunctionalStatusAssessmentBodyType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:codes="urn:riv:clinicalprocess:healthcond:description:enums:2"  xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='FunctionalStatusAssessmentBodyType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.assessmentCategory is not None:
            namespaceprefix_ = self.assessmentCategory_nsprefix_ + ':' if (UseCapturedNS_ and self.assessmentCategory_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sassessmentCategory>%s</%sassessmentCategory>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.assessmentCategory), input_name='assessmentCategory')), namespaceprefix_ , eol_))
        if self.comment is not None:
            namespaceprefix_ = self.comment_nsprefix_ + ':' if (UseCapturedNS_ and self.comment_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%scomment>%s</%scomment>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.comment), input_name='comment')), namespaceprefix_ , eol_))
        for padl_ in self.padl:
            namespaceprefix_ = self.padl_nsprefix_ + ':' if (UseCapturedNS_ and self.padl_nsprefix_) else ''
            padl_.export(outfile, level, namespaceprefix_, namespacedef_='', name_='padl', pretty_print=pretty_print)
        if self.disability is not None:
            namespaceprefix_ = self.disability_nsprefix_ + ':' if (UseCapturedNS_ and self.disability_nsprefix_) else ''
            self.disability.export(outfile, level, namespaceprefix_, namespacedef_='', name_='disability', pretty_print=pretty_print)
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'assessmentCategory':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'assessmentCategory')
            value_ = self.gds_validate_string(value_, node, 'assessmentCategory')
            self.assessmentCategory = value_
            self.assessmentCategory_nsprefix_ = child_.prefix
            # validate type AssessmentCategoryEnum
            self.validate_AssessmentCategoryEnum(self.assessmentCategory)
        elif nodeName_ == 'comment':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'comment')
            value_ = self.gds_validate_string(value_, node, 'comment')
            self.comment = value_
            self.comment_nsprefix_ = child_.prefix
        elif nodeName_ == 'padl':
            obj_ = PADLType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.padl.append(obj_)
            obj_.original_tagname_ = 'padl'
        elif nodeName_ == 'disability':
            obj_ = DisabilityType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.disability = obj_
            obj_.original_tagname_ = 'disability'
        else:
            content_ = self.gds_build_any(child_, 'FunctionalStatusAssessmentBodyType')
            self.anytypeobjs_.append(content_)
# end class FunctionalStatusAssessmentBodyType


class ClinicalDocumentNoteType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, clinicalDocumentNoteCode=None, clinicalDocumentTypeCode=None, clinicalDocumentNoteTitle=None, clinicalDocumentNoteText=None, multimediaEntry=None, dissentintOpinion=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.clinicalDocumentNoteCode = clinicalDocumentNoteCode
        self.validate_ClinicalDocumentNoteCodeEnum(self.clinicalDocumentNoteCode)
        self.clinicalDocumentNoteCode_nsprefix_ = "codes"
        self.clinicalDocumentTypeCode = clinicalDocumentTypeCode
        self.validate_ClinicalDocumentTypeCodeEnum(self.clinicalDocumentTypeCode)
        self.clinicalDocumentTypeCode_nsprefix_ = "codes"
        self.clinicalDocumentNoteTitle = clinicalDocumentNoteTitle
        self.clinicalDocumentNoteTitle_nsprefix_ = "tns"
        self.clinicalDocumentNoteText = clinicalDocumentNoteText
        self.clinicalDocumentNoteText_nsprefix_ = "tns"
        self.multimediaEntry = multimediaEntry
        self.multimediaEntry_nsprefix_ = "tns"
        if dissentintOpinion is None:
            self.dissentintOpinion = []
        else:
            self.dissentintOpinion = dissentintOpinion
        self.dissentintOpinion_nsprefix_ = "tns"
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, ClinicalDocumentNoteType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if ClinicalDocumentNoteType.subclass:
            return ClinicalDocumentNoteType.subclass(*args_, **kwargs_)
        else:
            return ClinicalDocumentNoteType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_clinicalDocumentNoteCode(self):
        return self.clinicalDocumentNoteCode
    def set_clinicalDocumentNoteCode(self, clinicalDocumentNoteCode):
        self.clinicalDocumentNoteCode = clinicalDocumentNoteCode
    def get_clinicalDocumentTypeCode(self):
        return self.clinicalDocumentTypeCode
    def set_clinicalDocumentTypeCode(self, clinicalDocumentTypeCode):
        self.clinicalDocumentTypeCode = clinicalDocumentTypeCode
    def get_clinicalDocumentNoteTitle(self):
        return self.clinicalDocumentNoteTitle
    def set_clinicalDocumentNoteTitle(self, clinicalDocumentNoteTitle):
        self.clinicalDocumentNoteTitle = clinicalDocumentNoteTitle
    def get_clinicalDocumentNoteText(self):
        return self.clinicalDocumentNoteText
    def set_clinicalDocumentNoteText(self, clinicalDocumentNoteText):
        self.clinicalDocumentNoteText = clinicalDocumentNoteText
    def get_multimediaEntry(self):
        return self.multimediaEntry
    def set_multimediaEntry(self, multimediaEntry):
        self.multimediaEntry = multimediaEntry
    def get_dissentintOpinion(self):
        return self.dissentintOpinion
    def set_dissentintOpinion(self, dissentintOpinion):
        self.dissentintOpinion = dissentintOpinion
    def add_dissentintOpinion(self, value):
        self.dissentintOpinion.append(value)
    def insert_dissentintOpinion_at(self, index, value):
        self.dissentintOpinion.insert(index, value)
    def replace_dissentintOpinion_at(self, index, value):
        self.dissentintOpinion[index] = value
    def validate_ClinicalDocumentNoteCodeEnum(self, value):
        result = True
        # Validate type ClinicalDocumentNoteCodeEnum, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            value = value
            enumerations = ['utr', 'atb', 'sam', 'sao', 'ins', 'slu', 'auf', 'sva', 'bes']
            if value not in enumerations:
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s does not match xsd enumeration restriction on ClinicalDocumentNoteCodeEnum' % {"value" : encode_str_2_3(value), "lineno": lineno} )
                result = False
        return result
    def validate_ClinicalDocumentTypeCodeEnum(self, value):
        result = True
        # Validate type ClinicalDocumentTypeCodeEnum, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            value = value
            enumerations = ['epi', 'int', 'dag', 'ova', 'ovs', 'ovr']
            if value not in enumerations:
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s does not match xsd enumeration restriction on ClinicalDocumentTypeCodeEnum' % {"value" : encode_str_2_3(value), "lineno": lineno} )
                result = False
        return result
    def has__content(self):
        if (
            self.clinicalDocumentNoteCode is not None or
            self.clinicalDocumentTypeCode is not None or
            self.clinicalDocumentNoteTitle is not None or
            self.clinicalDocumentNoteText is not None or
            self.multimediaEntry is not None or
            self.dissentintOpinion
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:codes="urn:riv:clinicalprocess:healthcond:description:enums:2"  xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='ClinicalDocumentNoteType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('ClinicalDocumentNoteType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'ClinicalDocumentNoteType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='ClinicalDocumentNoteType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='ClinicalDocumentNoteType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='ClinicalDocumentNoteType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:codes="urn:riv:clinicalprocess:healthcond:description:enums:2"  xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='ClinicalDocumentNoteType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.clinicalDocumentNoteCode is not None:
            namespaceprefix_ = self.clinicalDocumentNoteCode_nsprefix_ + ':' if (UseCapturedNS_ and self.clinicalDocumentNoteCode_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sclinicalDocumentNoteCode>%s</%sclinicalDocumentNoteCode>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.clinicalDocumentNoteCode), input_name='clinicalDocumentNoteCode')), namespaceprefix_ , eol_))
        if self.clinicalDocumentTypeCode is not None:
            namespaceprefix_ = self.clinicalDocumentTypeCode_nsprefix_ + ':' if (UseCapturedNS_ and self.clinicalDocumentTypeCode_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sclinicalDocumentTypeCode>%s</%sclinicalDocumentTypeCode>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.clinicalDocumentTypeCode), input_name='clinicalDocumentTypeCode')), namespaceprefix_ , eol_))
        if self.clinicalDocumentNoteTitle is not None:
            namespaceprefix_ = self.clinicalDocumentNoteTitle_nsprefix_ + ':' if (UseCapturedNS_ and self.clinicalDocumentNoteTitle_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sclinicalDocumentNoteTitle>%s</%sclinicalDocumentNoteTitle>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.clinicalDocumentNoteTitle), input_name='clinicalDocumentNoteTitle')), namespaceprefix_ , eol_))
        if self.clinicalDocumentNoteText is not None:
            namespaceprefix_ = self.clinicalDocumentNoteText_nsprefix_ + ':' if (UseCapturedNS_ and self.clinicalDocumentNoteText_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sclinicalDocumentNoteText>%s</%sclinicalDocumentNoteText>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.clinicalDocumentNoteText), input_name='clinicalDocumentNoteText')), namespaceprefix_ , eol_))
        if self.multimediaEntry is not None:
            namespaceprefix_ = self.multimediaEntry_nsprefix_ + ':' if (UseCapturedNS_ and self.multimediaEntry_nsprefix_) else ''
            self.multimediaEntry.export(outfile, level, namespaceprefix_, namespacedef_='', name_='multimediaEntry', pretty_print=pretty_print)
        for dissentintOpinion_ in self.dissentintOpinion:
            namespaceprefix_ = self.dissentintOpinion_nsprefix_ + ':' if (UseCapturedNS_ and self.dissentintOpinion_nsprefix_) else ''
            dissentintOpinion_.export(outfile, level, namespaceprefix_, namespacedef_='', name_='dissentintOpinion', pretty_print=pretty_print)
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'clinicalDocumentNoteCode':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'clinicalDocumentNoteCode')
            value_ = self.gds_validate_string(value_, node, 'clinicalDocumentNoteCode')
            self.clinicalDocumentNoteCode = value_
            self.clinicalDocumentNoteCode_nsprefix_ = child_.prefix
            # validate type ClinicalDocumentNoteCodeEnum
            self.validate_ClinicalDocumentNoteCodeEnum(self.clinicalDocumentNoteCode)
        elif nodeName_ == 'clinicalDocumentTypeCode':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'clinicalDocumentTypeCode')
            value_ = self.gds_validate_string(value_, node, 'clinicalDocumentTypeCode')
            self.clinicalDocumentTypeCode = value_
            self.clinicalDocumentTypeCode_nsprefix_ = child_.prefix
            # validate type ClinicalDocumentTypeCodeEnum
            self.validate_ClinicalDocumentTypeCodeEnum(self.clinicalDocumentTypeCode)
        elif nodeName_ == 'clinicalDocumentNoteTitle':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'clinicalDocumentNoteTitle')
            value_ = self.gds_validate_string(value_, node, 'clinicalDocumentNoteTitle')
            self.clinicalDocumentNoteTitle = value_
            self.clinicalDocumentNoteTitle_nsprefix_ = child_.prefix
        elif nodeName_ == 'clinicalDocumentNoteText':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'clinicalDocumentNoteText')
            value_ = self.gds_validate_string(value_, node, 'clinicalDocumentNoteText')
            self.clinicalDocumentNoteText = value_
            self.clinicalDocumentNoteText_nsprefix_ = child_.prefix
        elif nodeName_ == 'multimediaEntry':
            obj_ = MultimediaType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.multimediaEntry = obj_
            obj_.original_tagname_ = 'multimediaEntry'
        elif nodeName_ == 'dissentintOpinion':
            obj_ = DissentingOpinionType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.dissentintOpinion.append(obj_)
            obj_.original_tagname_ = 'dissentintOpinion'
# end class ClinicalDocumentNoteType


class PatientSummaryHeaderType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, documentId=None, sourceSystemHSAId=None, documentTitle=None, documentTime=None, patientId=None, accountableHealthcareProfessional=None, legalAuthenticator=None, approvedForPatient=None, careContactId=None, nullified=None, nullifiedReason=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.documentId = documentId
        self.documentId_nsprefix_ = "tns"
        self.sourceSystemHSAId = sourceSystemHSAId
        self.validate_HSAIdType(self.sourceSystemHSAId)
        self.sourceSystemHSAId_nsprefix_ = "tns"
        self.documentTitle = documentTitle
        self.documentTitle_nsprefix_ = "tns"
        self.documentTime = documentTime
        self.validate_TimeStampType(self.documentTime)
        self.documentTime_nsprefix_ = "tns"
        self.patientId = patientId
        self.patientId_nsprefix_ = "tns"
        self.accountableHealthcareProfessional = accountableHealthcareProfessional
        self.accountableHealthcareProfessional_nsprefix_ = "tns"
        self.legalAuthenticator = legalAuthenticator
        self.legalAuthenticator_nsprefix_ = "tns"
        self.approvedForPatient = approvedForPatient
        self.approvedForPatient_nsprefix_ = "tns"
        self.careContactId = careContactId
        self.careContactId_nsprefix_ = "tns"
        self.nullified = nullified
        self.nullified_nsprefix_ = "tns"
        self.nullifiedReason = nullifiedReason
        self.nullifiedReason_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, PatientSummaryHeaderType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if PatientSummaryHeaderType.subclass:
            return PatientSummaryHeaderType.subclass(*args_, **kwargs_)
        else:
            return PatientSummaryHeaderType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_documentId(self):
        return self.documentId
    def set_documentId(self, documentId):
        self.documentId = documentId
    def get_sourceSystemHSAId(self):
        return self.sourceSystemHSAId
    def set_sourceSystemHSAId(self, sourceSystemHSAId):
        self.sourceSystemHSAId = sourceSystemHSAId
    def get_documentTitle(self):
        return self.documentTitle
    def set_documentTitle(self, documentTitle):
        self.documentTitle = documentTitle
    def get_documentTime(self):
        return self.documentTime
    def set_documentTime(self, documentTime):
        self.documentTime = documentTime
    def get_patientId(self):
        return self.patientId
    def set_patientId(self, patientId):
        self.patientId = patientId
    def get_accountableHealthcareProfessional(self):
        return self.accountableHealthcareProfessional
    def set_accountableHealthcareProfessional(self, accountableHealthcareProfessional):
        self.accountableHealthcareProfessional = accountableHealthcareProfessional
    def get_legalAuthenticator(self):
        return self.legalAuthenticator
    def set_legalAuthenticator(self, legalAuthenticator):
        self.legalAuthenticator = legalAuthenticator
    def get_approvedForPatient(self):
        return self.approvedForPatient
    def set_approvedForPatient(self, approvedForPatient):
        self.approvedForPatient = approvedForPatient
    def get_careContactId(self):
        return self.careContactId
    def set_careContactId(self, careContactId):
        self.careContactId = careContactId
    def get_nullified(self):
        return self.nullified
    def set_nullified(self, nullified):
        self.nullified = nullified
    def get_nullifiedReason(self):
        return self.nullifiedReason
    def set_nullifiedReason(self, nullifiedReason):
        self.nullifiedReason = nullifiedReason
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def validate_HSAIdType(self, value):
        result = True
        # Validate type HSAIdType, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            pass
        return result
    def validate_TimeStampType(self, value):
        result = True
        # Validate type TimeStampType, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            if not self.gds_validate_simple_patterns(
                    self.validate_TimeStampType_patterns_, value):
                self.gds_collector_.add_message('Value "%s" does not match xsd pattern restrictions: %s' % (encode_str_2_3(value), self.validate_TimeStampType_patterns_, ))
                result = False
        return result
    validate_TimeStampType_patterns_ = [['^((19|20)\\d\\d(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])(0[0-9]|1[0-9]|2[0123])([0-5])([0-9])([0-5])([0-9]))$']]
    def has__content(self):
        if (
            self.documentId is not None or
            self.sourceSystemHSAId is not None or
            self.documentTitle is not None or
            self.documentTime is not None or
            self.patientId is not None or
            self.accountableHealthcareProfessional is not None or
            self.legalAuthenticator is not None or
            self.approvedForPatient is not None or
            self.careContactId is not None or
            self.nullified is not None or
            self.nullifiedReason is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='PatientSummaryHeaderType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('PatientSummaryHeaderType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'PatientSummaryHeaderType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='PatientSummaryHeaderType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='PatientSummaryHeaderType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='PatientSummaryHeaderType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='PatientSummaryHeaderType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.documentId is not None:
            namespaceprefix_ = self.documentId_nsprefix_ + ':' if (UseCapturedNS_ and self.documentId_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sdocumentId>%s</%sdocumentId>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.documentId), input_name='documentId')), namespaceprefix_ , eol_))
        if self.sourceSystemHSAId is not None:
            namespaceprefix_ = self.sourceSystemHSAId_nsprefix_ + ':' if (UseCapturedNS_ and self.sourceSystemHSAId_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%ssourceSystemHSAId>%s</%ssourceSystemHSAId>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.sourceSystemHSAId), input_name='sourceSystemHSAId')), namespaceprefix_ , eol_))
        if self.documentTitle is not None:
            namespaceprefix_ = self.documentTitle_nsprefix_ + ':' if (UseCapturedNS_ and self.documentTitle_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sdocumentTitle>%s</%sdocumentTitle>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.documentTitle), input_name='documentTitle')), namespaceprefix_ , eol_))
        if self.documentTime is not None:
            namespaceprefix_ = self.documentTime_nsprefix_ + ':' if (UseCapturedNS_ and self.documentTime_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sdocumentTime>%s</%sdocumentTime>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.documentTime), input_name='documentTime')), namespaceprefix_ , eol_))
        if self.patientId is not None:
            namespaceprefix_ = self.patientId_nsprefix_ + ':' if (UseCapturedNS_ and self.patientId_nsprefix_) else ''
            self.patientId.export(outfile, level, namespaceprefix_, namespacedef_='', name_='patientId', pretty_print=pretty_print)
        if self.accountableHealthcareProfessional is not None:
            namespaceprefix_ = self.accountableHealthcareProfessional_nsprefix_ + ':' if (UseCapturedNS_ and self.accountableHealthcareProfessional_nsprefix_) else ''
            self.accountableHealthcareProfessional.export(outfile, level, namespaceprefix_, namespacedef_='', name_='accountableHealthcareProfessional', pretty_print=pretty_print)
        if self.legalAuthenticator is not None:
            namespaceprefix_ = self.legalAuthenticator_nsprefix_ + ':' if (UseCapturedNS_ and self.legalAuthenticator_nsprefix_) else ''
            self.legalAuthenticator.export(outfile, level, namespaceprefix_, namespacedef_='', name_='legalAuthenticator', pretty_print=pretty_print)
        if self.approvedForPatient is not None:
            namespaceprefix_ = self.approvedForPatient_nsprefix_ + ':' if (UseCapturedNS_ and self.approvedForPatient_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sapprovedForPatient>%s</%sapprovedForPatient>%s' % (namespaceprefix_ , self.gds_format_boolean(self.approvedForPatient, input_name='approvedForPatient'), namespaceprefix_ , eol_))
        if self.careContactId is not None:
            namespaceprefix_ = self.careContactId_nsprefix_ + ':' if (UseCapturedNS_ and self.careContactId_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%scareContactId>%s</%scareContactId>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.careContactId), input_name='careContactId')), namespaceprefix_ , eol_))
        if self.nullified is not None:
            namespaceprefix_ = self.nullified_nsprefix_ + ':' if (UseCapturedNS_ and self.nullified_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%snullified>%s</%snullified>%s' % (namespaceprefix_ , self.gds_format_boolean(self.nullified, input_name='nullified'), namespaceprefix_ , eol_))
        if self.nullifiedReason is not None:
            namespaceprefix_ = self.nullifiedReason_nsprefix_ + ':' if (UseCapturedNS_ and self.nullifiedReason_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%snullifiedReason>%s</%snullifiedReason>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.nullifiedReason), input_name='nullifiedReason')), namespaceprefix_ , eol_))
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'documentId':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'documentId')
            value_ = self.gds_validate_string(value_, node, 'documentId')
            self.documentId = value_
            self.documentId_nsprefix_ = child_.prefix
        elif nodeName_ == 'sourceSystemHSAId':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'sourceSystemHSAId')
            value_ = self.gds_validate_string(value_, node, 'sourceSystemHSAId')
            self.sourceSystemHSAId = value_
            self.sourceSystemHSAId_nsprefix_ = child_.prefix
            # validate type HSAIdType
            self.validate_HSAIdType(self.sourceSystemHSAId)
        elif nodeName_ == 'documentTitle':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'documentTitle')
            value_ = self.gds_validate_string(value_, node, 'documentTitle')
            self.documentTitle = value_
            self.documentTitle_nsprefix_ = child_.prefix
        elif nodeName_ == 'documentTime':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'documentTime')
            value_ = self.gds_validate_string(value_, node, 'documentTime')
            self.documentTime = value_
            self.documentTime_nsprefix_ = child_.prefix
            # validate type TimeStampType
            self.validate_TimeStampType(self.documentTime)
        elif nodeName_ == 'patientId':
            obj_ = PersonIdType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.patientId = obj_
            obj_.original_tagname_ = 'patientId'
        elif nodeName_ == 'accountableHealthcareProfessional':
            obj_ = HealthcareProfessionalType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.accountableHealthcareProfessional = obj_
            obj_.original_tagname_ = 'accountableHealthcareProfessional'
        elif nodeName_ == 'legalAuthenticator':
            obj_ = LegalAuthenticatorType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.legalAuthenticator = obj_
            obj_.original_tagname_ = 'legalAuthenticator'
        elif nodeName_ == 'approvedForPatient':
            sval_ = child_.text
            ival_ = self.gds_parse_boolean(sval_, node, 'approvedForPatient')
            ival_ = self.gds_validate_boolean(ival_, node, 'approvedForPatient')
            self.approvedForPatient = ival_
            self.approvedForPatient_nsprefix_ = child_.prefix
        elif nodeName_ == 'careContactId':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'careContactId')
            value_ = self.gds_validate_string(value_, node, 'careContactId')
            self.careContactId = value_
            self.careContactId_nsprefix_ = child_.prefix
        elif nodeName_ == 'nullified':
            sval_ = child_.text
            ival_ = self.gds_parse_boolean(sval_, node, 'nullified')
            ival_ = self.gds_validate_boolean(ival_, node, 'nullified')
            self.nullified = ival_
            self.nullified_nsprefix_ = child_.prefix
        elif nodeName_ == 'nullifiedReason':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'nullifiedReason')
            value_ = self.gds_validate_string(value_, node, 'nullifiedReason')
            self.nullifiedReason = value_
            self.nullifiedReason_nsprefix_ = child_.prefix
        else:
            content_ = self.gds_build_any(child_, 'PatientSummaryHeaderType')
            self.anytypeobjs_.append(content_)
# end class PatientSummaryHeaderType


class CPatientSummaryHeaderType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, documentId=None, sourceSystemHSAid=None, documentTitle=None, documentTime=None, patientId=None, accountableHealthcareProfessional=None, legalAuthenticator=None, approvedForPatient=None, careContactId=None, nullified=None, nullifiedReason=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.documentId = documentId
        self.documentId_nsprefix_ = "tns"
        self.sourceSystemHSAid = sourceSystemHSAid
        self.validate_HSAIdType(self.sourceSystemHSAid)
        self.sourceSystemHSAid_nsprefix_ = "tns"
        self.documentTitle = documentTitle
        self.documentTitle_nsprefix_ = "tns"
        self.documentTime = documentTime
        self.validate_TimeStampType(self.documentTime)
        self.documentTime_nsprefix_ = "tns"
        self.patientId = patientId
        self.patientId_nsprefix_ = "tns"
        self.accountableHealthcareProfessional = accountableHealthcareProfessional
        self.accountableHealthcareProfessional_nsprefix_ = "tns"
        self.legalAuthenticator = legalAuthenticator
        self.legalAuthenticator_nsprefix_ = "tns"
        self.approvedForPatient = approvedForPatient
        self.approvedForPatient_nsprefix_ = "tns"
        self.careContactId = careContactId
        self.careContactId_nsprefix_ = "tns"
        self.nullified = nullified
        self.nullified_nsprefix_ = "tns"
        self.nullifiedReason = nullifiedReason
        self.nullifiedReason_nsprefix_ = "tns"
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, CPatientSummaryHeaderType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if CPatientSummaryHeaderType.subclass:
            return CPatientSummaryHeaderType.subclass(*args_, **kwargs_)
        else:
            return CPatientSummaryHeaderType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_documentId(self):
        return self.documentId
    def set_documentId(self, documentId):
        self.documentId = documentId
    def get_sourceSystemHSAid(self):
        return self.sourceSystemHSAid
    def set_sourceSystemHSAid(self, sourceSystemHSAid):
        self.sourceSystemHSAid = sourceSystemHSAid
    def get_documentTitle(self):
        return self.documentTitle
    def set_documentTitle(self, documentTitle):
        self.documentTitle = documentTitle
    def get_documentTime(self):
        return self.documentTime
    def set_documentTime(self, documentTime):
        self.documentTime = documentTime
    def get_patientId(self):
        return self.patientId
    def set_patientId(self, patientId):
        self.patientId = patientId
    def get_accountableHealthcareProfessional(self):
        return self.accountableHealthcareProfessional
    def set_accountableHealthcareProfessional(self, accountableHealthcareProfessional):
        self.accountableHealthcareProfessional = accountableHealthcareProfessional
    def get_legalAuthenticator(self):
        return self.legalAuthenticator
    def set_legalAuthenticator(self, legalAuthenticator):
        self.legalAuthenticator = legalAuthenticator
    def get_approvedForPatient(self):
        return self.approvedForPatient
    def set_approvedForPatient(self, approvedForPatient):
        self.approvedForPatient = approvedForPatient
    def get_careContactId(self):
        return self.careContactId
    def set_careContactId(self, careContactId):
        self.careContactId = careContactId
    def get_nullified(self):
        return self.nullified
    def set_nullified(self, nullified):
        self.nullified = nullified
    def get_nullifiedReason(self):
        return self.nullifiedReason
    def set_nullifiedReason(self, nullifiedReason):
        self.nullifiedReason = nullifiedReason
    def validate_HSAIdType(self, value):
        result = True
        # Validate type HSAIdType, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            pass
        return result
    def validate_TimeStampType(self, value):
        result = True
        # Validate type TimeStampType, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            if not self.gds_validate_simple_patterns(
                    self.validate_TimeStampType_patterns_, value):
                self.gds_collector_.add_message('Value "%s" does not match xsd pattern restrictions: %s' % (encode_str_2_3(value), self.validate_TimeStampType_patterns_, ))
                result = False
        return result
    validate_TimeStampType_patterns_ = [['^((19|20)\\d\\d(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])(0[0-9]|1[0-9]|2[0123])([0-5])([0-9])([0-5])([0-9]))$']]
    def has__content(self):
        if (
            self.documentId is not None or
            self.sourceSystemHSAid is not None or
            self.documentTitle is not None or
            self.documentTime is not None or
            self.patientId is not None or
            self.accountableHealthcareProfessional is not None or
            self.legalAuthenticator is not None or
            self.approvedForPatient is not None or
            self.careContactId is not None or
            self.nullified is not None or
            self.nullifiedReason is not None
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='CPatientSummaryHeaderType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('CPatientSummaryHeaderType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'CPatientSummaryHeaderType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='CPatientSummaryHeaderType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='CPatientSummaryHeaderType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='CPatientSummaryHeaderType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='CPatientSummaryHeaderType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.documentId is not None:
            namespaceprefix_ = self.documentId_nsprefix_ + ':' if (UseCapturedNS_ and self.documentId_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sdocumentId>%s</%sdocumentId>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.documentId), input_name='documentId')), namespaceprefix_ , eol_))
        if self.sourceSystemHSAid is not None:
            namespaceprefix_ = self.sourceSystemHSAid_nsprefix_ + ':' if (UseCapturedNS_ and self.sourceSystemHSAid_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%ssourceSystemHSAid>%s</%ssourceSystemHSAid>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.sourceSystemHSAid), input_name='sourceSystemHSAid')), namespaceprefix_ , eol_))
        if self.documentTitle is not None:
            namespaceprefix_ = self.documentTitle_nsprefix_ + ':' if (UseCapturedNS_ and self.documentTitle_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sdocumentTitle>%s</%sdocumentTitle>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.documentTitle), input_name='documentTitle')), namespaceprefix_ , eol_))
        if self.documentTime is not None:
            namespaceprefix_ = self.documentTime_nsprefix_ + ':' if (UseCapturedNS_ and self.documentTime_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sdocumentTime>%s</%sdocumentTime>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.documentTime), input_name='documentTime')), namespaceprefix_ , eol_))
        if self.patientId is not None:
            namespaceprefix_ = self.patientId_nsprefix_ + ':' if (UseCapturedNS_ and self.patientId_nsprefix_) else ''
            self.patientId.export(outfile, level, namespaceprefix_, namespacedef_='', name_='patientId', pretty_print=pretty_print)
        if self.accountableHealthcareProfessional is not None:
            namespaceprefix_ = self.accountableHealthcareProfessional_nsprefix_ + ':' if (UseCapturedNS_ and self.accountableHealthcareProfessional_nsprefix_) else ''
            self.accountableHealthcareProfessional.export(outfile, level, namespaceprefix_, namespacedef_='', name_='accountableHealthcareProfessional', pretty_print=pretty_print)
        if self.legalAuthenticator is not None:
            namespaceprefix_ = self.legalAuthenticator_nsprefix_ + ':' if (UseCapturedNS_ and self.legalAuthenticator_nsprefix_) else ''
            self.legalAuthenticator.export(outfile, level, namespaceprefix_, namespacedef_='', name_='legalAuthenticator', pretty_print=pretty_print)
        if self.approvedForPatient is not None:
            namespaceprefix_ = self.approvedForPatient_nsprefix_ + ':' if (UseCapturedNS_ and self.approvedForPatient_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sapprovedForPatient>%s</%sapprovedForPatient>%s' % (namespaceprefix_ , self.gds_format_boolean(self.approvedForPatient, input_name='approvedForPatient'), namespaceprefix_ , eol_))
        if self.careContactId is not None:
            namespaceprefix_ = self.careContactId_nsprefix_ + ':' if (UseCapturedNS_ and self.careContactId_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%scareContactId>%s</%scareContactId>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.careContactId), input_name='careContactId')), namespaceprefix_ , eol_))
        if self.nullified is not None:
            namespaceprefix_ = self.nullified_nsprefix_ + ':' if (UseCapturedNS_ and self.nullified_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%snullified>%s</%snullified>%s' % (namespaceprefix_ , self.gds_format_boolean(self.nullified, input_name='nullified'), namespaceprefix_ , eol_))
        if self.nullifiedReason is not None:
            namespaceprefix_ = self.nullifiedReason_nsprefix_ + ':' if (UseCapturedNS_ and self.nullifiedReason_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%snullifiedReason>%s</%snullifiedReason>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.nullifiedReason), input_name='nullifiedReason')), namespaceprefix_ , eol_))
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'documentId':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'documentId')
            value_ = self.gds_validate_string(value_, node, 'documentId')
            self.documentId = value_
            self.documentId_nsprefix_ = child_.prefix
        elif nodeName_ == 'sourceSystemHSAid':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'sourceSystemHSAid')
            value_ = self.gds_validate_string(value_, node, 'sourceSystemHSAid')
            self.sourceSystemHSAid = value_
            self.sourceSystemHSAid_nsprefix_ = child_.prefix
            # validate type HSAIdType
            self.validate_HSAIdType(self.sourceSystemHSAid)
        elif nodeName_ == 'documentTitle':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'documentTitle')
            value_ = self.gds_validate_string(value_, node, 'documentTitle')
            self.documentTitle = value_
            self.documentTitle_nsprefix_ = child_.prefix
        elif nodeName_ == 'documentTime':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'documentTime')
            value_ = self.gds_validate_string(value_, node, 'documentTime')
            self.documentTime = value_
            self.documentTime_nsprefix_ = child_.prefix
            # validate type TimeStampType
            self.validate_TimeStampType(self.documentTime)
        elif nodeName_ == 'patientId':
            obj_ = PersonIdType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.patientId = obj_
            obj_.original_tagname_ = 'patientId'
        elif nodeName_ == 'accountableHealthcareProfessional':
            obj_ = HealthcareProfessionalType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.accountableHealthcareProfessional = obj_
            obj_.original_tagname_ = 'accountableHealthcareProfessional'
        elif nodeName_ == 'legalAuthenticator':
            obj_ = LegalAuthenticatorType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.legalAuthenticator = obj_
            obj_.original_tagname_ = 'legalAuthenticator'
        elif nodeName_ == 'approvedForPatient':
            sval_ = child_.text
            ival_ = self.gds_parse_boolean(sval_, node, 'approvedForPatient')
            ival_ = self.gds_validate_boolean(ival_, node, 'approvedForPatient')
            self.approvedForPatient = ival_
            self.approvedForPatient_nsprefix_ = child_.prefix
        elif nodeName_ == 'careContactId':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'careContactId')
            value_ = self.gds_validate_string(value_, node, 'careContactId')
            self.careContactId = value_
            self.careContactId_nsprefix_ = child_.prefix
        elif nodeName_ == 'nullified':
            sval_ = child_.text
            ival_ = self.gds_parse_boolean(sval_, node, 'nullified')
            ival_ = self.gds_validate_boolean(ival_, node, 'nullified')
            self.nullified = ival_
            self.nullified_nsprefix_ = child_.prefix
        elif nodeName_ == 'nullifiedReason':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'nullifiedReason')
            value_ = self.gds_validate_string(value_, node, 'nullifiedReason')
            self.nullifiedReason = value_
            self.nullifiedReason_nsprefix_ = child_.prefix
# end class CPatientSummaryHeaderType


class HealthcareProfessionalType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, authorTime=None, healthcareProfessionalHSAId=None, healthcareProfessionalName=None, healthcareProfessionalRoleCode=None, healthcareProfessionalOrgUnit=None, healthcareProfessionalCareUnitHSAId=None, healthcareProfessionalCareGiverHSAId=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.authorTime = authorTime
        self.validate_TimeStampType(self.authorTime)
        self.authorTime_nsprefix_ = "tns"
        self.healthcareProfessionalHSAId = healthcareProfessionalHSAId
        self.validate_HSAIdType(self.healthcareProfessionalHSAId)
        self.healthcareProfessionalHSAId_nsprefix_ = "tns"
        self.healthcareProfessionalName = healthcareProfessionalName
        self.healthcareProfessionalName_nsprefix_ = "tns"
        self.healthcareProfessionalRoleCode = healthcareProfessionalRoleCode
        self.healthcareProfessionalRoleCode_nsprefix_ = "tns"
        self.healthcareProfessionalOrgUnit = healthcareProfessionalOrgUnit
        self.healthcareProfessionalOrgUnit_nsprefix_ = "tns"
        self.healthcareProfessionalCareUnitHSAId = healthcareProfessionalCareUnitHSAId
        self.validate_HSAIdType(self.healthcareProfessionalCareUnitHSAId)
        self.healthcareProfessionalCareUnitHSAId_nsprefix_ = "tns"
        self.healthcareProfessionalCareGiverHSAId = healthcareProfessionalCareGiverHSAId
        self.validate_HSAIdType(self.healthcareProfessionalCareGiverHSAId)
        self.healthcareProfessionalCareGiverHSAId_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, HealthcareProfessionalType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if HealthcareProfessionalType.subclass:
            return HealthcareProfessionalType.subclass(*args_, **kwargs_)
        else:
            return HealthcareProfessionalType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_authorTime(self):
        return self.authorTime
    def set_authorTime(self, authorTime):
        self.authorTime = authorTime
    def get_healthcareProfessionalHSAId(self):
        return self.healthcareProfessionalHSAId
    def set_healthcareProfessionalHSAId(self, healthcareProfessionalHSAId):
        self.healthcareProfessionalHSAId = healthcareProfessionalHSAId
    def get_healthcareProfessionalName(self):
        return self.healthcareProfessionalName
    def set_healthcareProfessionalName(self, healthcareProfessionalName):
        self.healthcareProfessionalName = healthcareProfessionalName
    def get_healthcareProfessionalRoleCode(self):
        return self.healthcareProfessionalRoleCode
    def set_healthcareProfessionalRoleCode(self, healthcareProfessionalRoleCode):
        self.healthcareProfessionalRoleCode = healthcareProfessionalRoleCode
    def get_healthcareProfessionalOrgUnit(self):
        return self.healthcareProfessionalOrgUnit
    def set_healthcareProfessionalOrgUnit(self, healthcareProfessionalOrgUnit):
        self.healthcareProfessionalOrgUnit = healthcareProfessionalOrgUnit
    def get_healthcareProfessionalCareUnitHSAId(self):
        return self.healthcareProfessionalCareUnitHSAId
    def set_healthcareProfessionalCareUnitHSAId(self, healthcareProfessionalCareUnitHSAId):
        self.healthcareProfessionalCareUnitHSAId = healthcareProfessionalCareUnitHSAId
    def get_healthcareProfessionalCareGiverHSAId(self):
        return self.healthcareProfessionalCareGiverHSAId
    def set_healthcareProfessionalCareGiverHSAId(self, healthcareProfessionalCareGiverHSAId):
        self.healthcareProfessionalCareGiverHSAId = healthcareProfessionalCareGiverHSAId
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def validate_TimeStampType(self, value):
        result = True
        # Validate type TimeStampType, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            if not self.gds_validate_simple_patterns(
                    self.validate_TimeStampType_patterns_, value):
                self.gds_collector_.add_message('Value "%s" does not match xsd pattern restrictions: %s' % (encode_str_2_3(value), self.validate_TimeStampType_patterns_, ))
                result = False
        return result
    validate_TimeStampType_patterns_ = [['^((19|20)\\d\\d(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])(0[0-9]|1[0-9]|2[0123])([0-5])([0-9])([0-5])([0-9]))$']]
    def validate_HSAIdType(self, value):
        result = True
        # Validate type HSAIdType, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            pass
        return result
    def has__content(self):
        if (
            self.authorTime is not None or
            self.healthcareProfessionalHSAId is not None or
            self.healthcareProfessionalName is not None or
            self.healthcareProfessionalRoleCode is not None or
            self.healthcareProfessionalOrgUnit is not None or
            self.healthcareProfessionalCareUnitHSAId is not None or
            self.healthcareProfessionalCareGiverHSAId is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='HealthcareProfessionalType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('HealthcareProfessionalType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'HealthcareProfessionalType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='HealthcareProfessionalType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='HealthcareProfessionalType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='HealthcareProfessionalType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='HealthcareProfessionalType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.authorTime is not None:
            namespaceprefix_ = self.authorTime_nsprefix_ + ':' if (UseCapturedNS_ and self.authorTime_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sauthorTime>%s</%sauthorTime>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.authorTime), input_name='authorTime')), namespaceprefix_ , eol_))
        if self.healthcareProfessionalHSAId is not None:
            namespaceprefix_ = self.healthcareProfessionalHSAId_nsprefix_ + ':' if (UseCapturedNS_ and self.healthcareProfessionalHSAId_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%shealthcareProfessionalHSAId>%s</%shealthcareProfessionalHSAId>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.healthcareProfessionalHSAId), input_name='healthcareProfessionalHSAId')), namespaceprefix_ , eol_))
        if self.healthcareProfessionalName is not None:
            namespaceprefix_ = self.healthcareProfessionalName_nsprefix_ + ':' if (UseCapturedNS_ and self.healthcareProfessionalName_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%shealthcareProfessionalName>%s</%shealthcareProfessionalName>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.healthcareProfessionalName), input_name='healthcareProfessionalName')), namespaceprefix_ , eol_))
        if self.healthcareProfessionalRoleCode is not None:
            namespaceprefix_ = self.healthcareProfessionalRoleCode_nsprefix_ + ':' if (UseCapturedNS_ and self.healthcareProfessionalRoleCode_nsprefix_) else ''
            self.healthcareProfessionalRoleCode.export(outfile, level, namespaceprefix_, namespacedef_='', name_='healthcareProfessionalRoleCode', pretty_print=pretty_print)
        if self.healthcareProfessionalOrgUnit is not None:
            namespaceprefix_ = self.healthcareProfessionalOrgUnit_nsprefix_ + ':' if (UseCapturedNS_ and self.healthcareProfessionalOrgUnit_nsprefix_) else ''
            self.healthcareProfessionalOrgUnit.export(outfile, level, namespaceprefix_, namespacedef_='', name_='healthcareProfessionalOrgUnit', pretty_print=pretty_print)
        if self.healthcareProfessionalCareUnitHSAId is not None:
            namespaceprefix_ = self.healthcareProfessionalCareUnitHSAId_nsprefix_ + ':' if (UseCapturedNS_ and self.healthcareProfessionalCareUnitHSAId_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%shealthcareProfessionalCareUnitHSAId>%s</%shealthcareProfessionalCareUnitHSAId>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.healthcareProfessionalCareUnitHSAId), input_name='healthcareProfessionalCareUnitHSAId')), namespaceprefix_ , eol_))
        if self.healthcareProfessionalCareGiverHSAId is not None:
            namespaceprefix_ = self.healthcareProfessionalCareGiverHSAId_nsprefix_ + ':' if (UseCapturedNS_ and self.healthcareProfessionalCareGiverHSAId_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%shealthcareProfessionalCareGiverHSAId>%s</%shealthcareProfessionalCareGiverHSAId>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.healthcareProfessionalCareGiverHSAId), input_name='healthcareProfessionalCareGiverHSAId')), namespaceprefix_ , eol_))
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'authorTime':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'authorTime')
            value_ = self.gds_validate_string(value_, node, 'authorTime')
            self.authorTime = value_
            self.authorTime_nsprefix_ = child_.prefix
            # validate type TimeStampType
            self.validate_TimeStampType(self.authorTime)
        elif nodeName_ == 'healthcareProfessionalHSAId':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'healthcareProfessionalHSAId')
            value_ = self.gds_validate_string(value_, node, 'healthcareProfessionalHSAId')
            self.healthcareProfessionalHSAId = value_
            self.healthcareProfessionalHSAId_nsprefix_ = child_.prefix
            # validate type HSAIdType
            self.validate_HSAIdType(self.healthcareProfessionalHSAId)
        elif nodeName_ == 'healthcareProfessionalName':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'healthcareProfessionalName')
            value_ = self.gds_validate_string(value_, node, 'healthcareProfessionalName')
            self.healthcareProfessionalName = value_
            self.healthcareProfessionalName_nsprefix_ = child_.prefix
        elif nodeName_ == 'healthcareProfessionalRoleCode':
            obj_ = CVType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.healthcareProfessionalRoleCode = obj_
            obj_.original_tagname_ = 'healthcareProfessionalRoleCode'
        elif nodeName_ == 'healthcareProfessionalOrgUnit':
            obj_ = OrgUnitType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.healthcareProfessionalOrgUnit = obj_
            obj_.original_tagname_ = 'healthcareProfessionalOrgUnit'
        elif nodeName_ == 'healthcareProfessionalCareUnitHSAId':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'healthcareProfessionalCareUnitHSAId')
            value_ = self.gds_validate_string(value_, node, 'healthcareProfessionalCareUnitHSAId')
            self.healthcareProfessionalCareUnitHSAId = value_
            self.healthcareProfessionalCareUnitHSAId_nsprefix_ = child_.prefix
            # validate type HSAIdType
            self.validate_HSAIdType(self.healthcareProfessionalCareUnitHSAId)
        elif nodeName_ == 'healthcareProfessionalCareGiverHSAId':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'healthcareProfessionalCareGiverHSAId')
            value_ = self.gds_validate_string(value_, node, 'healthcareProfessionalCareGiverHSAId')
            self.healthcareProfessionalCareGiverHSAId = value_
            self.healthcareProfessionalCareGiverHSAId_nsprefix_ = child_.prefix
            # validate type HSAIdType
            self.validate_HSAIdType(self.healthcareProfessionalCareGiverHSAId)
        else:
            content_ = self.gds_build_any(child_, 'HealthcareProfessionalType')
            self.anytypeobjs_.append(content_)
# end class HealthcareProfessionalType


class OrgUnitType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, orgUnitHSAId=None, orgUnitName=None, orgUnitTelecom=None, orgUnitEmail=None, orgUnitAddress=None, orgUnitLocation=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.orgUnitHSAId = orgUnitHSAId
        self.validate_HSAIdType(self.orgUnitHSAId)
        self.orgUnitHSAId_nsprefix_ = "tns"
        self.orgUnitName = orgUnitName
        self.orgUnitName_nsprefix_ = "tns"
        self.orgUnitTelecom = orgUnitTelecom
        self.orgUnitTelecom_nsprefix_ = "tns"
        self.orgUnitEmail = orgUnitEmail
        self.orgUnitEmail_nsprefix_ = "tns"
        self.orgUnitAddress = orgUnitAddress
        self.orgUnitAddress_nsprefix_ = "tns"
        self.orgUnitLocation = orgUnitLocation
        self.orgUnitLocation_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, OrgUnitType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if OrgUnitType.subclass:
            return OrgUnitType.subclass(*args_, **kwargs_)
        else:
            return OrgUnitType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_orgUnitHSAId(self):
        return self.orgUnitHSAId
    def set_orgUnitHSAId(self, orgUnitHSAId):
        self.orgUnitHSAId = orgUnitHSAId
    def get_orgUnitName(self):
        return self.orgUnitName
    def set_orgUnitName(self, orgUnitName):
        self.orgUnitName = orgUnitName
    def get_orgUnitTelecom(self):
        return self.orgUnitTelecom
    def set_orgUnitTelecom(self, orgUnitTelecom):
        self.orgUnitTelecom = orgUnitTelecom
    def get_orgUnitEmail(self):
        return self.orgUnitEmail
    def set_orgUnitEmail(self, orgUnitEmail):
        self.orgUnitEmail = orgUnitEmail
    def get_orgUnitAddress(self):
        return self.orgUnitAddress
    def set_orgUnitAddress(self, orgUnitAddress):
        self.orgUnitAddress = orgUnitAddress
    def get_orgUnitLocation(self):
        return self.orgUnitLocation
    def set_orgUnitLocation(self, orgUnitLocation):
        self.orgUnitLocation = orgUnitLocation
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def validate_HSAIdType(self, value):
        result = True
        # Validate type HSAIdType, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            pass
        return result
    def has__content(self):
        if (
            self.orgUnitHSAId is not None or
            self.orgUnitName is not None or
            self.orgUnitTelecom is not None or
            self.orgUnitEmail is not None or
            self.orgUnitAddress is not None or
            self.orgUnitLocation is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='OrgUnitType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('OrgUnitType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'OrgUnitType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='OrgUnitType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='OrgUnitType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='OrgUnitType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='OrgUnitType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.orgUnitHSAId is not None:
            namespaceprefix_ = self.orgUnitHSAId_nsprefix_ + ':' if (UseCapturedNS_ and self.orgUnitHSAId_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sorgUnitHSAId>%s</%sorgUnitHSAId>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.orgUnitHSAId), input_name='orgUnitHSAId')), namespaceprefix_ , eol_))
        if self.orgUnitName is not None:
            namespaceprefix_ = self.orgUnitName_nsprefix_ + ':' if (UseCapturedNS_ and self.orgUnitName_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sorgUnitName>%s</%sorgUnitName>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.orgUnitName), input_name='orgUnitName')), namespaceprefix_ , eol_))
        if self.orgUnitTelecom is not None:
            namespaceprefix_ = self.orgUnitTelecom_nsprefix_ + ':' if (UseCapturedNS_ and self.orgUnitTelecom_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sorgUnitTelecom>%s</%sorgUnitTelecom>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.orgUnitTelecom), input_name='orgUnitTelecom')), namespaceprefix_ , eol_))
        if self.orgUnitEmail is not None:
            namespaceprefix_ = self.orgUnitEmail_nsprefix_ + ':' if (UseCapturedNS_ and self.orgUnitEmail_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sorgUnitEmail>%s</%sorgUnitEmail>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.orgUnitEmail), input_name='orgUnitEmail')), namespaceprefix_ , eol_))
        if self.orgUnitAddress is not None:
            namespaceprefix_ = self.orgUnitAddress_nsprefix_ + ':' if (UseCapturedNS_ and self.orgUnitAddress_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sorgUnitAddress>%s</%sorgUnitAddress>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.orgUnitAddress), input_name='orgUnitAddress')), namespaceprefix_ , eol_))
        if self.orgUnitLocation is not None:
            namespaceprefix_ = self.orgUnitLocation_nsprefix_ + ':' if (UseCapturedNS_ and self.orgUnitLocation_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sorgUnitLocation>%s</%sorgUnitLocation>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.orgUnitLocation), input_name='orgUnitLocation')), namespaceprefix_ , eol_))
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'orgUnitHSAId':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'orgUnitHSAId')
            value_ = self.gds_validate_string(value_, node, 'orgUnitHSAId')
            self.orgUnitHSAId = value_
            self.orgUnitHSAId_nsprefix_ = child_.prefix
            # validate type HSAIdType
            self.validate_HSAIdType(self.orgUnitHSAId)
        elif nodeName_ == 'orgUnitName':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'orgUnitName')
            value_ = self.gds_validate_string(value_, node, 'orgUnitName')
            self.orgUnitName = value_
            self.orgUnitName_nsprefix_ = child_.prefix
        elif nodeName_ == 'orgUnitTelecom':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'orgUnitTelecom')
            value_ = self.gds_validate_string(value_, node, 'orgUnitTelecom')
            self.orgUnitTelecom = value_
            self.orgUnitTelecom_nsprefix_ = child_.prefix
        elif nodeName_ == 'orgUnitEmail':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'orgUnitEmail')
            value_ = self.gds_validate_string(value_, node, 'orgUnitEmail')
            self.orgUnitEmail = value_
            self.orgUnitEmail_nsprefix_ = child_.prefix
        elif nodeName_ == 'orgUnitAddress':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'orgUnitAddress')
            value_ = self.gds_validate_string(value_, node, 'orgUnitAddress')
            self.orgUnitAddress = value_
            self.orgUnitAddress_nsprefix_ = child_.prefix
        elif nodeName_ == 'orgUnitLocation':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'orgUnitLocation')
            value_ = self.gds_validate_string(value_, node, 'orgUnitLocation')
            self.orgUnitLocation = value_
            self.orgUnitLocation_nsprefix_ = child_.prefix
        else:
            content_ = self.gds_build_any(child_, 'OrgUnitType')
            self.anytypeobjs_.append(content_)
# end class OrgUnitType


class DiagnosisType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, diagnosisHeader=None, diagnosisBody=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.diagnosisHeader = diagnosisHeader
        self.diagnosisHeader_nsprefix_ = "tns"
        self.diagnosisBody = diagnosisBody
        self.diagnosisBody_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, DiagnosisType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if DiagnosisType.subclass:
            return DiagnosisType.subclass(*args_, **kwargs_)
        else:
            return DiagnosisType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_diagnosisHeader(self):
        return self.diagnosisHeader
    def set_diagnosisHeader(self, diagnosisHeader):
        self.diagnosisHeader = diagnosisHeader
    def get_diagnosisBody(self):
        return self.diagnosisBody
    def set_diagnosisBody(self, diagnosisBody):
        self.diagnosisBody = diagnosisBody
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.diagnosisHeader is not None or
            self.diagnosisBody is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='DiagnosisType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('DiagnosisType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'DiagnosisType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='DiagnosisType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='DiagnosisType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='DiagnosisType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='DiagnosisType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.diagnosisHeader is not None:
            namespaceprefix_ = self.diagnosisHeader_nsprefix_ + ':' if (UseCapturedNS_ and self.diagnosisHeader_nsprefix_) else ''
            self.diagnosisHeader.export(outfile, level, namespaceprefix_, namespacedef_='', name_='diagnosisHeader', pretty_print=pretty_print)
        if self.diagnosisBody is not None:
            namespaceprefix_ = self.diagnosisBody_nsprefix_ + ':' if (UseCapturedNS_ and self.diagnosisBody_nsprefix_) else ''
            self.diagnosisBody.export(outfile, level, namespaceprefix_, namespacedef_='', name_='diagnosisBody', pretty_print=pretty_print)
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'diagnosisHeader':
            obj_ = PatientSummaryHeaderType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.diagnosisHeader = obj_
            obj_.original_tagname_ = 'diagnosisHeader'
        elif nodeName_ == 'diagnosisBody':
            obj_ = DiagnosisBodyType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.diagnosisBody = obj_
            obj_.original_tagname_ = 'diagnosisBody'
        else:
            content_ = self.gds_build_any(child_, 'DiagnosisType')
            self.anytypeobjs_.append(content_)
# end class DiagnosisType


class DiagnosisBodyType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, typeOfDiagnosis=None, chronicDiagnosis=None, diagnosisTime=None, diagnosisCode=None, relatedDiagnosis=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.typeOfDiagnosis = typeOfDiagnosis
        self.validate_DiagnosisTypeEnum(self.typeOfDiagnosis)
        self.typeOfDiagnosis_nsprefix_ = "codes"
        self.chronicDiagnosis = chronicDiagnosis
        self.chronicDiagnosis_nsprefix_ = "tns"
        self.diagnosisTime = diagnosisTime
        self.validate_TimeStampType(self.diagnosisTime)
        self.diagnosisTime_nsprefix_ = "tns"
        self.diagnosisCode = diagnosisCode
        self.diagnosisCode_nsprefix_ = "tns"
        if relatedDiagnosis is None:
            self.relatedDiagnosis = []
        else:
            self.relatedDiagnosis = relatedDiagnosis
        self.relatedDiagnosis_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, DiagnosisBodyType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if DiagnosisBodyType.subclass:
            return DiagnosisBodyType.subclass(*args_, **kwargs_)
        else:
            return DiagnosisBodyType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_typeOfDiagnosis(self):
        return self.typeOfDiagnosis
    def set_typeOfDiagnosis(self, typeOfDiagnosis):
        self.typeOfDiagnosis = typeOfDiagnosis
    def get_chronicDiagnosis(self):
        return self.chronicDiagnosis
    def set_chronicDiagnosis(self, chronicDiagnosis):
        self.chronicDiagnosis = chronicDiagnosis
    def get_diagnosisTime(self):
        return self.diagnosisTime
    def set_diagnosisTime(self, diagnosisTime):
        self.diagnosisTime = diagnosisTime
    def get_diagnosisCode(self):
        return self.diagnosisCode
    def set_diagnosisCode(self, diagnosisCode):
        self.diagnosisCode = diagnosisCode
    def get_relatedDiagnosis(self):
        return self.relatedDiagnosis
    def set_relatedDiagnosis(self, relatedDiagnosis):
        self.relatedDiagnosis = relatedDiagnosis
    def add_relatedDiagnosis(self, value):
        self.relatedDiagnosis.append(value)
    def insert_relatedDiagnosis_at(self, index, value):
        self.relatedDiagnosis.insert(index, value)
    def replace_relatedDiagnosis_at(self, index, value):
        self.relatedDiagnosis[index] = value
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def validate_DiagnosisTypeEnum(self, value):
        result = True
        # Validate type DiagnosisTypeEnum, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            value = value
            enumerations = ['Huvuddiagnos', 'Bidiagnos']
            if value not in enumerations:
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s does not match xsd enumeration restriction on DiagnosisTypeEnum' % {"value" : encode_str_2_3(value), "lineno": lineno} )
                result = False
        return result
    def validate_TimeStampType(self, value):
        result = True
        # Validate type TimeStampType, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            if not self.gds_validate_simple_patterns(
                    self.validate_TimeStampType_patterns_, value):
                self.gds_collector_.add_message('Value "%s" does not match xsd pattern restrictions: %s' % (encode_str_2_3(value), self.validate_TimeStampType_patterns_, ))
                result = False
        return result
    validate_TimeStampType_patterns_ = [['^((19|20)\\d\\d(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])(0[0-9]|1[0-9]|2[0123])([0-5])([0-9])([0-5])([0-9]))$']]
    def has__content(self):
        if (
            self.typeOfDiagnosis is not None or
            self.chronicDiagnosis is not None or
            self.diagnosisTime is not None or
            self.diagnosisCode is not None or
            self.relatedDiagnosis or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:codes="urn:riv:clinicalprocess:healthcond:description:enums:2"  xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='DiagnosisBodyType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('DiagnosisBodyType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'DiagnosisBodyType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='DiagnosisBodyType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='DiagnosisBodyType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='DiagnosisBodyType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:codes="urn:riv:clinicalprocess:healthcond:description:enums:2"  xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='DiagnosisBodyType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.typeOfDiagnosis is not None:
            namespaceprefix_ = self.typeOfDiagnosis_nsprefix_ + ':' if (UseCapturedNS_ and self.typeOfDiagnosis_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%stypeOfDiagnosis>%s</%stypeOfDiagnosis>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.typeOfDiagnosis), input_name='typeOfDiagnosis')), namespaceprefix_ , eol_))
        if self.chronicDiagnosis is not None:
            namespaceprefix_ = self.chronicDiagnosis_nsprefix_ + ':' if (UseCapturedNS_ and self.chronicDiagnosis_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%schronicDiagnosis>%s</%schronicDiagnosis>%s' % (namespaceprefix_ , self.gds_format_boolean(self.chronicDiagnosis, input_name='chronicDiagnosis'), namespaceprefix_ , eol_))
        if self.diagnosisTime is not None:
            namespaceprefix_ = self.diagnosisTime_nsprefix_ + ':' if (UseCapturedNS_ and self.diagnosisTime_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sdiagnosisTime>%s</%sdiagnosisTime>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.diagnosisTime), input_name='diagnosisTime')), namespaceprefix_ , eol_))
        if self.diagnosisCode is not None:
            namespaceprefix_ = self.diagnosisCode_nsprefix_ + ':' if (UseCapturedNS_ and self.diagnosisCode_nsprefix_) else ''
            self.diagnosisCode.export(outfile, level, namespaceprefix_, namespacedef_='', name_='diagnosisCode', pretty_print=pretty_print)
        for relatedDiagnosis_ in self.relatedDiagnosis:
            namespaceprefix_ = self.relatedDiagnosis_nsprefix_ + ':' if (UseCapturedNS_ and self.relatedDiagnosis_nsprefix_) else ''
            relatedDiagnosis_.export(outfile, level, namespaceprefix_, namespacedef_='', name_='relatedDiagnosis', pretty_print=pretty_print)
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'typeOfDiagnosis':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'typeOfDiagnosis')
            value_ = self.gds_validate_string(value_, node, 'typeOfDiagnosis')
            self.typeOfDiagnosis = value_
            self.typeOfDiagnosis_nsprefix_ = child_.prefix
            # validate type DiagnosisTypeEnum
            self.validate_DiagnosisTypeEnum(self.typeOfDiagnosis)
        elif nodeName_ == 'chronicDiagnosis':
            sval_ = child_.text
            ival_ = self.gds_parse_boolean(sval_, node, 'chronicDiagnosis')
            ival_ = self.gds_validate_boolean(ival_, node, 'chronicDiagnosis')
            self.chronicDiagnosis = ival_
            self.chronicDiagnosis_nsprefix_ = child_.prefix
        elif nodeName_ == 'diagnosisTime':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'diagnosisTime')
            value_ = self.gds_validate_string(value_, node, 'diagnosisTime')
            self.diagnosisTime = value_
            self.diagnosisTime_nsprefix_ = child_.prefix
            # validate type TimeStampType
            self.validate_TimeStampType(self.diagnosisTime)
        elif nodeName_ == 'diagnosisCode':
            obj_ = CVType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.diagnosisCode = obj_
            obj_.original_tagname_ = 'diagnosisCode'
        elif nodeName_ == 'relatedDiagnosis':
            obj_ = RelatedDiagnosisType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.relatedDiagnosis.append(obj_)
            obj_.original_tagname_ = 'relatedDiagnosis'
        else:
            content_ = self.gds_build_any(child_, 'DiagnosisBodyType')
            self.anytypeobjs_.append(content_)
# end class DiagnosisBodyType


class AlertInformationType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, alertInformationHeader=None, alertInformationBody=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.alertInformationHeader = alertInformationHeader
        self.alertInformationHeader_nsprefix_ = "tns"
        self.alertInformationBody = alertInformationBody
        self.alertInformationBody_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, AlertInformationType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if AlertInformationType.subclass:
            return AlertInformationType.subclass(*args_, **kwargs_)
        else:
            return AlertInformationType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_alertInformationHeader(self):
        return self.alertInformationHeader
    def set_alertInformationHeader(self, alertInformationHeader):
        self.alertInformationHeader = alertInformationHeader
    def get_alertInformationBody(self):
        return self.alertInformationBody
    def set_alertInformationBody(self, alertInformationBody):
        self.alertInformationBody = alertInformationBody
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.alertInformationHeader is not None or
            self.alertInformationBody is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='AlertInformationType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('AlertInformationType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'AlertInformationType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='AlertInformationType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='AlertInformationType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='AlertInformationType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='AlertInformationType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.alertInformationHeader is not None:
            namespaceprefix_ = self.alertInformationHeader_nsprefix_ + ':' if (UseCapturedNS_ and self.alertInformationHeader_nsprefix_) else ''
            self.alertInformationHeader.export(outfile, level, namespaceprefix_, namespacedef_='', name_='alertInformationHeader', pretty_print=pretty_print)
        if self.alertInformationBody is not None:
            namespaceprefix_ = self.alertInformationBody_nsprefix_ + ':' if (UseCapturedNS_ and self.alertInformationBody_nsprefix_) else ''
            self.alertInformationBody.export(outfile, level, namespaceprefix_, namespacedef_='', name_='alertInformationBody', pretty_print=pretty_print)
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'alertInformationHeader':
            obj_ = PatientSummaryHeaderType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.alertInformationHeader = obj_
            obj_.original_tagname_ = 'alertInformationHeader'
        elif nodeName_ == 'alertInformationBody':
            obj_ = AlertInformationBodyType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.alertInformationBody = obj_
            obj_.original_tagname_ = 'alertInformationBody'
        else:
            content_ = self.gds_build_any(child_, 'AlertInformationType')
            self.anytypeobjs_.append(content_)
# end class AlertInformationType


class RelatedDiagnosisType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, documentId=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.documentId = documentId
        self.documentId_nsprefix_ = None
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, RelatedDiagnosisType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if RelatedDiagnosisType.subclass:
            return RelatedDiagnosisType.subclass(*args_, **kwargs_)
        else:
            return RelatedDiagnosisType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_documentId(self):
        return self.documentId
    def set_documentId(self, documentId):
        self.documentId = documentId
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.documentId is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:2"', name_='RelatedDiagnosisType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('RelatedDiagnosisType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'RelatedDiagnosisType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='RelatedDiagnosisType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='RelatedDiagnosisType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='RelatedDiagnosisType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:2"', name_='RelatedDiagnosisType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.documentId is not None:
            namespaceprefix_ = self.documentId_nsprefix_ + ':' if (UseCapturedNS_ and self.documentId_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sdocumentId>%s</%sdocumentId>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.documentId), input_name='documentId')), namespaceprefix_ , eol_))
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'documentId':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'documentId')
            value_ = self.gds_validate_string(value_, node, 'documentId')
            self.documentId = value_
            self.documentId_nsprefix_ = child_.prefix
        else:
            content_ = self.gds_build_any(child_, 'RelatedDiagnosisType')
            self.anytypeobjs_.append(content_)
# end class RelatedDiagnosisType


class AlertInformationBodyType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, typeOfAlertInformation=None, ascertainedDate=None, verifiedTime=None, validityTimePeriod=None, alertInformationComment=None, obsoleteTime=None, obsoleteComment=None, hypersensitivity=None, seriousDisease=None, treatment=None, communicableDisease=None, restrictionOfCare=None, unstructuredAlertInformation=None, relatedAlertInformation=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.typeOfAlertInformation = typeOfAlertInformation
        self.typeOfAlertInformation_nsprefix_ = "tns"
        self.ascertainedDate = ascertainedDate
        self.validate_DateType(self.ascertainedDate)
        self.ascertainedDate_nsprefix_ = "tns"
        self.verifiedTime = verifiedTime
        self.validate_TimeStampType(self.verifiedTime)
        self.verifiedTime_nsprefix_ = "tns"
        self.validityTimePeriod = validityTimePeriod
        self.validityTimePeriod_nsprefix_ = "tns"
        self.alertInformationComment = alertInformationComment
        self.alertInformationComment_nsprefix_ = "tns"
        self.obsoleteTime = obsoleteTime
        self.validate_TimeStampType(self.obsoleteTime)
        self.obsoleteTime_nsprefix_ = "tns"
        self.obsoleteComment = obsoleteComment
        self.obsoleteComment_nsprefix_ = "tns"
        self.hypersensitivity = hypersensitivity
        self.hypersensitivity_nsprefix_ = "tns"
        self.seriousDisease = seriousDisease
        self.seriousDisease_nsprefix_ = "tns"
        self.treatment = treatment
        self.treatment_nsprefix_ = "tns"
        self.communicableDisease = communicableDisease
        self.communicableDisease_nsprefix_ = "tns"
        self.restrictionOfCare = restrictionOfCare
        self.restrictionOfCare_nsprefix_ = "tns"
        self.unstructuredAlertInformation = unstructuredAlertInformation
        self.unstructuredAlertInformation_nsprefix_ = "tns"
        if relatedAlertInformation is None:
            self.relatedAlertInformation = []
        else:
            self.relatedAlertInformation = relatedAlertInformation
        self.relatedAlertInformation_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, AlertInformationBodyType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if AlertInformationBodyType.subclass:
            return AlertInformationBodyType.subclass(*args_, **kwargs_)
        else:
            return AlertInformationBodyType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_typeOfAlertInformation(self):
        return self.typeOfAlertInformation
    def set_typeOfAlertInformation(self, typeOfAlertInformation):
        self.typeOfAlertInformation = typeOfAlertInformation
    def get_ascertainedDate(self):
        return self.ascertainedDate
    def set_ascertainedDate(self, ascertainedDate):
        self.ascertainedDate = ascertainedDate
    def get_verifiedTime(self):
        return self.verifiedTime
    def set_verifiedTime(self, verifiedTime):
        self.verifiedTime = verifiedTime
    def get_validityTimePeriod(self):
        return self.validityTimePeriod
    def set_validityTimePeriod(self, validityTimePeriod):
        self.validityTimePeriod = validityTimePeriod
    def get_alertInformationComment(self):
        return self.alertInformationComment
    def set_alertInformationComment(self, alertInformationComment):
        self.alertInformationComment = alertInformationComment
    def get_obsoleteTime(self):
        return self.obsoleteTime
    def set_obsoleteTime(self, obsoleteTime):
        self.obsoleteTime = obsoleteTime
    def get_obsoleteComment(self):
        return self.obsoleteComment
    def set_obsoleteComment(self, obsoleteComment):
        self.obsoleteComment = obsoleteComment
    def get_hypersensitivity(self):
        return self.hypersensitivity
    def set_hypersensitivity(self, hypersensitivity):
        self.hypersensitivity = hypersensitivity
    def get_seriousDisease(self):
        return self.seriousDisease
    def set_seriousDisease(self, seriousDisease):
        self.seriousDisease = seriousDisease
    def get_treatment(self):
        return self.treatment
    def set_treatment(self, treatment):
        self.treatment = treatment
    def get_communicableDisease(self):
        return self.communicableDisease
    def set_communicableDisease(self, communicableDisease):
        self.communicableDisease = communicableDisease
    def get_restrictionOfCare(self):
        return self.restrictionOfCare
    def set_restrictionOfCare(self, restrictionOfCare):
        self.restrictionOfCare = restrictionOfCare
    def get_unstructuredAlertInformation(self):
        return self.unstructuredAlertInformation
    def set_unstructuredAlertInformation(self, unstructuredAlertInformation):
        self.unstructuredAlertInformation = unstructuredAlertInformation
    def get_relatedAlertInformation(self):
        return self.relatedAlertInformation
    def set_relatedAlertInformation(self, relatedAlertInformation):
        self.relatedAlertInformation = relatedAlertInformation
    def add_relatedAlertInformation(self, value):
        self.relatedAlertInformation.append(value)
    def insert_relatedAlertInformation_at(self, index, value):
        self.relatedAlertInformation.insert(index, value)
    def replace_relatedAlertInformation_at(self, index, value):
        self.relatedAlertInformation[index] = value
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def validate_DateType(self, value):
        result = True
        # Validate type DateType, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            if not self.gds_validate_simple_patterns(
                    self.validate_DateType_patterns_, value):
                self.gds_collector_.add_message('Value "%s" does not match xsd pattern restrictions: %s' % (encode_str_2_3(value), self.validate_DateType_patterns_, ))
                result = False
        return result
    validate_DateType_patterns_ = [['^((19|20)\\d\\d(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01]))$']]
    def validate_TimeStampType(self, value):
        result = True
        # Validate type TimeStampType, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            if not self.gds_validate_simple_patterns(
                    self.validate_TimeStampType_patterns_, value):
                self.gds_collector_.add_message('Value "%s" does not match xsd pattern restrictions: %s' % (encode_str_2_3(value), self.validate_TimeStampType_patterns_, ))
                result = False
        return result
    validate_TimeStampType_patterns_ = [['^((19|20)\\d\\d(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])(0[0-9]|1[0-9]|2[0123])([0-5])([0-9])([0-5])([0-9]))$']]
    def has__content(self):
        if (
            self.typeOfAlertInformation is not None or
            self.ascertainedDate is not None or
            self.verifiedTime is not None or
            self.validityTimePeriod is not None or
            self.alertInformationComment is not None or
            self.obsoleteTime is not None or
            self.obsoleteComment is not None or
            self.hypersensitivity is not None or
            self.seriousDisease is not None or
            self.treatment is not None or
            self.communicableDisease is not None or
            self.restrictionOfCare is not None or
            self.unstructuredAlertInformation is not None or
            self.relatedAlertInformation or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='AlertInformationBodyType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('AlertInformationBodyType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'AlertInformationBodyType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='AlertInformationBodyType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='AlertInformationBodyType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='AlertInformationBodyType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='AlertInformationBodyType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.typeOfAlertInformation is not None:
            namespaceprefix_ = self.typeOfAlertInformation_nsprefix_ + ':' if (UseCapturedNS_ and self.typeOfAlertInformation_nsprefix_) else ''
            self.typeOfAlertInformation.export(outfile, level, namespaceprefix_, namespacedef_='', name_='typeOfAlertInformation', pretty_print=pretty_print)
        if self.ascertainedDate is not None:
            namespaceprefix_ = self.ascertainedDate_nsprefix_ + ':' if (UseCapturedNS_ and self.ascertainedDate_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sascertainedDate>%s</%sascertainedDate>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.ascertainedDate), input_name='ascertainedDate')), namespaceprefix_ , eol_))
        if self.verifiedTime is not None:
            namespaceprefix_ = self.verifiedTime_nsprefix_ + ':' if (UseCapturedNS_ and self.verifiedTime_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sverifiedTime>%s</%sverifiedTime>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.verifiedTime), input_name='verifiedTime')), namespaceprefix_ , eol_))
        if self.validityTimePeriod is not None:
            namespaceprefix_ = self.validityTimePeriod_nsprefix_ + ':' if (UseCapturedNS_ and self.validityTimePeriod_nsprefix_) else ''
            self.validityTimePeriod.export(outfile, level, namespaceprefix_, namespacedef_='', name_='validityTimePeriod', pretty_print=pretty_print)
        if self.alertInformationComment is not None:
            namespaceprefix_ = self.alertInformationComment_nsprefix_ + ':' if (UseCapturedNS_ and self.alertInformationComment_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%salertInformationComment>%s</%salertInformationComment>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.alertInformationComment), input_name='alertInformationComment')), namespaceprefix_ , eol_))
        if self.obsoleteTime is not None:
            namespaceprefix_ = self.obsoleteTime_nsprefix_ + ':' if (UseCapturedNS_ and self.obsoleteTime_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sobsoleteTime>%s</%sobsoleteTime>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.obsoleteTime), input_name='obsoleteTime')), namespaceprefix_ , eol_))
        if self.obsoleteComment is not None:
            namespaceprefix_ = self.obsoleteComment_nsprefix_ + ':' if (UseCapturedNS_ and self.obsoleteComment_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sobsoleteComment>%s</%sobsoleteComment>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.obsoleteComment), input_name='obsoleteComment')), namespaceprefix_ , eol_))
        if self.hypersensitivity is not None:
            namespaceprefix_ = self.hypersensitivity_nsprefix_ + ':' if (UseCapturedNS_ and self.hypersensitivity_nsprefix_) else ''
            self.hypersensitivity.export(outfile, level, namespaceprefix_, namespacedef_='', name_='hypersensitivity', pretty_print=pretty_print)
        if self.seriousDisease is not None:
            namespaceprefix_ = self.seriousDisease_nsprefix_ + ':' if (UseCapturedNS_ and self.seriousDisease_nsprefix_) else ''
            self.seriousDisease.export(outfile, level, namespaceprefix_, namespacedef_='', name_='seriousDisease', pretty_print=pretty_print)
        if self.treatment is not None:
            namespaceprefix_ = self.treatment_nsprefix_ + ':' if (UseCapturedNS_ and self.treatment_nsprefix_) else ''
            self.treatment.export(outfile, level, namespaceprefix_, namespacedef_='', name_='treatment', pretty_print=pretty_print)
        if self.communicableDisease is not None:
            namespaceprefix_ = self.communicableDisease_nsprefix_ + ':' if (UseCapturedNS_ and self.communicableDisease_nsprefix_) else ''
            self.communicableDisease.export(outfile, level, namespaceprefix_, namespacedef_='', name_='communicableDisease', pretty_print=pretty_print)
        if self.restrictionOfCare is not None:
            namespaceprefix_ = self.restrictionOfCare_nsprefix_ + ':' if (UseCapturedNS_ and self.restrictionOfCare_nsprefix_) else ''
            self.restrictionOfCare.export(outfile, level, namespaceprefix_, namespacedef_='', name_='restrictionOfCare', pretty_print=pretty_print)
        if self.unstructuredAlertInformation is not None:
            namespaceprefix_ = self.unstructuredAlertInformation_nsprefix_ + ':' if (UseCapturedNS_ and self.unstructuredAlertInformation_nsprefix_) else ''
            self.unstructuredAlertInformation.export(outfile, level, namespaceprefix_, namespacedef_='', name_='unstructuredAlertInformation', pretty_print=pretty_print)
        for relatedAlertInformation_ in self.relatedAlertInformation:
            namespaceprefix_ = self.relatedAlertInformation_nsprefix_ + ':' if (UseCapturedNS_ and self.relatedAlertInformation_nsprefix_) else ''
            relatedAlertInformation_.export(outfile, level, namespaceprefix_, namespacedef_='', name_='relatedAlertInformation', pretty_print=pretty_print)
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'typeOfAlertInformation':
            obj_ = CVType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.typeOfAlertInformation = obj_
            obj_.original_tagname_ = 'typeOfAlertInformation'
        elif nodeName_ == 'ascertainedDate':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'ascertainedDate')
            value_ = self.gds_validate_string(value_, node, 'ascertainedDate')
            self.ascertainedDate = value_
            self.ascertainedDate_nsprefix_ = child_.prefix
            # validate type DateType
            self.validate_DateType(self.ascertainedDate)
        elif nodeName_ == 'verifiedTime':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'verifiedTime')
            value_ = self.gds_validate_string(value_, node, 'verifiedTime')
            self.verifiedTime = value_
            self.verifiedTime_nsprefix_ = child_.prefix
            # validate type TimeStampType
            self.validate_TimeStampType(self.verifiedTime)
        elif nodeName_ == 'validityTimePeriod':
            obj_ = TimePeriodType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.validityTimePeriod = obj_
            obj_.original_tagname_ = 'validityTimePeriod'
        elif nodeName_ == 'alertInformationComment':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'alertInformationComment')
            value_ = self.gds_validate_string(value_, node, 'alertInformationComment')
            self.alertInformationComment = value_
            self.alertInformationComment_nsprefix_ = child_.prefix
        elif nodeName_ == 'obsoleteTime':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'obsoleteTime')
            value_ = self.gds_validate_string(value_, node, 'obsoleteTime')
            self.obsoleteTime = value_
            self.obsoleteTime_nsprefix_ = child_.prefix
            # validate type TimeStampType
            self.validate_TimeStampType(self.obsoleteTime)
        elif nodeName_ == 'obsoleteComment':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'obsoleteComment')
            value_ = self.gds_validate_string(value_, node, 'obsoleteComment')
            self.obsoleteComment = value_
            self.obsoleteComment_nsprefix_ = child_.prefix
        elif nodeName_ == 'hypersensitivity':
            obj_ = HyperSensitivityType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.hypersensitivity = obj_
            obj_.original_tagname_ = 'hypersensitivity'
        elif nodeName_ == 'seriousDisease':
            obj_ = SeriousDiseaseType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.seriousDisease = obj_
            obj_.original_tagname_ = 'seriousDisease'
        elif nodeName_ == 'treatment':
            obj_ = TreatmentType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.treatment = obj_
            obj_.original_tagname_ = 'treatment'
        elif nodeName_ == 'communicableDisease':
            obj_ = CommunicableDiseaseType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.communicableDisease = obj_
            obj_.original_tagname_ = 'communicableDisease'
        elif nodeName_ == 'restrictionOfCare':
            obj_ = RestrictionOfCareType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.restrictionOfCare = obj_
            obj_.original_tagname_ = 'restrictionOfCare'
        elif nodeName_ == 'unstructuredAlertInformation':
            obj_ = UnstructuredAlertInformationType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.unstructuredAlertInformation = obj_
            obj_.original_tagname_ = 'unstructuredAlertInformation'
        elif nodeName_ == 'relatedAlertInformation':
            obj_ = RelatedAlertInformationType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.relatedAlertInformation.append(obj_)
            obj_.original_tagname_ = 'relatedAlertInformation'
        else:
            content_ = self.gds_build_any(child_, 'AlertInformationBodyType')
            self.anytypeobjs_.append(content_)
# end class AlertInformationBodyType


class HyperSensitivityType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, typeOfHypersensitivity=None, degreeOfSeverity=None, degreeOfCertainty=None, pharmaceuticalHypersensitivity=None, otherHypersensitivity=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.typeOfHypersensitivity = typeOfHypersensitivity
        self.typeOfHypersensitivity_nsprefix_ = "tns"
        self.degreeOfSeverity = degreeOfSeverity
        self.degreeOfSeverity_nsprefix_ = "tns"
        self.degreeOfCertainty = degreeOfCertainty
        self.degreeOfCertainty_nsprefix_ = "tns"
        self.pharmaceuticalHypersensitivity = pharmaceuticalHypersensitivity
        self.pharmaceuticalHypersensitivity_nsprefix_ = "tns"
        self.otherHypersensitivity = otherHypersensitivity
        self.otherHypersensitivity_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, HyperSensitivityType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if HyperSensitivityType.subclass:
            return HyperSensitivityType.subclass(*args_, **kwargs_)
        else:
            return HyperSensitivityType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_typeOfHypersensitivity(self):
        return self.typeOfHypersensitivity
    def set_typeOfHypersensitivity(self, typeOfHypersensitivity):
        self.typeOfHypersensitivity = typeOfHypersensitivity
    def get_degreeOfSeverity(self):
        return self.degreeOfSeverity
    def set_degreeOfSeverity(self, degreeOfSeverity):
        self.degreeOfSeverity = degreeOfSeverity
    def get_degreeOfCertainty(self):
        return self.degreeOfCertainty
    def set_degreeOfCertainty(self, degreeOfCertainty):
        self.degreeOfCertainty = degreeOfCertainty
    def get_pharmaceuticalHypersensitivity(self):
        return self.pharmaceuticalHypersensitivity
    def set_pharmaceuticalHypersensitivity(self, pharmaceuticalHypersensitivity):
        self.pharmaceuticalHypersensitivity = pharmaceuticalHypersensitivity
    def get_otherHypersensitivity(self):
        return self.otherHypersensitivity
    def set_otherHypersensitivity(self, otherHypersensitivity):
        self.otherHypersensitivity = otherHypersensitivity
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.typeOfHypersensitivity is not None or
            self.degreeOfSeverity is not None or
            self.degreeOfCertainty is not None or
            self.pharmaceuticalHypersensitivity is not None or
            self.otherHypersensitivity is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='HyperSensitivityType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('HyperSensitivityType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'HyperSensitivityType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='HyperSensitivityType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='HyperSensitivityType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='HyperSensitivityType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='HyperSensitivityType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.typeOfHypersensitivity is not None:
            namespaceprefix_ = self.typeOfHypersensitivity_nsprefix_ + ':' if (UseCapturedNS_ and self.typeOfHypersensitivity_nsprefix_) else ''
            self.typeOfHypersensitivity.export(outfile, level, namespaceprefix_, namespacedef_='', name_='typeOfHypersensitivity', pretty_print=pretty_print)
        if self.degreeOfSeverity is not None:
            namespaceprefix_ = self.degreeOfSeverity_nsprefix_ + ':' if (UseCapturedNS_ and self.degreeOfSeverity_nsprefix_) else ''
            self.degreeOfSeverity.export(outfile, level, namespaceprefix_, namespacedef_='', name_='degreeOfSeverity', pretty_print=pretty_print)
        if self.degreeOfCertainty is not None:
            namespaceprefix_ = self.degreeOfCertainty_nsprefix_ + ':' if (UseCapturedNS_ and self.degreeOfCertainty_nsprefix_) else ''
            self.degreeOfCertainty.export(outfile, level, namespaceprefix_, namespacedef_='', name_='degreeOfCertainty', pretty_print=pretty_print)
        if self.pharmaceuticalHypersensitivity is not None:
            namespaceprefix_ = self.pharmaceuticalHypersensitivity_nsprefix_ + ':' if (UseCapturedNS_ and self.pharmaceuticalHypersensitivity_nsprefix_) else ''
            self.pharmaceuticalHypersensitivity.export(outfile, level, namespaceprefix_, namespacedef_='', name_='pharmaceuticalHypersensitivity', pretty_print=pretty_print)
        if self.otherHypersensitivity is not None:
            namespaceprefix_ = self.otherHypersensitivity_nsprefix_ + ':' if (UseCapturedNS_ and self.otherHypersensitivity_nsprefix_) else ''
            self.otherHypersensitivity.export(outfile, level, namespaceprefix_, namespacedef_='', name_='otherHypersensitivity', pretty_print=pretty_print)
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'typeOfHypersensitivity':
            obj_ = CVType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.typeOfHypersensitivity = obj_
            obj_.original_tagname_ = 'typeOfHypersensitivity'
        elif nodeName_ == 'degreeOfSeverity':
            obj_ = CVType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.degreeOfSeverity = obj_
            obj_.original_tagname_ = 'degreeOfSeverity'
        elif nodeName_ == 'degreeOfCertainty':
            obj_ = CVType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.degreeOfCertainty = obj_
            obj_.original_tagname_ = 'degreeOfCertainty'
        elif nodeName_ == 'pharmaceuticalHypersensitivity':
            obj_ = PharmaceuticalHypersensitivityType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.pharmaceuticalHypersensitivity = obj_
            obj_.original_tagname_ = 'pharmaceuticalHypersensitivity'
        elif nodeName_ == 'otherHypersensitivity':
            obj_ = OtherHypersensitivityType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.otherHypersensitivity = obj_
            obj_.original_tagname_ = 'otherHypersensitivity'
        else:
            content_ = self.gds_build_any(child_, 'HyperSensitivityType')
            self.anytypeobjs_.append(content_)
# end class HyperSensitivityType


class PharmaceuticalHypersensitivityType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, atcSubstance=None, nonATCSubstance=None, nonATCSubstanceComment=None, pharmaceuticalProductId=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.atcSubstance = atcSubstance
        self.atcSubstance_nsprefix_ = "tns"
        self.nonATCSubstance = nonATCSubstance
        self.nonATCSubstance_nsprefix_ = "tns"
        self.nonATCSubstanceComment = nonATCSubstanceComment
        self.nonATCSubstanceComment_nsprefix_ = "tns"
        if pharmaceuticalProductId is None:
            self.pharmaceuticalProductId = []
        else:
            self.pharmaceuticalProductId = pharmaceuticalProductId
        self.pharmaceuticalProductId_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, PharmaceuticalHypersensitivityType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if PharmaceuticalHypersensitivityType.subclass:
            return PharmaceuticalHypersensitivityType.subclass(*args_, **kwargs_)
        else:
            return PharmaceuticalHypersensitivityType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_atcSubstance(self):
        return self.atcSubstance
    def set_atcSubstance(self, atcSubstance):
        self.atcSubstance = atcSubstance
    def get_nonATCSubstance(self):
        return self.nonATCSubstance
    def set_nonATCSubstance(self, nonATCSubstance):
        self.nonATCSubstance = nonATCSubstance
    def get_nonATCSubstanceComment(self):
        return self.nonATCSubstanceComment
    def set_nonATCSubstanceComment(self, nonATCSubstanceComment):
        self.nonATCSubstanceComment = nonATCSubstanceComment
    def get_pharmaceuticalProductId(self):
        return self.pharmaceuticalProductId
    def set_pharmaceuticalProductId(self, pharmaceuticalProductId):
        self.pharmaceuticalProductId = pharmaceuticalProductId
    def add_pharmaceuticalProductId(self, value):
        self.pharmaceuticalProductId.append(value)
    def insert_pharmaceuticalProductId_at(self, index, value):
        self.pharmaceuticalProductId.insert(index, value)
    def replace_pharmaceuticalProductId_at(self, index, value):
        self.pharmaceuticalProductId[index] = value
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.atcSubstance is not None or
            self.nonATCSubstance is not None or
            self.nonATCSubstanceComment is not None or
            self.pharmaceuticalProductId or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='PharmaceuticalHypersensitivityType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('PharmaceuticalHypersensitivityType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'PharmaceuticalHypersensitivityType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='PharmaceuticalHypersensitivityType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='PharmaceuticalHypersensitivityType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='PharmaceuticalHypersensitivityType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='PharmaceuticalHypersensitivityType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.atcSubstance is not None:
            namespaceprefix_ = self.atcSubstance_nsprefix_ + ':' if (UseCapturedNS_ and self.atcSubstance_nsprefix_) else ''
            self.atcSubstance.export(outfile, level, namespaceprefix_, namespacedef_='', name_='atcSubstance', pretty_print=pretty_print)
        if self.nonATCSubstance is not None:
            namespaceprefix_ = self.nonATCSubstance_nsprefix_ + ':' if (UseCapturedNS_ and self.nonATCSubstance_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%snonATCSubstance>%s</%snonATCSubstance>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.nonATCSubstance), input_name='nonATCSubstance')), namespaceprefix_ , eol_))
        if self.nonATCSubstanceComment is not None:
            namespaceprefix_ = self.nonATCSubstanceComment_nsprefix_ + ':' if (UseCapturedNS_ and self.nonATCSubstanceComment_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%snonATCSubstanceComment>%s</%snonATCSubstanceComment>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.nonATCSubstanceComment), input_name='nonATCSubstanceComment')), namespaceprefix_ , eol_))
        for pharmaceuticalProductId_ in self.pharmaceuticalProductId:
            namespaceprefix_ = self.pharmaceuticalProductId_nsprefix_ + ':' if (UseCapturedNS_ and self.pharmaceuticalProductId_nsprefix_) else ''
            pharmaceuticalProductId_.export(outfile, level, namespaceprefix_, namespacedef_='', name_='pharmaceuticalProductId', pretty_print=pretty_print)
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'atcSubstance':
            obj_ = CVType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.atcSubstance = obj_
            obj_.original_tagname_ = 'atcSubstance'
        elif nodeName_ == 'nonATCSubstance':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'nonATCSubstance')
            value_ = self.gds_validate_string(value_, node, 'nonATCSubstance')
            self.nonATCSubstance = value_
            self.nonATCSubstance_nsprefix_ = child_.prefix
        elif nodeName_ == 'nonATCSubstanceComment':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'nonATCSubstanceComment')
            value_ = self.gds_validate_string(value_, node, 'nonATCSubstanceComment')
            self.nonATCSubstanceComment = value_
            self.nonATCSubstanceComment_nsprefix_ = child_.prefix
        elif nodeName_ == 'pharmaceuticalProductId':
            obj_ = CVType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.pharmaceuticalProductId.append(obj_)
            obj_.original_tagname_ = 'pharmaceuticalProductId'
        else:
            content_ = self.gds_build_any(child_, 'PharmaceuticalHypersensitivityType')
            self.anytypeobjs_.append(content_)
# end class PharmaceuticalHypersensitivityType


class OtherHypersensitivityType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, hypersensitivityAgent=None, hypersensitivityAgentCode=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.hypersensitivityAgent = hypersensitivityAgent
        self.hypersensitivityAgent_nsprefix_ = "tns"
        self.hypersensitivityAgentCode = hypersensitivityAgentCode
        self.hypersensitivityAgentCode_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, OtherHypersensitivityType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if OtherHypersensitivityType.subclass:
            return OtherHypersensitivityType.subclass(*args_, **kwargs_)
        else:
            return OtherHypersensitivityType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_hypersensitivityAgent(self):
        return self.hypersensitivityAgent
    def set_hypersensitivityAgent(self, hypersensitivityAgent):
        self.hypersensitivityAgent = hypersensitivityAgent
    def get_hypersensitivityAgentCode(self):
        return self.hypersensitivityAgentCode
    def set_hypersensitivityAgentCode(self, hypersensitivityAgentCode):
        self.hypersensitivityAgentCode = hypersensitivityAgentCode
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.hypersensitivityAgent is not None or
            self.hypersensitivityAgentCode is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='OtherHypersensitivityType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('OtherHypersensitivityType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'OtherHypersensitivityType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='OtherHypersensitivityType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='OtherHypersensitivityType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='OtherHypersensitivityType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='OtherHypersensitivityType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.hypersensitivityAgent is not None:
            namespaceprefix_ = self.hypersensitivityAgent_nsprefix_ + ':' if (UseCapturedNS_ and self.hypersensitivityAgent_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%shypersensitivityAgent>%s</%shypersensitivityAgent>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.hypersensitivityAgent), input_name='hypersensitivityAgent')), namespaceprefix_ , eol_))
        if self.hypersensitivityAgentCode is not None:
            namespaceprefix_ = self.hypersensitivityAgentCode_nsprefix_ + ':' if (UseCapturedNS_ and self.hypersensitivityAgentCode_nsprefix_) else ''
            self.hypersensitivityAgentCode.export(outfile, level, namespaceprefix_, namespacedef_='', name_='hypersensitivityAgentCode', pretty_print=pretty_print)
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'hypersensitivityAgent':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'hypersensitivityAgent')
            value_ = self.gds_validate_string(value_, node, 'hypersensitivityAgent')
            self.hypersensitivityAgent = value_
            self.hypersensitivityAgent_nsprefix_ = child_.prefix
        elif nodeName_ == 'hypersensitivityAgentCode':
            obj_ = CVType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.hypersensitivityAgentCode = obj_
            obj_.original_tagname_ = 'hypersensitivityAgentCode'
        else:
            content_ = self.gds_build_any(child_, 'OtherHypersensitivityType')
            self.anytypeobjs_.append(content_)
# end class OtherHypersensitivityType


class SeriousDiseaseType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, disease=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.disease = disease
        self.disease_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, SeriousDiseaseType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if SeriousDiseaseType.subclass:
            return SeriousDiseaseType.subclass(*args_, **kwargs_)
        else:
            return SeriousDiseaseType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_disease(self):
        return self.disease
    def set_disease(self, disease):
        self.disease = disease
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.disease is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='SeriousDiseaseType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('SeriousDiseaseType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'SeriousDiseaseType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='SeriousDiseaseType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='SeriousDiseaseType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='SeriousDiseaseType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='SeriousDiseaseType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.disease is not None:
            namespaceprefix_ = self.disease_nsprefix_ + ':' if (UseCapturedNS_ and self.disease_nsprefix_) else ''
            self.disease.export(outfile, level, namespaceprefix_, namespacedef_='', name_='disease', pretty_print=pretty_print)
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'disease':
            obj_ = CVType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.disease = obj_
            obj_.original_tagname_ = 'disease'
        else:
            content_ = self.gds_build_any(child_, 'SeriousDiseaseType')
            self.anytypeobjs_.append(content_)
# end class SeriousDiseaseType


class TreatmentType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, treatmentDescription=None, treatmentCode=None, pharmaceuticalTreatment=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.treatmentDescription = treatmentDescription
        self.treatmentDescription_nsprefix_ = "tns"
        self.treatmentCode = treatmentCode
        self.treatmentCode_nsprefix_ = "tns"
        if pharmaceuticalTreatment is None:
            self.pharmaceuticalTreatment = []
        else:
            self.pharmaceuticalTreatment = pharmaceuticalTreatment
        self.pharmaceuticalTreatment_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, TreatmentType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if TreatmentType.subclass:
            return TreatmentType.subclass(*args_, **kwargs_)
        else:
            return TreatmentType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_treatmentDescription(self):
        return self.treatmentDescription
    def set_treatmentDescription(self, treatmentDescription):
        self.treatmentDescription = treatmentDescription
    def get_treatmentCode(self):
        return self.treatmentCode
    def set_treatmentCode(self, treatmentCode):
        self.treatmentCode = treatmentCode
    def get_pharmaceuticalTreatment(self):
        return self.pharmaceuticalTreatment
    def set_pharmaceuticalTreatment(self, pharmaceuticalTreatment):
        self.pharmaceuticalTreatment = pharmaceuticalTreatment
    def add_pharmaceuticalTreatment(self, value):
        self.pharmaceuticalTreatment.append(value)
    def insert_pharmaceuticalTreatment_at(self, index, value):
        self.pharmaceuticalTreatment.insert(index, value)
    def replace_pharmaceuticalTreatment_at(self, index, value):
        self.pharmaceuticalTreatment[index] = value
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.treatmentDescription is not None or
            self.treatmentCode is not None or
            self.pharmaceuticalTreatment or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='TreatmentType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('TreatmentType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'TreatmentType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='TreatmentType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='TreatmentType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='TreatmentType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='TreatmentType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.treatmentDescription is not None:
            namespaceprefix_ = self.treatmentDescription_nsprefix_ + ':' if (UseCapturedNS_ and self.treatmentDescription_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%streatmentDescription>%s</%streatmentDescription>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.treatmentDescription), input_name='treatmentDescription')), namespaceprefix_ , eol_))
        if self.treatmentCode is not None:
            namespaceprefix_ = self.treatmentCode_nsprefix_ + ':' if (UseCapturedNS_ and self.treatmentCode_nsprefix_) else ''
            self.treatmentCode.export(outfile, level, namespaceprefix_, namespacedef_='', name_='treatmentCode', pretty_print=pretty_print)
        for pharmaceuticalTreatment_ in self.pharmaceuticalTreatment:
            namespaceprefix_ = self.pharmaceuticalTreatment_nsprefix_ + ':' if (UseCapturedNS_ and self.pharmaceuticalTreatment_nsprefix_) else ''
            pharmaceuticalTreatment_.export(outfile, level, namespaceprefix_, namespacedef_='', name_='pharmaceuticalTreatment', pretty_print=pretty_print)
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'treatmentDescription':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'treatmentDescription')
            value_ = self.gds_validate_string(value_, node, 'treatmentDescription')
            self.treatmentDescription = value_
            self.treatmentDescription_nsprefix_ = child_.prefix
        elif nodeName_ == 'treatmentCode':
            obj_ = CVType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.treatmentCode = obj_
            obj_.original_tagname_ = 'treatmentCode'
        elif nodeName_ == 'pharmaceuticalTreatment':
            obj_ = CVType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.pharmaceuticalTreatment.append(obj_)
            obj_.original_tagname_ = 'pharmaceuticalTreatment'
        else:
            content_ = self.gds_build_any(child_, 'TreatmentType')
            self.anytypeobjs_.append(content_)
# end class TreatmentType


class CommunicableDiseaseType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, communicableDiseaseCode=None, routeOfTransmission=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.communicableDiseaseCode = communicableDiseaseCode
        self.communicableDiseaseCode_nsprefix_ = "tns"
        self.routeOfTransmission = routeOfTransmission
        self.routeOfTransmission_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, CommunicableDiseaseType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if CommunicableDiseaseType.subclass:
            return CommunicableDiseaseType.subclass(*args_, **kwargs_)
        else:
            return CommunicableDiseaseType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_communicableDiseaseCode(self):
        return self.communicableDiseaseCode
    def set_communicableDiseaseCode(self, communicableDiseaseCode):
        self.communicableDiseaseCode = communicableDiseaseCode
    def get_routeOfTransmission(self):
        return self.routeOfTransmission
    def set_routeOfTransmission(self, routeOfTransmission):
        self.routeOfTransmission = routeOfTransmission
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.communicableDiseaseCode is not None or
            self.routeOfTransmission is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='CommunicableDiseaseType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('CommunicableDiseaseType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'CommunicableDiseaseType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='CommunicableDiseaseType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='CommunicableDiseaseType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='CommunicableDiseaseType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='CommunicableDiseaseType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.communicableDiseaseCode is not None:
            namespaceprefix_ = self.communicableDiseaseCode_nsprefix_ + ':' if (UseCapturedNS_ and self.communicableDiseaseCode_nsprefix_) else ''
            self.communicableDiseaseCode.export(outfile, level, namespaceprefix_, namespacedef_='', name_='communicableDiseaseCode', pretty_print=pretty_print)
        if self.routeOfTransmission is not None:
            namespaceprefix_ = self.routeOfTransmission_nsprefix_ + ':' if (UseCapturedNS_ and self.routeOfTransmission_nsprefix_) else ''
            self.routeOfTransmission.export(outfile, level, namespaceprefix_, namespacedef_='', name_='routeOfTransmission', pretty_print=pretty_print)
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'communicableDiseaseCode':
            obj_ = CVType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.communicableDiseaseCode = obj_
            obj_.original_tagname_ = 'communicableDiseaseCode'
        elif nodeName_ == 'routeOfTransmission':
            obj_ = CVType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.routeOfTransmission = obj_
            obj_.original_tagname_ = 'routeOfTransmission'
        else:
            content_ = self.gds_build_any(child_, 'CommunicableDiseaseType')
            self.anytypeobjs_.append(content_)
# end class CommunicableDiseaseType


class RestrictionOfCareType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, restrictionOfCareComment=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.restrictionOfCareComment = restrictionOfCareComment
        self.restrictionOfCareComment_nsprefix_ = None
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, RestrictionOfCareType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if RestrictionOfCareType.subclass:
            return RestrictionOfCareType.subclass(*args_, **kwargs_)
        else:
            return RestrictionOfCareType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_restrictionOfCareComment(self):
        return self.restrictionOfCareComment
    def set_restrictionOfCareComment(self, restrictionOfCareComment):
        self.restrictionOfCareComment = restrictionOfCareComment
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.restrictionOfCareComment is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:2"', name_='RestrictionOfCareType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('RestrictionOfCareType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'RestrictionOfCareType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='RestrictionOfCareType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='RestrictionOfCareType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='RestrictionOfCareType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:2"', name_='RestrictionOfCareType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.restrictionOfCareComment is not None:
            namespaceprefix_ = self.restrictionOfCareComment_nsprefix_ + ':' if (UseCapturedNS_ and self.restrictionOfCareComment_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%srestrictionOfCareComment>%s</%srestrictionOfCareComment>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.restrictionOfCareComment), input_name='restrictionOfCareComment')), namespaceprefix_ , eol_))
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'restrictionOfCareComment':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'restrictionOfCareComment')
            value_ = self.gds_validate_string(value_, node, 'restrictionOfCareComment')
            self.restrictionOfCareComment = value_
            self.restrictionOfCareComment_nsprefix_ = child_.prefix
        else:
            content_ = self.gds_build_any(child_, 'RestrictionOfCareType')
            self.anytypeobjs_.append(content_)
# end class RestrictionOfCareType


class UnstructuredAlertInformationType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, unstructuredAlertInformationHeading=None, unstructuredAlertInformationContent=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.unstructuredAlertInformationHeading = unstructuredAlertInformationHeading
        self.unstructuredAlertInformationHeading_nsprefix_ = None
        self.unstructuredAlertInformationContent = unstructuredAlertInformationContent
        self.unstructuredAlertInformationContent_nsprefix_ = None
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, UnstructuredAlertInformationType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if UnstructuredAlertInformationType.subclass:
            return UnstructuredAlertInformationType.subclass(*args_, **kwargs_)
        else:
            return UnstructuredAlertInformationType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_unstructuredAlertInformationHeading(self):
        return self.unstructuredAlertInformationHeading
    def set_unstructuredAlertInformationHeading(self, unstructuredAlertInformationHeading):
        self.unstructuredAlertInformationHeading = unstructuredAlertInformationHeading
    def get_unstructuredAlertInformationContent(self):
        return self.unstructuredAlertInformationContent
    def set_unstructuredAlertInformationContent(self, unstructuredAlertInformationContent):
        self.unstructuredAlertInformationContent = unstructuredAlertInformationContent
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.unstructuredAlertInformationHeading is not None or
            self.unstructuredAlertInformationContent is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:2"', name_='UnstructuredAlertInformationType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('UnstructuredAlertInformationType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'UnstructuredAlertInformationType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='UnstructuredAlertInformationType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='UnstructuredAlertInformationType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='UnstructuredAlertInformationType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:2"', name_='UnstructuredAlertInformationType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.unstructuredAlertInformationHeading is not None:
            namespaceprefix_ = self.unstructuredAlertInformationHeading_nsprefix_ + ':' if (UseCapturedNS_ and self.unstructuredAlertInformationHeading_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sunstructuredAlertInformationHeading>%s</%sunstructuredAlertInformationHeading>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.unstructuredAlertInformationHeading), input_name='unstructuredAlertInformationHeading')), namespaceprefix_ , eol_))
        if self.unstructuredAlertInformationContent is not None:
            namespaceprefix_ = self.unstructuredAlertInformationContent_nsprefix_ + ':' if (UseCapturedNS_ and self.unstructuredAlertInformationContent_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sunstructuredAlertInformationContent>%s</%sunstructuredAlertInformationContent>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.unstructuredAlertInformationContent), input_name='unstructuredAlertInformationContent')), namespaceprefix_ , eol_))
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'unstructuredAlertInformationHeading':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'unstructuredAlertInformationHeading')
            value_ = self.gds_validate_string(value_, node, 'unstructuredAlertInformationHeading')
            self.unstructuredAlertInformationHeading = value_
            self.unstructuredAlertInformationHeading_nsprefix_ = child_.prefix
        elif nodeName_ == 'unstructuredAlertInformationContent':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'unstructuredAlertInformationContent')
            value_ = self.gds_validate_string(value_, node, 'unstructuredAlertInformationContent')
            self.unstructuredAlertInformationContent = value_
            self.unstructuredAlertInformationContent_nsprefix_ = child_.prefix
        else:
            content_ = self.gds_build_any(child_, 'UnstructuredAlertInformationType')
            self.anytypeobjs_.append(content_)
# end class UnstructuredAlertInformationType


class RelatedAlertInformationType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, typeOfAlertInformationRelationship=None, relationComment=None, documentId=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.typeOfAlertInformationRelationship = typeOfAlertInformationRelationship
        self.typeOfAlertInformationRelationship_nsprefix_ = "tns"
        self.relationComment = relationComment
        self.relationComment_nsprefix_ = "tns"
        if documentId is None:
            self.documentId = []
        else:
            self.documentId = documentId
        self.documentId_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, RelatedAlertInformationType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if RelatedAlertInformationType.subclass:
            return RelatedAlertInformationType.subclass(*args_, **kwargs_)
        else:
            return RelatedAlertInformationType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_typeOfAlertInformationRelationship(self):
        return self.typeOfAlertInformationRelationship
    def set_typeOfAlertInformationRelationship(self, typeOfAlertInformationRelationship):
        self.typeOfAlertInformationRelationship = typeOfAlertInformationRelationship
    def get_relationComment(self):
        return self.relationComment
    def set_relationComment(self, relationComment):
        self.relationComment = relationComment
    def get_documentId(self):
        return self.documentId
    def set_documentId(self, documentId):
        self.documentId = documentId
    def add_documentId(self, value):
        self.documentId.append(value)
    def insert_documentId_at(self, index, value):
        self.documentId.insert(index, value)
    def replace_documentId_at(self, index, value):
        self.documentId[index] = value
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.typeOfAlertInformationRelationship is not None or
            self.relationComment is not None or
            self.documentId or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='RelatedAlertInformationType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('RelatedAlertInformationType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'RelatedAlertInformationType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='RelatedAlertInformationType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='RelatedAlertInformationType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='RelatedAlertInformationType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='RelatedAlertInformationType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.typeOfAlertInformationRelationship is not None:
            namespaceprefix_ = self.typeOfAlertInformationRelationship_nsprefix_ + ':' if (UseCapturedNS_ and self.typeOfAlertInformationRelationship_nsprefix_) else ''
            self.typeOfAlertInformationRelationship.export(outfile, level, namespaceprefix_, namespacedef_='', name_='typeOfAlertInformationRelationship', pretty_print=pretty_print)
        if self.relationComment is not None:
            namespaceprefix_ = self.relationComment_nsprefix_ + ':' if (UseCapturedNS_ and self.relationComment_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%srelationComment>%s</%srelationComment>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.relationComment), input_name='relationComment')), namespaceprefix_ , eol_))
        for documentId_ in self.documentId:
            namespaceprefix_ = self.documentId_nsprefix_ + ':' if (UseCapturedNS_ and self.documentId_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sdocumentId>%s</%sdocumentId>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(documentId_), input_name='documentId')), namespaceprefix_ , eol_))
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'typeOfAlertInformationRelationship':
            obj_ = CVType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.typeOfAlertInformationRelationship = obj_
            obj_.original_tagname_ = 'typeOfAlertInformationRelationship'
        elif nodeName_ == 'relationComment':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'relationComment')
            value_ = self.gds_validate_string(value_, node, 'relationComment')
            self.relationComment = value_
            self.relationComment_nsprefix_ = child_.prefix
        elif nodeName_ == 'documentId':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'documentId')
            value_ = self.gds_validate_string(value_, node, 'documentId')
            self.documentId.append(value_)
            self.documentId_nsprefix_ = child_.prefix
        else:
            content_ = self.gds_build_any(child_, 'RelatedAlertInformationType')
            self.anytypeobjs_.append(content_)
# end class RelatedAlertInformationType


class PADLType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, typeOfAssessment=None, assessment=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.typeOfAssessment = typeOfAssessment
        self.typeOfAssessment_nsprefix_ = "tns"
        self.assessment = assessment
        self.assessment_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, PADLType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if PADLType.subclass:
            return PADLType.subclass(*args_, **kwargs_)
        else:
            return PADLType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_typeOfAssessment(self):
        return self.typeOfAssessment
    def set_typeOfAssessment(self, typeOfAssessment):
        self.typeOfAssessment = typeOfAssessment
    def get_assessment(self):
        return self.assessment
    def set_assessment(self, assessment):
        self.assessment = assessment
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.typeOfAssessment is not None or
            self.assessment is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='PADLType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('PADLType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'PADLType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='PADLType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='PADLType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='PADLType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='PADLType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.typeOfAssessment is not None:
            namespaceprefix_ = self.typeOfAssessment_nsprefix_ + ':' if (UseCapturedNS_ and self.typeOfAssessment_nsprefix_) else ''
            self.typeOfAssessment.export(outfile, level, namespaceprefix_, namespacedef_='', name_='typeOfAssessment', pretty_print=pretty_print)
        if self.assessment is not None:
            namespaceprefix_ = self.assessment_nsprefix_ + ':' if (UseCapturedNS_ and self.assessment_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sassessment>%s</%sassessment>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.assessment), input_name='assessment')), namespaceprefix_ , eol_))
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'typeOfAssessment':
            obj_ = CVType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.typeOfAssessment = obj_
            obj_.original_tagname_ = 'typeOfAssessment'
        elif nodeName_ == 'assessment':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'assessment')
            value_ = self.gds_validate_string(value_, node, 'assessment')
            self.assessment = value_
            self.assessment_nsprefix_ = child_.prefix
        else:
            content_ = self.gds_build_any(child_, 'PADLType')
            self.anytypeobjs_.append(content_)
# end class PADLType


class DisabilityType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, disabilityAssessment=None, comment=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.disabilityAssessment = disabilityAssessment
        self.disabilityAssessment_nsprefix_ = "tns"
        self.comment = comment
        self.comment_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, DisabilityType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if DisabilityType.subclass:
            return DisabilityType.subclass(*args_, **kwargs_)
        else:
            return DisabilityType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_disabilityAssessment(self):
        return self.disabilityAssessment
    def set_disabilityAssessment(self, disabilityAssessment):
        self.disabilityAssessment = disabilityAssessment
    def get_comment(self):
        return self.comment
    def set_comment(self, comment):
        self.comment = comment
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.disabilityAssessment is not None or
            self.comment is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='DisabilityType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('DisabilityType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'DisabilityType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='DisabilityType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='DisabilityType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='DisabilityType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='DisabilityType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.disabilityAssessment is not None:
            namespaceprefix_ = self.disabilityAssessment_nsprefix_ + ':' if (UseCapturedNS_ and self.disabilityAssessment_nsprefix_) else ''
            self.disabilityAssessment.export(outfile, level, namespaceprefix_, namespacedef_='', name_='disabilityAssessment', pretty_print=pretty_print)
        if self.comment is not None:
            namespaceprefix_ = self.comment_nsprefix_ + ':' if (UseCapturedNS_ and self.comment_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%scomment>%s</%scomment>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.comment), input_name='comment')), namespaceprefix_ , eol_))
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'disabilityAssessment':
            obj_ = CVType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.disabilityAssessment = obj_
            obj_.original_tagname_ = 'disabilityAssessment'
        elif nodeName_ == 'comment':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'comment')
            value_ = self.gds_validate_string(value_, node, 'comment')
            self.comment = value_
            self.comment_nsprefix_ = child_.prefix
        else:
            content_ = self.gds_build_any(child_, 'DisabilityType')
            self.anytypeobjs_.append(content_)
# end class DisabilityType


class DatePeriodType(GeneratedsSuper):
    """DatePeriodType --  Anv
    ä
    nds f
    ö
    r att specificera ett datumintervall med hj
    ä
    lp av start-
    och slutdatum. start: Startdatum p
    å
    formatet
    Å
    Å
    Å
    Å
    MMDD end: Slutdatum p
    å
    formatet
    Å
    Å
    Å
    Å
    MMDD
    
    """
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, start=None, end=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.start = start
        self.validate_DateType(self.start)
        self.start_nsprefix_ = "tns"
        self.end = end
        self.validate_DateType(self.end)
        self.end_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, DatePeriodType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if DatePeriodType.subclass:
            return DatePeriodType.subclass(*args_, **kwargs_)
        else:
            return DatePeriodType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_start(self):
        return self.start
    def set_start(self, start):
        self.start = start
    def get_end(self):
        return self.end
    def set_end(self, end):
        self.end = end
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def validate_DateType(self, value):
        result = True
        # Validate type DateType, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            if not self.gds_validate_simple_patterns(
                    self.validate_DateType_patterns_, value):
                self.gds_collector_.add_message('Value "%s" does not match xsd pattern restrictions: %s' % (encode_str_2_3(value), self.validate_DateType_patterns_, ))
                result = False
        return result
    validate_DateType_patterns_ = [['^((19|20)\\d\\d(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01]))$']]
    def has__content(self):
        if (
            self.start is not None or
            self.end is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='DatePeriodType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('DatePeriodType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'DatePeriodType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='DatePeriodType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='DatePeriodType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='DatePeriodType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='DatePeriodType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.start is not None:
            namespaceprefix_ = self.start_nsprefix_ + ':' if (UseCapturedNS_ and self.start_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sstart>%s</%sstart>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.start), input_name='start')), namespaceprefix_ , eol_))
        if self.end is not None:
            namespaceprefix_ = self.end_nsprefix_ + ':' if (UseCapturedNS_ and self.end_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%send>%s</%send>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.end), input_name='end')), namespaceprefix_ , eol_))
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'start':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'start')
            value_ = self.gds_validate_string(value_, node, 'start')
            self.start = value_
            self.start_nsprefix_ = child_.prefix
            # validate type DateType
            self.validate_DateType(self.start)
        elif nodeName_ == 'end':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'end')
            value_ = self.gds_validate_string(value_, node, 'end')
            self.end = value_
            self.end_nsprefix_ = child_.prefix
            # validate type DateType
            self.validate_DateType(self.end)
        else:
            content_ = self.gds_build_any(child_, 'DatePeriodType')
            self.anytypeobjs_.append(content_)
# end class DatePeriodType


class MultimediaType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, mediaType=None, value=None, reference=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.mediaType = mediaType
        self.validate_MediaTypeEnum(self.mediaType)
        self.mediaType_nsprefix_ = "codes"
        self.value = value
        self.value_nsprefix_ = None
        self.reference = reference
        self.reference_nsprefix_ = None
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, MultimediaType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if MultimediaType.subclass:
            return MultimediaType.subclass(*args_, **kwargs_)
        else:
            return MultimediaType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_mediaType(self):
        return self.mediaType
    def set_mediaType(self, mediaType):
        self.mediaType = mediaType
    def get_value(self):
        return self.value
    def set_value(self, value):
        self.value = value
    def get_reference(self):
        return self.reference
    def set_reference(self, reference):
        self.reference = reference
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def validate_MediaTypeEnum(self, value):
        result = True
        # Validate type MediaTypeEnum, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            value = value
            enumerations = ['application/dicom', 'application/msword', 'application/pdf', 'audio/basic', 'audio/k32adpcm', 'audio/mpeg', 'image/g3fax', 'image/gif', 'image/jpeg', 'image/png', 'image/tiff', 'model/vrml', 'multipart/x-hl7-cda-level1', 'text/html', 'text/plain', 'text/rtf', 'text/sgml', 'text/x-hl7-ft', 'text/xml', 'video/mpeg', 'video/x-avi']
            if value not in enumerations:
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s does not match xsd enumeration restriction on MediaTypeEnum' % {"value" : encode_str_2_3(value), "lineno": lineno} )
                result = False
        return result
    def has__content(self):
        if (
            self.mediaType is not None or
            self.value is not None or
            self.reference is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:codes="urn:riv:clinicalprocess:healthcond:description:enums:2"  xmlns:tns="urn:riv:clinicalprocess:healthcond:description:2"', name_='MultimediaType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('MultimediaType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'MultimediaType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='MultimediaType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='MultimediaType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='MultimediaType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:codes="urn:riv:clinicalprocess:healthcond:description:enums:2"  xmlns:tns="urn:riv:clinicalprocess:healthcond:description:2"', name_='MultimediaType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.mediaType is not None:
            namespaceprefix_ = self.mediaType_nsprefix_ + ':' if (UseCapturedNS_ and self.mediaType_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%smediaType>%s</%smediaType>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.mediaType), input_name='mediaType')), namespaceprefix_ , eol_))
        if self.value is not None:
            namespaceprefix_ = self.value_nsprefix_ + ':' if (UseCapturedNS_ and self.value_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%svalue>%s</%svalue>%s' % (namespaceprefix_ , self.gds_format_base64(self.value, input_name='value'), namespaceprefix_ , eol_))
        if self.reference is not None:
            namespaceprefix_ = self.reference_nsprefix_ + ':' if (UseCapturedNS_ and self.reference_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sreference>%s</%sreference>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.reference), input_name='reference')), namespaceprefix_ , eol_))
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'mediaType':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'mediaType')
            value_ = self.gds_validate_string(value_, node, 'mediaType')
            self.mediaType = value_
            self.mediaType_nsprefix_ = child_.prefix
            # validate type MediaTypeEnum
            self.validate_MediaTypeEnum(self.mediaType)
        elif nodeName_ == 'value':
            sval_ = child_.text
            if sval_ is not None:
                try:
                    bval_ = base64.b64decode(sval_)
                except (TypeError, ValueError) as exp:
                    raise_parse_error(child_, 'requires base64 encoded string: %s' % exp)
                bval_ = self.gds_validate_base64(bval_, node, 'value')
            else:
                bval_ = None
            self.value = bval_
            self.value_nsprefix_ = child_.prefix
        elif nodeName_ == 'reference':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'reference')
            value_ = self.gds_validate_string(value_, node, 'reference')
            self.reference = value_
            self.reference_nsprefix_ = child_.prefix
        else:
            content_ = self.gds_build_any(child_, 'MultimediaType')
            self.anytypeobjs_.append(content_)
# end class MultimediaType


class TimePeriodType(GeneratedsSuper):
    """TimePeriodType --  Anv
    ä
    nds f
    ö
    r att specificera ett datumintervall med hj
    ä
    lp av start-
    och slutdatum. start: Startdatum p
    å
    formatet YYYYMMDDhhmmss end: Slutdatum p
    å
    formatet YYYYMMDDhhmmss
    
    """
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, start=None, end=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.start = start
        self.validate_TimeStampType(self.start)
        self.start_nsprefix_ = "tns"
        self.end = end
        self.validate_TimeStampType(self.end)
        self.end_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, TimePeriodType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if TimePeriodType.subclass:
            return TimePeriodType.subclass(*args_, **kwargs_)
        else:
            return TimePeriodType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_start(self):
        return self.start
    def set_start(self, start):
        self.start = start
    def get_end(self):
        return self.end
    def set_end(self, end):
        self.end = end
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def validate_TimeStampType(self, value):
        result = True
        # Validate type TimeStampType, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            if not self.gds_validate_simple_patterns(
                    self.validate_TimeStampType_patterns_, value):
                self.gds_collector_.add_message('Value "%s" does not match xsd pattern restrictions: %s' % (encode_str_2_3(value), self.validate_TimeStampType_patterns_, ))
                result = False
        return result
    validate_TimeStampType_patterns_ = [['^((19|20)\\d\\d(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])(0[0-9]|1[0-9]|2[0123])([0-5])([0-9])([0-5])([0-9]))$']]
    def has__content(self):
        if (
            self.start is not None or
            self.end is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='TimePeriodType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('TimePeriodType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'TimePeriodType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='TimePeriodType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='TimePeriodType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='TimePeriodType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='TimePeriodType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.start is not None:
            namespaceprefix_ = self.start_nsprefix_ + ':' if (UseCapturedNS_ and self.start_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sstart>%s</%sstart>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.start), input_name='start')), namespaceprefix_ , eol_))
        if self.end is not None:
            namespaceprefix_ = self.end_nsprefix_ + ':' if (UseCapturedNS_ and self.end_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%send>%s</%send>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.end), input_name='end')), namespaceprefix_ , eol_))
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'start':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'start')
            value_ = self.gds_validate_string(value_, node, 'start')
            self.start = value_
            self.start_nsprefix_ = child_.prefix
            # validate type TimeStampType
            self.validate_TimeStampType(self.start)
        elif nodeName_ == 'end':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'end')
            value_ = self.gds_validate_string(value_, node, 'end')
            self.end = value_
            self.end_nsprefix_ = child_.prefix
            # validate type TimeStampType
            self.validate_TimeStampType(self.end)
        else:
            content_ = self.gds_build_any(child_, 'TimePeriodType')
            self.anytypeobjs_.append(content_)
# end class TimePeriodType


class CVType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, code=None, codeSystem=None, codeSystemName=None, codeSystemVersion=None, displayName=None, originalText=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.code = code
        self.code_nsprefix_ = None
        self.codeSystem = codeSystem
        self.codeSystem_nsprefix_ = None
        self.codeSystemName = codeSystemName
        self.codeSystemName_nsprefix_ = None
        self.codeSystemVersion = codeSystemVersion
        self.codeSystemVersion_nsprefix_ = None
        self.displayName = displayName
        self.displayName_nsprefix_ = None
        self.originalText = originalText
        self.originalText_nsprefix_ = None
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, CVType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if CVType.subclass:
            return CVType.subclass(*args_, **kwargs_)
        else:
            return CVType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_code(self):
        return self.code
    def set_code(self, code):
        self.code = code
    def get_codeSystem(self):
        return self.codeSystem
    def set_codeSystem(self, codeSystem):
        self.codeSystem = codeSystem
    def get_codeSystemName(self):
        return self.codeSystemName
    def set_codeSystemName(self, codeSystemName):
        self.codeSystemName = codeSystemName
    def get_codeSystemVersion(self):
        return self.codeSystemVersion
    def set_codeSystemVersion(self, codeSystemVersion):
        self.codeSystemVersion = codeSystemVersion
    def get_displayName(self):
        return self.displayName
    def set_displayName(self, displayName):
        self.displayName = displayName
    def get_originalText(self):
        return self.originalText
    def set_originalText(self, originalText):
        self.originalText = originalText
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.code is not None or
            self.codeSystem is not None or
            self.codeSystemName is not None or
            self.codeSystemVersion is not None or
            self.displayName is not None or
            self.originalText is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:2"', name_='CVType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('CVType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'CVType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='CVType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='CVType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='CVType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:2"', name_='CVType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.code is not None:
            namespaceprefix_ = self.code_nsprefix_ + ':' if (UseCapturedNS_ and self.code_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%scode>%s</%scode>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.code), input_name='code')), namespaceprefix_ , eol_))
        if self.codeSystem is not None:
            namespaceprefix_ = self.codeSystem_nsprefix_ + ':' if (UseCapturedNS_ and self.codeSystem_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%scodeSystem>%s</%scodeSystem>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.codeSystem), input_name='codeSystem')), namespaceprefix_ , eol_))
        if self.codeSystemName is not None:
            namespaceprefix_ = self.codeSystemName_nsprefix_ + ':' if (UseCapturedNS_ and self.codeSystemName_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%scodeSystemName>%s</%scodeSystemName>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.codeSystemName), input_name='codeSystemName')), namespaceprefix_ , eol_))
        if self.codeSystemVersion is not None:
            namespaceprefix_ = self.codeSystemVersion_nsprefix_ + ':' if (UseCapturedNS_ and self.codeSystemVersion_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%scodeSystemVersion>%s</%scodeSystemVersion>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.codeSystemVersion), input_name='codeSystemVersion')), namespaceprefix_ , eol_))
        if self.displayName is not None:
            namespaceprefix_ = self.displayName_nsprefix_ + ':' if (UseCapturedNS_ and self.displayName_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sdisplayName>%s</%sdisplayName>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.displayName), input_name='displayName')), namespaceprefix_ , eol_))
        if self.originalText is not None:
            namespaceprefix_ = self.originalText_nsprefix_ + ':' if (UseCapturedNS_ and self.originalText_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%soriginalText>%s</%soriginalText>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.originalText), input_name='originalText')), namespaceprefix_ , eol_))
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'code':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'code')
            value_ = self.gds_validate_string(value_, node, 'code')
            self.code = value_
            self.code_nsprefix_ = child_.prefix
        elif nodeName_ == 'codeSystem':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'codeSystem')
            value_ = self.gds_validate_string(value_, node, 'codeSystem')
            self.codeSystem = value_
            self.codeSystem_nsprefix_ = child_.prefix
        elif nodeName_ == 'codeSystemName':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'codeSystemName')
            value_ = self.gds_validate_string(value_, node, 'codeSystemName')
            self.codeSystemName = value_
            self.codeSystemName_nsprefix_ = child_.prefix
        elif nodeName_ == 'codeSystemVersion':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'codeSystemVersion')
            value_ = self.gds_validate_string(value_, node, 'codeSystemVersion')
            self.codeSystemVersion = value_
            self.codeSystemVersion_nsprefix_ = child_.prefix
        elif nodeName_ == 'displayName':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'displayName')
            value_ = self.gds_validate_string(value_, node, 'displayName')
            self.displayName = value_
            self.displayName_nsprefix_ = child_.prefix
        elif nodeName_ == 'originalText':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'originalText')
            value_ = self.gds_validate_string(value_, node, 'originalText')
            self.originalText = value_
            self.originalText_nsprefix_ = child_.prefix
        else:
            content_ = self.gds_build_any(child_, 'CVType')
            self.anytypeobjs_.append(content_)
# end class CVType


class LegalAuthenticatorType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, signatureTime=None, legalAuthenticatorHSAId=None, legalAuthenticatorName=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.signatureTime = signatureTime
        self.validate_TimeStampType(self.signatureTime)
        self.signatureTime_nsprefix_ = "tns"
        self.legalAuthenticatorHSAId = legalAuthenticatorHSAId
        self.validate_HSAIdType(self.legalAuthenticatorHSAId)
        self.legalAuthenticatorHSAId_nsprefix_ = "tns"
        self.legalAuthenticatorName = legalAuthenticatorName
        self.legalAuthenticatorName_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, LegalAuthenticatorType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if LegalAuthenticatorType.subclass:
            return LegalAuthenticatorType.subclass(*args_, **kwargs_)
        else:
            return LegalAuthenticatorType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_signatureTime(self):
        return self.signatureTime
    def set_signatureTime(self, signatureTime):
        self.signatureTime = signatureTime
    def get_legalAuthenticatorHSAId(self):
        return self.legalAuthenticatorHSAId
    def set_legalAuthenticatorHSAId(self, legalAuthenticatorHSAId):
        self.legalAuthenticatorHSAId = legalAuthenticatorHSAId
    def get_legalAuthenticatorName(self):
        return self.legalAuthenticatorName
    def set_legalAuthenticatorName(self, legalAuthenticatorName):
        self.legalAuthenticatorName = legalAuthenticatorName
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def validate_TimeStampType(self, value):
        result = True
        # Validate type TimeStampType, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            if not self.gds_validate_simple_patterns(
                    self.validate_TimeStampType_patterns_, value):
                self.gds_collector_.add_message('Value "%s" does not match xsd pattern restrictions: %s' % (encode_str_2_3(value), self.validate_TimeStampType_patterns_, ))
                result = False
        return result
    validate_TimeStampType_patterns_ = [['^((19|20)\\d\\d(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])(0[0-9]|1[0-9]|2[0123])([0-5])([0-9])([0-5])([0-9]))$']]
    def validate_HSAIdType(self, value):
        result = True
        # Validate type HSAIdType, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            pass
        return result
    def has__content(self):
        if (
            self.signatureTime is not None or
            self.legalAuthenticatorHSAId is not None or
            self.legalAuthenticatorName is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='LegalAuthenticatorType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('LegalAuthenticatorType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'LegalAuthenticatorType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='LegalAuthenticatorType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='LegalAuthenticatorType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='LegalAuthenticatorType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='LegalAuthenticatorType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.signatureTime is not None:
            namespaceprefix_ = self.signatureTime_nsprefix_ + ':' if (UseCapturedNS_ and self.signatureTime_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%ssignatureTime>%s</%ssignatureTime>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.signatureTime), input_name='signatureTime')), namespaceprefix_ , eol_))
        if self.legalAuthenticatorHSAId is not None:
            namespaceprefix_ = self.legalAuthenticatorHSAId_nsprefix_ + ':' if (UseCapturedNS_ and self.legalAuthenticatorHSAId_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%slegalAuthenticatorHSAId>%s</%slegalAuthenticatorHSAId>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.legalAuthenticatorHSAId), input_name='legalAuthenticatorHSAId')), namespaceprefix_ , eol_))
        if self.legalAuthenticatorName is not None:
            namespaceprefix_ = self.legalAuthenticatorName_nsprefix_ + ':' if (UseCapturedNS_ and self.legalAuthenticatorName_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%slegalAuthenticatorName>%s</%slegalAuthenticatorName>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.legalAuthenticatorName), input_name='legalAuthenticatorName')), namespaceprefix_ , eol_))
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'signatureTime':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'signatureTime')
            value_ = self.gds_validate_string(value_, node, 'signatureTime')
            self.signatureTime = value_
            self.signatureTime_nsprefix_ = child_.prefix
            # validate type TimeStampType
            self.validate_TimeStampType(self.signatureTime)
        elif nodeName_ == 'legalAuthenticatorHSAId':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'legalAuthenticatorHSAId')
            value_ = self.gds_validate_string(value_, node, 'legalAuthenticatorHSAId')
            self.legalAuthenticatorHSAId = value_
            self.legalAuthenticatorHSAId_nsprefix_ = child_.prefix
            # validate type HSAIdType
            self.validate_HSAIdType(self.legalAuthenticatorHSAId)
        elif nodeName_ == 'legalAuthenticatorName':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'legalAuthenticatorName')
            value_ = self.gds_validate_string(value_, node, 'legalAuthenticatorName')
            self.legalAuthenticatorName = value_
            self.legalAuthenticatorName_nsprefix_ = child_.prefix
        else:
            content_ = self.gds_build_any(child_, 'LegalAuthenticatorType')
            self.anytypeobjs_.append(content_)
# end class LegalAuthenticatorType


class DissentingOpinionType(GeneratedsSuper):
    """DissentingOpinionType --
    
    """
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, opinionId=None, authorTime=None, opinion=None, personId=None, personName=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.opinionId = opinionId
        self.opinionId_nsprefix_ = "tns"
        self.authorTime = authorTime
        self.validate_TimeStampType(self.authorTime)
        self.authorTime_nsprefix_ = "tns"
        self.opinion = opinion
        self.opinion_nsprefix_ = "tns"
        self.personId = personId
        self.personId_nsprefix_ = "tns"
        self.personName = personName
        self.personName_nsprefix_ = "tns"
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, DissentingOpinionType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if DissentingOpinionType.subclass:
            return DissentingOpinionType.subclass(*args_, **kwargs_)
        else:
            return DissentingOpinionType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_opinionId(self):
        return self.opinionId
    def set_opinionId(self, opinionId):
        self.opinionId = opinionId
    def get_authorTime(self):
        return self.authorTime
    def set_authorTime(self, authorTime):
        self.authorTime = authorTime
    def get_opinion(self):
        return self.opinion
    def set_opinion(self, opinion):
        self.opinion = opinion
    def get_personId(self):
        return self.personId
    def set_personId(self, personId):
        self.personId = personId
    def get_personName(self):
        return self.personName
    def set_personName(self, personName):
        self.personName = personName
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def validate_TimeStampType(self, value):
        result = True
        # Validate type TimeStampType, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            if not self.gds_validate_simple_patterns(
                    self.validate_TimeStampType_patterns_, value):
                self.gds_collector_.add_message('Value "%s" does not match xsd pattern restrictions: %s' % (encode_str_2_3(value), self.validate_TimeStampType_patterns_, ))
                result = False
        return result
    validate_TimeStampType_patterns_ = [['^((19|20)\\d\\d(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])(0[0-9]|1[0-9]|2[0123])([0-5])([0-9])([0-5])([0-9]))$']]
    def has__content(self):
        if (
            self.opinionId is not None or
            self.authorTime is not None or
            self.opinion is not None or
            self.personId is not None or
            self.personName is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='DissentingOpinionType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('DissentingOpinionType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'DissentingOpinionType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='DissentingOpinionType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='DissentingOpinionType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='DissentingOpinionType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2" ', name_='DissentingOpinionType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.opinionId is not None:
            namespaceprefix_ = self.opinionId_nsprefix_ + ':' if (UseCapturedNS_ and self.opinionId_nsprefix_) else ''
            self.opinionId.export(outfile, level, namespaceprefix_, namespacedef_='', name_='opinionId', pretty_print=pretty_print)
        if self.authorTime is not None:
            namespaceprefix_ = self.authorTime_nsprefix_ + ':' if (UseCapturedNS_ and self.authorTime_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sauthorTime>%s</%sauthorTime>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.authorTime), input_name='authorTime')), namespaceprefix_ , eol_))
        if self.opinion is not None:
            namespaceprefix_ = self.opinion_nsprefix_ + ':' if (UseCapturedNS_ and self.opinion_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sopinion>%s</%sopinion>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.opinion), input_name='opinion')), namespaceprefix_ , eol_))
        if self.personId is not None:
            namespaceprefix_ = self.personId_nsprefix_ + ':' if (UseCapturedNS_ and self.personId_nsprefix_) else ''
            self.personId.export(outfile, level, namespaceprefix_, namespacedef_='', name_='personId', pretty_print=pretty_print)
        if self.personName is not None:
            namespaceprefix_ = self.personName_nsprefix_ + ':' if (UseCapturedNS_ and self.personName_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%spersonName>%s</%spersonName>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.personName), input_name='personName')), namespaceprefix_ , eol_))
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'opinionId':
            obj_ = IIType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.opinionId = obj_
            obj_.original_tagname_ = 'opinionId'
        elif nodeName_ == 'authorTime':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'authorTime')
            value_ = self.gds_validate_string(value_, node, 'authorTime')
            self.authorTime = value_
            self.authorTime_nsprefix_ = child_.prefix
            # validate type TimeStampType
            self.validate_TimeStampType(self.authorTime)
        elif nodeName_ == 'opinion':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'opinion')
            value_ = self.gds_validate_string(value_, node, 'opinion')
            self.opinion = value_
            self.opinion_nsprefix_ = child_.prefix
        elif nodeName_ == 'personId':
            obj_ = PersonIdType.factory(parent_object_=self)
            obj_.build(child_, gds_collector_=gds_collector_)
            self.personId = obj_
            obj_.original_tagname_ = 'personId'
        elif nodeName_ == 'personName':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'personName')
            value_ = self.gds_validate_string(value_, node, 'personName')
            self.personName = value_
            self.personName_nsprefix_ = child_.prefix
        else:
            content_ = self.gds_build_any(child_, 'DissentingOpinionType')
            self.anytypeobjs_.append(content_)
# end class DissentingOpinionType


class PQType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, value=None, unit=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.value = value
        self.value_nsprefix_ = None
        self.unit = unit
        self.unit_nsprefix_ = None
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, PQType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if PQType.subclass:
            return PQType.subclass(*args_, **kwargs_)
        else:
            return PQType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_value(self):
        return self.value
    def set_value(self, value):
        self.value = value
    def get_unit(self):
        return self.unit
    def set_unit(self, unit):
        self.unit = unit
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.value is not None or
            self.unit is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:2"', name_='PQType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('PQType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'PQType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='PQType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='PQType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='PQType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:2"', name_='PQType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.value is not None:
            namespaceprefix_ = self.value_nsprefix_ + ':' if (UseCapturedNS_ and self.value_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%svalue>%s</%svalue>%s' % (namespaceprefix_ , self.gds_format_double(self.value, input_name='value'), namespaceprefix_ , eol_))
        if self.unit is not None:
            namespaceprefix_ = self.unit_nsprefix_ + ':' if (UseCapturedNS_ and self.unit_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sunit>%s</%sunit>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.unit), input_name='unit')), namespaceprefix_ , eol_))
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'value' and child_.text:
            sval_ = child_.text
            fval_ = self.gds_parse_double(sval_, node, 'value')
            fval_ = self.gds_validate_double(fval_, node, 'value')
            self.value = fval_
            self.value_nsprefix_ = child_.prefix
        elif nodeName_ == 'unit':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'unit')
            value_ = self.gds_validate_string(value_, node, 'unit')
            self.unit = value_
            self.unit_nsprefix_ = child_.prefix
        else:
            content_ = self.gds_build_any(child_, 'PQType')
            self.anytypeobjs_.append(content_)
# end class PQType


class IIType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, root=None, extension=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.root = root
        self.root_nsprefix_ = None
        self.extension = extension
        self.extension_nsprefix_ = None
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, IIType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if IIType.subclass:
            return IIType.subclass(*args_, **kwargs_)
        else:
            return IIType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_root(self):
        return self.root
    def set_root(self, root):
        self.root = root
    def get_extension(self):
        return self.extension
    def set_extension(self, extension):
        self.extension = extension
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.root is not None or
            self.extension is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:2"', name_='IIType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('IIType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'IIType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='IIType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='IIType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='IIType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:2"', name_='IIType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.root is not None:
            namespaceprefix_ = self.root_nsprefix_ + ':' if (UseCapturedNS_ and self.root_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sroot>%s</%sroot>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.root), input_name='root')), namespaceprefix_ , eol_))
        if self.extension is not None:
            namespaceprefix_ = self.extension_nsprefix_ + ':' if (UseCapturedNS_ and self.extension_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sextension>%s</%sextension>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.extension), input_name='extension')), namespaceprefix_ , eol_))
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'root':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'root')
            value_ = self.gds_validate_string(value_, node, 'root')
            self.root = value_
            self.root_nsprefix_ = child_.prefix
        elif nodeName_ == 'extension':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'extension')
            value_ = self.gds_validate_string(value_, node, 'extension')
            self.extension = value_
            self.extension_nsprefix_ = child_.prefix
        else:
            content_ = self.gds_build_any(child_, 'IIType')
            self.anytypeobjs_.append(content_)
# end class IIType


class PersonIdType(GeneratedsSuper):
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, id=None, type_=None, anytypeobjs_=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.id = id
        self.id_nsprefix_ = None
        self.type_ = type_
        self.type__nsprefix_ = None
        if anytypeobjs_ is None:
            self.anytypeobjs_ = []
        else:
            self.anytypeobjs_ = anytypeobjs_
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, PersonIdType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if PersonIdType.subclass:
            return PersonIdType.subclass(*args_, **kwargs_)
        else:
            return PersonIdType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_id(self):
        return self.id
    def set_id(self, id):
        self.id = id
    def get_type(self):
        return self.type_
    def set_type(self, type_):
        self.type_ = type_
    def get_anytypeobjs_(self): return self.anytypeobjs_
    def set_anytypeobjs_(self, anytypeobjs_): self.anytypeobjs_ = anytypeobjs_
    def add_anytypeobjs_(self, value): self.anytypeobjs_.append(value)
    def insert_anytypeobjs_(self, index, value): self._anytypeobjs_[index] = value
    def has__content(self):
        if (
            self.id is not None or
            self.type_ is not None or
            self.anytypeobjs_
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:2"', name_='PersonIdType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('PersonIdType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'PersonIdType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='PersonIdType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='PersonIdType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='PersonIdType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:tns="urn:riv:clinicalprocess:healthcond:description:2"', name_='PersonIdType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.id is not None:
            namespaceprefix_ = self.id_nsprefix_ + ':' if (UseCapturedNS_ and self.id_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sid>%s</%sid>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.id), input_name='id')), namespaceprefix_ , eol_))
        if self.type_ is not None:
            namespaceprefix_ = self.type__nsprefix_ + ':' if (UseCapturedNS_ and self.type__nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%stype>%s</%stype>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.type_), input_name='type')), namespaceprefix_ , eol_))
        if not fromsubclass_:
            for obj_ in self.anytypeobjs_:
                showIndent(outfile, level, pretty_print)
                outfile.write(str(obj_))
                outfile.write('\n')
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'id':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'id')
            value_ = self.gds_validate_string(value_, node, 'id')
            self.id = value_
            self.id_nsprefix_ = child_.prefix
        elif nodeName_ == 'type':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'type')
            value_ = self.gds_validate_string(value_, node, 'type')
            self.type_ = value_
            self.type_nsprefix_ = child_.prefix
        else:
            content_ = self.gds_build_any(child_, 'PersonIdType')
            self.anytypeobjs_.append(content_)
# end class PersonIdType


class ResultType(GeneratedsSuper):
    """ResultType --
    
    """
    __hash__ = GeneratedsSuper.__hash__
    subclass = None
    superclass = None
    def __init__(self, resultCode=None, errorCode=None, logId=None, subCode=None, message=None, gds_collector_=None, **kwargs_):
        self.gds_collector_ = gds_collector_
        self.gds_elementtree_node_ = None
        self.original_tagname_ = None
        self.parent_object_ = kwargs_.get('parent_object_')
        self.ns_prefix_ = "core"
        self.resultCode = resultCode
        self.validate_ResultCodeEnum(self.resultCode)
        self.resultCode_nsprefix_ = "codes"
        self.errorCode = errorCode
        self.validate_ErrorCodeEnum(self.errorCode)
        self.errorCode_nsprefix_ = "codes"
        self.logId = logId
        self.logId_nsprefix_ = None
        self.subCode = subCode
        self.subCode_nsprefix_ = None
        self.message = message
        self.message_nsprefix_ = None
    def factory(*args_, **kwargs_):
        if CurrentSubclassModule_ is not None:
            subclass = getSubclassFromModule_(
                CurrentSubclassModule_, ResultType)
            if subclass is not None:
                return subclass(*args_, **kwargs_)
        if ResultType.subclass:
            return ResultType.subclass(*args_, **kwargs_)
        else:
            return ResultType(*args_, **kwargs_)
    factory = staticmethod(factory)
    def get_ns_prefix_(self):
        return self.ns_prefix_
    def set_ns_prefix_(self, ns_prefix):
        self.ns_prefix_ = ns_prefix
    def get_resultCode(self):
        return self.resultCode
    def set_resultCode(self, resultCode):
        self.resultCode = resultCode
    def get_errorCode(self):
        return self.errorCode
    def set_errorCode(self, errorCode):
        self.errorCode = errorCode
    def get_logId(self):
        return self.logId
    def set_logId(self, logId):
        self.logId = logId
    def get_subCode(self):
        return self.subCode
    def set_subCode(self, subCode):
        self.subCode = subCode
    def get_message(self):
        return self.message
    def set_message(self, message):
        self.message = message
    def validate_ResultCodeEnum(self, value):
        result = True
        # Validate type ResultCodeEnum, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            value = value
            enumerations = ['OK', 'ERROR', 'INFO']
            if value not in enumerations:
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s does not match xsd enumeration restriction on ResultCodeEnum' % {"value" : encode_str_2_3(value), "lineno": lineno} )
                result = False
        return result
    def validate_ErrorCodeEnum(self, value):
        result = True
        # Validate type ErrorCodeEnum, a restriction on xs:string.
        if value is not None and Validate_simpletypes_ and self.gds_collector_ is not None:
            if not isinstance(value, str):
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s is not of the correct base simple type (str)' % {"value": value, "lineno": lineno, })
                return False
            value = value
            enumerations = ['INVALID_REQUEST']
            if value not in enumerations:
                lineno = self.gds_get_node_lineno_()
                self.gds_collector_.add_message('Value "%(value)s"%(lineno)s does not match xsd enumeration restriction on ErrorCodeEnum' % {"value" : encode_str_2_3(value), "lineno": lineno} )
                result = False
        return result
    def has__content(self):
        if (
            self.resultCode is not None or
            self.errorCode is not None or
            self.logId is not None or
            self.subCode is not None or
            self.message is not None
        ):
            return True
        else:
            return False
    def export(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:codes="urn:riv:clinicalprocess:healthcond:description:enums:2"  xmlns:tns="urn:riv:clinicalprocess:healthcond:description:2"', name_='ResultType', pretty_print=True):
        imported_ns_def_ = GenerateDSNamespaceDefs_.get('ResultType')
        if imported_ns_def_ is not None:
            namespacedef_ = imported_ns_def_
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.original_tagname_ is not None and name_ == 'ResultType':
            name_ = self.original_tagname_
        if UseCapturedNS_ and self.ns_prefix_:
            namespaceprefix_ = self.ns_prefix_ + ':'
        showIndent(outfile, level, pretty_print)
        outfile.write('<%s%s%s' % (namespaceprefix_, name_, namespacedef_ and ' ' + namespacedef_ or '', ))
        already_processed = set()
        self._exportAttributes(outfile, level, already_processed, namespaceprefix_, name_='ResultType')
        if self.has__content():
            outfile.write('>%s' % (eol_, ))
            self._exportChildren(outfile, level + 1, namespaceprefix_, namespacedef_, name_='ResultType', pretty_print=pretty_print)
            showIndent(outfile, level, pretty_print)
            outfile.write('</%s%s>%s' % (namespaceprefix_, name_, eol_))
        else:
            outfile.write('/>%s' % (eol_, ))
    def _exportAttributes(self, outfile, level, already_processed, namespaceprefix_='tns:', name_='ResultType'):
        pass
    def _exportChildren(self, outfile, level, namespaceprefix_='tns:', namespacedef_='xmlns:core="urn:riv:clinicalprocess:healthcond:description:2" xmlns:codes="urn:riv:clinicalprocess:healthcond:description:enums:2"  xmlns:tns="urn:riv:clinicalprocess:healthcond:description:2"', name_='ResultType', fromsubclass_=False, pretty_print=True):
        if pretty_print:
            eol_ = '\n'
        else:
            eol_ = ''
        if self.resultCode is not None:
            namespaceprefix_ = self.resultCode_nsprefix_ + ':' if (UseCapturedNS_ and self.resultCode_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%sresultCode>%s</%sresultCode>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.resultCode), input_name='resultCode')), namespaceprefix_ , eol_))
        if self.errorCode is not None:
            namespaceprefix_ = self.errorCode_nsprefix_ + ':' if (UseCapturedNS_ and self.errorCode_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%serrorCode>%s</%serrorCode>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.errorCode), input_name='errorCode')), namespaceprefix_ , eol_))
        if self.logId is not None:
            namespaceprefix_ = self.logId_nsprefix_ + ':' if (UseCapturedNS_ and self.logId_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%slogId>%s</%slogId>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.logId), input_name='logId')), namespaceprefix_ , eol_))
        if self.subCode is not None:
            namespaceprefix_ = self.subCode_nsprefix_ + ':' if (UseCapturedNS_ and self.subCode_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%ssubCode>%s</%ssubCode>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.subCode), input_name='subCode')), namespaceprefix_ , eol_))
        if self.message is not None:
            namespaceprefix_ = self.message_nsprefix_ + ':' if (UseCapturedNS_ and self.message_nsprefix_) else ''
            showIndent(outfile, level, pretty_print)
            outfile.write('<%smessage>%s</%smessage>%s' % (namespaceprefix_ , self.gds_encode(self.gds_format_string(quote_xml(self.message), input_name='message')), namespaceprefix_ , eol_))
    def build(self, node, gds_collector_=None):
        self.gds_collector_ = gds_collector_
        if SaveElementTreeNode:
            self.gds_elementtree_node_ = node
        already_processed = set()
        self.ns_prefix_ = node.prefix
        self._buildAttributes(node, node.attrib, already_processed)
        for child in node:
            nodeName_ = Tag_pattern_.match(child.tag).groups()[-1]
            self._buildChildren(child, node, nodeName_, gds_collector_=gds_collector_)
        return self
    def _buildAttributes(self, node, attrs, already_processed):
        pass
    def _buildChildren(self, child_, node, nodeName_, fromsubclass_=False, gds_collector_=None):
        if nodeName_ == 'resultCode':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'resultCode')
            value_ = self.gds_validate_string(value_, node, 'resultCode')
            self.resultCode = value_
            self.resultCode_nsprefix_ = child_.prefix
            # validate type ResultCodeEnum
            self.validate_ResultCodeEnum(self.resultCode)
        elif nodeName_ == 'errorCode':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'errorCode')
            value_ = self.gds_validate_string(value_, node, 'errorCode')
            self.errorCode = value_
            self.errorCode_nsprefix_ = child_.prefix
            # validate type ErrorCodeEnum
            self.validate_ErrorCodeEnum(self.errorCode)
        elif nodeName_ == 'logId':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'logId')
            value_ = self.gds_validate_string(value_, node, 'logId')
            self.logId = value_
            self.logId_nsprefix_ = child_.prefix
        elif nodeName_ == 'subCode':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'subCode')
            value_ = self.gds_validate_string(value_, node, 'subCode')
            self.subCode = value_
            self.subCode_nsprefix_ = child_.prefix
        elif nodeName_ == 'message':
            value_ = child_.text
            value_ = self.gds_parse_string(value_, node, 'message')
            value_ = self.gds_validate_string(value_, node, 'message')
            self.message = value_
            self.message_nsprefix_ = child_.prefix
# end class ResultType


#
# End data representation classes.
#


GDSClassesMapping = {
    'GetDiagnosis': GetDiagnosisType,
    'GetDiagnosisResponse': GetDiagnosisResponseType,
    'communicableDisease': CommunicableDiseaseType,
}


USAGE_TEXT = """
Usage: python <Parser>.py [ -s ] <in_xml_file>
"""


def usage():
    print(USAGE_TEXT)
    sys.exit(1)


def get_root_tag(node):
    tag = Tag_pattern_.match(node.tag).groups()[-1]
    prefix_tag = TagNamePrefix + tag
    rootClass = GDSClassesMapping.get(prefix_tag)
    if rootClass is None:
        rootClass = globals().get(prefix_tag)
    return tag, rootClass


def get_required_ns_prefix_defs(rootNode):
    '''Get all name space prefix definitions required in this XML doc.
    Return a dictionary of definitions and a char string of definitions.
    '''
    nsmap = {
        prefix: uri
        for node in rootNode.iter()
        for (prefix, uri) in node.nsmap.items()
        if prefix is not None
    }
    namespacedefs = ' '.join([
        'xmlns:{}="{}"'.format(prefix, uri)
        for prefix, uri in nsmap.items()
    ])
    return nsmap, namespacedefs


def parse(inFileName, silence=False, print_warnings=True):
    global CapturedNsmap_
    gds_collector = GdsCollector_()
    parser = None
    doc = parsexml_(inFileName, parser)
    rootNode = doc.getroot()
    rootTag, rootClass = get_root_tag(rootNode)
    if rootClass is None:
        rootTag = 'GetDiagnosisType'
        rootClass = GetDiagnosisType
    rootObj = rootClass.factory()
    rootObj.build(rootNode, gds_collector_=gds_collector)
    CapturedNsmap_, namespacedefs = get_required_ns_prefix_defs(rootNode)
    if not SaveElementTreeNode:
        doc = None
        rootNode = None
    if not silence:
        sys.stdout.write('<?xml version="1.0" ?>\n')
        rootObj.export(
            sys.stdout, 0, name_=rootTag,
            namespacedef_=namespacedefs,
            pretty_print=True)
    if print_warnings and len(gds_collector.get_messages()) > 0:
        separator = ('-' * 50) + '\n'
        sys.stderr.write(separator)
        sys.stderr.write('----- Warnings -- count: {} -----\n'.format(
            len(gds_collector.get_messages()), ))
        gds_collector.write_messages(sys.stderr)
        sys.stderr.write(separator)
    return rootObj


def parseEtree(inFileName, silence=False, print_warnings=True,
               mapping=None, reverse_mapping=None, nsmap=None):
    parser = None
    doc = parsexml_(inFileName, parser)
    gds_collector = GdsCollector_()
    rootNode = doc.getroot()
    rootTag, rootClass = get_root_tag(rootNode)
    if rootClass is None:
        rootTag = 'GetDiagnosisType'
        rootClass = GetDiagnosisType
    rootObj = rootClass.factory()
    rootObj.build(rootNode, gds_collector_=gds_collector)
    if mapping is None:
        mapping = {}
    if reverse_mapping is None:
        reverse_mapping = {}
    rootElement = rootObj.to_etree(
        None, name_=rootTag, mapping_=mapping,
        reverse_mapping_=reverse_mapping, nsmap_=nsmap)
    reverse_node_mapping = rootObj.gds_reverse_node_mapping(mapping)
    # Enable Python to collect the space used by the DOM.
    if not SaveElementTreeNode:
        doc = None
        rootNode = None
    if not silence:
        content = etree_.tostring(
            rootElement, pretty_print=True,
            xml_declaration=True, encoding="utf-8")
        sys.stdout.write(str(content))
        sys.stdout.write('\n')
    if print_warnings and len(gds_collector.get_messages()) > 0:
        separator = ('-' * 50) + '\n'
        sys.stderr.write(separator)
        sys.stderr.write('----- Warnings -- count: {} -----\n'.format(
            len(gds_collector.get_messages()), ))
        gds_collector.write_messages(sys.stderr)
        sys.stderr.write(separator)
    return rootObj, rootElement, mapping, reverse_node_mapping


def parseString(inString, silence=False, print_warnings=True):
    '''Parse a string, create the object tree, and export it.

    Arguments:
    - inString -- A string.  This XML fragment should not start
      with an XML declaration containing an encoding.
    - silence -- A boolean.  If False, export the object.
    Returns -- The root object in the tree.
    '''
    parser = None
    rootNode= parsexmlstring_(inString, parser)
    gds_collector = GdsCollector_()
    rootTag, rootClass = get_root_tag(rootNode)
    if rootClass is None:
        rootTag = 'GetDiagnosisType'
        rootClass = GetDiagnosisType
    rootObj = rootClass.factory()
    rootObj.build(rootNode, gds_collector_=gds_collector)
    if not SaveElementTreeNode:
        rootNode = None
    if not silence:
        sys.stdout.write('<?xml version="1.0" ?>\n')
        rootObj.export(
            sys.stdout, 0, name_=rootTag,
            namespacedef_='xmlns:tns="urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2"')
    if print_warnings and len(gds_collector.get_messages()) > 0:
        separator = ('-' * 50) + '\n'
        sys.stderr.write(separator)
        sys.stderr.write('----- Warnings -- count: {} -----\n'.format(
            len(gds_collector.get_messages()), ))
        gds_collector.write_messages(sys.stderr)
        sys.stderr.write(separator)
    return rootObj


def parseLiteral(inFileName, silence=False, print_warnings=True):
    parser = None
    doc = parsexml_(inFileName, parser)
    gds_collector = GdsCollector_()
    rootNode = doc.getroot()
    rootTag, rootClass = get_root_tag(rootNode)
    if rootClass is None:
        rootTag = 'GetDiagnosisType'
        rootClass = GetDiagnosisType
    rootObj = rootClass.factory()
    rootObj.build(rootNode, gds_collector_=gds_collector)
    # Enable Python to collect the space used by the DOM.
    if not SaveElementTreeNode:
        doc = None
        rootNode = None
    if not silence:
        sys.stdout.write('#from rivta_getdiagnosis_model import *\n\n')
        sys.stdout.write('import rivta_getdiagnosis_model as model_\n\n')
        sys.stdout.write('rootObj = model_.rootClass(\n')
        rootObj.exportLiteral(sys.stdout, 0, name_=rootTag)
        sys.stdout.write(')\n')
    if print_warnings and len(gds_collector.get_messages()) > 0:
        separator = ('-' * 50) + '\n'
        sys.stderr.write(separator)
        sys.stderr.write('----- Warnings -- count: {} -----\n'.format(
            len(gds_collector.get_messages()), ))
        gds_collector.write_messages(sys.stderr)
        sys.stderr.write(separator)
    return rootObj


def main():
    args = sys.argv[1:]
    if len(args) == 1:
        parse(args[0])
    else:
        usage()


if __name__ == '__main__':
    #import pdb; pdb.set_trace()
    main()

RenameMappings_ = {
}

#
# Mapping of namespaces to types defined in them
# and the file in which each is defined.
# simpleTypes are marked "ST" and complexTypes "CT".
NamespaceToDefMappings_ = {'urn:riv:clinicalprocess:healthcond:description:2': [('DateType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'ST'),
                                                      ('HSAIdType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'ST'),
                                                      ('TimeStampType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'ST'),
                                                      ('CareDocumentationType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('CareDocumentationBodyType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('FunctionalStatusAssessmentType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('FunctionalStatusAssessmentBodyType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('ClinicalDocumentNoteType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('PatientSummaryHeaderType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('CPatientSummaryHeaderType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('HealthcareProfessionalType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('OrgUnitType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('DiagnosisType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('DiagnosisBodyType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('AlertInformationType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('RelatedDiagnosisType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('AlertInformationBodyType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('HyperSensitivityType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('PharmaceuticalHypersensitivityType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('OtherHypersensitivityType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('SeriousDiseaseType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('TreatmentType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('CommunicableDiseaseType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('RestrictionOfCareType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('UnstructuredAlertInformationType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('RelatedAlertInformationType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('PADLType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('DisabilityType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('DatePeriodType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('MultimediaType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('TimePeriodType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('CVType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('LegalAuthenticatorType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('DissentingOpinionType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('PQType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('IIType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('PersonIdType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT'),
                                                      ('ResultType',
                                                       '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_2.1.xsd',
                                                       'CT')],
 'urn:riv:clinicalprocess:healthcond:description:GetDiagnosisResponder:2': [('GetDiagnosisType',
                                                                             '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/GetDiagnosisResponder_2.0.xsd',
                                                                             'CT'),
                                                                            ('GetDiagnosisResponseType',
                                                                             '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/GetDiagnosisResponder_2.0.xsd',
                                                                             'CT')],
 'urn:riv:clinicalprocess:healthcond:description:enums:2': [('ClinicalDocumentNoteCodeEnum',
                                                             '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/./clinicalprocess_healthcond_description_enum_2.1.xsd',
                                                             'ST'),
                                                            ('MediaTypeEnum',
                                                             '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/./clinicalprocess_healthcond_description_enum_2.1.xsd',
                                                             'ST'),
                                                            ('ClinicalDocumentTypeCodeEnum',
                                                             '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/./clinicalprocess_healthcond_description_enum_2.1.xsd',
                                                             'ST'),
                                                            ('AssessmentCategoryEnum',
                                                             '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/./clinicalprocess_healthcond_description_enum_2.1.xsd',
                                                             'ST'),
                                                            ('DiagnosisTypeEnum',
                                                             '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/./clinicalprocess_healthcond_description_enum_2.1.xsd',
                                                             'ST'),
                                                            ('ResultCodeEnum',
                                                             '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/./clinicalprocess_healthcond_description_enum_2.1.xsd',
                                                             'ST'),
                                                            ('ErrorCodeEnum',
                                                             '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/./clinicalprocess_healthcond_description_enum_2.1.xsd',
                                                             'ST'),
                                                            ('ClinicalDocumentNoteCodeEnum',
                                                             '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_enum_2.1.xsd',
                                                             'ST'),
                                                            ('MediaTypeEnum',
                                                             '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_enum_2.1.xsd',
                                                             'ST'),
                                                            ('ClinicalDocumentTypeCodeEnum',
                                                             '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_enum_2.1.xsd',
                                                             'ST'),
                                                            ('AssessmentCategoryEnum',
                                                             '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_enum_2.1.xsd',
                                                             'ST'),
                                                            ('DiagnosisTypeEnum',
                                                             '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_enum_2.1.xsd',
                                                             'ST'),
                                                            ('ResultCodeEnum',
                                                             '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_enum_2.1.xsd',
                                                             'ST'),
                                                            ('ErrorCodeEnum',
                                                             '../../../mapping-engine/src/test/resources/riv-schemas/clinicalprocess_healthcond_description_2.1/interactions/GetDiagnosisInteraction/../../core_components/clinicalprocess_healthcond_description_enum_2.1.xsd',
                                                             'ST')]}

__all__ = [
    "AlertInformationBodyType",
    "AlertInformationType",
    "CPatientSummaryHeaderType",
    "CVType",
    "CareDocumentationBodyType",
    "CareDocumentationType",
    "ClinicalDocumentNoteType",
    "CommunicableDiseaseType",
    "DatePeriodType",
    "DiagnosisBodyType",
    "DiagnosisType",
    "DisabilityType",
    "DissentingOpinionType",
    "FunctionalStatusAssessmentBodyType",
    "FunctionalStatusAssessmentType",
    "GetDiagnosisResponseType",
    "GetDiagnosisType",
    "HealthcareProfessionalType",
    "HyperSensitivityType",
    "IIType",
    "LegalAuthenticatorType",
    "MultimediaType",
    "OrgUnitType",
    "OtherHypersensitivityType",
    "PADLType",
    "PQType",
    "PatientSummaryHeaderType",
    "PersonIdType",
    "PharmaceuticalHypersensitivityType",
    "RelatedAlertInformationType",
    "RelatedDiagnosisType",
    "RestrictionOfCareType",
    "ResultType",
    "SeriousDiseaseType",
    "TimePeriodType",
    "TreatmentType",
    "UnstructuredAlertInformationType"
]
