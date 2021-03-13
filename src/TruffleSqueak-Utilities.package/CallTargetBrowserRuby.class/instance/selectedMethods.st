methods
selectedMethods
	^ self selectedModuleName
		ifNotNil: [ :name | self class methodList: name ]
		ifNil: [ #() ]