

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
public class SwarmMission   implements Copyable, Serializable{

    private static final long serialVersionUID = -321447699L;

    public String mission_id= (String)""; /* maximum length = (64) */
    public int team_size = (int)0;
    public String leader_drone_id= (String)""; /* maximum length = (32) */
    public String mission_type= (String)""; /* maximum length = (32) */
    public StringSeq drone_ids =  new StringSeq(20);
    public String waypoints= (String)""; /* maximum length = (512) */
    public int timestamp = (int)0;

    public SwarmMission() {

    }
    public SwarmMission (SwarmMission other) {

        this();
        copy_from(other);
    }

    public static java.lang.Object create() {

        SwarmMission self;
        self = new  SwarmMission();
        self.clear();
        return self;

    }

    public void clear() {

        mission_id = (String)"";
        team_size = (int)0;
        leader_drone_id = (String)"";
        mission_type = (String)"";
        if (drone_ids != null) {
            drone_ids.clear();
        }
        waypoints = (String)"";
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

        SwarmMission otherObj = (SwarmMission)o;

        if(!this.mission_id.equals(otherObj.mission_id)) {
            return false;
        }
        if(this.team_size != otherObj.team_size) {
            return false;
        }
        if(!this.leader_drone_id.equals(otherObj.leader_drone_id)) {
            return false;
        }
        if(!this.mission_type.equals(otherObj.mission_type)) {
            return false;
        }
        if(!this.drone_ids.equals(otherObj.drone_ids)) {
            return false;
        }
        if(!this.waypoints.equals(otherObj.waypoints)) {
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
        __result = __prime * __result + mission_id.hashCode(); 
        __result = __prime * __result + (int)team_size;
        __result = __prime * __result + leader_drone_id.hashCode(); 
        __result = __prime * __result + mission_type.hashCode(); 
        __result = __prime * __result + drone_ids.hashCode(); 
        __result = __prime * __result + waypoints.hashCode(); 
        __result = __prime * __result + (int)timestamp;
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>SwarmMissionTypeSupport</code>
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

        SwarmMission typedSrc = (SwarmMission) src;
        SwarmMission typedDst = this;

        typedDst.mission_id = typedSrc.mission_id;
        typedDst.team_size = typedSrc.team_size;
        typedDst.leader_drone_id = typedSrc.leader_drone_id;
        typedDst.mission_type = typedSrc.mission_type;
        typedDst.drone_ids.copy_from(typedSrc.drone_ids);
        typedDst.waypoints = typedSrc.waypoints;
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
        strBuffer.append("mission_id: ").append(this.mission_id).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("team_size: ").append(this.team_size).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("leader_drone_id: ").append(this.leader_drone_id).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("mission_type: ").append(this.mission_type).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);
        strBuffer.append("drone_ids: ");
        for(int i__ = 0; i__ < this.drone_ids.size(); ++i__) {
            if (i__!=0) strBuffer.append(", ");
            strBuffer.append(this.drone_ids.get(i__));
        }
        strBuffer.append("\n"); 
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("waypoints: ").append(this.waypoints).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("timestamp: ").append(this.timestamp).append("\n");  

        return strBuffer.toString();
    }

}
