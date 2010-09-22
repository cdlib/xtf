#!/usr/bin/perl

open (INFILE, "urlList.txt");
open (OUTFILE,">>webRegress.log");

$time = time();
$date = localtime($time);

print OUTFILE "========================================================================\n$date\n========================================================================\n\n";

`rm -fr actual/*`;
chdir("actual");

while (<INFILE>) {
    $url = $_;
    if ($url =~ /^http:/) {
        chomp($url);
        print "URL: $url\n";
        print OUTFILE "URL: $url\n";
        `wget -q -r -l2 --user-agent="" '$url'`;
    }
}

chdir("..");

$out = `diff -r actual/* gold/*`;
print "$out\n";
print OUTFILE "$out\n";

$return = `echo $?`;

print "RETURN CODE: $return\n\n";
print OUTFILE "RETURN CODE: $return\n\n";
        
close(INFILE);
close(OUTFILE);