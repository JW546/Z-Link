

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
public class MissionCommand   implements Copyable, Serializable{

    private static final long serialVersionUID = -923818724L;

    public intentdds.urban.Header header = (intentdds.urban.Header)intentdds.urban.Header.create();
    public String natural_language= (String)""; /* maximum length = (256) */
    public String destination= (String)""; /* maximum length = (256) */
    public String urgency= (String)""; /* maximum length = (64) */
    public String preference= (String)""; /* maximum length = (64) */
    public StringSeq requested_capabilities =  new StringSeq(32);
    public StringSeq publish_topics =  new StringSeq(32);
    public StringSeq subscribe_topics =  new StringSeq(32);
    public String dsl_contract= (String)""; /* maximum length = (2048) */

    public MissionCommand() {

    }
    public MissionCommand (MissionCommand other) {

        this();
        copy_from(other);
    }

    public static java.lang.Object create() {

        MissionCommand self;
        self = new  MissionCommand();
        self.clear();
        return self;

    }

    public void clear() {

        if (header != null) {
            header.clear();
        }
        natural_language = (String)"";
        destination = (String)"";
        urgency = (String)"";
        preference = (String)"";
        if (requested_capabilities != null) {
            requested_capabilities.clear();
        }
        if (publish_topics != null) {
            publish_topics.clear();
        }
        if (subscribe_topics != null) {
            subscribe_topics.clear();
        }
        dsl_contract = (String)"";
    }

    @Override
    public boolean equals(java.lang.Object o) {

        if (o == null) {
            return false;
        }        

        if(getClass() != o.getClass()) {
            return false;
        }

        MissionCommand otherObj = (MissionCommand)o;

        if(!this.header.equals(otherObj.header)) {
            return false;
        }
        if(!this.natural_language.equals(otherObj.natural_language)) {
            return false;
        }
        if(!this.destination.equals(otherObj.destination)) {
            return false;
        }
        if(!this.urgency.equals(otherObj.urgency)) {
            return false;
        }
        if(!this.preference.equals(otherObj.preference)) {
            return false;
        }
        if(!this.requested_capabilities.equals(otherObj.requested_capabilities)) {
            return false;
        }
        if(!this.publish_topics.equals(otherObj.publish_topics)) {
            return false;
        }
        if(!this.subscribe_topics.equals(otherObj.subscribe_topics)) {
            return false;
        }
        if(!this.dsl_contract.equals(otherObj.dsl_contract)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + header.hashCode(); 
        __result = __prime * __result + natural_language.hashCode(); 
        __result = __prime * __result + destination.hashCode(); 
        __result = __prime * __result + urgency.hashCode(); 
        __result = __prime * __result + preference.hashCode(); 
        __result = __prime * __result + requested_capabilities.hashCode(); 
        __result = __prime * __result + publish_topics.hashCode(); 
        __result = __prime * __result + subscribe_topics.hashCode(); 
        __result = __prime * __result + dsl_contract.hashCode(); 
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>MissionCommandTypeSupport</code>
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

        MissionCommand typedSrc = (MissionCommand) src;
        MissionCommand typedDst = this;

        typedDst.header = (intentdds.urban.Header) typedDst.header.copy_from(typedSrc.header);
        typedDst.natural_language = typedSrc.natural_language;
        typedDst.destination = typedSrc.destination;
        typedDst.urgency = typedSrc.urgency;
        typedDst.preference = typedSrc.preference;
        typedDst.requested_capabilities.copy_from(typedSrc.requested_capabilities);
        typedDst.publish_topics.copy_from(typedSrc.publish_topics);
        typedDst.subscribe_topics.copy_from(typedSrc.subscribe_topics);
        typedDst.dsl_contract = typedSrc.dsl_contract;

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
        strBuffer.append("natural_language: ").append(this.natural_language).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("destination: ").append(this.destination).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("urgency: ").append(this.urgency).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("preference: ").append(this.preference).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);
        strBuffer.append("requested_capabilities: ");
        for(int i__ = 0; i__ < this.requested_capabilities.size(); ++i__) {
            if (i__!=0) strBuffer.append(", ");
            strBuffer.append(this.requested_capabilities.get(i__));
        }
        strBuffer.append("\n"); 
        CdrHelper.printIndent(strBuffer, indent+1);
        strBuffer.append("publish_topics: ");
        for(int i__ = 0; i__ < this.publish_topics.size(); ++i__) {
            if (i__!=0) strBuffer.append(", ");
            strBuffer.append(this.publish_topics.get(i__));
        }
        strBuffer.append("\n"); 
        CdrHelper.printIndent(strBuffer, indent+1);
        strBuffer.append("subscribe_topics: ");
        for(int i__ = 0; i__ < this.subscribe_topics.size(); ++i__) {
            if (i__!=0) strBuffer.append(", ");
            strBuffer.append(this.subscribe_topics.get(i__));
        }
        strBuffer.append("\n"); 
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("dsl_contract: ").append(this.dsl_contract).append("\n");  

        return strBuffer.toString();
    }

}
