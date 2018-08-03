set datafile separator comma

set terminal png
set output "nexus6-uncontested-read.png"

set size 1,1
set bmargin 3

set label "Nexus 6\nUncontested Read" at screen 0.1,0.9 center font "helvetica,12"

set xlabel "min" offset -13,0 font "helvetica,8"
set ylabel "ms" offset 0,-9 font "helvetica,8"
set tics font "helvetica,8"

set key lmargin center font "helvetica,8" width -7

set macros
threads = "$3==1"
minutes = "$1/60000"
testSync = "$2==1"
test2Check = "$2==2"
testSpin = "$2==3"
readMs = "$4/100"

f3(x) = m3*x + b3
fit f3(x) 'data.csv' using (@minutes):(((@testSync)&&(@threads)) ? @readMs : 1/0) via m3,b3
f5(x) = m5*x + b5
fit f5(x) 'data.csv' using (@minutes):(((@test2Check)&&(@threads)) ? @readMs : 1/0) via m5,b5
f7(x) = m7*x + b7
fit f7(x) 'data.csv' using (@minutes):(((@testSpin)&&(@threads)) ? @readMs : 1/0) via m7,b7

plot \
     'data.csv' using (@minutes):(((@testSync)&&(@threads)) ? @readMs : 1/0) title "sync, read" with points pt 1 lc rgb "#0000AA", \
     f3(x) title "fit" lc rgb "#0000AA", \
     'data.csv' using (@minutes):(((@test2Check)&&(@threads)) ? @readMs : 1/0) title "2-check, read" with points pt 1 lc rgb "#009900", \
     f5(x) title "fit" lc rgb "#009900", \
     'data.csv' using (@minutes):(((@testSpin)&&(@threads)) ? @readMs : 1/0) title "spin, read" with points pt 1 lc rgb "#CC0000", \
     f7(x) title "fit" lc rgb "#CC0000"

