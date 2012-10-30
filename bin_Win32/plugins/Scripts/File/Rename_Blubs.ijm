dir = getDirectory("Directory to rename blub<number> files");
list = getFileList(dir);

prefix = "blub";
prefixLength = lengthOf(prefix);

if (list.length < 2) {
	exit("No time-lapse?");
}

// sort numerically
sorted = newArray(list.length);
largest = -1;
counter = 0;
for (i = 0; i < list.length; i++) {
	if (substring(list[i], 0, prefixLength) == prefix) {
		dash = indexOf(list[i], "-");
		if (dash > prefixLength) {
			index = parseInt(substring(list[i], prefixLength, dash));
			sorted[index] = list[i];
			if (largest < index) {
				largest = index;
			}
			counter++;
		}
	}
}

if (counter != largest + 1) {
	exit("Number mismatch: got " + counter + " but largest is " + largest);
}

// find out how many angles we have
suffix = substring(sorted[0], prefixLength + 1);
nAngles = -1;
for (i = 1; i <= list.length; i++) {
	if (sorted[i] == prefix + i + suffix) {
		nAngles = i;
		i = list.length;
	}
}

if (nAngles < 0) {
	exit("Could not determine number of angles");
}

for (i = 0; i < counter; i++) {
	dash = indexOf(sorted[i], "-");
	timePoint = 1 + floor(i / nAngles);
	angle = i % nAngles;
	target = "spim_TL" + timePoint + "_Angle" + angle + ".ome.tiff";
	File.rename(dir + sorted[i], dir + target);
	//write("File.rename(" + dir + sorted[i] + ", " + dir + target + ")");
}