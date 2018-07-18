#!/opt/local/bin/perl -w
use strict;

use IPC::Open2;


my $DB_FILE = 'timings.db';
my $DATA_FILE = 'data.csv';

my $GNUPLOT_CMD = 'gnuplot';

my $DB_CMD = "sqlite3 $DB_FILE";
my $DB_SQL = <<EOSQL;
.mode csv
.output $DATA_FILE

SELECT ts - (SELECT min(ts) FROM Results) AS t, type, threads, tRead, tWrite
  FROM Results
  ORDER BY t, type, threads ASC;

.quit
EOSQL

{
    prep_data();
    plot_graphs();
}

sub prep_data {
    my($in, $out);
    my $pid = open2($out, $in, $DB_CMD);
    print $in $DB_SQL;
    close $in;
    close $out;
}

sub plot_graphs {
    my @plots;

    opendir(DIR, ".") or die $!;
    while (my $file = readdir(DIR)) {
        next unless (-f "$file");
        next unless ($file =~ m/\.plot$/);
        push(@plots, $file)
    }
    closedir(DIR);

    foreach my $file (@plots) {
        system("$GNUPLOT_CMD $file");
    }
}

