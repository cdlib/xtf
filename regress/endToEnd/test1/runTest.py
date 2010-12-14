#!/usr/bin/env python -u

# Get ready for Python 3.x
from __future__ import print_function, absolute_import, division, unicode_literals

import re, os, shutil, subprocess, sys
from optparse import OptionParser, make_option
from os import path
from os.path import join as pjoin


# Parse the command line
usage = "usage: %s [-v]" % path.basename(sys.argv[0])
parser = OptionParser()
parser.add_option("-v", "--verbose", action = 'store_true', dest = "verbose")
(options, args) = parser.parse_args()
if len(args) != 0:
  print(usage)
  sys.exit(1)

def remove(path):
  if os.path.isdir(path):
    print("Removing " + path)
    shutil.rmtree(path)
  elif os.path.exists(path):
    print("Removing " + path)
    os.remove(path)

def do(cmd):
  if len(cmd) > 100:
    print("$ " + cmd[0:37] + "..." + cmd[-60:])
  else:
    print("$ " + cmd)
  # Echo lines as they come out, and also record them
  p = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, stderr=sys.stderr)
  results = []
  while True:
    try:
      line = p.stdout.readline()
      results.append(unicode(line, 'utf-8', 'ignore'))
      if options.verbose:
        sys.stdout.write(line)
    except Exception as e:
      print("exception")
      pass
    if not line:
      break
  returncode = p.wait()
  if returncode != 0:
    raise Exception("Subprocess returned code %d" % returncode)
  return "".join(results)

def cleanup():
  remove(pjoin(scriptDir, "index"))
  remove(pjoin(scriptDir, "index-pending"))
  remove(pjoin(scriptDir, "index-spare"))
  remove(pjoin(scriptDir, "index-new"))
  remove(pjoin(scriptDir, "data", "doc1.xml"))

# Figure out where we are so we can find the XTF libraries
scriptDir = path.dirname(sys.argv[0])
if not path.isabs(scriptDir):
  scriptDir = path.join(os.getenv("PWD", os.getcwd()), scriptDir)
scriptDir = re.sub("/?\.?$", "", scriptDir)
xtfHome = scriptDir
while not xtfHome.endswith("regress"):
  xtfHome = path.dirname(xtfHome)
xtfHome = path.dirname(xtfHome)

# Construct the Java classpath
jars = [pjoin(xtfHome, "WEB-INF", "classes")]
libDir = pjoin(xtfHome, "WEB-INF", "lib")
for fileName in os.listdir(libDir):
  if fileName.endswith(".jar"):
    jars.append(pjoin(libDir, fileName))
classPath = ":".join(jars)

# Blow away the current index
cleanup()

# Start with version 1 of the document
remove(pjoin(scriptDir, "data", "doc1.xml"))
shutil.copyfile(pjoin(scriptDir, "data", "doc1.xml.ver1"), pjoin(scriptDir, "data", "doc1.xml"))

# Boilerplate for calling XTF Java code
javaCmd = "java -cp '%s' -Xms50m -Xmx100m -Dxtf.home='%s' -enableassertions " % (classPath, scriptDir)

# Make a new index with just doc1
do(javaCmd + "org.cdlib.xtf.textIndexer.TextIndexer -trace debug -index default")

# No rotation yet
assert os.path.exists(pjoin(scriptDir, "index-pending"))
assert not os.path.exists(pjoin(scriptDir, "index"))

# Check that we can query it for the document, first in crossQuery
result = do(javaCmd + "org.cdlib.xtf.test.FakeServletContainer 'http://foo/search?keyword=africa'")
assert 'Options for the New South <span class="hit">Africa</span>' in result

# Now should have rotated
assert not os.path.exists(pjoin(scriptDir, "index-pending"))
assert os.path.exists(pjoin(scriptDir, "index"))

# And then in dynaXML
result = do(javaCmd + "org.cdlib.xtf.test.FakeServletContainer 'http://foo/view?docId=doc1.xml;query=africa'")
assert re.search('The Global Relevance of South <xtf:hit[^>]*><xtf:term>Africa</xtf:term>', result)

# Let's make a change to the document (go to version 2)
os.remove(pjoin(scriptDir, "data", "doc1.xml"))
shutil.copyfile(pjoin(scriptDir, "data", "doc1.xml.ver2"), pjoin(scriptDir, "data", "doc1.xml"))

# Do an incremental index
do(javaCmd + "org.cdlib.xtf.textIndexer.TextIndexer -trace debug -index default")

# Verify pre-rotation status
assert os.path.exists(pjoin(scriptDir, "index-pending"))
assert os.path.exists(pjoin(scriptDir, "index"))
assert not os.path.exists(pjoin(scriptDir, "index-spare"))

# Do some searching
result = do(javaCmd + "org.cdlib.xtf.test.FakeServletContainer 'http://foo/search?keyword=america'")
assert 'Options for the New South <span class="hit">America</span>' in result

# Now should have rotated
assert not os.path.exists(pjoin(scriptDir, "index-pending"))
assert os.path.exists(pjoin(scriptDir, "index"))
assert os.path.exists(pjoin(scriptDir, "index-spare"))

# And then in dynaXML
result = do(javaCmd + "org.cdlib.xtf.test.FakeServletContainer 'http://foo/view?docId=doc1.xml;query=america'")
assert re.search('The Global Relevance of South <xtf:hit[^>]*><xtf:term>America</xtf:term>', result)

# Let's make a change to the document (go to version 3)
os.remove(pjoin(scriptDir, "data", "doc1.xml"))
shutil.copyfile(pjoin(scriptDir, "data", "doc1.xml.ver3"), pjoin(scriptDir, "data", "doc1.xml"))

# Do an incremental index
result = do(javaCmd + "org.cdlib.xtf.textIndexer.TextIndexer -trace debug -index default")

# Make sure only a partial rsync occurred
assert re.search("--relative [^ ]+/regress/endToEnd/test1/index-new/./lazy/default " +
                 "[^ ]+/regress/endToEnd/test1/index-new/./spellDict", result)
assert re.search("^lazy/default/doc1.xml.lazy$", result, re.MULTILINE)
assert re.search("^spellDict/edmap.dat$", result, re.MULTILINE)

# Verify pre-rotation status
assert os.path.exists(pjoin(scriptDir, "index-pending"))
assert os.path.exists(pjoin(scriptDir, "index"))
assert not os.path.exists(pjoin(scriptDir, "index-spare"))

# Do some searching
result = do(javaCmd + "org.cdlib.xtf.test.FakeServletContainer 'http://foo/search?keyword=antarctica'")
assert 'Options for the New South <span class="hit">Antarctica</span>' in result

# Now should have rotated
assert not os.path.exists(pjoin(scriptDir, "index-pending"))
assert os.path.exists(pjoin(scriptDir, "index"))
assert os.path.exists(pjoin(scriptDir, "index-spare"))

# And then in dynaXML
result = do(javaCmd + "org.cdlib.xtf.test.FakeServletContainer 'http://foo/view?docId=doc1.xml;query=antarctica'")
assert re.search('The Global Relevance of South <xtf:hit[^>]*><xtf:term>Antarctica</xtf:term>', result)

# Clean everything up
cleanup()
