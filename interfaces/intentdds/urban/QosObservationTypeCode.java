
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

public class  QosObservationTypeCode {

    public static final TypeCode VALUE_WO_MEMBERS = getTypeCodeWOMembers();

    // We need a type code without member in case of recursion
    @SuppressWarnings("cast")
    public static TypeCode getTypeCodeWOMembers() {
        TypeCode tc = null;
        StructMember sm[]=new StructMember[0];
        Annotations annotation = new Annotations();
        annotation.allowed_data_representation_mask(5);
        tc = TypeCodeFactory.TheTypeCodeFactory.create_struct_tc("urban::QosObservation",ExtensibilityKind.EXTENSIBLE_EXTENSIBILITY,  sm , annotation);        return tc;
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
        sm[__i] = new  StructMember("qos_profile", false, (short)-1,  false, intentdds.urban.IdStringTypeCode.VALUE
        , 1, false, memberAnnotation);__i++;
        memberAnnotation = new Annotations();
        memberAnnotation.default_annotation(AnnotationParameterValue.ZERO_ULONG);
        memberAnnotation.min_annotation(AnnotationParameterValue.MIN_ULONG);
        memberAnnotation.max_annotation(AnnotationParameterValue.MAX_ULONG);
        sm[__i] = new  StructMember("published_count", false, (short)-1,  false, TypeCode.TC_ULONG, 2, false, memberAnnotation);__i++;
        memberAnnotation = new Annotations();
        memberAnnotation.default_annotation(AnnotationParameterValue.ZERO_ULONG);
        memberAnnotation.min_annotation(AnnotationParameterValue.MIN_ULONG);
        memberAnnotation.max_annotation(AnnotationParameterValue.MAX_ULONG);
        sm[__i] = new  StructMember("delivered_count", false, (short)-1,  false, TypeCode.TC_ULONG, 3, false, memberAnnotation);__i++;
        memberAnnotation = new Annotations();
        memberAnnotation.default_annotation(AnnotationParameterValue.ZERO_ULONG);
        memberAnnotation.min_annotation(AnnotationParameterValue.MIN_ULONG);
        memberAnnotation.max_annotation(AnnotationParameterValue.MAX_ULONG);
        sm[__i] = new  StructMember("dropped_count", false, (short)-1,  false, TypeCode.TC_ULONG, 4, false, memberAnnotation);__i++;
        memberAnnotation = new Annotations();
        memberAnnotation.default_annotation(AnnotationParameterValue.ZERO_ULONG);
        memberAnnotation.min_annotation(AnnotationParameterValue.MIN_ULONG);
        memberAnnotation.max_annotation(AnnotationParameterValue.MAX_ULONG);
        sm[__i] = new  StructMember("retry_count", false, (short)-1,  false, TypeCode.TC_ULONG, 5, false, memberAnnotation);__i++;
        memberAnnotation = new Annotations();
        memberAnnotation.default_annotation(AnnotationParameterValue.ZERO_ULONG);
        memberAnnotation.min_annotation(AnnotationParameterValue.MIN_ULONG);
        memberAnnotation.max_annotation(AnnotationParameterValue.MAX_ULONG);
        sm[__i] = new  StructMember("stale_count", false, (short)-1,  false, TypeCode.TC_ULONG, 6, false, memberAnnotation);__i++;
        memberAnnotation = new Annotations();
        memberAnnotation.default_annotation(AnnotationParameterValue.ZERO_DOUBLE);
        memberAnnotation.min_annotation(AnnotationParameterValue.MIN_DOUBLE);
        memberAnnotation.max_annotation(AnnotationParameterValue.MAX_DOUBLE);
        sm[__i] = new  StructMember("average_latency_ms", false, (short)-1,  false, TypeCode.TC_DOUBLE, 7, false, memberAnnotation);__i++;
        memberAnnotation = new Annotations();
        memberAnnotation.default_annotation(AnnotationParameterValue.ZERO_DOUBLE);
        memberAnnotation.min_annotation(AnnotationParameterValue.MIN_DOUBLE);
        memberAnnotation.max_annotation(AnnotationParameterValue.MAX_DOUBLE);
        sm[__i] = new  StructMember("delivered_ratio", false, (short)-1,  false, TypeCode.TC_DOUBLE, 8, false, memberAnnotation);__i++;

        Annotations annotation = new Annotations();
        annotation.allowed_data_representation_mask(5);

        tc = TypeCodeFactory.TheTypeCodeFactory.create_struct_tc("urban::QosObservation",ExtensibilityKind.EXTENSIBLE_EXTENSIBILITY,  sm , annotation);        
        return tc;
    }
}
