
/*
WARNING: THIS FILE IS AUTO-GENERATED. DO NOT MODIFY.

This file was generated from .idl 
using RTI Code Generator (rtiddsgen) version 4.6.0.
The rtiddsgen tool is part of the RTI Connext DDS distribution.
For more information, type 'rtiddsgen -help' at a command shell
or consult the Code Generator User's Manual.
*/

package intentdds.urban;

import com.rti.dds.typecode.*;

public class  RouteAdviceTypeCode {

    public static final TypeCode VALUE_WO_MEMBERS = getTypeCodeWOMembers();

    // We need a type code without member in case of recursion
    @SuppressWarnings("cast")
    public static TypeCode getTypeCodeWOMembers() {
        TypeCode tc = null;
        StructMember sm[]=new StructMember[0];
        Annotations annotation = new Annotations();
        annotation.allowed_data_representation_mask(5);
        tc = TypeCodeFactory.TheTypeCodeFactory.create_struct_tc("urban::RouteAdvice",ExtensibilityKind.EXTENSIBLE_EXTENSIBILITY,  sm , annotation);        return tc;
    }

    public static final TypeCode VALUE = getTypeCode();

    // Depending on the type represented in the IDL, we may perform some redundant
    // casts, we are suppressing that warning
    @SuppressWarnings("cast")
    private static TypeCode getTypeCode() {
        TypeCode tc = null;
        int __i=0;
        StructMember sm[]=new StructMember[9];
        Annotations memberAnnotation;

        sm[__i] = new  StructMember("header", false, (short)-1,  false, intentdds.urban.HeaderTypeCode.VALUE
        , 0, false);__i++;
        memberAnnotation = new Annotations();
        memberAnnotation.default_annotation(AnnotationParameterValue.EMPTY_STRING);
        sm[__i] = new  StructMember("source_type", false, (short)-1,  false, intentdds.urban.IdStringTypeCode.VALUE
        , 1, false, memberAnnotation);__i++;
        memberAnnotation = new Annotations();
        memberAnnotation.default_annotation(AnnotationParameterValue.EMPTY_STRING);
        sm[__i] = new  StructMember("destination", false, (short)-1,  false, intentdds.urban.TextStringTypeCode.VALUE
        , 2, false, memberAnnotation);__i++;
        memberAnnotation = new Annotations();
        memberAnnotation.default_annotation(AnnotationParameterValue.EMPTY_STRING);
        sm[__i] = new  StructMember("route_candidate", false, (short)-1,  false, intentdds.urban.IdStringTypeCode.VALUE
        , 3, false, memberAnnotation);__i++;
        memberAnnotation = new Annotations();
        memberAnnotation.default_annotation(AnnotationParameterValue.ZERO_DOUBLE);
        memberAnnotation.min_annotation(AnnotationParameterValue.MIN_DOUBLE);
        memberAnnotation.max_annotation(AnnotationParameterValue.MAX_DOUBLE);
        sm[__i] = new  StructMember("risk_score", false, (short)-1,  false, TypeCode.TC_DOUBLE, 4, false, memberAnnotation);__i++;
        memberAnnotation = new Annotations();
        memberAnnotation.default_annotation(AnnotationParameterValue.ZERO_DOUBLE);
        memberAnnotation.min_annotation(AnnotationParameterValue.MIN_DOUBLE);
        memberAnnotation.max_annotation(AnnotationParameterValue.MAX_DOUBLE);
        sm[__i] = new  StructMember("confidence", false, (short)-1,  false, TypeCode.TC_DOUBLE, 5, false, memberAnnotation);__i++;
        memberAnnotation = new Annotations();
        memberAnnotation.default_annotation(AnnotationParameterValue.ZERO_DOUBLE);
        memberAnnotation.min_annotation(AnnotationParameterValue.MIN_DOUBLE);
        memberAnnotation.max_annotation(AnnotationParameterValue.MAX_DOUBLE);
        sm[__i] = new  StructMember("estimated_time_min", false, (short)-1,  false, TypeCode.TC_DOUBLE, 6, false, memberAnnotation);__i++;
        memberAnnotation = new Annotations();
        memberAnnotation.default_annotation(AnnotationParameterValue.EMPTY_STRING);
        sm[__i] = new  StructMember("advice_reason", false, (short)-1,  false, intentdds.urban.TextStringTypeCode.VALUE
        , 7, false, memberAnnotation);__i++;
        sm[__i] = new  StructMember("required_topics", false, (short)-1,  false, new TypeCode(32, intentdds.urban.TextStringTypeCode.VALUE
        ), 8, false);__i++;

        Annotations annotation = new Annotations();
        annotation.allowed_data_representation_mask(5);

        tc = TypeCodeFactory.TheTypeCodeFactory.create_struct_tc("urban::RouteAdvice",ExtensibilityKind.EXTENSIBLE_EXTENSIBILITY,  sm , annotation);        
        return tc;
    }
}
