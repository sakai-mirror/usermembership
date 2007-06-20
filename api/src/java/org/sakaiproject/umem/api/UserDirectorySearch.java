package org.sakaiproject.umem.api;

import java.util.List;

public interface UserDirectorySearch {

	/**
	 * Search all the users that match this criteria in id or email, first or last name, returning a subset of User records.<br>
	 * <br>
	 * Note: user id may be null.
	 * 
	 * @param criteria
	 *        The search criteria.<br>
	 *        If null, all (sakai relevant) users must be returned. 
	 * @param userType
	 * 		  The Sakai user type of matching users.<br>
	 * 			If null, users matching any usertype should be returned.<br>
	 * 			if empty (""), users with null or empty user type should be returned.
	 * @return A list (User) of all the users matching the criteria.
	 */
	public List searchUsers(String criteria, String userType);
	
}
