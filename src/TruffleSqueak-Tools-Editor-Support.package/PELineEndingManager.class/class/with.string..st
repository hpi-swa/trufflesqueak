converting
with: aLineEnding string: aStringOrText

	^ self perform: ((Dictionary newFrom: {
		String cr -> #withCr:.
		String lf -> #withLf:.
		String crlf -> #withCrlf:}) at: aLineEnding) with: aStringOrText