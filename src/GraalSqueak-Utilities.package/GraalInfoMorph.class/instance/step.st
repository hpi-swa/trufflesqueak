stepping and presenter
step
	| queueSize |
	queueSize := Graal runtime ifNil: [0] ifNotNil: [ :r | r getCompilationQueueSize asNumber ].
	self color: (queueSize < 100 ifTrue: [ Color gray ] ifFalse: [
		queueSize > 500 ifFalse: [ Color orange thriceDarker ] ifTrue: [ Color red darker ] ]).
	self contents: 'Graal Queue: ', queueSize