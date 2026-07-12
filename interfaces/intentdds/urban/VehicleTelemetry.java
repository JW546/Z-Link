

/*
WARNING: THIS FILE IS AUTO-GENERATED. DO NOT MODIFY.

This file was generated from .idl 
using RTI Code Generator (rtiddsgen) version 4.6.0.
The rtiddsgen tool is part of the RTI Connext DDS distribution.
For more information, type 'rtiddsgen -help' at a command shell
or consult the Code Generator User's Manual.
*/

package intentdds.urban;

import com.rti.dds.infrastructure.*;
import com.rti.dds.infrastructure.Copyable;
import java.io.Serializable;
import com.rti.dds.cdr.CdrHelper;

// Depending on the type represented in the IDL, we may perform some redundant
// casts, we are suppressing that warning
@SuppressWarnings("cast")
public class VehicleTelemetry   implements Copyable, Serializable{

    private static final long serialVersionUID = -1811079032L;

    public intentdds.urban.Header header = (intentdds.urban.Header)intentdds.urban.Header.create();
    public String vehicle_role= (String)""; /* maximum length = (64) */
    public double position_x_m = (double)0;
    public double position_y_m = (double)0;
    public double speed_mps = (double)0;
    public double heading_deg = (double)0;
    public double battery_pct = (double)0;
    public double road_friction = (double)0;
    public String traffic_signal= (String)""; /* maximum length = (64) */
    public String road_event= (String)""; /* maximum length = (256) */

    public VehicleTelemetry() {

    }
    public VehicleTelemetry (VehicleTelemetry other) {

        this();
        copy_from(other);
    }

    public static java.lang.Object create() {

        VehicleTelemetry self;
        self = new  VehicleTelemetry();
        self.clear();
        return self;

    }

    public void clear() {

        if (header != null) {
            header.clear();
        }
        vehicle_role = (String)"";
        position_x_m = (double)0;
        position_y_m = (double)0;
        speed_mps = (double)0;
        heading_deg = (double)0;
        battery_pct = (double)0;
        road_friction = (double)0;
        traffic_signal = (String)"";
        road_event = (String)"";
    }

    @Override
    public boolean equals(java.lang.Object o) {

        if (o == null) {
            return false;
        }        

        if(getClass() != o.getClass()) {
            return false;
        }

        VehicleTelemetry otherObj = (VehicleTelemetry)o;

        if(!this.header.equals(otherObj.header)) {
            return false;
        }
        if(!this.vehicle_role.equals(otherObj.vehicle_role)) {
            return false;
        }
        if(this.position_x_m != otherObj.position_x_m) {
            return false;
        }
        if(this.position_y_m != otherObj.position_y_m) {
            return false;
        }
        if(this.speed_mps != otherObj.speed_mps) {
            return false;
        }
        if(this.heading_deg != otherObj.heading_deg) {
            return false;
        }
        if(this.battery_pct != otherObj.battery_pct) {
            return false;
        }
        if(this.road_friction != otherObj.road_friction) {
            return false;
        }
        if(!this.traffic_signal.equals(otherObj.traffic_signal)) {
            return false;
        }
        if(!this.road_event.equals(otherObj.road_event)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + header.hashCode(); 
        __result = __prime * __result + vehicle_role.hashCode(); 
        __result = __prime * __result + (int)position_x_m;
        __result = __prime * __result + (int)position_y_m;
        __result = __prime * __result + (int)speed_mps;
        __result = __prime * __result + (int)heading_deg;
        __result = __prime * __result + (int)battery_pct;
        __result = __prime * __result + (int)road_friction;
        __result = __prime * __result + traffic_signal.hashCode(); 
        __result = __prime * __result + road_event.hashCode(); 
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>VehicleTelemetryTypeSupport</code>
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

        VehicleTelemetry typedSrc = (VehicleTelemetry) src;
        VehicleTelemetry typedDst = this;

        typedDst.header = (intentdds.urban.Header) typedDst.header.copy_from(typedSrc.header);
        typedDst.vehicle_role = typedSrc.vehicle_role;
        typedDst.position_x_m = typedSrc.position_x_m;
        typedDst.position_y_m = typedSrc.position_y_m;
        typedDst.speed_mps = typedSrc.speed_mps;
        typedDst.heading_deg = typedSrc.heading_deg;
        typedDst.battery_pct = typedSrc.battery_pct;
        typedDst.road_friction = typedSrc.road_friction;
        typedDst.traffic_signal = typedSrc.traffic_signal;
        typedDst.road_event = typedSrc.road_event;

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

        strBuffer.append(this.header.toString("header ", indent+1));
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("vehicle_role: ").append(this.vehicle_role).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("position_x_m: ").append(this.position_x_m).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("position_y_m: ").append(this.position_y_m).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("speed_mps: ").append(this.speed_mps).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("heading_deg: ").append(this.heading_deg).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("battery_pct: ").append(this.battery_pct).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("road_friction: ").append(this.road_friction).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("traffic_signal: ").append(this.traffic_signal).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("road_event: ").append(this.road_event).append("\n");  

        return strBuffer.toString();
    }

}
