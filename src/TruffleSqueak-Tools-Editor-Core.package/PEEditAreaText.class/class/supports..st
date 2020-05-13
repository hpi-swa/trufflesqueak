public
supports: aFileEntry

	| mimeType |
	mimeType := MIMEDocument guessTypeFromExtension: aFileEntry extension.
	(mimeType beginsWith: 'text/') ifTrue: [^ 10].
	^ [aFileEntry read. 3] ifError: [0]