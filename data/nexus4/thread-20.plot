set datafile separator comma

set terminal png
set output "nexus4-thread-20.png"

set size 1,1
set bmargin 3

set label "Nexus 4\n20 Threads" at screen 0.1,0.9 center font "helvetica,12"

set xlabel "min" offset -13,0 font "helvetica,8"
set ylabel "ms" offset 0,-9 font "helvetica,8"
set tics font "helvetica,8"

set key lmargin center font "helvetica,8" width -7

set macros
threads = "$3==20"
minutes = "$1/60000"
testNull = "$2==0"
testSync = "$2==1"
test2Check = "$2==2"
testSpin = "$2==3"
readMs = "$4/100"
writeMs = "$5/100"

f1(x) = m1*x + b1
fit f1(x) 'data.csv' using (@minutes):(((@testNull)&&(@threads)) ? @readMs : 1/0) via m1,b1
f2(x) = m2*x + b2
fit f2(x) 'data.csv' using (@minutes):(((@testNull)&&(@threads)) ? @writeMs : 1/0) via m2,b2
f3(x) = m3*x + b3
fit f3(x) 'data.csv' using (@minutes):(((@testSync)&&(@threads)) ? @readMs : 1/0) via m3,b3
f4(x) = m4*x + b4
fit f4(x) 'data.csv' using (@minutes):(((@testSync)&&(@threads)) ? @writeMs : 1/0) via m4,b4
f5(x) = m5*x + b5
fit f5(x) 'data.csv' using (@minutes):(((@test2Check)&&(@threads)) ? @readMs : 1/0) via m5,b5
f6(x) = m6*x + b6
fit f6(x) 'data.csv' using (@minutes):(((@test2Check)&&(@threads)) ? @writeMs : 1/0) via m6,b6
f7(x) = m7*x + b7
fit f7(x) 'data.csv' using (@minutes):(((@testSpin)&&(@threads)) ? @readMs : 1/0) via m7,b7
f8(x) = m8*x + b8
fit f8(x) 'data.csv' using (@minutes):(((@testSpin)&&(@threads)) ? @writeMs : 1/0) via m8,b8

plot \
     'data.csv' using (@minutes):(((@testNull)&&(@threads)) ? @readMs : 1/0) title "null, read" with points pt 1 lc rgb "#990099", \
     f1(x) title "fit" lc rgb "#990099", \
     'data.csv' using (@minutes):(((@testSync)&&(@threads)) ? @readMs : 1/0) title "sync, read" with points pt 1 lc rgb "#0000AA", \
     f3(x) title "fit" lc rgb "#0000AA", \
     'data.csv' using (@minutes):(((@test2Check)&&(@threads)) ? @readMs : 1/0) title "2-check, read" with points pt 1 lc rgb "#009900", \
     f5(x) title "fit" lc rgb "#009900", \
     'data.csv' using (@minutes):(((@testSpin)&&(@threads)) ? @readMs : 1/0) title "spin, read" with points pt 1 lc rgb "#CC0000", \
     f7(x) title "fit" lc rgb "#CC0000", \
     'data.csv' using (@minutes):(((@testNull)&&(@threads)) ? @writeMs : 1/0) title "null, write" with points pt 2 lc rgb "#BB00BB", \
     f2(x) title "fit" lc rgb "#BB00BB", \
     'data.csv' using (@minutes):(((@testSync)&&(@threads)) ? @writeMs : 1/0) title "sync, write" with points pt 2 lc rgb "#0000FF", \
     f4(x) title "fit" lc rgb "#0000FF", \
     'data.csv' using (@minutes):(((@test2Check)&&(@threads)) ? @writeMs : 1/0) title "2-check, write" with points pt 2 lc rgb "#00DD00", \
     f6(x) title "fit" lc rgb "#00DD00", \
     'data.csv' using (@minutes):(((@testSpin)&&(@threads)) ? @writeMs : 1/0) title "spin, write" with points pt 2 lc rgb "#FF0000", \
     f8(x) title "fit" lc rgb "#FF0000" 

