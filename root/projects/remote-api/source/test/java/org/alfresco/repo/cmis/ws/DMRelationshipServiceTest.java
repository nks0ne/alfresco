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
package org.alfresco.repo.cmis.ws;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

public class DMRelationshipServiceTest extends AbstractServiceTest
{

    public final static String SERVICE_WSDL_LOCATION = CmisServiceTestHelper.ALFRESCO_URL + "/cmis/RelationshipService?wsdl";
    public final static QName SERVICE_NAME = new QName("http://docs.oasis-open.org/ns/cmis/ws/200908/", "RelationshipService");

    @SuppressWarnings("unused")
    private String relationshipId;
    private String document2Id;

    public DMRelationshipServiceTest()
    {
        super();
    }

    public DMRelationshipServiceTest(String testCase, String username, String password)
    {
        super(testCase, username, password);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        documentId = helper.createDocument("Test cmis document (" + System.currentTimeMillis() + ")", companyHomeId);
        document2Id = helper.createDocument("Test cmis document (" + System.currentTimeMillis() + ")", companyHomeId);
        // TODO: uncomment
        // try
        // {
        // relationshipId = helper.createRelationship("Test cmis relationship (" + System.currentTimeMillis() + ")", documentId, document2Id);
        // }
        // catch (Exception e)
        // {
        // if(documentId != null ){
        // helper.deleteDocument(documentId);
        // }
        // if(document2Id != null ){
        // helper.deleteDocument(document2Id);
        // }
        // throw e;
        // }
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        helper.deleteDocument(documentId);
        helper.deleteDocument(document2Id);
    }

    protected Object getServicePort()
    {
        URL serviceWsdlURL;
        try
        {
            serviceWsdlURL = new URL(SERVICE_WSDL_LOCATION);
        }
        catch (MalformedURLException e)
        {
            throw new java.lang.RuntimeException("Cannot get service Wsdl URL", e);
        }

        Service service = Service.create(serviceWsdlURL, SERVICE_NAME);
        return service.getPort(RelationshipServicePort.class);
    }

    public void testGetRelationships() throws Exception
    {
        // TODO: uncomment
        // GetRelationships request = cmisObjectFactory.createGetRelationships();
        // request.setRepositoryId(repositoryId);
        // request.setObjectId(documentId);
        // request.setFilter(cmisObjectFactory.createGetRelationshipsFilter("*"));
        // request.setTypeId(cmisObjectFactory.createGetRelationshipsTypeId(CMISMapping.RELATIONSHIP_TYPE_ID.getTypeId()));
        // request.setIncludeAllowableActions(cmisObjectFactory.createGetRelationshipsIncludeAllowableActions(true));
        // request.setIncludeSubRelationshipTypes(cmisObjectFactory.createGetRelationshipsIncludeSubRelationshipTypes(true));
        // request.setMaxItems(cmisObjectFactory.createGetRelationshipsMaxItems(BigInteger.valueOf(0)));
        // request.setSkipCount(cmisObjectFactory.createGetRelationshipsSkipCount(BigInteger.valueOf(0)));
        // request.setDirection(cmisObjectFactory.createGetRelationshipsDirection(EnumRelationshipDirection.SOURCE));
        //
        // // public GetRelationshipsResponse getRelationships(GetRelationships parameters)
        // GetRelationshipsResponse responce = ((RelationshipServicePort) servicePort).getRelationships(request);
        // assertNotNull(responce.getObject());
    }

    public void testGetRelationshipObjectProperties() throws Exception
    {

        // TODO: uncomment

        // GetPropertiesResponse response = helper.getObjectProperties(relationshipId);
        //
        // assertNotNull(response);
        // assertNotNull(response.getObject());
        // assertNotNull(response.getObject().getProperties());
        // assertNotNull(response.getObject().getProperties().getProperty());
        //
        // assertTrue(response.getObject().getProperties().getProperty().size() > 3);
        //
        // assertEquals(relationshipId.toString(), PropertyUtil.getValue((CmisProperty) PropertyUtil.getProperty(response.getObject().getProperties(),
        // CMISMapping.PROP_OBJECT_ID)));
    }
}
