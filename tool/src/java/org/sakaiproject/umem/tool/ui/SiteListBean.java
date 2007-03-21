/**********************************************************************************
 * $URL: https://source.sakaiproject.org/contrib/ufp/usermembership/trunk/tool/src/java/org/sakaiproject/umem/tool/ui/SiteListBean.java $
 * $Id: SiteListBean.java 4381 2007-03-21 11:25:54Z nuno@ufp.pt $
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 The Sakai Foundation.
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.umem.tool.ui;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.SiteService.SelectionType;
import org.sakaiproject.site.api.SiteService.SortType;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.umem.api.Authz;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.util.ResourceLoader;



/**
 * @author <a href="mailto:nuno@ufp.pt">Nuno Fernandes</a>
 */
public class SiteListBean {
	private static final long			serialVersionUID	= 2L;
	private static final String			SORT_SITE_NAME		= "siteName";
	private static final String			SORT_GROUPS_TYPE	= "groups";
	private static final String			SORT_SITE_TYPE		= "siteType";
	private static final String			SORT_SITE_RID		= "roleId";
	private static final String			SORT_SITE_PV		= "published";
	private static final String			SORT_USER_STATUS	= "userStatus";
	private static final String			SORT_SITE_TERM		= "siteTerm";
	/** Our log (commons). */
	private static Log					LOG					= LogFactory.getLog(SiteListBean.class);
	/** Resource bundle */
	private transient ResourceLoader	msgs				= new ResourceLoader("org.sakaiproject.umem.tool.bundle.Messages");
	/** Controller fields */
	private List						userSitesRows;
	/** Getter vars */
	private boolean						refreshQuery		= false;
	private boolean						allowed				= false;
	private String						thisUserId			= null;
	private String						userId				= null;
	private boolean						sitesSortAscending	= true;
	private String						sitesSortColumn		= SORT_SITE_NAME;
	/** Resource properties */
	private final static String 		PROP_SITE_TERM 		= "term";
	/** Sakai APIs */
	private SessionManager				M_session			= (SessionManager) ComponentManager.get(SessionManager.class.getName());
	private SqlService					M_sql				= (SqlService) ComponentManager.get(SqlService.class.getName());
	private SiteService					M_site				= (SiteService) ComponentManager.get(SiteService.class.getName());
	private ToolManager					M_tm				= (ToolManager) ComponentManager.get(ToolManager.class.getName());
	private Authz						authz				= (Authz) ComponentManager.get(Authz.class.getName());
	/** Private vars */
	private Collator					collator			= Collator.getInstance();
	private long						timeSpentInGroups	= 0;
	private String						portalURL			= ServerConfigurationService.getPortalUrl();
	private String						message				= "";

	// ######################################################################################
	// UserSitesRow CLASS
	// ######################################################################################

	public class UserSitesRow implements Serializable {
		private static final long	serialVersionUID	= 1L;
		private Site				site;
		private String				siteId;
		private String				siteTitle;
		private String				siteType;
		private String				siteURL;
		private String				groups;
		private String				roleName;
		private String				pubView;
		private String				userStatus;
		private String				siteTerm;

		public UserSitesRow() {
		}

		public UserSitesRow(String siteId, String siteTitle, String siteType, String groups, String roleName, String pubView, String userStatus, String term) {
			this.siteId = siteId;
			this.siteTitle = siteTitle;
			this.siteType = siteType;
			this.groups = groups;
			this.roleName = roleName;
			this.pubView = pubView;
			this.userStatus = userStatus;
		}

		public UserSitesRow(Site site, String groups, String roleName) {
			this.siteId = site.getId();
			this.siteTitle = site.getTitle();
			this.siteType = site.getType();
			this.groups = groups;
			this.roleName = roleName;
			this.pubView = site.isPublished() ? msgs.getString("status_published") : msgs.getString("status_unpublished");
			this.userStatus = site.getMember(userId).isActive() ? msgs.getString("site_user_status_active") : msgs.getString("site_user_status_inactive");
			this.siteTerm = site.getProperties().getProperty(PROP_SITE_TERM);
		}

		public String getSiteId() {
			return siteId;
		}

