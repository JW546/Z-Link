

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
public class RouteDecision   implements Copyable, Serializable{

    private static final long serialVersionUID = -1043119351L;

    public intentdds.urban.Header header = (intentdds.urban.Header)intentdds.urban.Header.create();
    public String decision_owner= (String)""; /* maximum length = (64) */
    public String autonomy_mode= (String)""; /* maximum length = (64) */
    public String final_route= (String)""; /* maximum length = (64) */
    public double final_risk_score = (double)0;
    public double confidence = (double)0;
    public StringSeq advice_sources =  new StringSeq(32);
    public String passenger_explanation= (String)""; /* maximum length = (256) */
    public String fusion_reason= (String)""; /* maximum length = (256) */

    public RouteDecision() {

    }
    public RouteDecision (RouteDecision other) {

        this();
        copy_from(other);
    }

    public static java.lang.Object create() {

        RouteDecision self;
        self = new  RouteDecision();
        self.clear();
        return self;

    }

    public void clear() {

        if (header != null) {
            header.clear();
        }
        decision_owner = (String)"";
        autonomy_mode = (String)"";
        final_route = (String)"";
        final_risk_score = (double)0;
        confidence = (double)0;
        if (advice_sources != null) {
            advice_sources.clear();
        }
        passenger_explanation = (String)"";
        fusion_reason = (String)"";
    }

    @Override
    public boolean equals(java.lang.Object o) {

        if (o == null) {
            return false;
        }        

        if(getClass() != o.getClass()) {
            return false;
        }

        RouteDecision otherObj = (RouteDecision)o;

        if(!this.header.equals(otherObj.header)) {
            return false;
        }
        if(!this.decision_owner.equals(otherObj.decision_owner)) {
            return false;
        }
        if(!this.autonomy_mode.equals(otherObj.autonomy_mode)) {
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
        if(!this.advice_sources.equals(otherObj.advice_sources)) {
            return false;
        }
        if(!this.passenger_explanation.equals(otherObj.passenger_explanation)) {
            return false;
        }
        if(!this.fusion_reason.equals(otherObj.fusion_reason)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + header.hashCode(); 
        __result = __prime * __result + decision_owner.hashCode(); 
        __result = __prime * __result + autonomy_mode.hashCode(); 
        __result = __prime * __result + final_route.hashCode(); 
        __result = __prime * __result + (int)final_risk_score;
        __result = __prime * __result + (int)confidence;
        __result = __prime * __result + advice_sources.hashCode(); 
        __result = __prime * __result + passenger_explanation.hashCode(); 
        __result = __prime * __result + fusion_reason.hashCode(); 
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>RouteDecisionTypeSupport</code>
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

        RouteDecision typedSrc = (RouteDecision) src;
        RouteDecision typedDst = this;

        typedDst.header = (intentdds.urban.Header) typedDst.header.copy_from(typedSrc.header);
        typedDst.decision_owner = typedSrc.decision_owner;
        typedDst.autonomy_mode = typedSrc.autonomy_mode;
        typedDst.final_route = typedSrc.final_route;
        typedDst.final_risk_score = typedSrc.final_risk_score;
        typedDst.confidence = typedSrc.confidence;
        typedDst.advice_sources.copy_from(typedSrc.advice_sources);
        typedDst.passenger_explanation = typedSrc.passenger_explanation;
        typedDst.fusion_reason = typedSrc.fusion_reason;

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
        strBuffer.append("decision_owner: ").append(this.decision_owner).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("autonomy_mode: ").append(this.autonomy_mode).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("final_route: ").append(this.final_route).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("final_risk_score: ").append(this.final_risk_score).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("confidence: ").append(this.confidence).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);
        strBuffer.append("advice_sources: ");
        for(int i__ = 0; i__ < this.advice_sources.size(); ++i__) {
            if (i__!=0) strBuffer.append(", ");
            strBuffer.append(this.advice_sources.get(i__));
        }
        strBuffer.append("\n"); 
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("passenger_explanation: ").append(this.passenger_explanation).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("fusion_reason: ").append(this.fusion_reason).append("\n");  

        return strBuffer.toString();
    }

}
