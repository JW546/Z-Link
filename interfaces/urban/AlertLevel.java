

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
public class AlertLevel  extends Enum {

    private static final long serialVersionUID = -1059276546L;

    public static final AlertLevel LOW = new AlertLevel("LOW", 0);
    public static final int _LOW = 0;
    public static final AlertLevel MEDIUM = new AlertLevel("MEDIUM", 1);
    public static final int _MEDIUM = 1;
    public static final AlertLevel HIGH = new AlertLevel("HIGH", 2);
    public static final int _HIGH = 2;
    public static final AlertLevel CRITICAL = new AlertLevel("CRITICAL", 3);
    public static final int _CRITICAL = 3;
    public static AlertLevel valueOf(int ordinal) {
        switch(ordinal) {

            case 0: return AlertLevel.LOW;
            case 1: return AlertLevel.MEDIUM;
            case 2: return AlertLevel.HIGH;
            case 3: return AlertLevel.CRITICAL;

        }
        return null;
    }

    public static AlertLevel from_int(int __value) {
        return valueOf(__value);
    }

    public static int[] getOrdinals() {
        int i = 0;
        int[] values = new int[4];

        values[i] = LOW.ordinal();
        i++;
        values[i] = MEDIUM.ordinal();
        i++;
        values[i] = HIGH.ordinal();
        i++;
        values[i] = CRITICAL.ordinal();
        i++;

        return values;
    }

    public int value() {
        return super.ordinal();
    }

    /**
    * Create a default instance
    */  
    public static AlertLevel create() {

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

    private AlertLevel(java.lang.String name, int ordinal) {
        super(name, ordinal);
    }
}