		public String getSiteTitle() {
			return siteTitle;
		}

		public String getSiteType() {
			return siteType;
		}

		public String getSiteURL() {
			try{
				// return M_ss.getSite(siteId).getUrl();
				if(site != null) return site.getUrl();
				else return portalURL + "/site/" + siteId;
			}catch(Exception e){
				LOG.warn("Unable to generate URL for site id: " + siteId);
				return "";
			}
		}

		public String getGroups() {
			return groups;
		}

		public String getRoleName() {
			return roleName;
		}

		public String getPubView() {
			return pubView;
		}
		
		public String getUserStatus(){
			return this.userStatus;
		}

		public String getSiteTerm() {
			return siteTerm;
		}
		
	}

	public static final Comparator getUserSitesRowComparator(final String fieldName, final boolean sortAscending, final Collator collator) {
		return new Comparator() {
			public int compare(Object o1, Object o2) {
				if(o1 instanceof UserSitesRow && o2 instanceof UserSitesRow){
					UserSitesRow r1 = (UserSitesRow) o1;
					UserSitesRow r2 = (UserSitesRow) o2;
					try{
						if(fieldName.equals(SORT_SITE_NAME)){
							String s1 = r1.getSiteTitle();
							String s2 = r2.getSiteTitle();
							int res = collator.compare(s1!=null? s1.toLowerCase():"", s2!=null? s2.toLowerCase():"");
							if(sortAscending) return res;
							else return -res;
						}else if(fieldName.equals(SORT_SITE_TYPE)){
							String s1 = r1.getSiteType();
							String s2 = r2.getSiteType();
							int res = collator.compare(s1!=null? s1.toLowerCase():"", s2!=null? s2.toLowerCase():"");
							if(sortAscending) return res;
							else return -res;
						}else if(fieldName.equals(SORT_SITE_RID)){
							String s1 = r1.getRoleName();
							String s2 = r2.getRoleName();
							int res = collator.compare(s1!=null? s1.toLowerCase():"", s2!=null? s2.toLowerCase():"");
							if(sortAscending) return res;
							else return -res;
						}else if(fieldName.equals(SORT_SITE_PV)){
							String s1 = r1.getPubView();
							String s2 = r2.getPubView();
							int res = collator.compare(s1!=null? s1.toLowerCase():"", s2!=null? s2.toLowerCase():"");
							if(sortAscending) return res;
							else return -res;
						}else if(fieldName.equals(SORT_USER_STATUS)){
							String s1 = r1.getUserStatus();
							String s2 = r2.getUserStatus();
							int res = collator.compare(s1!=null? s1.toLowerCase():"", s2!=null? s2.toLowerCase():"");
							if(sortAscending) return res;
							else return -res;
						}else if(fieldName.equals(SORT_SITE_TERM)){
							String s1 = r1.getSiteTerm();
							String s2 = r2.getSiteTerm();
							int res = collator.compare(s1!=null? s1.toLowerCase():"", s2!=null? s2.toLowerCase():"");
							if(sortAscending) return res;
							else return -res;
						}
					}catch(Exception e){
						LOG.warn("Error occurred while sorting by: "+fieldName, e);
					}
				}
				return 0;
			}
		};
	}

	// ######################################################################################
	// Main methods
	// ######################################################################################
	
