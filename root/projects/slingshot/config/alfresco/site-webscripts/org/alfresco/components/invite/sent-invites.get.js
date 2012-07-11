// Widget instantiation metadata...
var searchConfig = config.scoped['Search']['search'],
    defaultMinSearchTermLength = searchConfig.getChildValue('min-search-term-length'),
    defaultMaxSearchResults = searchConfig.getChildValue('max-search-results');

model.webScriptWidgets = [];
var sentInvites = {};
sentInvites.name = "Alfresco.SentInvites";
sentInvites.provideMessages = true;
sentInvites.provideOptions = true;
sentInvites.options = {};
sentInvites.options.siteId = (page.url.templateArgs.site != null) ? page.url.templateArgs.site : "";
sentInvites.options.minSearchTermLength = (args.minSearchTermLength != null) ? args.minSearchTermLength : defaultMinSearchTermLength;
sentInvites.options.maxSearchResults = (args.maxSearchResults != null) ? args.maxSearchResults : defaultMaxSearchResults;
sentInvites.options.setFocus = (args.setFocus != null) ? args.setFocus : "false";
model.webScriptWidgets.push(sentInvites);