preferences
emphasizeInterop

	<preference: 'Emphasize Language Interoperability'
		category: 'Polyglot'
		description: 'If true, more information on language interoperability is globally shown.'
		type: #Boolean>
	^ EmphasizeInterop ifNil: [ false ]