

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
public class EmergencyStatus   implements Copyable, Serializable{

    private static final long serialVersionUID = -375587894L;

    public String drone_id= (String)""; /* maximum length = (32) */
    public chongqing.DeviceType device_type = (chongqing.DeviceType)chongqing.DeviceType.valueOf(0);
    public chongqing.MissionType mission_type = (chongqing.MissionType)chongqing.MissionType.valueOf(0);
    public chongqing.StatusCode status = (chongqing.StatusCode)chongqing.StatusCode.valueOf(0);
    public chongqing.Position3D current_position = (chongqing.Position3D)chongqing.Position3D.create();
    public double speed = (double)0;
    public double heading = (double)0;
    public double battery_level = (double)0;
    public double power_consumption = (double)0;
    public java.lang.Double  payload_weight = null;
    public String cargo_type= null; /* maximum length = (64) */
    public int timestamp_sec = (int)0;
    public int timestamp_usec = (int)0;
    public int priority = (int)0;
    public chongqing.Position3D target_position = null ;
    public String destination= null; /* maximum length = (128) */

    public EmergencyStatus() {

    }
    public EmergencyStatus (EmergencyStatus other) {

        this();
        copy_from(other);
    }

    public static java.lang.Object create() {

        EmergencyStatus self;
        self = new  EmergencyStatus();
        self.clear();
        return self;

    }

    public void clear() {

        drone_id = (String)"";
        device_type = chongqing.DeviceType.valueOf(0);
        mission_type = chongqing.MissionType.valueOf(0);
        status = chongqing.StatusCode.valueOf(0);
        if (current_position != null) {
            current_position.clear();
        }
        speed = (double)0;
        heading = (double)0;
        battery_level = (double)0;
        power_consumption = (double)0;
        payload_weight=null; 
        cargo_type=null; 
        timestamp_sec = (int)0;
        timestamp_usec = (int)0;
        priority = (int)0;
        target_position=null; 
        destination=null; 
    }

