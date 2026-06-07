

/*
WARNING: THIS FILE IS AUTO-GENERATED. DO NOT MODIFY.

This file was generated from .idl 
using RTI Code Generator (rtiddsgen) version 4.6.0.
The rtiddsgen tool is part of the RTI Connext DDS distribution.
For more information, type 'rtiddsgen -help' at a command shell
or consult the Code Generator User's Manual.
*/

package chongqing;

import com.rti.dds.util.Enum;
import com.rti.dds.cdr.CdrHelper;
import java.util.Arrays;
import java.io.ObjectStreamException;
// Depending on the type represented in the IDL, we may perform some redundant
// casts, we are suppressing that warning
@SuppressWarnings("cast")
public class MissionType  extends Enum {

    private static final long serialVersionUID = 100645927L;

    public static final MissionType MEDICAL_SUPPLY_DELIVERY = new MissionType("MEDICAL_SUPPLY_DELIVERY", 0);
    public static final int _MEDICAL_SUPPLY_DELIVERY = 0;
    public static final MissionType EMERGENCY_RESCUE = new MissionType("EMERGENCY_RESCUE", 1);
    public static final int _EMERGENCY_RESCUE = 1;
    public static final MissionType SURVEILLANCE_PATROL = new MissionType("SURVEILLANCE_PATROL", 2);
    public static final int _SURVEILLANCE_PATROL = 2;
    public static final MissionType CARGO_TRANSPORT = new MissionType("CARGO_TRANSPORT", 3);
    public static final int _CARGO_TRANSPORT = 3;
    public static MissionType valueOf(int ordinal) {
        switch(ordinal) {

            case 0: return MissionType.MEDICAL_SUPPLY_DELIVERY;
            case 1: return MissionType.EMERGENCY_RESCUE;
            case 2: return MissionType.SURVEILLANCE_PATROL;
            case 3: return MissionType.CARGO_TRANSPORT;

        }
        return null;
    }

    public static MissionType from_int(int __value) {
        return valueOf(__value);
    }

    public static int[] getOrdinals() {
        int i = 0;
        int[] values = new int[4];

        values[i] = MEDICAL_SUPPLY_DELIVERY.ordinal();
        i++;
        values[i] = EMERGENCY_RESCUE.ordinal();
        i++;
        values[i] = SURVEILLANCE_PATROL.ordinal();
        i++;
        values[i] = CARGO_TRANSPORT.ordinal();
        i++;

        return values;
    }

    public int value() {
        return super.ordinal();
    }

    /**
    * Create a default instance
    */  
    public static MissionType create() {

        return valueOf(0);
    }

    /**
    * Print Method
    */     
    public java.lang.String toString(java.lang.String desc, int indent) {
        java.lang.StringBuffer strBuffer = new java.lang.StringBuffer();

        CdrHelper.printIndent(strBuffer, indent);

        if (desc != null) {
            strBuffer.append(desc).append(": ");
        }

        strBuffer.append(this);
        strBuffer.append("\n");              
        return strBuffer.toString();
    }

    private java.lang.Object readResolve() throws ObjectStreamException {
        return valueOf(ordinal());
    }

    private MissionType(java.lang.String name, int ordinal) {
        super(name, ordinal);
    }
}

