

/*
WARNING: THIS FILE IS AUTO-GENERATED. DO NOT MODIFY.

This file was generated from .idl 
using RTI Code Generator (rtiddsgen) version 4.6.0.
The rtiddsgen tool is part of the RTI Connext DDS distribution.
For more information, type 'rtiddsgen -help' at a command shell
or consult the Code Generator User's Manual.
*/

package urban;

import com.rti.dds.util.Enum;
import com.rti.dds.cdr.CdrHelper;
import java.util.Arrays;
import java.io.ObjectStreamException;
// Depending on the type represented in the IDL, we may perform some redundant
// casts, we are suppressing that warning
@SuppressWarnings("cast")
public class ProtocolType  extends Enum {

    private static final long serialVersionUID = -1750614152L;

    public static final ProtocolType MAVLINK = new ProtocolType("MAVLINK", 0);
    public static final int _MAVLINK = 0;
    public static final ProtocolType DDS_STANDARD = new ProtocolType("DDS_STANDARD", 1);
    public static final int _DDS_STANDARD = 1;
    public static final ProtocolType BYD_PROTOCOL = new ProtocolType("BYD_PROTOCOL", 2);
    public static final int _BYD_PROTOCOL = 2;
    public static final ProtocolType DJI_PROTOCOL = new ProtocolType("DJI_PROTOCOL", 3);
    public static final int _DJI_PROTOCOL = 3;
    public static final ProtocolType CAN_BUS = new ProtocolType("CAN_BUS", 4);
    public static final int _CAN_BUS = 4;
    public static final ProtocolType V2X = new ProtocolType("V2X", 5);
    public static final int _V2X = 5;
    public static ProtocolType valueOf(int ordinal) {
        switch(ordinal) {

            case 0: return ProtocolType.MAVLINK;
            case 1: return ProtocolType.DDS_STANDARD;
            case 2: return ProtocolType.BYD_PROTOCOL;
            case 3: return ProtocolType.DJI_PROTOCOL;
            case 4: return ProtocolType.CAN_BUS;
            case 5: return ProtocolType.V2X;

        }
        return null;
    }

    public static ProtocolType from_int(int __value) {
        return valueOf(__value);
    }

    public static int[] getOrdinals() {
        int i = 0;
        int[] values = new int[6];

        values[i] = MAVLINK.ordinal();
        i++;
        values[i] = DDS_STANDARD.ordinal();
        i++;
        values[i] = BYD_PROTOCOL.ordinal();
        i++;
        values[i] = DJI_PROTOCOL.ordinal();
        i++;
        values[i] = CAN_BUS.ordinal();
        i++;
        values[i] = V2X.ordinal();
        i++;

        return values;
    }

    public int value() {
        return super.ordinal();
    }

    /**
    * Create a default instance
    */  
    public static ProtocolType create() {

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

    private ProtocolType(java.lang.String name, int ordinal) {
        super(name, ordinal);
    }
}

