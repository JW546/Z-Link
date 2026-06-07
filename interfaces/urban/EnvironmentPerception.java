

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
public class EnvironmentPerception   implements Copyable, Serializable{

    private static final long serialVersionUID = -1258910705L;

    public String drone_id= (String)""; /* maximum length = (32) */
    public double temperature = (double)0;
    public double wind_speed = (double)0;
    public double visibility = (double)0;
    public double turbulence_level = (double)0;
    public int illumination = (int)0;
    public int timestamp = (int)0;

    public EnvironmentPerception() {

    }
    public EnvironmentPerception (EnvironmentPerception other) {

        this();
        copy_from(other);
    }

    public static java.lang.Object create() {

        EnvironmentPerception self;
        self = new  EnvironmentPerception();
        self.clear();
        return self;

    }

    public void clear() {

        drone_id = (String)"";
        temperature = (double)0;
        wind_speed = (double)0;
        visibility = (double)0;
        turbulence_level = (double)0;
        illumination = (int)0;
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

        EnvironmentPerception otherObj = (EnvironmentPerception)o;

        if(!this.drone_id.equals(otherObj.drone_id)) {
            return false;
        }
        if(this.temperature != otherObj.temperature) {
            return false;
        }
        if(this.wind_speed != otherObj.wind_speed) {
            return false;
        }
        if(this.visibility != otherObj.visibility) {
            return false;
        }
        if(this.turbulence_level != otherObj.turbulence_level) {
            return false;
        }
        if(this.illumination != otherObj.illumination) {
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
        __result = __prime * __result + (int)temperature;
        __result = __prime * __result + (int)wind_speed;
        __result = __prime * __result + (int)visibility;
        __result = __prime * __result + (int)turbulence_level;
        __result = __prime * __result + (int)illumination;
        __result = __prime * __result + (int)timestamp;
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>EnvironmentPerceptionTypeSupport</code>
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

        EnvironmentPerception typedSrc = (EnvironmentPerception) src;
        EnvironmentPerception typedDst = this;

        typedDst.drone_id = typedSrc.drone_id;
        typedDst.temperature = typedSrc.temperature;
        typedDst.wind_speed = typedSrc.wind_speed;
        typedDst.visibility = typedSrc.visibility;
        typedDst.turbulence_level = typedSrc.turbulence_level;
        typedDst.illumination = typedSrc.illumination;
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
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("temperature: ").append(this.temperature).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("wind_speed: ").append(this.wind_speed).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("visibility: ").append(this.visibility).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("turbulence_level: ").append(this.turbulence_level).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("illumination: ").append(this.illumination).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("timestamp: ").append(this.timestamp).append("\n");  

        return strBuffer.toString();
    }

}
