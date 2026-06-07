

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
public class ProtocolConversionLog   implements Copyable, Serializable{

    private static final long serialVersionUID = 1843771327L;

    public int log_id = (int)0;
    public String source_protocol= (String)""; /* maximum length = (32) */
    public String target_protocol= (String)""; /* maximum length = (32) */
    public String original_data= (String)""; /* maximum length = (256) */
    public String converted_data= (String)""; /* maximum length = (256) */
    public int conversion_time_ms = (int)0;
    public boolean success = (boolean)false;
    public int timestamp = (int)0;

    public ProtocolConversionLog() {

    }
    public ProtocolConversionLog (ProtocolConversionLog other) {

        this();
        copy_from(other);
    }

    public static java.lang.Object create() {

        ProtocolConversionLog self;
        self = new  ProtocolConversionLog();
        self.clear();
        return self;

    }

    public void clear() {

        log_id = (int)0;
        source_protocol = (String)"";
        target_protocol = (String)"";
        original_data = (String)"";
        converted_data = (String)"";
        conversion_time_ms = (int)0;
        success = (boolean)false;
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

        ProtocolConversionLog otherObj = (ProtocolConversionLog)o;

        if(this.log_id != otherObj.log_id) {
            return false;
        }
        if(!this.source_protocol.equals(otherObj.source_protocol)) {
            return false;
        }
        if(!this.target_protocol.equals(otherObj.target_protocol)) {
            return false;
        }
        if(!this.original_data.equals(otherObj.original_data)) {
            return false;
        }
        if(!this.converted_data.equals(otherObj.converted_data)) {
            return false;
        }
        if(this.conversion_time_ms != otherObj.conversion_time_ms) {
            return false;
        }
        if(this.success != otherObj.success) {
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
        __result = __prime * __result + (int)log_id;
        __result = __prime * __result + source_protocol.hashCode(); 
        __result = __prime * __result + target_protocol.hashCode(); 
        __result = __prime * __result + original_data.hashCode(); 
        __result = __prime * __result + converted_data.hashCode(); 
        __result = __prime * __result + (int)conversion_time_ms;
        __result = __prime * __result + ((success == true)?1:0);
        __result = __prime * __result + (int)timestamp;
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>ProtocolConversionLogTypeSupport</code>
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

        ProtocolConversionLog typedSrc = (ProtocolConversionLog) src;
        ProtocolConversionLog typedDst = this;

        typedDst.log_id = typedSrc.log_id;
        typedDst.source_protocol = typedSrc.source_protocol;
        typedDst.target_protocol = typedSrc.target_protocol;
        typedDst.original_data = typedSrc.original_data;
        typedDst.converted_data = typedSrc.converted_data;
        typedDst.conversion_time_ms = typedSrc.conversion_time_ms;
        typedDst.success = typedSrc.success;
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
        strBuffer.append("log_id: ").append(this.log_id).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("source_protocol: ").append(this.source_protocol).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("target_protocol: ").append(this.target_protocol).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("original_data: ").append(this.original_data).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("converted_data: ").append(this.converted_data).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("conversion_time_ms: ").append(this.conversion_time_ms).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("success: ").append(this.success).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("timestamp: ").append(this.timestamp).append("\n");  

        return strBuffer.toString();
    }

}
