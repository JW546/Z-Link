

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
public class RouteAdvice   implements Copyable, Serializable{

    private static final long serialVersionUID = 83272635L;

    public intentdds.urban.Header header = (intentdds.urban.Header)intentdds.urban.Header.create();
    public String source_type= (String)""; /* maximum length = (64) */
    public String destination= (String)""; /* maximum length = (256) */
    public String route_candidate= (String)""; /* maximum length = (64) */
    public double risk_score = (double)0;
    public double confidence = (double)0;
    public double estimated_time_min = (double)0;
    public String advice_reason= (String)""; /* maximum length = (256) */
    public StringSeq required_topics =  new StringSeq(32);

    public RouteAdvice() {

    }
    public RouteAdvice (RouteAdvice other) {

        this();
        copy_from(other);
    }

    public static java.lang.Object create() {

        RouteAdvice self;
        self = new  RouteAdvice();
        self.clear();
        return self;

    }

    public void clear() {

        if (header != null) {
            header.clear();
        }
        source_type = (String)"";
        destination = (String)"";
        route_candidate = (String)"";
        risk_score = (double)0;
        confidence = (double)0;
        estimated_time_min = (double)0;
        advice_reason = (String)"";
        if (required_topics != null) {
            required_topics.clear();
        }
    }

    @Override
    public boolean equals(java.lang.Object o) {

        if (o == null) {
            return false;
        }        

        if(getClass() != o.getClass()) {
            return false;
        }

        RouteAdvice otherObj = (RouteAdvice)o;

        if(!this.header.equals(otherObj.header)) {
            return false;
        }
        if(!this.source_type.equals(otherObj.source_type)) {
            return false;
        }
        if(!this.destination.equals(otherObj.destination)) {
            return false;
        }
        if(!this.route_candidate.equals(otherObj.route_candidate)) {
            return false;
        }
        if(this.risk_score != otherObj.risk_score) {
            return false;
        }
        if(this.confidence != otherObj.confidence) {
            return false;
        }
        if(this.estimated_time_min != otherObj.estimated_time_min) {
            return false;
        }
        if(!this.advice_reason.equals(otherObj.advice_reason)) {
            return false;
        }
        if(!this.required_topics.equals(otherObj.required_topics)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + header.hashCode(); 
        __result = __prime * __result + source_type.hashCode(); 
        __result = __prime * __result + destination.hashCode(); 
        __result = __prime * __result + route_candidate.hashCode(); 
        __result = __prime * __result + (int)risk_score;
        __result = __prime * __result + (int)confidence;
        __result = __prime * __result + (int)estimated_time_min;
        __result = __prime * __result + advice_reason.hashCode(); 
        __result = __prime * __result + required_topics.hashCode(); 
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>RouteAdviceTypeSupport</code>
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

        RouteAdvice typedSrc = (RouteAdvice) src;
        RouteAdvice typedDst = this;

        typedDst.header = (intentdds.urban.Header) typedDst.header.copy_from(typedSrc.header);
        typedDst.source_type = typedSrc.source_type;
        typedDst.destination = typedSrc.destination;
        typedDst.route_candidate = typedSrc.route_candidate;
        typedDst.risk_score = typedSrc.risk_score;
        typedDst.confidence = typedSrc.confidence;
        typedDst.estimated_time_min = typedSrc.estimated_time_min;
        typedDst.advice_reason = typedSrc.advice_reason;
        typedDst.required_topics.copy_from(typedSrc.required_topics);

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
        strBuffer.append("source_type: ").append(this.source_type).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("destination: ").append(this.destination).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("route_candidate: ").append(this.route_candidate).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("risk_score: ").append(this.risk_score).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("confidence: ").append(this.confidence).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("estimated_time_min: ").append(this.estimated_time_min).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("advice_reason: ").append(this.advice_reason).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);
        strBuffer.append("required_topics: ");
        for(int i__ = 0; i__ < this.required_topics.size(); ++i__) {
            if (i__!=0) strBuffer.append(", ");
            strBuffer.append(this.required_topics.get(i__));
        }
        strBuffer.append("\n"); 

        return strBuffer.toString();
    }

}
