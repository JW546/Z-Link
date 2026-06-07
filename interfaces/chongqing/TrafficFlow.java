

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
public class TrafficFlow   implements Copyable, Serializable{

    private static final long serialVersionUID = -1722013477L;

    public String zone_id= (String)""; /* maximum length = (32) */
    public int vehicle_count = (int)0;
    public double average_speed = (double)0;
    public double congestion_level = (double)0;
    public double latitude = (double)0;
    public double longitude = (double)0;
    public double radius = (double)0;
    public int timestamp = (int)0;

    public TrafficFlow() {

    }
    public TrafficFlow (TrafficFlow other) {

        this();
        copy_from(other);
    }

    public static java.lang.Object create() {

        TrafficFlow self;
        self = new  TrafficFlow();
        self.clear();
        return self;

    }

    public void clear() {

        zone_id = (String)"";
        vehicle_count = (int)0;
        average_speed = (double)0;
        congestion_level = (double)0;
        latitude = (double)0;
        longitude = (double)0;
        radius = (double)0;
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

        TrafficFlow otherObj = (TrafficFlow)o;

        if(!this.zone_id.equals(otherObj.zone_id)) {
            return false;
        }
        if(this.vehicle_count != otherObj.vehicle_count) {
            return false;
        }
        if(this.average_speed != otherObj.average_speed) {
            return false;
        }
        if(this.congestion_level != otherObj.congestion_level) {
            return false;
        }
        if(this.latitude != otherObj.latitude) {
            return false;
        }
        if(this.longitude != otherObj.longitude) {
            return false;
        }
        if(this.radius != otherObj.radius) {
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
        __result = __prime * __result + zone_id.hashCode(); 
        __result = __prime * __result + (int)vehicle_count;
        __result = __prime * __result + (int)average_speed;
        __result = __prime * __result + (int)congestion_level;
        __result = __prime * __result + (int)latitude;
        __result = __prime * __result + (int)longitude;
        __result = __prime * __result + (int)radius;
        __result = __prime * __result + (int)timestamp;
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>TrafficFlowTypeSupport</code>
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

        TrafficFlow typedSrc = (TrafficFlow) src;
        TrafficFlow typedDst = this;

        typedDst.zone_id = typedSrc.zone_id;
        typedDst.vehicle_count = typedSrc.vehicle_count;
        typedDst.average_speed = typedSrc.average_speed;
        typedDst.congestion_level = typedSrc.congestion_level;
        typedDst.latitude = typedSrc.latitude;
        typedDst.longitude = typedSrc.longitude;
        typedDst.radius = typedSrc.radius;
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
        strBuffer.append("zone_id: ").append(this.zone_id).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("vehicle_count: ").append(this.vehicle_count).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("average_speed: ").append(this.average_speed).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("congestion_level: ").append(this.congestion_level).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("latitude: ").append(this.latitude).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("longitude: ").append(this.longitude).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("radius: ").append(this.radius).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("timestamp: ").append(this.timestamp).append("\n");  

        return strBuffer.toString();
    }

}
