nbformat
isValidNBJson: aJsonString
	validateNBJson ifNil: [
		[ {[ validateNBJson := (Polyglot eval: #python string: 'import nbformat
def is_valid_nb_json(nb_json):
  nb = nbformat.reads(nb_json, 4)
  try:
    nbformat.validate(nb)
    return True
  except nbformat.ValidationError:
    return False
is_valid_nb_json') ]} do: [ :block | block value ]
				displayingProgress: [ :block | 'Initializing nbformat in Python...' ]
				every: 100 ] on: Error do: [ :e |
					self error: 'nbformat does not seem to be installed (`pip install nbformat`).'.
					^ false ] ].
	^ validateNBJson value: aJsonString interopAsString "interopAsString hack: this might not be needed after 21.2"