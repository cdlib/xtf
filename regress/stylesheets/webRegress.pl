#!/usr/bin/perl

open (INFILE, "urlList.txt");
open (OUTFILE,">>webRegress.log");

$time = time();
$date = localtime($time);
# change to server being tested
$server = "espresso.ad.ucop.edu:8084";

print OUTFILE "========================================================================\n$date\n========================================================================\n\n";

`rm -fr actual/*`;
chdir("actual");

while (<INFILE>) {
    $queryString = $_;
    if ($queryString =~ /\w+/) {
        $url = "http://" . $server . $queryString;
        chomp($url);
        print "URL: $url\n";
        print OUTFILE "URL: $url\n";
        `wget -q -r -l2 -nH --user-agent="" -e robots=off '$url'`;
    }
}

chdir("..");

$out = `diff -r actual gold`;
print "$out\n";
print OUTFILE "$out\n";

$return = `echo $?`;

print "RETURN CODE: $return\n\n";
print OUTFILE "RETURN CODE: $return\n\n";
        
close(INFILE);
close(OUTFILE);