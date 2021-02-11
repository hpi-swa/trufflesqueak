helpers
getLanguageInfo: anObject
	| info |
	info := (Interop getLanguageInfo: anObject).
	^ 'Name: {1}
ID: {2}
Version: {3}
Default Mime Type: {4}
Mime Types: {5}
isInternal: {6}
isInteractive: {7}' format: {
	info getName asString. info getId asString. info getVersion asString. info getDefaultMimeType asString.
	info getMimeTypes toString asString. info isInternal. info isInteractive }