set datafile separator comma

set terminal png
set output "nexus4-uncontested-write.png"

set size 1,1
set bmargin 3

set label "Nexus 4\nUncontested Write" at screen 0.1,0.9 center font "helvetica,12"

set xlabel "min" offset -13,0 font "helvetica,8"
set ylabel "ms" offset 0,-9 font "helvetica,8"
set tics font "helvetica,8"

set key lmargin center font "helvetica,8" width -7

set macros
threads = "$3==1"
minutes = "$1/60000"
testNull = "$2==0"
testSync = "$2==1"
test2Check = "$2==2"
testSpin = "$2==3"
writeMs = "$5/100"

f4(x) = m4*x + b4
fit f4(x) 'data.csv' using (@minutes):(((@testSync)&&(@threads)) ? @writeMs : 1/0) via m4,b4
f6(x) = m6*x + b6
fit f6(x) 'data.csv' using (@minutes):(((@test2Check)&&(@threads)) ? @writeMs : 1/0) via m6,b6
f8(x) = m8*x + b8
fit f8(x) 'data.csv' using (@minutes):(((@testSpin)&&(@threads)) ? @writeMs : 1/0) via m8,b8

plot \
     'data.csv' using (@minutes):(((@testSync)&&(@threads)) ? @writeMs : 1/0) title "sync, write" with points pt 2 lc rgb "#0000FF", \
     f4(x) title "fit" lc rgb "#0000FF", \
     'data.csv' using (@minutes):(((@test2Check)&&(@threads)) ? @writeMs : 1/0) title "2-check, write" with points pt 2 lc rgb "#00DD00", \
     f6(x) title "fit" lc rgb "#00DD00", \
     'data.csv' using (@minutes):(((@testSpin)&&(@threads)) ? @writeMs : 1/0) title "spin, write" with points pt 2 lc rgb "#FF0000", \
     f8(x) title "fit" lc rgb "#FF0000" 

