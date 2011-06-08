<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/documentlibrary/evaluator.lib.js">
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/documentlibrary/filters.lib.js">
<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/documentlibrary/parse-args.lib.js">

/**
 * Main entry point: Create collection of documents and folders in the given space
 *
 * @method getDoclist
 */
function getDoclist()
{
   // Use helper function to get the arguments
   var parsedArgs = ParseArgs.getParsedArgs();
   if (parsedArgs === null)
   {
      return;
   }

   var filter = args.filter,
      items = [];

   // Try to find a filter query based on the passed-in arguments
   var allNodes = [],
      totalRecords,
      paged = false,
      favourites = Common.getFavourites(),
      filterParams = Filters.getFilterParams(filter, parsedArgs,
      {
         favourites: favourites
      }),
      query = filterParams.query;
   
   var useDB = true;
   
   if ((useDB == true) && ((filter == "path") || (filter == "") || (filter == null)))
   {
       // TODO also add DB filter by "node" (in addition to "path")
       
       var parentNode = parsedArgs.pathNode;
       if (parentNode !== null)
       {
          var ignoreTypes=["cm:thumbnail", "fm:forums","fm:forum","fm:topic","fm:post"];
          
          var skip = -1;
          var max = -1;
          
          if (args.size != null)
          {
             max = args.size;
             
             if (args.pos > 0)
             {
                skip = (args.pos - 1) * max;
             }
          }
          
          var sortField = (args.sortField == null ? "cm:name" : args.sortField);
          var sortAsc = (((args.sortAsc == null) || (args.sortAsc == "true")) ? true : false);
          
          var pagedResult = parentNode.childFileFolders(true, true, ignoreTypes, skip, max, sortField, sortAsc);
          
          allNodes = pagedResult.result;
          totalRecords = pagedResult.totalCount;
          
          paged = true;
       }
   }
   else
   {
       // Query the nodes - passing in sort and result limit parameters
       if (query !== "")
       {
          allNodes = search.query(
          {
             query: query,
             language: filterParams.language,
             page:
             {
                maxItems: (filterParams.limitResults ? parseInt(filterParams.limitResults, 10) : 0)
             },
             sort: filterParams.sort,
             templates: filterParams.templates,
             namespace: (filterParams.namespace ? filterParams.namespace : null)
          });
       }
   }
   
   
   // Ensure folders and folderlinks appear at the top of the list
   var folderNodes = [],
      documentNodes = [];
   
   for each (node in allNodes)
   {
      try
      {
         if (node.isContainer || node.typeShort == "app:folderlink")
         {
            folderNodes.push(node);
         }
         else
         {
            documentNodes.push(node);
         }
      }
      catch (e)
      {
         // Possibly an old indexed node - ignore it
      }
   }
   
   // Node type counts
   var folderNodesCount = folderNodes.length,
      documentNodesCount = documentNodes.length,
      nodes;
   
   if (parsedArgs.type === "documents")
   {
      nodes = documentNodes;
   }
   else
   {
      // TODO: Sorting with folders at end -- swap order of concat()
      nodes = folderNodes.concat(documentNodes);
   }
   
   // Pagination
   var pageSize = args.size || nodes.length,
      pagePos = args.pos || "1",
      startIndex = (pagePos - 1) * pageSize;
   
   if (! paged)
   {
       totalRecords = nodes.length;
       
       // Trim the nodes array down to the page size
       nodes = nodes.slice(startIndex, pagePos * pageSize);
   }
   
   // Common or variable parent container?
   var parent = null;
   
   if (!filterParams.variablePath)
   {
      // Parent node permissions (and Site role if applicable)
      parent =
      {
         node: parsedArgs.pathNode,
         userAccess: Evaluator.run(parsedArgs.pathNode, true).actionPermissions
      };
   }

   var isThumbnailNameRegistered = thumbnailService.isThumbnailNameRegistered(THUMBNAIL_NAME),
      thumbnail = null,
      locationNode,
      item;
   
   // Loop through and evaluate each node in this result set
   for each (node in nodes)
   {
      // Get evaluated properties.
      item = Evaluator.run(node);
      if (item !== null)
      {
         item.isFavourite = (favourites[item.node.nodeRef] === true);
         item.likes = Common.getLikes(node);

         // Does this collection of nodes have potentially differering paths?
         if (filterParams.variablePath || item.isLink)
         {
            locationNode = item.isLink ? item.linkedNode : item.node;
            location = Common.getLocation(locationNode, parsedArgs.libraryRoot);
         }
         else
         {
            location =
            {
               site: parsedArgs.location.site,
               siteTitle: parsedArgs.location.siteTitle,
               container: parsedArgs.location.container,
               path: parsedArgs.location.path,
               file: node.name
            };
         }
         location.parent = {};
         if (node.parent != null && node.parent.hasPermission("Read"))
         {
            location.parent.nodeRef = String(node.parent.nodeRef.toString());  
         }
         
         // Resolved location
         item.location = location;
         
         // Is our thumbnail type registered?
         if (isThumbnailNameRegistered && item.node.isSubType("cm:content"))
         {
            // Make sure we have a thumbnail.
            thumbnail = item.node.getThumbnail(THUMBNAIL_NAME);
            if (thumbnail === null)
            {
               // No thumbnail, so queue creation
               item.node.createThumbnail(THUMBNAIL_NAME, true);
            }
         }
         
         items.push(item);
      }
      else
      {
         totalRecords -= 1;
      }
   }

   // Array Remove - By John Resig (MIT Licensed)
   var fnArrayRemove = function fnArrayRemove(array, from, to)
   {
     var rest = array.slice((to || from) + 1 || array.length);
     array.length = from < 0 ? array.length + from : from;
     return array.push.apply(array, rest);
   };
   
   /**
    * De-duplicate orignals for any existing working copies.
    * This can't be done in evaluator.lib.js as it has no knowledge of the current filter or UI operation.
    * Note: This may result in pages containing less than the configured amount of items (50 by default).
   */
   for each (item in items)
   {
      if (item.customObj.isWorkingCopy)
      {
         var workingCopyOriginal = String(item.customObj.workingCopyOriginal);
         for (var i = 0, ii = items.length; i < ii; i++)
         {
            if (String(items[i].node.nodeRef) == workingCopyOriginal)
            {
               fnArrayRemove(items, i);
               --totalRecords;
               break;
            }
         }
      }
   }

   return (
   {
      luceneQuery: query,
      paging:
      {
         totalRecords: totalRecords,
         startIndex: startIndex
      },
      container: parsedArgs.rootNode,
      parent: parent,
      onlineEditing: utils.moduleInstalled("org.alfresco.module.vti"),
      itemCount:
      {
         folders: folderNodesCount,
         documents: documentNodesCount
      },
      items: items
   });
}

/**
 * Document List Component: doclist
 */
model.doclist = getDoclist();