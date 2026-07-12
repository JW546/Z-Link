

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
public class RouteFusionExperimentResult   implements Copyable, Serializable{

    private static final long serialVersionUID = 1984643572L;

    public intentdds.urban.Header header = (intentdds.urban.Header)intentdds.urban.Header.create();
    public String qos_mode= (String)""; /* maximum length = (64) */
    public String latency_profile= (String)""; /* maximum length = (64) */
    public String conflict_profile= (String)""; /* maximum length = (64) */
    public int total_advice = (int)0;
    public int delivered_advice = (int)0;
    public int dropped_advice = (int)0;
    public int retry_count = (int)0;
    public int stale_count = (int)0;
    public double average_latency_ms = (double)0;
    public double delivered_ratio = (double)0;
    public double route_stability = (double)0;
    public String final_route= (String)""; /* maximum length = (64) */
    public double final_risk_score = (double)0;
    public double confidence = (double)0;

    public RouteFusionExperimentResult() {

    }
    public RouteFusionExperimentResult (RouteFusionExperimentResult other) {

        this();
        copy_from(other);
    }

    public static java.lang.Object create() {

        RouteFusionExperimentResult self;
        self = new  RouteFusionExperimentResult();
        self.clear();
        return self;

    }

    public void clear() {

        if (header != null) {
            header.clear();
        }
        qos_mode = (String)"";
        latency_profile = (String)"";
        conflict_profile = (String)"";
        total_advice = (int)0;
        delivered_advice = (int)0;
        dropped_advice = (int)0;
        retry_count = (int)0;
        stale_count = (int)0;
        average_latency_ms = (double)0;
        delivered_ratio = (double)0;
        route_stability = (double)0;
        final_route = (String)"";
        final_risk_score = (double)0;
        confidence = (double)0;
    }

    @Override
    public boolean equals(java.lang.Object o) {

        if (o == null) {
            return false;
        }        

        if(getClass() != o.getClass()) {
            return false;
        }

        RouteFusionExperimentResult otherObj = (RouteFusionExperimentResult)o;

        if(!this.header.equals(otherObj.header)) {
            return false;
        }
        if(!this.qos_mode.equals(otherObj.qos_mode)) {
            return false;
        }
        if(!this.latency_profile.equals(otherObj.latency_profile)) {
            return false;
        }
        if(!this.conflict_profile.equals(otherObj.conflict_profile)) {
            return false;
        }
        if(this.total_advice != otherObj.total_advice) {
            return false;
        }
        if(this.delivered_advice != otherObj.delivered_advice) {
            return false;
        }
        if(this.dropped_advice != otherObj.dropped_advice) {
            return false;
        }
        if(this.retry_count != otherObj.retry_count) {
            return false;
        }
        if(this.stale_count != otherObj.stale_count) {
            return false;
        }
        if(this.average_latency_ms != otherObj.average_latency_ms) {
            return false;
        }
        if(this.delivered_ratio != otherObj.delivered_ratio) {
            return false;
        }
        if(this.route_stability != otherObj.route_stability) {
            return false;
        }
        if(!this.final_route.equals(otherObj.final_route)) {
            return false;
        }
        if(this.final_risk_score != otherObj.final_risk_score) {
            return false;
        }
        if(this.confidence != otherObj.confidence) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + header.hashCode(); 
        __result = __prime * __result + qos_mode.hashCode(); 
        __result = __prime * __result + latency_profile.hashCode(); 
        __result = __prime * __result + conflict_profile.hashCode(); 
        __result = __prime * __result + (int)total_advice;
        __result = __prime * __result + (int)delivered_advice;
        __result = __prime * __result + (int)dropped_advice;
        __result = __prime * __result + (int)retry_count;
        __result = __prime * __result + (int)stale_count;
        __result = __prime * __result + (int)average_latency_ms;
        __result = __prime * __result + (int)delivered_ratio;
        __result = __prime * __result + (int)route_stability;
        __result = __prime * __result + final_route.hashCode(); 
        __result = __prime * __result + (int)final_risk_score;
        __result = __prime * __result + (int)confidence;
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>RouteFusionExperimentResultTypeSupport</code>
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

        RouteFusionExperimentResult typedSrc = (RouteFusionExperimentResult) src;
        RouteFusionExperimentResult typedDst = this;

        typedDst.header = (intentdds.urban.Header) typedDst.header.copy_from(typedSrc.header);
        typedDst.qos_mode = typedSrc.qos_mode;
        typedDst.latency_profile = typedSrc.latency_profile;
        typedDst.conflict_profile = typedSrc.conflict_profile;
        typedDst.total_advice = typedSrc.total_advice;
        typedDst.delivered_advice = typedSrc.delivered_advice;
        typedDst.dropped_advice = typedSrc.dropped_advice;
        typedDst.retry_count = typedSrc.retry_count;
        typedDst.stale_count = typedSrc.stale_count;
        typedDst.average_latency_ms = typedSrc.average_latency_ms;
        typedDst.delivered_ratio = typedSrc.delivered_ratio;
        typedDst.route_stability = typedSrc.route_stability;
        typedDst.final_route = typedSrc.final_route;
        typedDst.final_risk_score = typedSrc.final_risk_score;
        typedDst.confidence = typedSrc.confidence;

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
        strBuffer.append("qos_mode: ").append(this.qos_mode).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("latency_profile: ").append(this.latency_profile).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("conflict_profile: ").append(this.conflict_profile).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("total_advice: ").append(this.total_advice).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("delivered_advice: ").append(this.delivered_advice).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("dropped_advice: ").append(this.dropped_advice).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("retry_count: ").append(this.retry_count).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("stale_count: ").append(this.stale_count).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("average_latency_ms: ").append(this.average_latency_ms).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("delivered_ratio: ").append(this.delivered_ratio).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("route_stability: ").append(this.route_stability).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("final_route: ").append(this.final_route).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("final_risk_score: ").append(this.final_risk_score).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("confidence: ").append(this.confidence).append("\n");  

        return strBuffer.toString();
    }

}
