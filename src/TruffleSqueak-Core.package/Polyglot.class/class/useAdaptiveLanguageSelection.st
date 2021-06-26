preferences
useAdaptiveLanguageSelection

	<preference: 'The selected object determines the language in use'
		category: 'Polyglot'
		description: 'If true, ...'
		type: #Boolean>
	^ UseAdaptiveLanguageSelection ifNil: [ false ]