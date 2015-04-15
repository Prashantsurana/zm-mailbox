/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeansException;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.datasource.DataSourceListener;
import com.zimbra.cs.fb.FreeBusyProvider;
import com.zimbra.cs.filter.FilterListener;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.acl.AclPushListener;
import com.zimbra.cs.mailbox.acl.ShareExpirationListener;
import com.zimbra.cs.mailbox.alerts.CalItemReminderService;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraApplication;
import com.zimbra.cs.util.ZimbraConfig;


public abstract class MailboxListener {

    public static class ChangeNotification {
        public Account mailboxAccount;
        public OperationContext ctxt;
        public int lastChangeId;
        public PendingModifications mods;
        public MailboxOperation op;
        public long timestamp;

        public ChangeNotification(Account account, PendingModifications mods, OperationContext ctxt, int lastChangeId,
                MailboxOperation op, long timestamp) {
            this.mailboxAccount = account;
            this.mods = mods;
            this.ctxt = ctxt;
            this.lastChangeId = lastChangeId;
            this.op = op;
            this.timestamp = timestamp;
        }
    }

    /**
     * Listeners will be notified at the end of each <code>Mailbox</code>
     * transaction.  The listener must not throw any Exception in this method.
     * The listener must refrain from making synchronous network operation
     * or other long latency operation within notify method.
     *
     * @param notification
     * @throws ServiceException
     * @throws BeansException
     */
    public abstract void notify(ChangeNotification notification) throws BeansException, ServiceException;

    protected static final Set<Type> ALL_ITEM_TYPES = EnumSet.allOf(Type.class);

    /**
     * Listener can indicate specific item types it is interested in,
     * which will reduce the number of notification callbacks.
     *
     * @return Set of item types listener wants to be notified of
     */
    public Set<MailItem.Type> registerForItemTypes() {
        return ALL_ITEM_TYPES;
    }

    private static final HashSet<MailboxListener> sListeners;

    static {
        sListeners = new HashSet<MailboxListener>();
        reset();
    }

    static void reset() {
        sListeners.clear();
        ZimbraApplication application = ZimbraApplication.getInstance();
        if (application.supports(CalItemReminderService.class) && !DebugConfig.disableCalendarReminderEmail) {
            register(new CalItemReminderService());
        }
        register(new FilterListener());
        register(new CacheManager());
        register(new FreeBusyProvider.Listener());
        register(new DataSourceListener());
        register(new ShareStartStopListener());
        if (application.supports(AclPushListener.class)) {
            register(new AclPushListener());
        }
        if (application.supports(ShareExpirationListener.class) && !DebugConfig.isDisableShareExpirationListener()) {
            register(new ShareExpirationListener());
        }

        // Register external listeners configured by the zimbraMailboxListenerUrl attribute
        try {
            ZimbraConfig config = Zimbra.getAppContext().getBean(ZimbraConfig.class);
            List<MailboxListenerTransport> managers = config.externalMailboxListeners();
            managers = managers == null ? Collections.emptyList() : managers;
            for (MailboxListenerTransport manager: managers) {
                MailboxListener listener = new MailboxListener() {
                    @Override
                    public void notify(ChangeNotification notification) {
                        try {
                            manager.publish(notification);
                        } catch (ServiceException e) {
                            ZimbraLog.session.warn("Failed publishing ChangeNotification to external mailbox listener", e);
                        }
                    }
                };
                register(listener);
            }
        } catch (Exception e) {
            ZimbraLog.session.warn("Failed reading external mailbox listener configuration", e);
        }
    }

    public static void register(MailboxListener listener) {
        synchronized (sListeners) {
            sListeners.add(listener);
        }
    }

    @VisibleForTesting
    static void unregister(MailboxListener listener) {
        synchronized (sListeners) {
            sListeners.remove(listener);
        }
    }

    public static void notifyListeners(ChangeNotification notification) throws BeansException, ServiceException {
        for (MailboxListener l : sListeners) {
            if (!Collections.disjoint(notification.mods.changedTypes, l.registerForItemTypes())) {
                l.notify(notification);
            }
        }
    }
}
