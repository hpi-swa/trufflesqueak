instance creation
default
	^ Default ifNil: [ Default := self basicNew initialize ]