	/**
	 * Uses complex SQL for site membership, user role and group membership.<br>
	 * For a 12 site users it takes < 1 secs!
	 */
	private void doSearch() {
		userSitesRows = new ArrayList();
		try{
			Connection c = M_sql.borrowConnection();
			String sql = "select distinct(SITE_ID),TITLE,TYPE,PUBLISHED,ROLE_NAME,ACTIVE from SAKAI_SITE,SAKAI_REALM,SAKAI_REALM_RL_GR,SAKAI_REALM_ROLE where SAKAI_REALM.REALM_ID=CONCAT('/site/',SAKAI_SITE.SITE_ID) and SAKAI_REALM.REALM_KEY=SAKAI_REALM_RL_GR.REALM_KEY and SAKAI_REALM_RL_GR.ROLE_KEY=SAKAI_REALM_ROLE.ROLE_KEY and SAKAI_REALM_RL_GR.USER_ID=? and IS_USER=0 and IS_SPECIAL=0 ORDER BY TITLE";
			PreparedStatement pst = c.prepareStatement(sql);
			pst.setString(1, userId);
			ResultSet rs = pst.executeQuery();
			while (rs.next()){
				String id = rs.getString("SITE_ID");
				String t = rs.getString("TITLE");
				String tp = rs.getString("TYPE");
				String pv = rs.getString("PUBLISHED").equals("1")? msgs.getString("status_published") : msgs.getString("status_unpublished");;
				String rn = rs.getString("ROLE_NAME");
				String grps = getGroups(userId, id);
				String active = rs.getString("ACTIVE").trim().equals("1")? msgs.getString("site_user_status_active") : msgs.getString("site_user_status_inactive");
				UserSitesRow row = new UserSitesRow(id, t, tp, grps, rn, pv, active,"");
				userSitesRows.add(row);
			}
			rs.close();
			pst.close();
			M_sql.returnConnection(c);
		}catch(SQLException e){
			LOG.warn("SQL error occurred while retrieving user memberships for user: " + userId, e);
			LOG.warn("UserMembership will use alternative methods for retrieving user memberships.");
			doSearch3();
		}
	}

	/**
	 * Uses ONLY Sakai API for site membership, user role and group membership.
	 */
	private void doSearch2() {
		long start = (new Date()).getTime();
		userSitesRows = new ArrayList();
		thisUserId = M_session.getCurrentSessionUserId();
		setSakaiSessionUser(userId);
		LOG.debug("Switched CurrentSessionUserId: " + M_session.getCurrentSessionUserId());
		List siteList = org.sakaiproject.site.cover.SiteService.getSites(SelectionType.ACCESS, null, null, null, SortType.TITLE_ASC, null);
		setSakaiSessionUser(thisUserId);

		Iterator i = siteList.iterator();
		while (i.hasNext()){
			Site s = (Site) i.next();
			UserSitesRow row = new UserSitesRow(s, getGroups(userId, s.getId()), getActiveUserRoleInSite(userId, s));
			userSitesRows.add(row);
		}
		long end = (new Date()).getTime();
		LOG.debug("doSearch2() took total of "+((end - start)/1000)+" sec.");
	}

	/**
	 * Uses single simple SQL for site membership, uses API for user role and
	 * group membership.<br>
	 * For a 12 site users it takes ~30secs!
	 * @deprecated
	 */
	private void doSearch3() {
		userSitesRows = new ArrayList();
		timeSpentInGroups = 0;
		try{
			Connection c = M_sql.borrowConnection();
			String sql = "select distinct(SAKAI_SITE_USER.SITE_ID) from SAKAI_SITE_USER,SAKAI_SITE where SAKAI_SITE.SITE_ID=SAKAI_SITE_USER.SITE_ID and IS_USER=0 and IS_SPECIAL=0 and USER_ID=?";
			PreparedStatement pst = c.prepareStatement(sql);
			pst.setString(1, userId);
			ResultSet rs = pst.executeQuery();
			while (rs.next()){
				String id = rs.getString("SITE_ID");
				try{
					Site site = M_site.getSite(id);
					UserSitesRow row = new UserSitesRow(site, getGroups(userId, site), getActiveUserRoleInSite(userId, site));
					userSitesRows.add(row);
				}catch(IdUnusedException e){
					LOG.warn("Unable to retrieve site for site id: " + id, e);
				}
			}
			rs.close();
			pst.close();
			M_sql.returnConnection(c);
		}catch(SQLException e){
			LOG.warn("SQL error occurred while retrieving user memberships for user: " + userId, e);
			LOG.warn("UserMembership will use alternative methods for retrieving user memberships (ONLY Published sites will be listed).");
			doSearch2();
		}
		LOG.debug("Group ops took " + (timeSpentInGroups / 1000) + " secs");
	}

