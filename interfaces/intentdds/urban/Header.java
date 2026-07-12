

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
public class Header   implements Copyable, Serializable{

    private static final long serialVersionUID = 79644892L;

    public String message_id= (String)""; /* maximum length = (64) */
    public String source_id= (String)""; /* maximum length = (64) */
    public String target_id= (String)""; /* maximum length = (64) */
    public String timestamp_iso8601= (String)""; /* maximum length = (64) */
    public String frame_id= (String)""; /* maximum length = (64) */
    public int sample_sequence = (int)0;

    public Header() {

    }
    public Header (Header other) {

        this();
        copy_from(other);
    }

    public static java.lang.Object create() {

        Header self;
        self = new  Header();
        self.clear();
        return self;

    }

    public void clear() {

        message_id = (String)"";
        source_id = (String)"";
        target_id = (String)"";
        timestamp_iso8601 = (String)"";
        frame_id = (String)"";
        sample_sequence = (int)0;
    }

    @Override
    public boolean equals(java.lang.Object o) {

        if (o == null) {
            return false;
        }        

        if(getClass() != o.getClass()) {
            return false;
        }

        Header otherObj = (Header)o;

        if(!this.message_id.equals(otherObj.message_id)) {
            return false;
        }
        if(!this.source_id.equals(otherObj.source_id)) {
            return false;
        }
        if(!this.target_id.equals(otherObj.target_id)) {
            return false;
        }
        if(!this.timestamp_iso8601.equals(otherObj.timestamp_iso8601)) {
            return false;
        }
        if(!this.frame_id.equals(otherObj.frame_id)) {
            return false;
        }
        if(this.sample_sequence != otherObj.sample_sequence) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + message_id.hashCode(); 
        __result = __prime * __result + source_id.hashCode(); 
        __result = __prime * __result + target_id.hashCode(); 
        __result = __prime * __result + timestamp_iso8601.hashCode(); 
        __result = __prime * __result + frame_id.hashCode(); 
        __result = __prime * __result + (int)sample_sequence;
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>HeaderTypeSupport</code>
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

        Header typedSrc = (Header) src;
        Header typedDst = this;

        typedDst.message_id = typedSrc.message_id;
        typedDst.source_id = typedSrc.source_id;
        typedDst.target_id = typedSrc.target_id;
        typedDst.timestamp_iso8601 = typedSrc.timestamp_iso8601;
        typedDst.frame_id = typedSrc.frame_id;
        typedDst.sample_sequence = typedSrc.sample_sequence;

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
        strBuffer.append("message_id: ").append(this.message_id).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("source_id: ").append(this.source_id).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("target_id: ").append(this.target_id).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("timestamp_iso8601: ").append(this.timestamp_iso8601).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("frame_id: ").append(this.frame_id).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("sample_sequence: ").append(this.sample_sequence).append("\n");  

        return strBuffer.toString();
    }

}
