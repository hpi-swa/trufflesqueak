preferences
evaluateSmalltalkDirectly

	<preference: 'Evaluate Smalltalk directly'
		category: 'Polyglot Editor'
		description: 'When active, running a file will evaluate Smalltalk code using Compiler>>evaluate: instead of Polyglot>>evalFile:'
		type: #Boolean>
	^ EvaluateSmalltalkDirectly ifNil: [true]