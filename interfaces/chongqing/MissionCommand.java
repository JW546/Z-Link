

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
public class MissionCommand   implements Copyable, Serializable{

    private static final long serialVersionUID = 187298929L;

    public String command_id= (String)""; /* maximum length = (32) */
    public String drone_id= (String)""; /* maximum length = (32) */
    public chongqing.MissionType mission_type = (chongqing.MissionType)chongqing.MissionType.valueOf(0);
    public String parameters= (String)""; /* maximum length = (256) */
    public int priority = (int)0;
    public int valid_until = (int)0;
    public String source_system= (String)""; /* maximum length = (64) */
    public String operator_id= (String)""; /* maximum length = (64) */

    public MissionCommand() {

    }
    public MissionCommand (MissionCommand other) {

        this();
        copy_from(other);
    }

    public static java.lang.Object create() {

        MissionCommand self;
        self = new  MissionCommand();
        self.clear();
        return self;

    }

    public void clear() {

        command_id = (String)"";
        drone_id = (String)"";
        mission_type = chongqing.MissionType.valueOf(0);
        parameters = (String)"";
        priority = (int)0;
        valid_until = (int)0;
        source_system = (String)"";
        operator_id = (String)"";
    }

    @Override
    public boolean equals(java.lang.Object o) {

        if (o == null) {
            return false;
        }        

        if(getClass() != o.getClass()) {
            return false;
        }

        MissionCommand otherObj = (MissionCommand)o;

        if(!this.command_id.equals(otherObj.command_id)) {
            return false;
        }
        if(!this.drone_id.equals(otherObj.drone_id)) {
            return false;
        }
        if(!this.mission_type.equals(otherObj.mission_type)) {
            return false;
        }
        if(!this.parameters.equals(otherObj.parameters)) {
            return false;
        }
        if(this.priority != otherObj.priority) {
            return false;
        }
        if(this.valid_until != otherObj.valid_until) {
            return false;
        }
        if(!this.source_system.equals(otherObj.source_system)) {
            return false;
        }
        if(!this.operator_id.equals(otherObj.operator_id)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + command_id.hashCode(); 
        __result = __prime * __result + drone_id.hashCode(); 
        __result = __prime * __result + mission_type.hashCode(); 
        __result = __prime * __result + parameters.hashCode(); 
        __result = __prime * __result + (int)priority;
        __result = __prime * __result + (int)valid_until;
        __result = __prime * __result + source_system.hashCode(); 
        __result = __prime * __result + operator_id.hashCode(); 
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>MissionCommandTypeSupport</code>
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

        MissionCommand typedSrc = (MissionCommand) src;
        MissionCommand typedDst = this;

        typedDst.command_id = typedSrc.command_id;
        typedDst.drone_id = typedSrc.drone_id;
        typedDst.mission_type = (chongqing.MissionType) typedDst.mission_type.copy_from(typedSrc.mission_type);
        typedDst.parameters = typedSrc.parameters;
        typedDst.priority = typedSrc.priority;
        typedDst.valid_until = typedSrc.valid_until;
        typedDst.source_system = typedSrc.source_system;
        typedDst.operator_id = typedSrc.operator_id;

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
        strBuffer.append("command_id: ").append(this.command_id).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("drone_id: ").append(this.drone_id).append("\n");  
        strBuffer.append(this.mission_type.toString("mission_type ", indent+1));
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("parameters: ").append(this.parameters).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("priority: ").append(this.priority).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("valid_until: ").append(this.valid_until).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("source_system: ").append(this.source_system).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("operator_id: ").append(this.operator_id).append("\n");  

        return strBuffer.toString();
    }

}