    @Override
    public boolean equals(java.lang.Object o) {

        if (o == null) {
            return false;
        }        

        if(getClass() != o.getClass()) {
            return false;
        }

        EmergencyStatus otherObj = (EmergencyStatus)o;

        if(!this.drone_id.equals(otherObj.drone_id)) {
            return false;
        }
        if(!this.device_type.equals(otherObj.device_type)) {
            return false;
        }
        if(!this.mission_type.equals(otherObj.mission_type)) {
            return false;
        }
        if(!this.status.equals(otherObj.status)) {
            return false;
        }
        if(!this.current_position.equals(otherObj.current_position)) {
            return false;
        }
        if(this.speed != otherObj.speed) {
            return false;
        }
        if(this.heading != otherObj.heading) {
            return false;
        }
        if(this.battery_level != otherObj.battery_level) {
            return false;
        }
        if(this.power_consumption != otherObj.power_consumption) {
            return false;
        }
        if ((this.payload_weight == null && otherObj.payload_weight != null) ||
        (this.payload_weight != null && otherObj.payload_weight == null)) {
            return false;
        }
        if (this.payload_weight != null) {
            if(!this.payload_weight.equals(otherObj.payload_weight)) {
                return false;
            }
        }
        if ((this.cargo_type == null && otherObj.cargo_type != null) ||
        (this.cargo_type != null && otherObj.cargo_type == null)) {
            return false;
        }
        if (this.cargo_type != null) {
            if(!this.cargo_type.equals(otherObj.cargo_type)) {
                return false;
            }
        }
        if(this.timestamp_sec != otherObj.timestamp_sec) {
            return false;
        }
        if(this.timestamp_usec != otherObj.timestamp_usec) {
            return false;
        }
        if(this.priority != otherObj.priority) {
            return false;
        }
        if ((this.target_position == null && otherObj.target_position != null) ||
        (this.target_position != null && otherObj.target_position == null)) {
            return false;
        }
        if (this.target_position != null) {
            if(!this.target_position.equals(otherObj.target_position)) {
                return false;
            }
        }
        if ((this.destination == null && otherObj.destination != null) ||
        (this.destination != null && otherObj.destination == null)) {
            return false;
        }
        if (this.destination != null) {
            if(!this.destination.equals(otherObj.destination)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + drone_id.hashCode(); 
        __result = __prime * __result + device_type.hashCode(); 
        __result = __prime * __result + mission_type.hashCode(); 
        __result = __prime * __result + status.hashCode(); 
        __result = __prime * __result + current_position.hashCode(); 
        __result = __prime * __result + (int)speed;
        __result = __prime * __result + (int)heading;
        __result = __prime * __result + (int)battery_level;
        __result = __prime * __result + (int)power_consumption;
        if (payload_weight != null) {
            __result = __prime * __result + payload_weight.hashCode();
        }
        if (cargo_type != null) {
            __result = __prime * __result + cargo_type.hashCode(); 
        }
        __result = __prime * __result + (int)timestamp_sec;
        __result = __prime * __result + (int)timestamp_usec;
        __result = __prime * __result + (int)priority;
        if (target_position != null) {
            __result = __prime * __result + target_position.hashCode(); 
        }
        if (destination != null) {
            __result = __prime * __result + destination.hashCode(); 
        }
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>EmergencyStatusTypeSupport</code>
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

        EmergencyStatus typedSrc = (EmergencyStatus) src;
        EmergencyStatus typedDst = this;

        typedDst.drone_id = typedSrc.drone_id;
        typedDst.device_type = (chongqing.DeviceType) typedDst.device_type.copy_from(typedSrc.device_type);
        typedDst.mission_type = (chongqing.MissionType) typedDst.mission_type.copy_from(typedSrc.mission_type);
        typedDst.status = (chongqing.StatusCode) typedDst.status.copy_from(typedSrc.status);
        typedDst.current_position = (chongqing.Position3D) typedDst.current_position.copy_from(typedSrc.current_position);
        typedDst.speed = typedSrc.speed;
        typedDst.heading = typedSrc.heading;
        typedDst.battery_level = typedSrc.battery_level;
        typedDst.power_consumption = typedSrc.power_consumption;

        typedDst.payload_weight = typedSrc.payload_weight;

        typedDst.cargo_type = typedSrc.cargo_type;
        typedDst.timestamp_sec = typedSrc.timestamp_sec;
        typedDst.timestamp_usec = typedSrc.timestamp_usec;
        typedDst.priority = typedSrc.priority;

        if (typedDst.target_position == null && typedSrc.target_position !=null){
            typedDst.target_position = 
            (chongqing.Position3D)chongqing.Position3D.create();
        }
        if(typedSrc.target_position !=null){
            typedDst.target_position = (chongqing.Position3D) typedDst.target_position.copy_from(typedSrc.target_position);
        } else{
            typedDst.target_position=null;
        }

        typedDst.destination = typedSrc.destination;

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
        strBuffer.append("drone_id: ").append(this.drone_id).append("\n");  
        strBuffer.append(this.device_type.toString("device_type ", indent+1));
        strBuffer.append(this.mission_type.toString("mission_type ", indent+1));
        strBuffer.append(this.status.toString("status ", indent+1));
        strBuffer.append(this.current_position.toString("current_position ", indent+1));
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("speed: ").append(this.speed).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("heading: ").append(this.heading).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("battery_level: ").append(this.battery_level).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("power_consumption: ").append(this.power_consumption).append("\n");  
        if (this.payload_weight != null) {
            CdrHelper.printIndent(strBuffer, indent+1);        
            strBuffer.append("payload_weight: ").append(this.payload_weight).append("\n");  
        } else {
            CdrHelper.printIndent(strBuffer, indent+1);
            strBuffer.append("payload_weight: null\n");
        }
        if (this.cargo_type != null) {
            CdrHelper.printIndent(strBuffer, indent+1);        
            strBuffer.append("cargo_type: ").append(this.cargo_type).append("\n");  
        } else {
            CdrHelper.printIndent(strBuffer, indent+1);
            strBuffer.append("cargo_type: null\n");
        }
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("timestamp_sec: ").append(this.timestamp_sec).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("timestamp_usec: ").append(this.timestamp_usec).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("priority: ").append(this.priority).append("\n");  
        if (this.target_position != null) {
            strBuffer.append(this.target_position.toString("target_position ", indent+1));
        } else {
            CdrHelper.printIndent(strBuffer, indent+1);
            strBuffer.append("target_position: null\n");
        }
        if (this.destination != null) {
            CdrHelper.printIndent(strBuffer, indent+1);        
            strBuffer.append("destination: ").append(this.destination).append("\n");  
        } else {
            CdrHelper.printIndent(strBuffer, indent+1);
            strBuffer.append("destination: null\n");
        }

        return strBuffer.toString();
    }

}
