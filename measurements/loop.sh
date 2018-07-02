#!/bin/bash

counter=1
while [ $counter -le $1 ]
do
	echo "run $counter"
	mx --dynamicimports /compiler squeak images/trunk.image
	((counter++))
done

