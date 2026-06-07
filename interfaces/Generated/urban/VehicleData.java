

/*
WARNING: THIS FILE IS AUTO-GENERATED. DO NOT MODIFY.

This file was generated from .idl 
using RTI Code Generator (rtiddsgen) version 4.6.0.
The rtiddsgen tool is part of the RTI Connext DDS distribution.
For more information, type 'rtiddsgen -help' at a command shell
or consult the Code Generator User's Manual.
*/

package urban;

import com.rti.dds.infrastructure.*;
import com.rti.dds.infrastructure.Copyable;
import java.io.Serializable;
import com.rti.dds.cdr.CdrHelper;
// Depending on the type represented in the IDL, we may perform some redundant
// casts, we are suppressing that warning
@SuppressWarnings("cast")
public class VehicleData   implements Copyable, Serializable{

    private static final long serialVersionUID = -1695613332L;

    public String vehicle_id= (String)""; /* maximum length = (32) */
    public double latitude = (double)0;
    public double longitude = (double)0;
    public double altitude = (double)0;
    public double speed = (double)0;
    public double heading = (double)0;
    public double acceleration = (double)0;
    public int battery_level = (int)0;
    public String drive_mode= (String)""; /* maximum length = (32) */
    public int timestamp = (int)0;

    public VehicleData() {

    }
    public VehicleData (VehicleData other) {

        this();
        copy_from(other);
    }

    public static java.lang.Object create() {

        VehicleData self;
        self = new  VehicleData();
        self.clear();
        return self;

    }

    public void clear() {

        vehicle_id = (String)"";
        latitude = (double)0;
        longitude = (double)0;
        altitude = (double)0;
        speed = (double)0;
        heading = (double)0;
        acceleration = (double)0;
        battery_level = (int)0;
        drive_mode = (String)"";
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

        VehicleData otherObj = (VehicleData)o;

        if(!this.vehicle_id.equals(otherObj.vehicle_id)) {
            return false;
        }
        if(this.latitude != otherObj.latitude) {
            return false;
        }
        if(this.longitude != otherObj.longitude) {
            return false;
        }
        if(this.altitude != otherObj.altitude) {
            return false;
        }
        if(this.speed != otherObj.speed) {
            return false;
        }
        if(this.heading != otherObj.heading) {
            return false;
        }
        if(this.acceleration != otherObj.acceleration) {
            return false;
        }
        if(this.battery_level != otherObj.battery_level) {
            return false;
        }
        if(!this.drive_mode.equals(otherObj.drive_mode)) {
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
        __result = __prime * __result + vehicle_id.hashCode(); 
        __result = __prime * __result + (int)latitude;
        __result = __prime * __result + (int)longitude;
        __result = __prime * __result + (int)altitude;
        __result = __prime * __result + (int)speed;
        __result = __prime * __result + (int)heading;
        __result = __prime * __result + (int)acceleration;
        __result = __prime * __result + (int)battery_level;
        __result = __prime * __result + drive_mode.hashCode(); 
        __result = __prime * __result + (int)timestamp;
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>VehicleDataTypeSupport</code>
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

        VehicleData typedSrc = (VehicleData) src;
        VehicleData typedDst = this;

        typedDst.vehicle_id = typedSrc.vehicle_id;
        typedDst.latitude = typedSrc.latitude;
        typedDst.longitude = typedSrc.longitude;
        typedDst.altitude = typedSrc.altitude;
        typedDst.speed = typedSrc.speed;
        typedDst.heading = typedSrc.heading;
        typedDst.acceleration = typedSrc.acceleration;
        typedDst.battery_level = typedSrc.battery_level;
        typedDst.drive_mode = typedSrc.drive_mode;
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
        strBuffer.append("vehicle_id: ").append(this.vehicle_id).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("latitude: ").append(this.latitude).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("longitude: ").append(this.longitude).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("altitude: ").append(this.altitude).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("speed: ").append(this.speed).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("heading: ").append(this.heading).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("acceleration: ").append(this.acceleration).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("battery_level: ").append(this.battery_level).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("drive_mode: ").append(this.drive_mode).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("timestamp: ").append(this.timestamp).append("\n");  

        return strBuffer.toString();
    }

}
