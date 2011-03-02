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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class CompletedTestInfo {

    @XmlAttribute(name="name", required=true)
    private final String name;

    @XmlAttribute(name="execSeconds", required=true)
    private final String execSeconds;

    @XmlAttribute(name="class", required=true)
    private final String className;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CompletedTestInfo() {
        this((String) null, (String) null, (String) null);
    }

    public CompletedTestInfo(String name,
                    String execSeconds, String className) {
        this.name = name;
        this.execSeconds = execSeconds;
        this.className = className;
    }

    public String getName() { return name; }
    public String getExecSeconds() { return execSeconds; }
    public String getClassName() { return className; }
}
