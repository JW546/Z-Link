

/*
WARNING: THIS FILE IS AUTO-GENERATED. DO NOT MODIFY.

This file was generated from .idl 
using RTI Code Generator (rtiddsgen) version 4.6.0.
The rtiddsgen tool is part of the RTI Connext DDS distribution.
For more information, type 'rtiddsgen -help' at a command shell
or consult the Code Generator User's Manual.
*/

package chongqing;

import com.rti.dds.infrastructure.*;
import com.rti.dds.infrastructure.Copyable;
import java.io.Serializable;
import com.rti.dds.cdr.CdrHelper;
// Depending on the type represented in the IDL, we may perform some redundant
// casts, we are suppressing that warning
@SuppressWarnings("cast")
public class Position3D   implements Copyable, Serializable{

    private static final long serialVersionUID = 1829928137L;

    public double latitude = (double)0;
    public double longitude = (double)0;
    public double altitude = (double)0;

    public Position3D() {

    }
    public Position3D (Position3D other) {

        this();
        copy_from(other);
    }

    public static java.lang.Object create() {

        Position3D self;
        self = new  Position3D();
        self.clear();
        return self;

    }

    public void clear() {

        latitude = (double)0;
        longitude = (double)0;
        altitude = (double)0;
    }

    @Override
    public boolean equals(java.lang.Object o) {

        if (o == null) {
            return false;
        }        

        if(getClass() != o.getClass()) {
            return false;
        }

        Position3D otherObj = (Position3D)o;

        if(this.latitude != otherObj.latitude) {
            return false;
        }
        if(this.longitude != otherObj.longitude) {
            return false;
        }
        if(this.altitude != otherObj.altitude) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + (int)latitude;
        __result = __prime * __result + (int)longitude;
        __result = __prime * __result + (int)altitude;
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>Position3DTypeSupport</code>
    * rather than here by using the <code>-noCopyable</code> option
    * to rtiddsgen.
    * 
    * @param src The Object which contains the data to be copied.
    * @return Returns <code>this</code>.
    * @exception NullPointerException If <code>src</code> is null.
    * @exception ClassCastException If <code>src</code> is not the 
    * same type as <code>this</code>.
    * @see com.rti.dds.infrastructure.Copyable#copy_from(java.lang.Object)
    */
    public java.lang.Object copy_from(java.lang.Object src) {

        Position3D typedSrc = (Position3D) src;
        Position3D typedDst = this;

        typedDst.latitude = typedSrc.latitude;
        typedDst.longitude = typedSrc.longitude;
        typedDst.altitude = typedSrc.altitude;

        return this;
    }

    @Override
    public java.lang.String toString(){
        return toString("", 0);
    }

    public java.lang.String toString(java.lang.String desc, int indent) {
        java.lang.StringBuffer strBuffer = new java.lang.StringBuffer();

        if (desc != null) {
            CdrHelper.printIndent(strBuffer, indent);
            strBuffer.append(desc).append(":\n");
        }

        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("latitude: ").append(this.latitude).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("longitude: ").append(this.longitude).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("altitude: ").append(this.altitude).append("\n");  

        return strBuffer.toString();
    }

}
