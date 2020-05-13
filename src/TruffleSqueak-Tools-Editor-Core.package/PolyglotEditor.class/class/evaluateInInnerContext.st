preferences
evaluateInInnerContext

	<preference: 'Evaluate code in an inner context'
		category: 'Polyglot Editor'
		description: 'When on, code is evaluate in an inner context and therefore does not pollute the global scope'
		type: #Boolean>
	^ EvaluateCodeInInnerContext ifNil: [false]