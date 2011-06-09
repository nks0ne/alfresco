/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.security.authority.script;

import static org.alfresco.repo.security.authority.script.ScriptGroup.makeScriptGroups;

import java.util.Collections;
import java.util.Set;

import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.alfresco.repo.security.authority.UnknownAuthorityException;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.util.ScriptPagingDetails;

/**
 * Script object representing the authority service.
 * 
 * Provides Script access to groups and may in future be extended for roles and people.
 * 
 * @author Mark Rogers
 */
public class ScriptAuthorityService extends BaseScopableProcessorExtension
{    
    /** The service */
    private AuthorityService authorityService;

	public void setAuthorityService(AuthorityService authorityService)
	{
		this.authorityService = authorityService;
	}

	public AuthorityService getAuthorityService()
	{
		return authorityService;
	}
	
	/**
	 * Search the root groups, those without a parent group.
	 * @return The root groups (empty if there are no root groups)
	 */
	public ScriptGroup[] searchRootGroupsInZone(String displayNamePattern, String zone)
    {
	    return searchRootGroupsInZone(displayNamePattern, zone, -1, -1);
    }
	    
    /**
     * Search the root groups, those without a parent group.
     * 
     * @param maxItems Maximum number of items returned.
     * @param skipCount number of items to skip.
     * @return The root groups (empty if there are no root groups)
     */
    public ScriptGroup[] searchRootGroupsInZone(String displayNamePattern, String zone, int maxItems, int skipCount)
    {
        return searchRootGroupsInZone(displayNamePattern, zone, new ScriptPagingDetails(maxItems, skipCount), null);
    }
	
    /**
     * Search the root groups, those without a parent group.
     * 
     * @param paging Paging object with max number to return, and items to skip
     * @param sortBy What to sort on (authorityName, shortName or displayName)
     * @return The root groups (empty if there are no root groups)
     */
    public ScriptGroup[] searchRootGroupsInZone(String displayNamePattern, String zone, ScriptPagingDetails paging, String sortBy)
    {
        Set<String> authorities;
        try {
         authorities = authorityService.findAuthorities(AuthorityType.GROUP,
                    null, true, displayNamePattern, zone);
        }
        catch(UnknownAuthorityException e)
        {
            authorities = Collections.emptySet();
        }
        return makeScriptGroups(authorities, paging, sortBy, authorityService);
    }
    
	/**
	 * Search the root groups, those without a parent group.
	 * @return The root groups (empty if there are no root groups)
	 */
	public ScriptGroup[] searchRootGroups(String displayNamePattern)
    {
	    return searchRootGroupsInZone(displayNamePattern, null);
    }
    
    /**
     * Search the root groups, those without a parent group.   Searches in all zones.
     * @return The root groups (empty if there are no root groups)
     */
    public ScriptGroup[] getAllRootGroups()
    {
        return getAllRootGroups(-1, -1);
    }
	
	/**
	 * Search the root groups, those without a parent group.   Searches in all zones.
	 * @return The root groups (empty if there are no root groups)
	 */
	public ScriptGroup[] getAllRootGroups(int maxItems, int skipCount)
	{
	    return getAllRootGroups(new ScriptPagingDetails(maxItems, skipCount));
	}
	
    /**
     * Search the root groups, those without a parent group.   Searches in all zones.
     * @return The root groups (empty if there are no root groups)
     */
    public ScriptGroup[] getAllRootGroups(ScriptPagingDetails paging)
    {
        Set<String> authorities;
        try{
            authorities = authorityService.getAllRootAuthorities(AuthorityType.GROUP);
        }
        catch(UnknownAuthorityException e)
        {
            authorities = Collections.emptySet();
        }
        return makeScriptGroups(authorities, paging, authorityService);
    }
    
    /**
     * Get the root groups, those without a parent group.
     * @param zone zone to search in.
     * @return The root groups (empty if there are no root groups)
     */
    public ScriptGroup[] getAllRootGroupsInZone(String zone)
    {
        return getAllRootGroupsInZone(zone, -1, -1);
    }
        
	/**
	 * Get the root groups, those without a parent group.
	 * @param zone zone to search in.
     * @param maxItems Maximum number of items returned.
     * @param skipCount number of items to skip.
	 * @return The root groups (empty if there are no root groups)
	 */
	public ScriptGroup[] getAllRootGroupsInZone(String zone, int maxItems, int skipCount)
	{
		Set<String> authorities;
		try
		{
		    authorities= authorityService.getAllRootAuthoritiesInZone(zone, AuthorityType.GROUP);
        }
        catch(UnknownAuthorityException e)
        {
            authorities = Collections.emptySet();
        }

		return makeScriptGroups(authorities, new ScriptPagingDetails(maxItems, skipCount), null, authorityService);
	}
    
