explorer
explorableMembers
	^ self allInstVarNames reject: [ :ea | ea = 'bindings' ]