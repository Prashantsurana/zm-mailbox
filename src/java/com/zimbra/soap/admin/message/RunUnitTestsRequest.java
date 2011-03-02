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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.StringValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_RUN_UNIT_TESTS_REQUEST)
public class RunUnitTestsRequest {

    @XmlElement(name=AdminConstants.E_TEST, required=false)
    private List<StringValue> tests = Lists.newArrayList();

    public RunUnitTestsRequest() {
    }

    public void setTests(Iterable <StringValue> tests) {
        this.tests.clear();
        if (tests != null) {
            Iterables.addAll(this.tests,tests);
        }
    }

    public RunUnitTestsRequest addTest(StringValue test) {
        this.tests.add(test);
        return this;
    }

    public List<StringValue> getTests() {
        return Collections.unmodifiableList(tests);
    }
}
