package org.sakaiproject.umem.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.umem.api.UserDirectorySearch;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;


public class DefaultUserDirectorySearchImpl implements UserDirectorySearch {

	/** Our log (commons). */
	private static Log						LOG		= LogFactory.getLog(DefaultUserDirectorySearchImpl.class);

	/** Sakai services vars */
	private transient UserDirectoryService	M_uds	= (UserDirectoryService) ComponentManager.get(UserDirectoryService.class.getName());
	private transient SqlService			M_sql	= (SqlService) ComponentManager.get(SqlService.class.getName());

	/*
	 * (non-Javadoc)
	 * @see org.sakaiproject.umem.api.UserDirectorySearch#searchUsers(java.lang.String,
	 *      java.lang.String)
	 */
	public List searchUsers(String criteria, String userType) {
		List users = new ArrayList();

		try{
			// list external user ids
			List externalIds = new ArrayList();
			try{
				Connection c = M_sql.borrowConnection();
				String sqlE = "SELECT DISTINCT USER_ID FROM SAKAI_REALM_RL_GR WHERE USER_ID NOT IN (SELECT USER_ID FROM SAKAI_USER)";
				Statement st = c.createStatement();
				ResultSet rs = st.executeQuery(sqlE);
				while (rs.next()){
					String id = rs.getString("USER_ID");
					externalIds.add(id);
				}
				rs.close();
				st.close();
				M_sql.returnConnection(c);
			}catch(SQLException e){
				LOG.error("SQL error occurred while retrieving list of external users: " + e.getMessage());
			}

			// get user info and filter users using specified criteria and user type
			String regexp = null;
			if(criteria != null)
				regexp = ".*" + criteria.toLowerCase() + ".*";
			List externalUsers = M_uds.getUsers(externalIds);
			Iterator it = externalUsers.iterator();
			while (it.hasNext()){
				User u = (User) it.next();

				if(userType == null 
						|| (userType.equals("") && (u.getType() == null || u.getType().equals("")))
						|| (userType.equals(u.getType()))){
					if(criteria == null
							|| u.getDisplayName().toLowerCase().matches(regexp)
							|| u.getEmail().toLowerCase().matches(regexp)
							|| u.getEid().toLowerCase().matches(regexp)){
						users.add(u);
					}
				}

				// boolean add = false;
				// if(userType != null && !searching){
				// if((!userType.equals(USER_TYPE_NONE) &&
				// t.equals(selectedUserType)) ||
				// (selectedUserType.equals(USER_TYPE_NONE) && t.equals("")))
				// add = true;
				// }else if(userType == null && searching){
				// if(n.toLowerCase().matches(regexp) ||
				// e.toLowerCase().matches(regexp) ||
				// id.toLowerCase().matches(regexp)) add = true;
				// }else if(userType != null && searching){
				// if((!selectedUserType.equals(USER_TYPE_NONE) &&
				// t.equals(selectedUserType)) ||
				// (selectedUserType.equals(USER_TYPE_NONE) && t.equals(""))){
				// if(n.toLowerCase().matches(regexp) ||
				// e.toLowerCase().matches(regexp) ||
				// id.toLowerCase().matches(regexp)) add = true;
				// }
				// }else{
				// add = true;
				// }
				// if(add) users.add(new UserRow(id, eid, n, e, t,
				// USER_AUTH_EXTERNAL));
			}
		}catch(Exception e){
			LOG.warn("Exception occurred while querying external users: " + e.getMessage());
			e.printStackTrace();
		}

		return users;
	}

}
