

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
public class DronePosition   implements Copyable, Serializable{

    private static final long serialVersionUID = 2098063946L;

    public String drone_id= (String)""; /* maximum length = (32) */
    public chongqing.Position3D position = (chongqing.Position3D)chongqing.Position3D.create();
    public double velocity_north = (double)0;
    public double velocity_east = (double)0;
    public double speed_ground = (double)0;
    public double course = (double)0;
    public int timestamp = (int)0;

    public DronePosition() {

    }
    public DronePosition (DronePosition other) {

        this();
        copy_from(other);
    }

    public static java.lang.Object create() {

        DronePosition self;
        self = new  DronePosition();
        self.clear();
        return self;

    }

    public void clear() {

        drone_id = (String)"";
        if (position != null) {
            position.clear();
        }
        velocity_north = (double)0;
        velocity_east = (double)0;
        speed_ground = (double)0;
        course = (double)0;
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

        DronePosition otherObj = (DronePosition)o;

        if(!this.drone_id.equals(otherObj.drone_id)) {
            return false;
        }
        if(!this.position.equals(otherObj.position)) {
            return false;
        }
        if(this.velocity_north != otherObj.velocity_north) {
            return false;
        }
        if(this.velocity_east != otherObj.velocity_east) {
            return false;
        }
        if(this.speed_ground != otherObj.speed_ground) {
            return false;
        }
        if(this.course != otherObj.course) {
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
        __result = __prime * __result + drone_id.hashCode(); 
        __result = __prime * __result + position.hashCode(); 
        __result = __prime * __result + (int)velocity_north;
        __result = __prime * __result + (int)velocity_east;
        __result = __prime * __result + (int)speed_ground;
        __result = __prime * __result + (int)course;
        __result = __prime * __result + (int)timestamp;
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>DronePositionTypeSupport</code>
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

        DronePosition typedSrc = (DronePosition) src;
        DronePosition typedDst = this;

        typedDst.drone_id = typedSrc.drone_id;
        typedDst.position = (chongqing.Position3D) typedDst.position.copy_from(typedSrc.position);
        typedDst.velocity_north = typedSrc.velocity_north;
        typedDst.velocity_east = typedSrc.velocity_east;
        typedDst.speed_ground = typedSrc.speed_ground;
        typedDst.course = typedSrc.course;
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
        strBuffer.append("drone_id: ").append(this.drone_id).append("\n");  
        strBuffer.append(this.position.toString("position ", indent+1));
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("velocity_north: ").append(this.velocity_north).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("velocity_east: ").append(this.velocity_east).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("speed_ground: ").append(this.speed_ground).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("course: ").append(this.course).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("timestamp: ").append(this.timestamp).append("\n");  

        return strBuffer.toString();
    }

}
