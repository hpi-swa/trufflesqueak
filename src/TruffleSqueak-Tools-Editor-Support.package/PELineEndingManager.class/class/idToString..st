id <-> string
idToString: aSymbol

	^ (Dictionary newFrom: {
		#lf -> String lf.
		#cr -> String cr.
		#crlf -> String crlf}) at: aSymbol