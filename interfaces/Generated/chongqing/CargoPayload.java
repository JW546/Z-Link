

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
public class CargoPayload   implements Copyable, Serializable{

    private static final long serialVersionUID = -935652869L;

    public String cargo_id= (String)""; /* maximum length = (32) */
    public String drone_id= (String)""; /* maximum length = (32) */
    public double weight = (double)0;
    public String cargo_type= (String)""; /* maximum length = (64) */
    public double volume = (double)0;
    public java.lang.Double  temperature_min = null;
    public java.lang.Double  temperature_max = null;
    public boolean is_hazardous = (boolean)false;
    public boolean requires_fridge = (boolean)false;
    public int timestamp = (int)0;

    public CargoPayload() {

    }
    public CargoPayload (CargoPayload other) {

        this();
        copy_from(other);
    }

    public static java.lang.Object create() {

        CargoPayload self;
        self = new  CargoPayload();
        self.clear();
        return self;

    }

    public void clear() {

        cargo_id = (String)"";
        drone_id = (String)"";
        weight = (double)0;
        cargo_type = (String)"";
        volume = (double)0;
        temperature_min=null; 
        temperature_max=null; 
        is_hazardous = (boolean)false;
        requires_fridge = (boolean)false;
        timestamp = (int)0;
    }

    @Override
    public boolean equals(java.lang.Object o) {

        if (o == null) {
            return false;
        }        

        if(getClass() != o.getClass()) {
            return false;
        }

        CargoPayload otherObj = (CargoPayload)o;

        if(!this.cargo_id.equals(otherObj.cargo_id)) {
            return false;
        }
        if(!this.drone_id.equals(otherObj.drone_id)) {
            return false;
        }
        if(this.weight != otherObj.weight) {
            return false;
        }
        if(!this.cargo_type.equals(otherObj.cargo_type)) {
            return false;
        }
        if(this.volume != otherObj.volume) {
            return false;
        }
        if ((this.temperature_min == null && otherObj.temperature_min != null) ||
        (this.temperature_min != null && otherObj.temperature_min == null)) {
            return false;
        }
        if (this.temperature_min != null) {
            if(!this.temperature_min.equals(otherObj.temperature_min)) {
                return false;
            }
        }
        if ((this.temperature_max == null && otherObj.temperature_max != null) ||
        (this.temperature_max != null && otherObj.temperature_max == null)) {
            return false;
        }
        if (this.temperature_max != null) {
            if(!this.temperature_max.equals(otherObj.temperature_max)) {
                return false;
            }
        }
        if(this.is_hazardous != otherObj.is_hazardous) {
            return false;
        }
        if(this.requires_fridge != otherObj.requires_fridge) {
            return false;
        }
        if(this.timestamp != otherObj.timestamp) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + cargo_id.hashCode(); 
        __result = __prime * __result + drone_id.hashCode(); 
        __result = __prime * __result + (int)weight;
        __result = __prime * __result + cargo_type.hashCode(); 
        __result = __prime * __result + (int)volume;
        if (temperature_min != null) {
            __result = __prime * __result + temperature_min.hashCode();
        }
        if (temperature_max != null) {
            __result = __prime * __result + temperature_max.hashCode();
        }
        __result = __prime * __result + ((is_hazardous == true)?1:0);
        __result = __prime * __result + ((requires_fridge == true)?1:0);
        __result = __prime * __result + (int)timestamp;
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>CargoPayloadTypeSupport</code>
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

        CargoPayload typedSrc = (CargoPayload) src;
        CargoPayload typedDst = this;

        typedDst.cargo_id = typedSrc.cargo_id;
        typedDst.drone_id = typedSrc.drone_id;
        typedDst.weight = typedSrc.weight;
        typedDst.cargo_type = typedSrc.cargo_type;
        typedDst.volume = typedSrc.volume;

        typedDst.temperature_min = typedSrc.temperature_min;

        typedDst.temperature_max = typedSrc.temperature_max;
        typedDst.is_hazardous = typedSrc.is_hazardous;
        typedDst.requires_fridge = typedSrc.requires_fridge;
        typedDst.timestamp = typedSrc.timestamp;

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
        strBuffer.append("cargo_id: ").append(this.cargo_id).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("drone_id: ").append(this.drone_id).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("weight: ").append(this.weight).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("cargo_type: ").append(this.cargo_type).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("volume: ").append(this.volume).append("\n");  
        if (this.temperature_min != null) {
            CdrHelper.printIndent(strBuffer, indent+1);        
            strBuffer.append("temperature_min: ").append(this.temperature_min).append("\n");  
        } else {
            CdrHelper.printIndent(strBuffer, indent+1);
            strBuffer.append("temperature_min: null\n");
        }
        if (this.temperature_max != null) {
            CdrHelper.printIndent(strBuffer, indent+1);        
            strBuffer.append("temperature_max: ").append(this.temperature_max).append("\n");  
        } else {
            CdrHelper.printIndent(strBuffer, indent+1);
            strBuffer.append("temperature_max: null\n");
        }
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("is_hazardous: ").append(this.is_hazardous).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("requires_fridge: ").append(this.requires_fridge).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("timestamp: ").append(this.timestamp).append("\n");  

        return strBuffer.toString();
    }

}
