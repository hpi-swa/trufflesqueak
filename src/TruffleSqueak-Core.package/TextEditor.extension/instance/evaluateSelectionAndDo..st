*TruffleSqueak-Core-overrides
evaluateSelectionAndDo: aBlock
	"Treat the current selection as an expression; evaluate it and invoke aBlock with the result."
	| result rcvr ctxt languageId |
	self lineSelectAndEmptyCheck: [^ nil].

	(model respondsTo: #evaluateExpression:) ifTrue: [
		^ aBlock value: (model perform: #evaluateExpression: with: self selection)].

	(model respondsTo: #doItReceiver) 
		ifTrue: [ rcvr := model doItReceiver.
				ctxt := model doItContext]
		ifFalse: [rcvr := ctxt := nil].

	languageId := (model respondsTo: #languageId)
		ifTrue: [ model languageId ]
		ifFalse: [ Polyglot defaultLanguageId ].

	result := languageId = Polyglot languageIdSmalltalk
		ifTrue: [ [
		rcvr class evaluatorClass new 
			evaluate: self selectionAsStream
			in: ctxt
			to: rcvr
			environment: (model environment ifNil: [Smalltalk globals])
			notifying: self
			ifFail: [morph flash. ^ nil]
			logged: true.
	] 
		on: OutOfScopeNotification 
		do: [ :ex | ex resume: true] ]
		ifFalse: [
			rcvr ifNotNil: [ [ (Polyglot primitiveGetScope: languageId) at: 'self' put: rcvr ] on: Error do: [ :e | "ignore error" ] ].
			Polyglot eval: languageId string: self selection asString ].
		
	(model respondsTo: #expressionEvaluated:result:) ifTrue: [
		model perform: #expressionEvaluated:result: with: self selection with: result].
		
	^aBlock value: result