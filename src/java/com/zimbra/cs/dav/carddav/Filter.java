/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.carddav;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.resource.AddressObject;
import com.zimbra.cs.dav.resource.AddressbookCollection;
import com.zimbra.cs.index.ContactHit;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

/*
 * draft-daboo-carddav-02 section 10.7
 * 
 *          <!ELEMENT filter (prop-filter)>
 *          
 *          <!ELEMENT prop-filter (is-not-defined |
 *                                 (text-match?, param-filter*))>
 *          <!ATTLIST prop-filter name CDATA #REQUIRED>
 *          
 *          <!ELEMENT param-filter (is-not-defined | text-match)?>
 *          <!ATTLIST param-filter name CDATA #REQUIRED>
 *
 *          <!ELEMENT is-not-defined EMPTY>
 *
 *          <!ELEMENT text-match (#PCDATA)>
 *          PCDATA value: string
 *          <!ATTLIST text-match collation        CDATA "i;ascii-casemap"
 *                               negate-condition (yes | no) "no">
 */
public abstract class Filter {
    protected String mName;
    protected boolean mIsNotDefinedSet;
    protected HashSet<PropFilter> mProps;
    protected HashSet<ParamFilter> mParams;
    protected TextMatch mTextMatch;
    protected Filter mEnclosingFilter;

    public Filter(Element elem) {
        mProps = new HashSet<PropFilter>();
        mParams = new HashSet<ParamFilter>();
        mName = elem.attributeValue(DavElements.P_NAME);
        parse(elem);
    }

    public String getName() {
        return mName;
    }

    private enum MappingType { simple, structured };
    private static final class AttrMapping {
        public String name;
        public MappingType type;
        public String contactAttr;
        public HashMap<String,String[]> structures;
        public static AttrMapping createSimple(String n, String attr) {
            AttrMapping m = new AttrMapping();
            m.type = MappingType.simple;
            m.name = n;
            m.contactAttr = attr;
            sAttrMappings.put(n, m);
            return m;
        }
        public static AttrMapping createStructured(String n, String ... args) {
            AttrMapping m = new AttrMapping();
            m.type = MappingType.structured;
            m.name = n;
            m.structures = new HashMap<String,String[]>();
            int pos = 0;
            while (pos <= args.length - 2) {
                m.structures.put(args[pos], new String[] { args[pos+1] });
                pos += 2;
            }
            sAttrMappings.put(n, m);
            return m;
        }
        public static AttrMapping createStructured(String n) {
            AttrMapping m = new AttrMapping();
            m.type = MappingType.structured;
            m.name = n;
            m.structures = new HashMap<String,String[]>();
            sAttrMappings.put(n, m);
            return m;
        }
        public void addSubType(String k, String ... vals) {
            if (type == MappingType.simple)
                return;
            String[] v = new String[vals.length];
            int i = 0;
            for (String val : vals)
                v[i++] = val;
            structures.put(k, v);
        }
        public String[] getAttrs() {
            if (type == MappingType.simple)
                return new String[] { contactAttr };
            HashSet<String> ret = new HashSet<String>();
            for (String[] val : structures.values())
                Collections.addAll(ret, val);
            return ret.toArray(new String[0]);
        }
        public String[] getAttrs(String key) {
            if (type == MappingType.simple)
                return new String[] { contactAttr };
            return structures.get(key);
        }
    }
    
    private static final HashMap<String,AttrMapping> sAttrMappings;
    
    static {
        sAttrMappings = new HashMap<String,AttrMapping>();
        AttrMapping.createSimple("FN", Contact.A_fullName);
        AttrMapping.createSimple("NICKNAME", Contact.A_nickname);
        AttrMapping.createSimple("TITLE", Contact.A_jobTitle);
        AttrMapping.createSimple("NOTE", Contact.A_notes);
        AttrMapping.createStructured("ADR", "home", Contact.A_homeStreet, "work", Contact.A_workStreet);
        AttrMapping.createStructured("URL", "home", Contact.A_homeURL, "work", Contact.A_workURL);
        
        AttrMapping m = AttrMapping.createStructured("TEL", 
                "car", Contact.A_carPhone,
                "cell", Contact.A_mobilePhone,
                "pager", Contact.A_pager,
                "other", Contact.A_otherPhone
                );
        m.addSubType("work", Contact.A_workPhone, Contact.A_workPhone2);
        m.addSubType("home", Contact.A_homePhone, Contact.A_homePhone2);
        m.addSubType("fax", Contact.A_homeFax, Contact.A_workFax);
        
        m = AttrMapping.createStructured("EMAIL");
        m.addSubType("internet", Contact.A_email, Contact.A_email2, Contact.A_email3);
        
        m = AttrMapping.createStructured("ORG");
        m.addSubType("work", Contact.A_company, Contact.A_department);
    }
    
    public String getNameAsContactAttr() {
        return mName;
    }
    
