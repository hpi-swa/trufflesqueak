Class {
	#name : #AWFYCollisionDetector,
	#superclass : #Object,
	#instVars : [
		'state'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYCollisionDetector class >> new [
 ^ super new initialize 
]

{ #category : #'as yet unclassified' }
AWFYCollisionDetector >> draw: motion on: voxelMap [

    | seen |
    seen := AWFYRedBlackTree new.
    self recurse: voxelMap seen: seen voxel: (self voxelHash: motion posOne) motion: motion

]

{ #category : #'as yet unclassified' }
AWFYCollisionDetector >> handleNewFrame: frame [

    | motions seen toRemove allReduced collisions |
    motions := AWFYVector new.
    seen := AWFYRedBlackTree new.

    frame forEach: [:aircraft |
      | oldPosition newPosition |
      oldPosition := state at: aircraft callsign put: aircraft position.
      newPosition := aircraft position.
      seen at: aircraft callsign put: true.

      oldPosition isNil ifTrue: [
        "Treat newly introduced aircraft as if they were stationary"
        oldPosition := newPosition ].

      motions append: (AWFYMotion new: aircraft callsign old: oldPosition new: newPosition) ].

    " Remove aircraft that are no longer present "
    toRemove := AWFYVector new.
    state forEach: [:e |
      (seen at: e key) ifFalse: [ toRemove append: e key ] ].

    toRemove forEach: [:e | state remove: e ].

    allReduced := self reduceCollisionSet: motions.
    collisions := AWFYVector new.
    allReduced forEach: [:reduced |
      1 to: reduced size do: [:i |
        | motion1 |
        motion1 := reduced at: i.
        i + 1 to: reduced size do: [:j | 
          | motion2 collision |
          motion2 := reduced at: j.
          collision := motion1 findIntersection: motion2.
          collision notNil ifTrue: [
            collisions append: (AWFYCollision a: motion1 callsign b: motion2 callsign pos: collision) ] ] ] ].

    ^ collisions

]

{ #category : #'as yet unclassified' }
AWFYCollisionDetector >> initialize [

    state := AWFYRedBlackTree new

]

{ #category : #'as yet unclassified' }
AWFYCollisionDetector >> isInVoxel: voxel motion: motion [

    | init fin v_s r v_x x0 xv v_y y0 yv low_x high_x low_y high_y |
    (voxel x > AWFYConstants MaxX or: [
     voxel x < AWFYConstants MinX or: [
     voxel y > AWFYConstants MaxY or: [
     voxel y < AWFYConstants MinY ]]]) ifTrue: [ ^ false ].

    init := motion posOne.
    fin  := motion posTwo.

    v_s := AWFYConstants GoodVoxelSize.
    r   := AWFYConstants ProximityRadius / 2.0.

    v_x := voxel x.
    x0  := init x.
    xv  := fin x - init x.

    v_y := voxel y.
    y0  := init y.
    yv  := fin y - init y.

    xv = 0.0 "follow IEEE floating point semantics"
      ifTrue:  [
        low_x  := Float infinity.
        high_x := Float infinity ]
      ifFalse: [
        low_x := (v_x - r - x0) / xv.
        high_x := (v_x + v_s + r - x0) / xv ].

    xv < 0.0 ifTrue: [
      | tmp |
      tmp    := low_x.
      low_x  := high_x.
      high_x := tmp ].

    yv = 0.0 "follow IEEE floating point semantics"
      ifTrue: [
        low_y  := Float infinity.
        high_y := Float infinity ]
      ifFalse: [
        low_y  := (v_y - r - y0) / yv.
        high_y := (v_y + v_s + r - y0) / yv ].

    yv < 0.0 ifTrue: [
      | tmp |
      tmp    := low_y.
      low_y  := high_y.
      high_y := tmp ].

    ^ (((xv = 0.0 and: [v_x <= (x0 + r) and: [(x0 - r) <= (v_x + v_s)]]) or: [ "no motion in x"
        (low_x <= 1.0 and: [1.0 <= high_x]) or: [
        (low_x <= 0.0 and: [0.0 <= high_x]) or: [
        (0.0 <= low_x and: [high_x <= 1.0])]]]) and: [
          
        (yv = 0.0 and: [v_y <= (y0 + r) and: [(y0 - r) <= (v_y + v_s)]]) or: [ "no motion in y"
          (low_y <= 1.0 and: [1.0 <= high_y]) or: [
          (low_y <= 0.0 and: [0.0 <= high_y]) or: [
          (0.0   <= low_y and: [high_y <= 1.0])]]]]) and: [

         xv = 0.0 or: [
         yv = 0.0 or: [ "no motion in x or y or both"
         (low_y <= high_x and: [high_x <= high_y]) or: [
         (low_y <= low_x  and: [low_x <= high_y]) or: [
         (low_x <= low_y  and: [high_y <= high_x]) ]]]]]

]

{ #category : #'as yet unclassified' }
AWFYCollisionDetector >> put: motion and: voxel into: voxelMap [

    | array |
    array := voxelMap at: voxel.
    array isNil ifTrue: [
      array := AWFYVector new.
      voxelMap at: voxel put: array ].
    array append: motion

]

{ #category : #'as yet unclassified' }
AWFYCollisionDetector >> recurse: voxelMap seen: seen voxel: nextVoxel motion: motion [

    (self isInVoxel: nextVoxel motion: motion) ifFalse: [ ^ self ].
    (seen at: nextVoxel put: true) = true ifTrue: [ ^ self ].

    self put: motion and: nextVoxel into: voxelMap.

    self recurse: voxelMap seen: seen voxel: (nextVoxel minus: AWFYConstants horizontal) motion: motion.
    self recurse: voxelMap seen: seen voxel: (nextVoxel plus:  AWFYConstants horizontal) motion: motion.
    self recurse: voxelMap seen: seen voxel: (nextVoxel minus: AWFYConstants vertical)   motion: motion.
    self recurse: voxelMap seen: seen voxel: (nextVoxel plus:  AWFYConstants vertical)   motion: motion.
    self recurse: voxelMap seen: seen voxel: ((nextVoxel minus: AWFYConstants horizontal) minus: AWFYConstants vertical) motion: motion.
    self recurse: voxelMap seen: seen voxel: ((nextVoxel minus: AWFYConstants horizontal) plus:  AWFYConstants vertical) motion: motion.
    self recurse: voxelMap seen: seen voxel: ((nextVoxel plus:  AWFYConstants horizontal) minus: AWFYConstants vertical) motion: motion.
    self recurse: voxelMap seen: seen voxel: ((nextVoxel plus:  AWFYConstants horizontal) plus:  AWFYConstants vertical) motion: motion.

]

{ #category : #'as yet unclassified' }
AWFYCollisionDetector >> reduceCollisionSet: motions [

    | voxelMap result |
    voxelMap := AWFYRedBlackTree new.
    motions forEach: [:motion | self draw: motion on: voxelMap ].

    result := AWFYVector new.
    voxelMap forEach: [:e |
      e value size > 1 ifTrue: [ result append: e value ] ].
    ^ result

]

{ #category : #'as yet unclassified' }
AWFYCollisionDetector >> voxelHash: position [

    | xDiv yDiv x y |
    xDiv := (position x / AWFYConstants GoodVoxelSize) asInteger.
    yDiv := (position y / AWFYConstants GoodVoxelSize) asInteger.

    x := AWFYConstants GoodVoxelSize * xDiv.
    y := AWFYConstants GoodVoxelSize * yDiv.

    position x < 0 ifTrue: [ x := x - AWFYConstants GoodVoxelSize ].
    position y < 0 ifTrue: [ y := y - AWFYConstants GoodVoxelSize ].

    ^ AWFYVector2D x: x y: y

]
