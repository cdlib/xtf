# Required by all XTF Unix-style command line drivers to determine the proper
# XTF_HOME, and form a Java classpath for locating XTF code and libraries.
#
use File::Spec;

# If XTF_HOME isn't set, make a guess based on the directory this script is in.
$home = $ENV{XTF_HOME};
if ($home eq "") {
  my ($vol, $dir, $file) = File::Spec->splitpath(File::Spec->rel2abs($0));
  my @dirs = File::Spec->splitdir($dir);
  while (pop(@dirs) ne "bin") { }
  $home = File::Spec->catpath($vol, File::Spec->catdir(@dirs), '');
}

# Figure out what character Java is using for classpath separation
$joiner = `java`;
$joiner =~ /A (.) separated list of directories/;
if ($1 eq "") { $joiner =~ /files separated by (.)/; }
$joiner = $1;
if ($joiner eq "") { $joiner = ":"; }

# Make a list of all the JAR files in the lib directory
$lib  = "$home/WEB-INF/lib";
opendir DIR, $lib;
@jars = grep { /\.jar/ } readdir(DIR);
closedir DIR;

# Form the class path
$classpath = "$home/WEB-INF/classes";
foreach $jar (@jars) { 
  $classpath = "$classpath$joiner$lib/$jar"; 
}

# Also make a handy list of the arguments.
$args = join(' ', @ARGV);

# All done.
1;

# Copyright (c) 2004-2007, Regents of the University of California
# All rights reserved.
# 
# Redistribution and use in source and binary forms, with or without 
# modification, are permitted provided that the following conditions are met:
#
# - Redistributions of source code must retain the above copyright notice, 
#   this list of conditions and the following disclaimer.
# - Redistributions in binary form must reproduce the above copyright notice, 
#   this list of conditions and the following disclaimer in the documentation 
#   and/or other materials provided with the distribution.
# - Neither the name of the University of California nor the names of its
#   contributors may be used to endorse or promote products derived from this 
#   software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
# POSSIBILITY OF SUCH DAMAGE.

