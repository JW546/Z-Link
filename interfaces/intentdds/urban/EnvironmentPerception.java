

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
public class EnvironmentPerception   implements Copyable, Serializable{

    private static final long serialVersionUID = 580626327L;

    public intentdds.urban.Header header = (intentdds.urban.Header)intentdds.urban.Header.create();
    public String observer_type= (String)""; /* maximum length = (64) */
    public String event_type= (String)""; /* maximum length = (64) */
    public double confidence = (double)0;
    public double risk_score = (double)0;
    public double visibility_m = (double)0;
    public double wind_speed_mps = (double)0;
    public int pedestrian_count = (int)0;
    public String evidence_summary= (String)""; /* maximum length = (256) */

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

        if (header != null) {
            header.clear();
        }
        observer_type = (String)"";
        event_type = (String)"";
        confidence = (double)0;
        risk_score = (double)0;
        visibility_m = (double)0;
        wind_speed_mps = (double)0;
        pedestrian_count = (int)0;
        evidence_summary = (String)"";
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

        if(!this.header.equals(otherObj.header)) {
            return false;
        }
        if(!this.observer_type.equals(otherObj.observer_type)) {
            return false;
        }
        if(!this.event_type.equals(otherObj.event_type)) {
            return false;
        }
        if(this.confidence != otherObj.confidence) {
            return false;
        }
        if(this.risk_score != otherObj.risk_score) {
            return false;
        }
        if(this.visibility_m != otherObj.visibility_m) {
            return false;
        }
        if(this.wind_speed_mps != otherObj.wind_speed_mps) {
            return false;
        }
        if(this.pedestrian_count != otherObj.pedestrian_count) {
            return false;
        }
        if(!this.evidence_summary.equals(otherObj.evidence_summary)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + header.hashCode(); 
        __result = __prime * __result + observer_type.hashCode(); 
        __result = __prime * __result + event_type.hashCode(); 
        __result = __prime * __result + (int)confidence;
        __result = __prime * __result + (int)risk_score;
        __result = __prime * __result + (int)visibility_m;
        __result = __prime * __result + (int)wind_speed_mps;
        __result = __prime * __result + (int)pedestrian_count;
        __result = __prime * __result + evidence_summary.hashCode(); 
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

        typedDst.header = (intentdds.urban.Header) typedDst.header.copy_from(typedSrc.header);
        typedDst.observer_type = typedSrc.observer_type;
        typedDst.event_type = typedSrc.event_type;
        typedDst.confidence = typedSrc.confidence;
        typedDst.risk_score = typedSrc.risk_score;
        typedDst.visibility_m = typedSrc.visibility_m;
        typedDst.wind_speed_mps = typedSrc.wind_speed_mps;
        typedDst.pedestrian_count = typedSrc.pedestrian_count;
        typedDst.evidence_summary = typedSrc.evidence_summary;

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
        strBuffer.append("observer_type: ").append(this.observer_type).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("event_type: ").append(this.event_type).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("confidence: ").append(this.confidence).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("risk_score: ").append(this.risk_score).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("visibility_m: ").append(this.visibility_m).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("wind_speed_mps: ").append(this.wind_speed_mps).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("pedestrian_count: ").append(this.pedestrian_count).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("evidence_summary: ").append(this.evidence_summary).append("\n");  

        return strBuffer.toString();
    }

}
