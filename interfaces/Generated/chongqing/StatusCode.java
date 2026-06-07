

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
public class StatusCode  extends Enum {

    private static final long serialVersionUID = -629538511L;

    public static final StatusCode IDLE = new StatusCode("IDLE", 0);
    public static final int _IDLE = 0;
    public static final StatusCode MISSION_PLANNED = new StatusCode("MISSION_PLANNED", 1);
    public static final int _MISSION_PLANNED = 1;
    public static final StatusCode IN_TRANSIT = new StatusCode("IN_TRANSIT", 2);
    public static final int _IN_TRANSIT = 2;
    public static final StatusCode ARRIVED = new StatusCode("ARRIVED", 3);
    public static final int _ARRIVED = 3;
    public static final StatusCode RETURNING = new StatusCode("RETURNING", 4);
    public static final int _RETURNING = 4;
    public static final StatusCode EMERGENCY = new StatusCode("EMERGENCY", 5);
    public static final int _EMERGENCY = 5;
    public static final StatusCode ERROR = new StatusCode("ERROR", 6);
    public static final int _ERROR = 6;
    public static StatusCode valueOf(int ordinal) {
        switch(ordinal) {

            case 0: return StatusCode.IDLE;
            case 1: return StatusCode.MISSION_PLANNED;
            case 2: return StatusCode.IN_TRANSIT;
            case 3: return StatusCode.ARRIVED;
            case 4: return StatusCode.RETURNING;
            case 5: return StatusCode.EMERGENCY;
            case 6: return StatusCode.ERROR;

        }
        return null;
    }

    public static StatusCode from_int(int __value) {
        return valueOf(__value);
    }

    public static int[] getOrdinals() {
        int i = 0;
        int[] values = new int[7];

        values[i] = IDLE.ordinal();
        i++;
        values[i] = MISSION_PLANNED.ordinal();
        i++;
        values[i] = IN_TRANSIT.ordinal();
        i++;
        values[i] = ARRIVED.ordinal();
        i++;
        values[i] = RETURNING.ordinal();
        i++;
        values[i] = EMERGENCY.ordinal();
        i++;
        values[i] = ERROR.ordinal();
        i++;

        return values;
    }

    public int value() {
        return super.ordinal();
    }

    /**
    * Create a default instance
    */  
    public static StatusCode create() {

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

    private StatusCode(java.lang.String name, int ordinal) {
        super(name, ordinal);
    }
}

