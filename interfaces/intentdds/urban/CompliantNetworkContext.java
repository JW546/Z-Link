

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
public class CompliantNetworkContext   implements Copyable, Serializable{

    private static final long serialVersionUID = -118265009L;

    public intentdds.urban.Header header = (intentdds.urban.Header)intentdds.urban.Header.create();
    public String link_mode= (String)""; /* maximum length = (64) */
    public double link_health = (double)0;
    public double private_5g_signal = (double)0;
    public double rsu_load = (double)0;
    public double expected_latency_ms = (double)0;
    public String recommended_route= (String)""; /* maximum length = (64) */
    public double risk_score = (double)0;
    public double confidence = (double)0;

    public CompliantNetworkContext() {

    }
    public CompliantNetworkContext (CompliantNetworkContext other) {

        this();
        copy_from(other);
    }

    public static java.lang.Object create() {

        CompliantNetworkContext self;
        self = new  CompliantNetworkContext();
        self.clear();
        return self;

    }

    public void clear() {

        if (header != null) {
            header.clear();
        }
        link_mode = (String)"";
        link_health = (double)0;
        private_5g_signal = (double)0;
        rsu_load = (double)0;
        expected_latency_ms = (double)0;
        recommended_route = (String)"";
        risk_score = (double)0;
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

        CompliantNetworkContext otherObj = (CompliantNetworkContext)o;

        if(!this.header.equals(otherObj.header)) {
            return false;
        }
        if(!this.link_mode.equals(otherObj.link_mode)) {
            return false;
        }
        if(this.link_health != otherObj.link_health) {
            return false;
        }
        if(this.private_5g_signal != otherObj.private_5g_signal) {
            return false;
        }
        if(this.rsu_load != otherObj.rsu_load) {
            return false;
        }
        if(this.expected_latency_ms != otherObj.expected_latency_ms) {
            return false;
        }
        if(!this.recommended_route.equals(otherObj.recommended_route)) {
            return false;
        }
        if(this.risk_score != otherObj.risk_score) {
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
        __result = __prime * __result + link_mode.hashCode(); 
        __result = __prime * __result + (int)link_health;
        __result = __prime * __result + (int)private_5g_signal;
        __result = __prime * __result + (int)rsu_load;
        __result = __prime * __result + (int)expected_latency_ms;
        __result = __prime * __result + recommended_route.hashCode(); 
        __result = __prime * __result + (int)risk_score;
        __result = __prime * __result + (int)confidence;
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>CompliantNetworkContextTypeSupport</code>
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

        CompliantNetworkContext typedSrc = (CompliantNetworkContext) src;
        CompliantNetworkContext typedDst = this;

        typedDst.header = (intentdds.urban.Header) typedDst.header.copy_from(typedSrc.header);
        typedDst.link_mode = typedSrc.link_mode;
        typedDst.link_health = typedSrc.link_health;
        typedDst.private_5g_signal = typedSrc.private_5g_signal;
        typedDst.rsu_load = typedSrc.rsu_load;
        typedDst.expected_latency_ms = typedSrc.expected_latency_ms;
        typedDst.recommended_route = typedSrc.recommended_route;
        typedDst.risk_score = typedSrc.risk_score;
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
        strBuffer.append("link_mode: ").append(this.link_mode).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("link_health: ").append(this.link_health).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("private_5g_signal: ").append(this.private_5g_signal).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("rsu_load: ").append(this.rsu_load).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("expected_latency_ms: ").append(this.expected_latency_ms).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("recommended_route: ").append(this.recommended_route).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("risk_score: ").append(this.risk_score).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("confidence: ").append(this.confidence).append("\n");  

        return strBuffer.toString();
    }

}