    protected void parse(Element elem) {
        for (Object o : elem.elements()) {
            if (o instanceof Element) {
                Element e = (Element) o;
                QName name = e.getQName();
                if (canHavePropFilter() && name.equals(DavElements.CardDav.E_PROP_FILTER))
                    mProps.add(new PropFilter(e));
                else if (canHaveParamFilter() && name.equals(DavElements.CardDav.E_PARAM_FILTER))
                    mParams.add(new ParamFilter(e, this));
                else if (name.equals(DavElements.CardDav.E_TEXT_MATCH))
                    mTextMatch = new TextMatch(e, this);
                else if (name.equals(DavElements.CardDav.E_IS_NOT_DEFINED))
                    mIsNotDefinedSet = true;
                else
                    ZimbraLog.dav.info("unrecognized filter "+name.getNamespaceURI()+":"+name.getName());
            }
        }
    }

    public Collection<AddressObject> match(DavContext ctxt, AddressbookCollection folder) {
        return null;
    }
    
    public boolean mIsNotDefinedSet() {
        return mIsNotDefinedSet;
    }

    protected boolean runTextMatch(String text) {
        boolean matched = true;
        return matched;
    }
    
    protected boolean canHavePropFilter()  { return true; }
    protected boolean canHaveParamFilter() { return true; }
    
    public static class TextMatch extends Filter {
        //private String mCollation;
        private String mText;
        private boolean mNegate;
        
        public TextMatch(Element elem, Filter parent) {
            super(elem);
            mEnclosingFilter = parent;
            //mCollation = elem.attributeValue(DavElements.P_COLLATION, DavElements.ASCII);
            mNegate = elem.attributeValue(DavElements.P_NEGATE_CONDITION, DavElements.NO).equals(DavElements.YES);
            mText = elem.getText();
        }
        
        public Collection<AddressObject> match(DavContext ctxt, AddressbookCollection folder) {
            // search the folder for #key:val where key is mName and val is mTextMatch.mText.
            //boolean ignoreCase = mCollation.equals(DavElements.ASCII);
            ArrayList<AddressObject> result = new ArrayList<AddressObject>();
            StringBuilder search = new StringBuilder();
            if (mNegate)
                search.append("!");
            AttrMapping mapping = sAttrMappings.get(mEnclosingFilter.getName().toUpperCase());
            if (mapping == null)
                return result;
            String[] attrs = mapping.getAttrs();
            search.append("(");
            boolean first = true;
            for (String key : attrs) {
                if (!first)
                    search.append(" OR ");
                search.append("#").append(key).append(":").append(mText);
                first = false;
            }
            search.append(")");
            String filter = search.toString();
            ZimbraQueryResults zqr = null;
            try {
                Mailbox mbox = ctxt.getTargetMailbox();
                if (mbox == null) {
                    ZimbraLog.dav.debug("Can't get target mailbox for %s", ctxt.getUser());
                    return result;
                }
                zqr = mbox.search(ctxt.getOperationContext(), filter, new byte[] { MailItem.TYPE_CONTACT }, SortBy.NAME_ASCENDING, 100);
                while (zqr.hasNext()) {
                    ZimbraHit hit = zqr.getNext();
                    if (hit instanceof ContactHit) {
                        result.add(new AddressObject(ctxt, ((ContactHit) hit).getContact()));
                    }
                }
            } catch (Exception e) {
                ZimbraLog.dav.warn("can't get target mailbox", e);
                return result;
            } finally {
                if (zqr != null)
                    try {
                        zqr.doneWithSearchResults();
                    } catch (ServiceException e) {}
            }
            
            ZimbraLog.dav.debug("Search Filter: %s", filter);
            return result;
        }
    }
    public static class ParamFilter extends Filter {
        public ParamFilter(Element elem, Filter parent) {
            super(elem);
        }
        public boolean match(String prop) {
            return runTextMatch(prop);
        }
        protected boolean canHavePropFilter()  { return false; }
        protected boolean canHaveParamFilter() { return false; }
    }
    public static class PropFilter extends Filter {
        public PropFilter(Element elem) {
            super(elem);
        }
        public boolean match(Contact contact) {
            String val = contact.get(mName);
            if (val == null)
                return mIsNotDefinedSet;
            boolean matched = true;
            for (ParamFilter pf : mParams)
                matched &= pf.match(val);
            matched &= runTextMatch(val);
            return matched;
        }
        public Collection<AddressObject> match(DavContext ctxt, AddressbookCollection folder) {
            if (mIsNotDefinedSet) {
                // go through all the contacts and return the ones that do not
                // contain the field specified in mName.
            } else if (mTextMatch != null) {
                return mTextMatch.match(ctxt, folder);
            } else if (mParams != null) {
                ArrayList<AddressObject> result = new ArrayList<AddressObject>();
                for (ParamFilter f : mParams)
                    result.addAll(f.match(ctxt, folder));
                return result;
            }
            return null;
        }
        protected boolean canHavePropFilter()  { return false; }
    }
}
