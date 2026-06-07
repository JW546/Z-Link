

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
public class DeviceType  extends Enum {

    private static final long serialVersionUID = -1161229081L;

    public static final DeviceType DRONE_VTOL = new DeviceType("DRONE_VTOL", 0);
    public static final int _DRONE_VTOL = 0;
    public static final DeviceType DRONE_MULTIROTOR = new DeviceType("DRONE_MULTIROTOR", 1);
    public static final int _DRONE_MULTIROTOR = 1;
    public static final DeviceType VEHICLE_CAR = new DeviceType("VEHICLE_CAR", 2);
    public static final int _VEHICLE_CAR = 2;
    public static final DeviceType VEHICLE_TRUCK = new DeviceType("VEHICLE_TRUCK", 3);
    public static final int _VEHICLE_TRUCK = 3;
    public static DeviceType valueOf(int ordinal) {
        switch(ordinal) {

            case 0: return DeviceType.DRONE_VTOL;
            case 1: return DeviceType.DRONE_MULTIROTOR;
            case 2: return DeviceType.VEHICLE_CAR;
            case 3: return DeviceType.VEHICLE_TRUCK;

        }
        return null;
    }

    public static DeviceType from_int(int __value) {
        return valueOf(__value);
    }

    public static int[] getOrdinals() {
        int i = 0;
        int[] values = new int[4];

        values[i] = DRONE_VTOL.ordinal();
        i++;
        values[i] = DRONE_MULTIROTOR.ordinal();
        i++;
        values[i] = VEHICLE_CAR.ordinal();
        i++;
        values[i] = VEHICLE_TRUCK.ordinal();
        i++;

        return values;
    }

    public int value() {
        return super.ordinal();
    }

    /**
    * Create a default instance
    */  
    public static DeviceType create() {

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

    private DeviceType(java.lang.String name, int ordinal) {
        super(name, ordinal);
    }
}

