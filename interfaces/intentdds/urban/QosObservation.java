

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
public class QosObservation   implements Copyable, Serializable{

    private static final long serialVersionUID = 51417907L;

    public intentdds.urban.Header header = (intentdds.urban.Header)intentdds.urban.Header.create();
    public String qos_profile= (String)""; /* maximum length = (64) */
    public int published_count = (int)0;
    public int delivered_count = (int)0;
    public int dropped_count = (int)0;
    public int retry_count = (int)0;
    public int stale_count = (int)0;
    public double average_latency_ms = (double)0;
    public double delivered_ratio = (double)0;

    public QosObservation() {

    }
    public QosObservation (QosObservation other) {

        this();
        copy_from(other);
    }

    public static java.lang.Object create() {

        QosObservation self;
        self = new  QosObservation();
        self.clear();
        return self;

    }

    public void clear() {

        if (header != null) {
            header.clear();
        }
        qos_profile = (String)"";
        published_count = (int)0;
        delivered_count = (int)0;
        dropped_count = (int)0;
        retry_count = (int)0;
        stale_count = (int)0;
        average_latency_ms = (double)0;
        delivered_ratio = (double)0;
    }

    @Override
    public boolean equals(java.lang.Object o) {

        if (o == null) {
            return false;
        }        

        if(getClass() != o.getClass()) {
            return false;
        }

        QosObservation otherObj = (QosObservation)o;

        if(!this.header.equals(otherObj.header)) {
            return false;
        }
        if(!this.qos_profile.equals(otherObj.qos_profile)) {
            return false;
        }
        if(this.published_count != otherObj.published_count) {
            return false;
        }
        if(this.delivered_count != otherObj.delivered_count) {
            return false;
        }
        if(this.dropped_count != otherObj.dropped_count) {
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

        return true;
    }

    @Override
    public int hashCode() {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + header.hashCode(); 
        __result = __prime * __result + qos_profile.hashCode(); 
        __result = __prime * __result + (int)published_count;
        __result = __prime * __result + (int)delivered_count;
        __result = __prime * __result + (int)dropped_count;
        __result = __prime * __result + (int)retry_count;
        __result = __prime * __result + (int)stale_count;
        __result = __prime * __result + (int)average_latency_ms;
        __result = __prime * __result + (int)delivered_ratio;
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>QosObservationTypeSupport</code>
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

        QosObservation typedSrc = (QosObservation) src;
        QosObservation typedDst = this;

        typedDst.header = (intentdds.urban.Header) typedDst.header.copy_from(typedSrc.header);
        typedDst.qos_profile = typedSrc.qos_profile;
        typedDst.published_count = typedSrc.published_count;
        typedDst.delivered_count = typedSrc.delivered_count;
        typedDst.dropped_count = typedSrc.dropped_count;
        typedDst.retry_count = typedSrc.retry_count;
        typedDst.stale_count = typedSrc.stale_count;
        typedDst.average_latency_ms = typedSrc.average_latency_ms;
        typedDst.delivered_ratio = typedSrc.delivered_ratio;

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
        strBuffer.append("qos_profile: ").append(this.qos_profile).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("published_count: ").append(this.published_count).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("delivered_count: ").append(this.delivered_count).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("dropped_count: ").append(this.dropped_count).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("retry_count: ").append(this.retry_count).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("stale_count: ").append(this.stale_count).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("average_latency_ms: ").append(this.average_latency_ms).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("delivered_ratio: ").append(this.delivered_ratio).append("\n");  

        return strBuffer.toString();
    }

}
