accessing
model: aModel
	
	model ifNotNil:[model removeDependent: self].
	model := aModel.
	model ifNotNil:[model addDependent: self].