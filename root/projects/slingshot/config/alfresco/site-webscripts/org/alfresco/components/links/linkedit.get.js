// Widget instantiation metadata...
model.webScriptWidgets = [];
var linkEdit = {};
linkEdit.name = "Alfresco.LinkEdit";
linkEdit.provideMessages = true;
linkEdit.provideOptions = true;
linkEdit.options = {};
linkEdit.options.siteId = page.url.templateArgs.site != null;
linkEdit.options.containerId = "links";
if (page.url.args.linkId != null)
{
   linkEdit.options.editMode = true;
   linkEdit.options.linkId = page.url.args.linkId;
}
else
{
   linkEdit.options.editMode = false;
   linkEdit.options.linkId = "";
}
model.webScriptWidgets.push(linkEdit);