	/**
	 * Uses SQL queries for getting group membership (very very fast).
	 * @param userId The user ID.
	 * @param site The Site object
	 * @return A String with group list.
	 */
	private String getGroups2(String userId, String siteId) {
		StringBuffer groups = new StringBuffer();
		try{
			Connection c = M_sql.borrowConnection();
			String sql = "select TITLE " + "from SAKAI_SITE_GROUP,SAKAI_REALM,SAKAI_REALM_RL_GR " + "where SITE_ID=? "
					+ "and SAKAI_REALM.REALM_ID=CONCAT('/site/',CONCAT(SITE_ID,CONCAT('/group/',GROUP_ID))) " + "and SAKAI_REALM.REALM_KEY=SAKAI_REALM_RL_GR.REALM_KEY "
					+ "and SAKAI_REALM_RL_GR.USER_ID=? " + "ORDER BY TITLE";
			PreparedStatement pst = c.prepareStatement(sql);
			pst.setString(1, siteId);
			pst.setString(2, userId);
			ResultSet rs = pst.executeQuery();
			while (rs.next()){
				String t = rs.getString("TITLE");
				if(groups.length() != 0) groups.append(", ");
				groups.append(t);
			}
			rs.close();
			pst.close();
			M_sql.returnConnection(c);
		}catch(SQLException e){
			LOG.error("SQL error occurred while retrieving group memberships for user: " + userId, e);
		}
		return groups.toString();
	}

	/**
	 * Uses Sakai API for getting group membership (very very slow).
	 * @param userId The user ID.
	 * @param site The Site object
	 * @return A String with group list.
	 */
	public String getGroups(String userId, Site site) {
		long start = (new Date()).getTime();
		StringBuffer groups = new StringBuffer();
		Iterator ig = site.getGroupsWithMember(userId).iterator();
		while (ig.hasNext()){
			Group g = (Group) ig.next();
			if(groups.length() != 0) groups.append(", ");
			groups.append(g.getTitle());
		}
		long end = (new Date()).getTime();
		timeSpentInGroups += (end - start);
		LOG.debug("getGroups("+userId+", "+site.getTitle()+") took "+((end - start)/1000)+" sec.");
		return groups.toString();
	}
	
	public String getGroups(String userId, String siteId) {
		long start = (new Date()).getTime();
		StringBuffer groups = new StringBuffer();
		String siteReference = M_site.siteReference(siteId);
		try{
			Connection c = M_sql.borrowConnection();
			String sql = "select SS.GROUP_ID, SS.TITLE, SS.DESCRIPTION " 
					+ "from SAKAI_SITE_GROUP SS, SAKAI_REALM R, SAKAI_REALM_RL_GR RRG " 
					+ "where R.REALM_ID = concat(concat('"+siteReference+"','/group/'), SS.GROUP_ID) "
					+ "and R.REALM_KEY = RRG.REALM_KEY " 
					+ "and RRG.USER_ID = ? "
					+ "and SS.SITE_ID = ? "
					+ "ORDER BY TITLE";
			PreparedStatement pst = c.prepareStatement(sql);
			pst.setString(1, userId);
			pst.setString(2, siteId);
			ResultSet rs = pst.executeQuery();
			while (rs.next()){
				String t = rs.getString("SS.TITLE");
				if(groups.length() != 0) groups.append(", ");
				groups.append(t);
			}
			rs.close();
			pst.close();
			M_sql.returnConnection(c);
		}catch(SQLException e){
			LOG.error("SQL error occurred while retrieving group memberships for user: " + userId, e);
		}
		long end = (new Date()).getTime();
		timeSpentInGroups += (end - start);
		LOG.debug("getGroups("+userId+", "+siteId+") took "+((end - start)/1000)+" sec.");
		return groups.toString();
	}

	/**
	 * Uses Sakai API for getting user role in site.
	 * @param userId The user ID.
	 * @param site The Site object.
	 * @return The user role in site as String.
	 */
	protected String getActiveUserRoleInSite(String userId, Site site) {
		Role r = site.getUserRole(userId);
		return (r != null) ? r.getId() : "";
	}

	private synchronized void setSakaiSessionUser(String id) {
		Session sakaiSession = M_session.getCurrentSession();
		sakaiSession.setUserId(id);
		sakaiSession.setUserEid(id);
	}

