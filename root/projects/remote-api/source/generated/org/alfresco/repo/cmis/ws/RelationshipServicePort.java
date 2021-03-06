package org.alfresco.repo.cmis.ws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

/**
 * This class was generated by Apache CXF 2.1.2
 * Wed Jan 13 20:13:28 GMT 2010
 * Generated source version: 2.1.2
 * 
 */
 
@WebService(targetNamespace = "http://docs.oasis-open.org/ns/cmis/ws/200908/", name = "RelationshipServicePort")
@XmlSeeAlso({ObjectFactory.class})
public interface RelationshipServicePort {

    @WebResult(name = "objects", targetNamespace = "http://docs.oasis-open.org/ns/cmis/messaging/200908/")
    @RequestWrapper(localName = "getObjectRelationships", targetNamespace = "http://docs.oasis-open.org/ns/cmis/messaging/200908/", className = "org.alfresco.repo.cmis.ws.GetObjectRelationships")
    @ResponseWrapper(localName = "getObjectRelationshipsResponse", targetNamespace = "http://docs.oasis-open.org/ns/cmis/messaging/200908/", className = "org.alfresco.repo.cmis.ws.GetObjectRelationshipsResponse")
    @WebMethod
    public org.alfresco.repo.cmis.ws.CmisObjectListType getObjectRelationships(
        @WebParam(name = "repositoryId", targetNamespace = "http://docs.oasis-open.org/ns/cmis/messaging/200908/")
        java.lang.String repositoryId,
        @WebParam(name = "objectId", targetNamespace = "http://docs.oasis-open.org/ns/cmis/messaging/200908/")
        java.lang.String objectId,
        @WebParam(name = "includeSubRelationshipTypes", targetNamespace = "http://docs.oasis-open.org/ns/cmis/messaging/200908/")
        java.lang.Boolean includeSubRelationshipTypes,
        @WebParam(name = "relationshipDirection", targetNamespace = "http://docs.oasis-open.org/ns/cmis/messaging/200908/")
        org.alfresco.repo.cmis.ws.EnumRelationshipDirection relationshipDirection,
        @WebParam(name = "typeId", targetNamespace = "http://docs.oasis-open.org/ns/cmis/messaging/200908/")
        java.lang.String typeId,
        @WebParam(name = "filter", targetNamespace = "http://docs.oasis-open.org/ns/cmis/messaging/200908/")
        java.lang.String filter,
        @WebParam(name = "includeAllowableActions", targetNamespace = "http://docs.oasis-open.org/ns/cmis/messaging/200908/")
        java.lang.Boolean includeAllowableActions,
        @WebParam(name = "maxItems", targetNamespace = "http://docs.oasis-open.org/ns/cmis/messaging/200908/")
        java.math.BigInteger maxItems,
        @WebParam(name = "skipCount", targetNamespace = "http://docs.oasis-open.org/ns/cmis/messaging/200908/")
        java.math.BigInteger skipCount,
        @WebParam(name = "extension", targetNamespace = "http://docs.oasis-open.org/ns/cmis/messaging/200908/")
        org.alfresco.repo.cmis.ws.CmisExtensionType extension
    ) throws CmisException;
}
