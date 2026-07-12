
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

public class  MissionCommandTypeCode {

    public static final TypeCode VALUE_WO_MEMBERS = getTypeCodeWOMembers();

    // We need a type code without member in case of recursion
    @SuppressWarnings("cast")
    public static TypeCode getTypeCodeWOMembers() {
        TypeCode tc = null;
        StructMember sm[]=new StructMember[0];
        Annotations annotation = new Annotations();
        annotation.allowed_data_representation_mask(5);
        tc = TypeCodeFactory.TheTypeCodeFactory.create_struct_tc("urban::MissionCommand",ExtensibilityKind.EXTENSIBLE_EXTENSIBILITY,  sm , annotation);        return tc;
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
        sm[__i] = new  StructMember("natural_language", false, (short)-1,  false, intentdds.urban.TextStringTypeCode.VALUE
        , 1, false, memberAnnotation);__i++;
        memberAnnotation = new Annotations();
        memberAnnotation.default_annotation(AnnotationParameterValue.EMPTY_STRING);
        sm[__i] = new  StructMember("destination", false, (short)-1,  false, intentdds.urban.TextStringTypeCode.VALUE
        , 2, false, memberAnnotation);__i++;
        memberAnnotation = new Annotations();
        memberAnnotation.default_annotation(AnnotationParameterValue.EMPTY_STRING);
        sm[__i] = new  StructMember("urgency", false, (short)-1,  false, intentdds.urban.IdStringTypeCode.VALUE
        , 3, false, memberAnnotation);__i++;
        memberAnnotation = new Annotations();
        memberAnnotation.default_annotation(AnnotationParameterValue.EMPTY_STRING);
        sm[__i] = new  StructMember("preference", false, (short)-1,  false, intentdds.urban.IdStringTypeCode.VALUE
        , 4, false, memberAnnotation);__i++;
        sm[__i] = new  StructMember("requested_capabilities", false, (short)-1,  false, new TypeCode(32, intentdds.urban.TextStringTypeCode.VALUE
        ), 5, false);__i++;
        sm[__i] = new  StructMember("publish_topics", false, (short)-1,  false, new TypeCode(32, intentdds.urban.TextStringTypeCode.VALUE
        ), 6, false);__i++;
        sm[__i] = new  StructMember("subscribe_topics", false, (short)-1,  false, new TypeCode(32, intentdds.urban.TextStringTypeCode.VALUE
        ), 7, false);__i++;
        memberAnnotation = new Annotations();
        memberAnnotation.default_annotation(AnnotationParameterValue.EMPTY_STRING);
        sm[__i] = new  StructMember("dsl_contract", false, (short)-1,  false, intentdds.urban.ContractStringTypeCode.VALUE
        , 8, false, memberAnnotation);__i++;

        Annotations annotation = new Annotations();
        annotation.allowed_data_representation_mask(5);

        tc = TypeCodeFactory.TheTypeCodeFactory.create_struct_tc("urban::MissionCommand",ExtensibilityKind.EXTENSIBLE_EXTENSIBILITY,  sm , annotation);        
        return tc;
    }
}
