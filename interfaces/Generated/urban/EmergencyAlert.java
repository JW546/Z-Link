

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
public class EmergencyAlert   implements Copyable, Serializable{

    private static final long serialVersionUID = 1467035390L;

    public int alert_id = (int)0;
    public urban.AlertLevel level = (urban.AlertLevel)urban.AlertLevel.valueOf(0);
    public String alert_type= (String)""; /* maximum length = (64) */
    public double latitude = (double)0;
    public double longitude = (double)0;
    public double affected_radius = (double)0;
    public String recommendation= (String)""; /* maximum length = (256) */
    public int timestamp = (int)0;
    public int expire_time = (int)0;

    public EmergencyAlert() {

    }
    public EmergencyAlert (EmergencyAlert other) {

        this();
        copy_from(other);
    }

    public static java.lang.Object create() {

        EmergencyAlert self;
        self = new  EmergencyAlert();
        self.clear();
        return self;

    }

    public void clear() {

        alert_id = (int)0;
        level = urban.AlertLevel.valueOf(0);
        alert_type = (String)"";
        latitude = (double)0;
        longitude = (double)0;
        affected_radius = (double)0;
        recommendation = (String)"";
        timestamp = (int)0;
        expire_time = (int)0;
    }

    @Override
    public boolean equals(java.lang.Object o) {

        if (o == null) {
            return false;
        }        

        if(getClass() != o.getClass()) {
            return false;
        }

        EmergencyAlert otherObj = (EmergencyAlert)o;

        if(this.alert_id != otherObj.alert_id) {
            return false;
        }
        if(!this.level.equals(otherObj.level)) {
            return false;
        }
        if(!this.alert_type.equals(otherObj.alert_type)) {
            return false;
        }
        if(this.latitude != otherObj.latitude) {
            return false;
        }
        if(this.longitude != otherObj.longitude) {
            return false;
        }
        if(this.affected_radius != otherObj.affected_radius) {
            return false;
        }
        if(!this.recommendation.equals(otherObj.recommendation)) {
            return false;
        }
        if(this.timestamp != otherObj.timestamp) {
            return false;
        }
        if(this.expire_time != otherObj.expire_time) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + (int)alert_id;
        __result = __prime * __result + level.hashCode(); 
        __result = __prime * __result + alert_type.hashCode(); 
        __result = __prime * __result + (int)latitude;
        __result = __prime * __result + (int)longitude;
        __result = __prime * __result + (int)affected_radius;
        __result = __prime * __result + recommendation.hashCode(); 
        __result = __prime * __result + (int)timestamp;
        __result = __prime * __result + (int)expire_time;
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>EmergencyAlertTypeSupport</code>
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

        EmergencyAlert typedSrc = (EmergencyAlert) src;
        EmergencyAlert typedDst = this;

        typedDst.alert_id = typedSrc.alert_id;
        typedDst.level = (urban.AlertLevel) typedDst.level.copy_from(typedSrc.level);
        typedDst.alert_type = typedSrc.alert_type;
        typedDst.latitude = typedSrc.latitude;
        typedDst.longitude = typedSrc.longitude;
        typedDst.affected_radius = typedSrc.affected_radius;
        typedDst.recommendation = typedSrc.recommendation;
        typedDst.timestamp = typedSrc.timestamp;
        typedDst.expire_time = typedSrc.expire_time;

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
        strBuffer.append("alert_id: ").append(this.alert_id).append("\n");  
        strBuffer.append(this.level.toString("level ", indent+1));
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("alert_type: ").append(this.alert_type).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("latitude: ").append(this.latitude).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("longitude: ").append(this.longitude).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("affected_radius: ").append(this.affected_radius).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("recommendation: ").append(this.recommendation).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("timestamp: ").append(this.timestamp).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("expire_time: ").append(this.expire_time).append("\n");  

        return strBuffer.toString();
    }

}
