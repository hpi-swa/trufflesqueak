updating
update: what
	what ifNil:[^self].	
	what == getRootsSelector ifTrue: [
		self wrapRoots: (realModel perform: getRealRootsSelector).
		^ self].
	
	super update: what