	// ######################################################################################
	// ActionListener methods
	// ######################################################################################
	public String processActionUserId() {
		try{
			ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
			Map paramMap = context.getRequestParameterMap();
			userId = (String) paramMap.get("userId");
			refreshQuery = true;
			return "sitelist";
		}catch(Exception e){
			LOG.error("Error getting userId var.");
			return "userlist";
		}
	}

	public String processActionBack() {
		return "userlist";
	}

	// ######################################################################################
	// Generic get/set methods
	// ######################################################################################
	public boolean isAllowed() {
		allowed = authz.isUserAbleToViewUmem(M_tm.getCurrentPlacement().getContext());
		
		if(!allowed){
			FacesContext fc = FacesContext.getCurrentInstance();
			message = msgs.getString("unauthorized");
			fc.addMessage("allowed", new FacesMessage(FacesMessage.SEVERITY_FATAL, message, null));
			allowed = false;
		}
		return allowed;
	}

	public List getUserSitesRows() {
		// warn if not allowed
		if(!isAllowed()) return new ArrayList();

		if(userId == null /*|| isNotValidated()*/){
			String param = (String) FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("userId");
			if(param != null){
				userId = param;
			}
		}

		if(refreshQuery){
			LOG.debug("Refreshing query...");
			doSearch2();
			refreshQuery = false;
		}
		
		if(userSitesRows != null && userSitesRows.size() > 0) Collections.sort(userSitesRows, getUserSitesRowComparator(sitesSortColumn, sitesSortAscending, collator));
		return userSitesRows;
	}

	public void setUserSitesRows(List userRows) {
		this.userSitesRows = userRows;
	}

	public boolean isEmptySiteList() {
		return (userSitesRows == null || userSitesRows.size() <= 0);
	}

	public String getUserEid() {
		try{
			return UserDirectoryService.getUserEid(userId);
		}catch(UserNotDefinedException e){
			return userId;
		}
	}

	public void setUserId(String id) {
		this.userId = id;
	}

	public boolean getSitesSortAscending() {
		return this.sitesSortAscending;
	}

	public void setSitesSortAscending(boolean sitesSortAscending) {
		this.sitesSortAscending = sitesSortAscending;
	}

	public String getSitesSortColumn() {
		return this.sitesSortColumn;
	}

	public void setSitesSortColumn(String sitesSortColumn) {
		this.sitesSortColumn = sitesSortColumn;
	}

	// ######################################################################################
	// CSV export
	// ######################################################################################
	public void exportAsCsv(ActionEvent event) {
		String prefix = "Membership_for_"+getUserEid();
		Export.writeAsCsv(getAsCsv(userSitesRows), prefix);
	}

	private String getAsCsv(List list) {
		StringBuffer sb = new StringBuffer();

		// Add the headers
		Export.appendQuoted(sb, msgs.getString("site_name"));
		sb.append(",");
		Export.appendQuoted(sb, msgs.getString("site_id"));
		sb.append(",");
		Export.appendQuoted(sb, msgs.getString("groups"));
		sb.append(",");
		Export.appendQuoted(sb, msgs.getString("site_type"));
		sb.append(",");
		Export.appendQuoted(sb, msgs.getString("role_name"));
		sb.append(",");
		Export.appendQuoted(sb, msgs.getString("status"));
		sb.append("\n");
		Export.appendQuoted(sb, msgs.getString("site_term"));
		sb.append("\n");

		// Add the data
		Iterator i = list.iterator();
		while (i.hasNext()){
			UserSitesRow usr = (UserSitesRow) i.next();
			// user name
			Export.appendQuoted(sb, usr.getSiteTitle());
			sb.append(",");
			Export.appendQuoted(sb, usr.getSiteId());
			sb.append(",");
			Export.appendQuoted(sb, usr.getGroups());
			sb.append(",");
			Export.appendQuoted(sb, usr.getSiteType());
			sb.append(",");
			Export.appendQuoted(sb, usr.getRoleName());
			sb.append(",");
			Export.appendQuoted(sb, usr.getPubView());
			sb.append(",");
			Export.appendQuoted(sb, usr.getSiteTerm());
			sb.append("\n");
		}
		return sb.toString();
	}
	
}
