/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.EffectiveRightsTargetSelector;
import com.zimbra.soap.admin.type.GranteeSelector;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_EFFECTIVE_RIGHTS_REQUEST)
public class GetEffectiveRightsRequest {

    public static final String EXPAND_SET_ATTRS = "setAttrs";
    public static final String EXPAND_GET_ATTRS = "getAttrs";

    private static Splitter COMMA_SPLITTER = Splitter.on(",");
    private static Joiner COMMA_JOINER = Joiner.on(",");

    @XmlTransient
    private Boolean expandSetAttrs;
    @XmlTransient
    private Boolean expandGetAttrs;

    @XmlElement(name=AdminConstants.E_TARGET, required=true)
    private final EffectiveRightsTargetSelector target;

    @XmlElement(name=AdminConstants.E_GRANTEE, required=false)
    private final GranteeSelector grantee;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetEffectiveRightsRequest() {
        this((EffectiveRightsTargetSelector) null, (GranteeSelector) null,
                (Boolean) null, (Boolean) null);
    }

    public GetEffectiveRightsRequest(EffectiveRightsTargetSelector target,
            GranteeSelector grantee,
            Boolean expandSetAttrs, Boolean expandGetAttrs) {
        this.target = target;
        this.grantee = grantee;
        setExpandSetAttrs(expandSetAttrs);
        setExpandGetAttrs(expandGetAttrs);
    }

    @XmlAttribute(name=AdminConstants.A_EXPAND_ALL_ATTRS, required=false)
    public String getExpandAllAttrs() {
        List <String> settings = Lists.newArrayList();
        if ((expandSetAttrs != null) && expandSetAttrs)
            settings.add(EXPAND_SET_ATTRS);
        if ((expandGetAttrs != null) && expandGetAttrs)
            settings.add(EXPAND_GET_ATTRS);
        String retVal = COMMA_JOINER.join(settings);
        if (retVal.length() == 0)
            return null;
        else
            return retVal;
    }

    public void setExpandAllAttrs(String types)
    throws ServiceException {
        expandGetAttrs = null;
        expandSetAttrs = null;
        for (String typeString : COMMA_SPLITTER.split(types)) {
            String exp = typeString.trim();
            if (exp.equals(EXPAND_SET_ATTRS))
                expandSetAttrs = true;
            else if (exp.equals(EXPAND_GET_ATTRS))
                expandGetAttrs = true;
            else
                throw ServiceException.INVALID_REQUEST(
                    "invalid " + AdminConstants.A_EXPAND_ALL_ATTRS +
                    " value: " + exp, null);
        }
    }

    public void setExpandGetAttrs(Boolean expandGetAttrs) {
        this.expandGetAttrs = expandGetAttrs;
    }

    public Boolean getExpandGetAttrs() { return expandGetAttrs; }

    public void setExpandSetAttrs(Boolean expandSetAttrs) {
        this.expandSetAttrs = expandSetAttrs;
    }

    public Boolean getExpandSetAttrs() { return expandSetAttrs; }

    public EffectiveRightsTargetSelector getTarget() { return target; }
    public GranteeSelector getGrantee() { return grantee; }
}