    /**
     * Get the root groups, those without a parent group.
     * @param zone zone to search in.
     * @param paging Paging object with max number to return, and items to skip
     * @param sortBy What to sort on (authorityName, shortName or displayName)
     * @return The root groups (empty if there are no root groups)
     */
    public ScriptGroup[] getAllRootGroupsInZone(String zone, ScriptPagingDetails paging, String sortBy)
    {
        Set<String> authorities;
        try
        {
            authorities= authorityService.getAllRootAuthoritiesInZone(zone, AuthorityType.GROUP);
        }
        catch(UnknownAuthorityException e)
        {
            authorities = Collections.emptySet();
        }

        return makeScriptGroups(authorities, paging, sortBy, authorityService);
    }
    
	/**
	 * Get a group given its short name
	 * @param shortName, the shortName of the group
	 * @return the authority or null if it can't be found
	 */
	public ScriptGroup getGroup(String shortName)
	{
		String fullName = authorityService.getName(AuthorityType.GROUP, shortName);
		
		if (authorityService.authorityExists(fullName))
		{
		    ScriptGroup group = new ScriptGroup(fullName, authorityService);
		    return group;		
		}
		// group not found.
		return null;
	}
	
	/**
	 * Get a group given it full authority name (Which must begin with 'GROUP_'
	 * @param fullAuthorityName, the shortName of the group
	 * @return the authority or null if it can't be found
	 */
	public ScriptGroup getGroupForFullAuthorityName(String fullAuthorityName)
	{
		if (authorityService.authorityExists(fullAuthorityName))
		{
		    ScriptGroup group = new ScriptGroup(fullAuthorityName, authorityService);
		    return group;		
		}
		// group not found.
		return null;
	}
	
	/**
	 * Create a new root group in the default application zones
	 * 
	 * @return the new root group.
	 */
	public ScriptGroup createRootGroup(String shortName, String displayName)
	{
		authorityService.createAuthority(AuthorityType.GROUP, shortName, displayName, authorityService.getDefaultZones());
		return getGroup(shortName);
	}
	
	/**
	 * Search for groups in all zones.
	 * 
	 * @param shortNameFilter partial match on shortName (* and ?) work.  If empty then matches everything.
	 * @return the groups matching the query
	 */
	public ScriptGroup[] searchGroups(String shortNameFilter)
	{
		return searchGroupsInZone(shortNameFilter, null);
	}
	
	/**
	 * Search for groups in a specific zone
	 * 
	 * @param shortNameFilter partial match on shortName (* and ?) work.  If empty then matches everything.
	 * @param zone zone to search in.
	 * @return the groups matching the query
	 */
	public ScriptGroup[] searchGroupsInZone(String shortNameFilter, String zone)
	{
		return searchGroupsInZone(shortNameFilter, zone, -1, -1);
	}
    
    /**
     * Search for groups in a specific zone
     * Includes paging parameters to limit size of results returned.
     * 
     * @param shortNameFilter partial match on shortName (* and ?) work.  If empty then matches everything.
     * @param zone zone to search in.
     * @param maxItems Maximum number of items returned.
     * @param skipCount number of items to skip.
     * @return the groups matching the query
     */
    public ScriptGroup[] searchGroupsInZone(String shortNameFilter, String zone, int maxItems, int skipCount)
    {
        return searchGroupsInZone(shortNameFilter, zone, new ScriptPagingDetails(maxItems, skipCount), null);
    }
	
    /**
     * Search for groups in a specific zone
     * Includes paging parameters to limit size of results returned.
     * 
     * @param shortNameFilter partial match on shortName (* and ?) work.  If empty then matches everything.
     * @param zone zone to search in.
     * @param paging Paging object with max number to return, and items to skip
     * @param sortBy What to sort on (authorityName, shortName or displayName)
     * @return the groups matching the query
     */
    public ScriptGroup[] searchGroupsInZone(String shortNameFilter, String zone, ScriptPagingDetails paging, String sortBy)
    {
        String filter = shortNameFilter;
        
        /**
         * Modify shortNameFilter to be "shortName*"
         */
        if (shortNameFilter.length() != 0)
        {
            filter = filter.replace("\"", "") + "*";
        }
        
        Set<String> authorities;
        try {
            authorities = authorityService.findAuthorities(AuthorityType.GROUP, null, false, filter, zone);
        }
        catch(UnknownAuthorityException e)
        {
            // Return an empty set if unrecognised authority.
            authorities = Collections.emptySet();
        }
        return makeScriptGroups(authorities, paging, sortBy, authorityService);
    }
}