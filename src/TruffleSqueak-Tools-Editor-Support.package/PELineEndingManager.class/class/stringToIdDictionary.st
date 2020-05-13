id <-> string
stringToIdDictionary

	^ Dictionary newFrom: {
		String lf -> #lf.
		String cr -> #cr.
		String crlf -> #crlf}