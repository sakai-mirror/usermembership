/**********************************************************************************
 * $URL: https://source.sakaiproject.org/contrib/ufp/usermembership/trunk/tool/src/java/org/sakaiproject/umem/tool/ui/Export.java $
 * $Id: Export.java 4298 2007-03-16 12:47:20Z nuno@ufp.pt $
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

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.faces.application.StateManager;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.util.ResourceLoader;


public class Export {
	/** Our log (commons). */
	private static Log						LOG		= LogFactory.getLog(SiteListBean.class);
	/** Resource bundle */
	private static transient ResourceLoader	msgs	= new ResourceLoader("org.sakaiproject.umem.tool.bundle.Messages");

	public static void writeAsCsv(String csvString, String prefixFileName) {
		String fileName = getFileName(prefixFileName);
		FacesContext faces = FacesContext.getCurrentInstance();
		HttpServletResponse response = (HttpServletResponse) faces.getExternalContext().getResponse();
		protectAgainstInstantDeletion(response);
		response.setContentType("text/comma-separated-values");
		response.setHeader("Content-disposition", "attachment; filename=" + fileName + ".csv");
		response.setContentLength(csvString.length());
		OutputStream out = null;
		try{
			out = response.getOutputStream();
			out.write(csvString.getBytes());
			out.flush();
		}catch(IOException e){
			LOG.error(e);
			e.printStackTrace();
		}finally{
			try{
				if(out != null) out.close();
			}catch(IOException e){
				LOG.error(e);
				e.printStackTrace();
			}
		}
		faces.responseComplete();
		StateManager stateManager = (StateManager) faces.getApplication().getStateManager();
		stateManager.saveSerializedView(faces);
	}

	public static StringBuffer appendQuoted(StringBuffer sb, String toQuote) {
		if(toQuote == null){
			;
		}else if((toQuote.indexOf(',') >= 0) || (toQuote.indexOf('"') >= 0)){
			String out = toQuote.replaceAll("\"", "\"\"");
			if(LOG.isDebugEnabled()) LOG.debug("Turning '" + toQuote + "' to '" + out + "'");
			sb.append("\"").append(out).append("\"");
		}else{
			sb.append(toQuote);
		}
		return sb;
	}

	/**
	 * Gets the filename for the export
	 * @param prefix Filenameprefix
	 * @return The appropriate filename for the export
	 */
	public static String getFileName(String prefix) {
		Date now = new Date();
		DateFormat df = new SimpleDateFormat(msgs.getString("export_filename_date_format"));
		StringBuffer fileName = new StringBuffer(prefix);
		fileName.append("-");
		fileName.append(df.format(now));
		return fileName.toString();
	}

	/**
	 * Try to head off a problem with downloading files from a secure HTTPS
	 * connection to Internet Explorer. When IE sees it's talking to a secure
	 * server, it decides to treat all hints or instructions about caching as
	 * strictly as possible. Immediately upon finishing the download, it throws
	 * the data away. Unfortunately, the way IE sends a downloaded file on to a
	 * helper application is to use the cached copy. Having just deleted the
	 * file, it naturally isn't able to find it in the cache. Whereupon it
	 * delivers a very misleading error message like: "Internet Explorer cannot
	 * download roster from sakai.yoursite.edu. Internet Explorer was not able
	 * to open this Internet site. The requested site is either unavailable or
	 * cannot be found. Please try again later." There are several ways to turn
	 * caching off, and so to be safe we use several ways to turn it back on
	 * again. This current workaround should let IE users save the files to
	 * disk. Unfortunately, errors may still occur if a user attempts to open
	 * the file directly in a helper application from a secure web server. TODO
	 * Keep checking on the status of this.
	 */
	public static void protectAgainstInstantDeletion(HttpServletResponse response) {
		response.reset(); // Eliminate the added-on stuff
		response.setHeader("Pragma", "public"); // Override old-style cache
		// control
		response.setHeader("Cache-Control", "public, must-revalidate, post-check=0, pre-check=0, max-age=0"); // New-style
	}
}
